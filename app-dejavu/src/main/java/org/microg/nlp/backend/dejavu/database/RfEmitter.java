package org.microg.nlp.backend.dejavu.database;

/*
 *    DejaVu - A location provider backend for microG/UnifiedNlp
 *
 *    Copyright (C) 2017 Tod Fitch
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Created by tfitch on 8/27/17.
 */

import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import org.microg.nlp.backend.dejavu.BackendService;

import java.util.Locale;

/**
 * Models everything we know about an RF emitter: Its identification, most recently received
 * signal level, an estimate of its coverage (center point and radius), how much we trust
 * the emitter (can we use information about it to compute a position), etc.
 *
 * Starting with v2 of the database, we store a north-south radius and an east-west radius which
 * allows for a rectangular bounding box rather than a square one.
 *
 * When an RF emitter is first observed we create a new object and, if information exists in
 * the database, populate it from saved information.
 *
 * Periodically we sync our current information about the emitter back to the flash memory
 * based storage.
 *
 * Trust is incremented everytime we see the emitter and the new observation has data compatible
 * with our current model. We decrease (or set to zero) our trust if it we think we should have
 * seen the emitter at our current location or if it looks like the emitter may have moved.
 */
public class RfEmitter {
    private final static String TAG = "DejaVu RfEmitter";

    private static final long SECONDS = 1000;               // In milliseconds
    private static final long MINUTES = 60 * SECONDS;
    private static final long HOURS = 60 * MINUTES;
    private static final long DAYS = HOURS * 24;

    private static final long METERS = 1;
    private static final long KM = METERS * 1000;

    private static final long MINIMUM_TRUST = 0;
    private static final long REQUIRED_TRUST = 48;
    private static final long MAXIMUM_TRUST = 100;

    // Tag/names for additional information on location records
    public static final String LOC_RF_ID = "rfid";
    public static final String LOC_RF_TYPE = "rftype";
    public static final String LOC_ASU = "asu";
    public static final String LOC_MIN_COUNT = "minCount";

    public enum EmitterType {WLAN_24GHZ, WLAN_5GHZ, MOBILE, INVALID}

    public enum EmitterStatus {
        STATUS_UNKNOWN,             // Newly discovered emitter, no data for it at all
        STATUS_NEW,                 // Not in database but we've got location data for it
        STATUS_CHANGED,             // In database but something has changed
        STATUS_CACHED,              // In database no changes pending
        STATUS_BLACKLISTED          // Has been blacklisted
    }

    public static class RfCharacteristics {
        public final float reqdGpsAccuracy;       // GPS accuracy needed in meters
        public final float minimumRange;          // Minimum believable coverage radius in meters
        public final float typicalRange;          // Typical range expected
        public final float moveDetectDistance;    // Maximum believable coverage radius in meters
        public final long discoveryTrust;         // Assumed trustiness of a rust an emitter seen for the first time.
        public final long incrTrust;              // Amount to increase trust
        public final long decrTrust;              // Amount to decrease trust
        public final long minCount;               // Minimum number of emitters before we can estimate location

        RfCharacteristics( float gps,
                           float min,
                           float typical,
                           float moveDist,
                           long newTrust,
                           long incr,
                           long decr,
                           long minC) {
            reqdGpsAccuracy = gps;
            minimumRange = min;
            typicalRange = typical;
            moveDetectDistance = moveDist;
            discoveryTrust = newTrust;
            incrTrust = incr;
            decrTrust = decr;
            minCount = minC;
        }
    }

    private RfCharacteristics ourCharacteristics;

    private EmitterType type;
    private String id;
    private long trust;
    private BoundingBox coverage;
    private String note;

    private Observation mLastObservation;

    private int ageSinceLastUse;        // Count of periods since last used (for caching purposes)

    private EmitterStatus status;

    public RfEmitter(RfIdentification ident) {
        initSelf(ident.getRfType(), ident.getRfId());
    }

    public RfEmitter(Observation o) {
        initSelf(o.getIdent().getRfType(), o.getIdent().getRfId());
        mLastObservation = o;
    }

    public RfEmitter(EmitterType mType, String ident) {
        initSelf(mType, ident);
    }

    /**
     * Shared/uniform initialization, called from the various constructors we allow.
     *
     * @param mType The type of the RF emitter (WLAN_24GHZ, MOBILE, etc.)
     * @param ident The identification of the emitter. Must be unique within type
     */
    private void initSelf(EmitterType mType, String ident) {
        type = mType;
        id = ident;
        coverage = null;
        mLastObservation = null;
        ourCharacteristics = getRfCharacteristics(mType);
        trust = ourCharacteristics.discoveryTrust;
        note = "";
        resetAge();
        status = EmitterStatus.STATUS_UNKNOWN;
    }

    /**
     * On equality check, we only check that our type and ID match as that
     * uniquely identifies our RF emitter.
     *
     * @param o The object to check for equality
     * @return True if the objects should be considered the same.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RfIdentification)) return false;

        RfIdentification e = (RfIdentification) o;
        return getRfIdent().equals(e);
    }

    /**
     * Hash code is used to determine unique objects. Our "uniqueness" is
     * based on which "real life" RF emitter we model, not our current
     * coverage, etc. So our hash code should be the same as the hash
     * code of our identification.
     *
     * @return A hash code for this object.
     */
    @Override
    public int hashCode() {
        return getRfIdent().hashCode();
    }

    public String getUniqueId() {
        return getRfIdent().getUniqueId();
    }

    public EmitterType getType() {
        return type;
    }

    public String getTypeString() {
        return type.toString();
    }

    public static EmitterType typeOf( String typeStr ) {
        if (typeStr.equals(EmitterType.MOBILE.toString()))
            return EmitterType.MOBILE;
        if (typeStr.equals(EmitterType.WLAN_24GHZ.toString()))
            return EmitterType.WLAN_24GHZ;
        if (typeStr.equals(EmitterType.WLAN_5GHZ.toString()))
            return EmitterType.WLAN_5GHZ;
        return EmitterType.INVALID;
    }

    public String getId() {
        return id;
    }

    public RfIdentification getRfIdent() {
        return new RfIdentification(id, type);
    }

    public long getTrust() {
        return trust;
    }

    public double getLat() {
        if (coverage != null)
            return coverage.getCenter_lat();
        return 0.0;
    }

    public double getLon() {
        if (coverage != null)
            return coverage.getCenter_lon();
        return 0.0;
    }

    public double getRadius() {
        if (coverage != null)
            return coverage.getRadius();
        return 0.0;
    }

    public double getRadiusNS() {
        if (coverage != null)
            return coverage.getRadius_ns();
        return 0.0;
    }

    public double getRadiusEW() {
        if (coverage != null)
            return coverage.getRadius_ew();
        return 0.0;
    }

    public void setLastObservation(Observation obs) {
        mLastObservation = obs;
        note = obs.getNote();
    }

    public void setNote(String n) {
        if (!note.equals(n)) {
            note = n;
            if (blacklistEmitter())
                changeStatus(EmitterStatus.STATUS_BLACKLISTED, "initSelf()");
        }
    }

    public String getNote() {
        return note;
    }

    /**
     * All RfEmitter objects are managed through a cache. The cache needs ages out
     * emitters that have not been seen (or used) in a while. To do that it needs
     * to maintain age information for each RfEmitter object. Having the RfEmitter
     * object itself store the cache age is a bit of a hack, but we do it anyway.
     *
     * @return The current cache age.
     */
    public int getAge() {
        return ageSinceLastUse;
    }

    /**
     * Resets the cache age to zero.
     */
    public void resetAge() {
        ageSinceLastUse = 0;
    }

    /**
     * Increment the cache age for this object.
     */
    public void incrementAge() {
        ageSinceLastUse++;
    }

    /**
     * Periodically the cache sync's all dirty objects to the flash database.
     * This routine is called by the cache to determine if it needs to be sync'd.
     *
     * @return True if this RfEmitter needs to be written to flash.
     */
    public boolean syncNeeded() {
        return (status == EmitterStatus.STATUS_NEW) ||
                (status == EmitterStatus.STATUS_CHANGED) ||
                ((status == EmitterStatus.STATUS_BLACKLISTED) &&
                        (coverage != null));
    }

    /**
     * Synchronize this object to the flash based database. This method is called
     * by the cache when it is an appropriate time to assure the flash based
     * database is up to date with our current coverage, trust, etc.
     *
     * @param db The database we should write our data to.
     */
    public void sync(Database db) {
        EmitterStatus newStatus = status;

        Log.i(TAG, "sync('" + logString() + "'), status: " + status);
        switch (status) {
            case STATUS_UNKNOWN:
                // Not in database, we have no location. Nothing to sync.
                break;

            case STATUS_BLACKLISTED:
                // If our coverage value is not null it implies that we exist in the
                // database. If so we ought to remove the entry.
                if (coverage != null) {
                    db.drop(this);
                    coverage = null;
                    Log.i(TAG, "sync('" + logString() + "') - Blacklisted dropping from database.");
                }
                break;

            case STATUS_NEW:
                // Not in database, we have location. Add to database
                db.insert(this);
                newStatus = EmitterStatus.STATUS_CACHED;
                Log.i(TAG, "sync('" + logString() + "') - Status new.");
                break;

            case STATUS_CHANGED:
                // In database but we have changes
                if (trust < MINIMUM_TRUST) {
                    Log.i(TAG, "sync('" + logString() + "') - Trust below minimum, dropping from database.");
                    db.drop(this);
                } else
                    db.update(this);
                newStatus = EmitterStatus.STATUS_CACHED;
                break;

            case STATUS_CACHED:
                // In database but we don't have any changes
                break;
        }
        changeStatus(newStatus, "sync('"+logString()+"')");

    }

    public String logString() {
        return "RF Emitter: Type=" + type + ", ID='" + id + "', Note='" + note + "'";
    }

    /**
     * Given an emitter type, return the various characteristics we need to know
     * to model it.
     *
     * @param t An emitter type (WLAN_24GHZ, MOBILE, etc.)
     * @return The characteristics needed to model the emitter
     */
    public static RfCharacteristics getRfCharacteristics(EmitterType t) {
        switch (t) {
            case WLAN_24GHZ:
                // For 2.4 GHz, indoor range seems to be described as about 46 meters
                // with outdoor range about 90 meters. Set the minimum range to be about
                // 3/4 of the indoor range and the typical range somewhere between
                // the indoor and outdoor ranges.
                // However we've seem really, really long range detection in rural areas
                // so base the move distance on that.
                return new RfCharacteristics(
                        20 * METERS,        // reqdGpsAccuracy
                        35 * METERS,        // minimumRange
                        65 * METERS,       // typicalRange
                        300 * METERS,       // moveDetectDistance - Seen pretty long detection in very rural areas
                        0,                  // discoveryTrust
                        REQUIRED_TRUST/3,   // incrTrust
                        1,                  // decrTrust
                        2                   // minCount
                );

            case WLAN_5GHZ:
                // For 2.4 GHz, indoor range seems to be described as about 46 meters
                // with outdoor range about 90 meters. Set the minimum range to be about
                // 3/4 of the indoor range and the typical range somewhere between
                // the indoor and outdoor ranges.
                // However we've seem really, really long range detection in rural areas
                // so base the move distance on that.
                return new RfCharacteristics(
                        20 * METERS,        // reqdGpsAccuracy
                        35 * METERS,        // minimumRange
                        65 * METERS,       // typicalRange
                        300 * METERS,       // moveDetectDistance - Seen pretty long detection in very rural areas
                        0,                  // discoveryTrust
                        REQUIRED_TRUST/3,   // incrTrust
                        1,                  // decrTrust
                        2                   // minCount
                );

            case MOBILE:
                return new RfCharacteristics(
                        100 * METERS,       // reqdGpsAccuracy
                        500 * METERS,       // minimumRange
                        2 * KM,             // typicalRange
                        100 * KM,           // moveDetectDistance - In the desert towers cover large areas
                        MAXIMUM_TRUST,      // discoveryTrust
                        MAXIMUM_TRUST,      // incrTrust
                        0,                  // decrTrust
                        1                   // minCount
                );
        }

        // Unknown emitter type, just throw out some values that make it unlikely that
        // we will ever use it (require too accurate a GPS location, never increment trust, etc.).
        return new RfCharacteristics(
                2 * METERS,         // reqdGpsAccuracy
                50 * METERS,        // minimumRange
                50 * METERS,        // typicalRange
                100 * METERS,       // moveDetectDistance
                0,                  // discoveryTrust
                0,                  // incrTrust
                1,                  // decrTrust
                99                  // minCount
        );
    }

    /**
     * Unfortunately some types of RF emitters are very mobile and a mobile emitter
     * should not be used to estimate our position. Part of the way to deal with this
     * issue is to maintain a trust metric. Trust has a maximum value, so when we
     * are asked to increment trust we need to check that we have not passed the limit.
     */
    public void incrementTrust() {
        //Log.i(TAG, "incrementTrust('"+id+"') - entry.");
        if (canUpdate()) {
            long newTrust = trust + ourCharacteristics.incrTrust;
            if (newTrust > MAXIMUM_TRUST)
                newTrust = MAXIMUM_TRUST;
            if (newTrust != trust) {
                // Log.i(TAG, "incrementTrust('" + logString() + "') - trust change: " + trust + "->" + newTrust);
                trust = newTrust;
                changeStatus(EmitterStatus.STATUS_CHANGED, "incrementTrust('"+logString()+"')");
            }
        }
    }

    /**
     * Decrease our trust of this emitter. This can happen because we expected to see it at our
     * current location and didn't.
     */
    public void decrementTrust() {
        if (canUpdate()) {
            long oldTrust = trust;
            trust -= ourCharacteristics.decrTrust;
            if (oldTrust != trust) {
                // Log.i(TAG, "decrementTrust('" + logString() + "') - trust change: " + oldTrust + "->" + trust);
                changeStatus(EmitterStatus.STATUS_CHANGED, "decrementTrust('" + logString() + "')");
            }
        }
    }

    /**
     * When a scan first detects an emitter a RfEmitter object is created. But at that time
     * no lookup of the saved information is needed or made. When appropriate, the database
     * is checked for saved information about the emitter and this method is called to add
     * that saved information to our model.
     *
     * @param emitterInfo Saved information about this emitter from the database.
     */
    public void updateInfo(Database.EmitterInfo emitterInfo) {
        if (emitterInfo != null) {
            if (coverage == null)
                coverage = new BoundingBox(emitterInfo);
            //Log.i(TAG,"updateInfo() - Setting info for '"+id+"'");
            trust = emitterInfo.trust;
            note = emitterInfo.note;
            changeStatus(EmitterStatus.STATUS_CACHED, "updateInfo('"+logString()+"')");
        }
    }

    /**
     * Update our estimate of the coverage and location of the emitter based on a
     * position report from the GPS system.
     *
     * @param gpsLoc A position report from a trusted (non RF emitter) source
     */
    public void updateLocation(Location gpsLoc) {

        if (status == EmitterStatus.STATUS_BLACKLISTED)
            return;

        if ((gpsLoc == null) || (gpsLoc.getAccuracy() > ourCharacteristics.reqdGpsAccuracy)) {
            Log.i(TAG, "updateLocation("+logString()+") No GPS location or location inaccurate:" + ((gpsLoc != null) ? gpsLoc.getAccuracy() : "null") + ", " + ourCharacteristics.reqdGpsAccuracy);
            return;
        }

        if (coverage == null) {
            Log.i(TAG, "updateLocation("+logString()+") emitter is new.");
            coverage = new BoundingBox(gpsLoc.getLatitude(), gpsLoc.getLongitude(), 0.0f);
            changeStatus(EmitterStatus.STATUS_NEW, "updateLocation('"+logString()+"') New");
            return;
        }

        // Add the GPS sample to the known bounding box of the emitter.

        if (coverage.update(gpsLoc.getLatitude(), gpsLoc.getLongitude())) {
            // Bounding box has increased, see if it is now unbelievably large
            Log.i(TAG, "updateLocation("+id+") coverage: " + coverage.getRadius() + ", " + ourCharacteristics.moveDetectDistance);
            if (coverage.getRadius() >= ourCharacteristics.moveDetectDistance) {
                Log.i(TAG, "updateLocation("+id+") emitter has moved (" + gpsLoc.distanceTo(_getLocation()) + ")");
                coverage = new BoundingBox(gpsLoc.getLatitude(), gpsLoc.getLongitude(), 0.0f);
                trust = ourCharacteristics.discoveryTrust;
                changeStatus(EmitterStatus.STATUS_CHANGED, "updateLocation('"+logString()+"') Moved");
            } else {
                changeStatus(EmitterStatus.STATUS_CHANGED, "updateLocation('" + logString() + "') BBOX update");
            }
        }
    }

    /**
     * User facing location value. Differs from internal one in that we don't report
     * locations that are guarded due to being new or moved.
     *
     * @return The coverage estimate for our RF emitter or null if we don't trust our
     * information.
     */
    public  Location getLocation() {
        // If we have no observation of the emitter we ought not give a
        // position estimate based on it.
        if (mLastObservation == null)
            return null;

        // If we don't trust the location, we ought not give a position
        // estimate based on it.
        if ((trust < REQUIRED_TRUST) || (status == EmitterStatus.STATUS_BLACKLISTED))
            return null;

        // If we don't have a coverage estimate we will get back a null location
        Location location = _getLocation();
        if (location == null)
            return null;

        // If we are unbelievably close to null island, don't report location
        if (!BackendService.notNullIsland(location))
            return null;

        // Time tags based on time of most recent observation
        location.setTime(mLastObservation.getLastUpdateTimeMs());
        location.setElapsedRealtimeNanos(mLastObservation.getElapsedRealtimeNanos());

        Bundle extras = new Bundle();
        extras.putString(LOC_RF_TYPE, type.toString());
        extras.putString(LOC_RF_ID, id);
        extras.putInt(LOC_ASU,mLastObservation.getAsu());
        extras.putLong(LOC_MIN_COUNT, ourCharacteristics.minCount);
        location.setExtras(extras);
        return location;
    }

    /**
     * If we have any coverage information, returns an estimate of that coverage.
     * For convenience, we use the standard Location record as it contains a center
     * point and radius (accuracy).
     *
     * @return Coverage estimate for emitter or null it does not exist.
     */
    private Location _getLocation() {
        if (coverage == null)
            return null;

        final Location location = new Location(BackendService.LOCATION_PROVIDER);

        location.setLatitude(coverage.getCenter_lat());
        location.setLongitude(coverage.getCenter_lon());

        // Hard limit the minimum accuracy based on the type of emitter
        location.setAccuracy((float)Math.max(this.getRadius(),ourCharacteristics.minimumRange));

        return location;
    }

    /**
     * As part of our effort to not use mobile emitters in estimating or location
     * we blacklist ones that match observed patterns.
     *
     * @return True if the emitter is blacklisted (should not be used in position computations).
     */
    private boolean blacklistEmitter() {
        switch (this.type) {
            case WLAN_24GHZ:
            case WLAN_5GHZ:
                return blacklistWifi();

            case MOBILE:
                return false;       // Not expecting mobile towers to move around.

        }
        return false;
    }

    /**
     * Checks the note field (where the SSID is saved) to see if it appears to be
     * an AP that is likely to be moving. Typical checks are to see if substrings
     * in the SSID match that of cell phone manufacturers or match known patterns
     * for public transport (busses, trains, etc.) or in car WLAN defaults.
     *
     * @return True if emitter should be blacklisted.
     */
    private boolean blacklistWifi() {
        final String lc = note.toLowerCase(Locale.US);

        // Seen a large number of WiFi networks where the SSID is the last
        // three octets of the MAC address. Often in rural areas where the
        // only obvious source would be other automobiles. So suspect that
        // this is the default setup for a number of vehicle manufactures.
        final String macSuffix = id.substring(id.length()-8).toLowerCase(Locale.US).replace(":", "");
        boolean rslt =
                // Mobile phone brands
                lc.contains("android") ||                   // mobile tethering
                lc.startsWith("HUAWEI-") ||
                lc.contains("ipad") ||                      // mobile tethering
                lc.contains("iphone") ||                    // mobile tethering
                lc.contains("motorola") ||                  // mobile tethering
                lc.endsWith(" phone") ||                    // "Lans Phone" seen
                lc.startsWith("moto ") ||                   // "Moto E (4) 9509" seen
                note.startsWith("MOTO") ||                  // "MOTO9564" and "MOTO9916" seen
                note.startsWith("Samsung Galaxy") ||        // mobile tethering
                lc.startsWith("lg aristo") ||               // "LG Aristo 7124" seen

                // Mobile network brands
                lc.contains("mobile hotspot") ||            // e.g "MetroPCS Portable Mobile Hotspot"
                note.startsWith("CellSpot") ||              // T-Mobile US portable cell based WiFi
                note.startsWith("Verizon-") ||              // Verizon mobile hotspot

                // Per some instructional videos on YouTube, recent (2015 and later)
                // General Motors built vehicles come with a default WiFi SSID of the
                // form "WiFi Hotspot 1234" where the 1234 is different for each car.
                // The SSID can be changed but the recommended SSID to change to
                // is of the form "first_name vehicle_model" (e.g. "Bryces Silverado").
                lc.startsWith("wifi hotspot ") ||           // Default GM vehicle WiFi name
                lc.endsWith("corvette") ||                 // Chevy Corvette. "TS Corvette" seen.
                lc.endsWith("silverado") ||                // GMC Silverado. "Bryces Silverado" seen.
                lc.endsWith("chevy") ||                    // Chevrolet. "Davids Chevy" seen
                lc.endsWith("truck") ||                    // "Morgans Truck" and "Wally Truck" seen
                lc.endsWith("suburban") ||                 // Chevy/GMC Suburban. "Laura Suburban" seen
                lc.endsWith("terrain") ||                  // GMC Terrain. "Nelson Terrain" seen
                lc.endsWith("sierra") ||                   // GMC pickup. "dees sierra" seen

                // Per an instructional video on YouTube, recent (2014 and later) Chrysler-Fiat
                // vehicles have a SSID of the form "Chrysler uconnect xxxxxx" where xxxxxx
                // seems to be a hex digit string (suffix of BSSID?).
                lc.contains(" uconnect ") ||                // Chrysler built vehicles

                // Per instructional video on YouTube, Mercedes cars have and SSID of
                // "MB WLAN nnnnn" where nnnnn is a 5 digit number.
                lc.startsWith("mb wlan ") ||                // Mercedes

                // Other automobile manufactures default naming

                lc.equals(macSuffix) ||                     // Apparent default SSID name for many cars
                note.startsWith("Audi") ||                  // some cars seem to have this AP on-board
                note.startsWith("Chevy ") ||                // "Chevy Cruz 7774" seen.
                note.startsWith("GMC WiFi") ||              // General Motors
                note.startsWith("MyVolvo") ||               // Volvo in car WiFi

                // Transit agencies
                lc.startsWith("oebb ") ||                   // WLAN network on Austrian Oebb trains
                lc.startsWith("westbahn ") ||               // WLAN network on Austrian Westbahn trains
                lc.contains("admin@ms ") ||                 // WLAN network on Hurtigruten ships
                lc.contains("contiki-wifi") ||              // WLAN network on board of bus
                lc.contains("db ic bus") ||                 // WLAN network on board of German bus
                lc.contains("deinbus.de") ||                // WLAN network on board of German bus
                lc.contains("ecolines") ||                  // WLAN network on board of German bus
                lc.contains("eurolines_wifi") ||            // WLAN network on board of German bus
                lc.contains("fernbus") ||                   // WLAN network on board of German bus
                lc.contains("flixbus") ||                   // WLAN network on board of German bus
                lc.contains("guest@ms ") ||                 // WLAN network on Hurtigruten ships
                lc.contains("muenchenlinie") ||             // WLAN network on board of bus
                lc.contains("postbus") ||                   // WLAN network on board of bus line
                lc.contains("telekom_ice") ||               // WLAN network on DB trains
                lc.contains("skanetrafiken") ||             // WLAN network on Skånetrafiken (Sweden) buses and trains
                lc.contains("oresundstag") ||               // WLAN network on Øresundståg (Sweden/Denmark) trains
                lc.contentEquals("amtrak") ||               // WLAN network on USA Amtrak trains
                lc.contentEquals("amtrakconnect") ||        // WLAN network on USA Amtrak trains
                lc.contentEquals("CDWiFi") ||               // WLAN network on Czech railways
                lc.contentEquals("megabus") ||              // WLAN network on MegaBus US bus
                lc.contentEquals("Regiojet - zluty") ||     // WLAN network on Czech airline
                lc.contentEquals("RegioJet - zluty") ||     // WLAN network on Czech airline
                lc.contentEquals("WESTlan") ||              // WLAN network on Austrian railways
                lc.contentEquals("Wifi in de trein") ||     // WLAN network on Dutch railway
                note.startsWith("BusWiFi") ||               // Some transit buses in LA Calif metro area
                note.startsWith("CoachAmerica") ||          // Charter bus service with on board WiFi
                note.startsWith("DisneyLandResortExpress") || // Bus with on board WiFi
                note.startsWith("TaxiLinQ") ||              // Taxi cab wifi system.
                note.startsWith("TransitWirelessWiFi") ||   // New York City public transport wifi

                // Dash cams
                note.startsWith("YICarCam") ||              // Dashcam WiFi.

                // Other
                lc.contains("mobile") ||                    // What I'd put into a mobile hotspot name
                lc.contains("nsb_interakti") ||             // ???
                lc.contains("NVRAM WARNING")                // NVRAM WARNING Error pseudo-network

                // lc.endsWith("_nomap")                    // Google unsubscibe option
        ;
        //if (rslt)
        //    Log.i(TAG, "blacklistWifi('" + logString() + "') blacklisted.");
        return rslt;
    }

    /**
     * Only some types of emitters can be updated when a GPS position is received. A
     * simple check but done in a couple places so extracted out to this routine so that
     * we are consistent in how we check things.
     *
     * @return True if coverage and/or trust can be updated.
     */
    private boolean canUpdate() {
        boolean rslt = true;
        switch (status) {
            case STATUS_BLACKLISTED:
            case STATUS_UNKNOWN:
                rslt = false;
                break;
        }
        return rslt;
    }

    /**
     * Our status can only make a small set of allowed transitions. Basically a simple
     * state machine. To assure our transistions are all legal, this routine is used for
     * all changes.
     *
     * @param newStatus The desired new status (state)
     * @param info Logging information for debug purposes
     */
    private void changeStatus( EmitterStatus newStatus, String info) {
        if (newStatus == status)
            return;

        EmitterStatus finalStatus = status;
        switch (finalStatus) {
            case STATUS_BLACKLISTED:
                // Once blacklisted cannot change.
                break;

            case STATUS_CACHED:
            case STATUS_CHANGED:
                switch (newStatus) {
                    case STATUS_BLACKLISTED:
                    case STATUS_CACHED:
                    case STATUS_CHANGED:
                        finalStatus = newStatus;
                        break;
                }
                break;

            case STATUS_NEW:
                switch (newStatus) {
                    case STATUS_BLACKLISTED:
                    case STATUS_CACHED:
                        finalStatus = newStatus;
                        break;
                }
                break;

            case STATUS_UNKNOWN:
                switch (newStatus) {
                    case STATUS_BLACKLISTED:
                    case STATUS_CACHED:
                    case STATUS_NEW:
                        finalStatus = newStatus;
                }
                break;
        }

        //Log.i(TAG,"changeStatus("+newStatus+", "+ info + ") " + status + " -> " + finalStatus);
        status = finalStatus;
    }
}
