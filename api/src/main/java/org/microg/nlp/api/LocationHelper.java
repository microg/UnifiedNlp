/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.location.Location;
import android.os.Bundle;

import java.util.Collection;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class LocationHelper {
    public static final String EXTRA_AVERAGED_OF = "AVERAGED_OF";
    public static final String EXTRA_TOTAL_WEIGHT = "org.microg.nlp.TOTAL_WEIGHT";
    public static final String EXTRA_TOTAL_ALTITUDE_WEIGHT = "org.microg.nlp.TOTAL_ALTITUDE_WEIGHT";
    public static final String EXTRA_WEIGHT = "org.microg.nlp.WEIGHT";

    private LocationHelper() {
    }

    public static Location create(String source) {
        Location l = new Location(source);
        l.setTime(System.currentTimeMillis());
        return l;
    }

    public static Location create(String source, double latitude, double longitude, float accuracy) {
        Location location = create(source);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        return location;
    }

    public static Location create(String source, double latitude, double longitude, float altitude, Bundle extras) {
        Location location = create(source, latitude, longitude, altitude);
        location.setExtras(extras);
        return location;
    }

    public static Location create(String source, double latitude, double longitude, double altitude, float accuracy) {
        Location location = create(source, latitude, longitude, accuracy);
        location.setAltitude(altitude);
        return location;
    }

    public static Location create(String source, double latitude, double longitude, double altitude, float accuracy, Bundle extras) {
        Location location = create(source, latitude, longitude, altitude, accuracy);
        location.setExtras(extras);
        return location;
    }

    public static Location create(String source, long time) {
        Location location = create(source);
        location.setTime(time);
        return location;
    }

    public static Location create(String source, long time, Bundle extras) {
        Location location = create(source, time);
        location.setExtras(extras);
        return location;
    }

    public static Location average(String source, Collection<Location> locations) {
        return weightedAverage(source, locations, LocationBalance.BALANCED, new Bundle());
    }

    public static Location weightedAverage(String source, Collection<Location> locations, LocationBalance balance, Bundle extras) {
        if (locations == null || locations.isEmpty()) {
            return null;
        }
        double total = 0;
        double lat = 0;
        double lon = 0;
        float acc = 0;
        double altTotal = 0;
        double alt = 0;
        for (Location value : locations) {
            if (value != null) {
                double weight = balance.getWeight(value);
                total += weight;
                lat += value.getLatitude() * weight;
                lon += value.getLongitude() * weight;
                acc += value.getAccuracy() * weight;
                if (value.hasAltitude()) {
                    alt += value.getAltitude();
                    altTotal += weight;
                }
            }
        }
        if (extras == null) extras = new Bundle();
        extras.putInt(EXTRA_AVERAGED_OF, locations.size());
        extras.putDouble(EXTRA_TOTAL_WEIGHT, total);
        if (altTotal > 0) {
            extras.putDouble(EXTRA_TOTAL_ALTITUDE_WEIGHT, altTotal);
            return create(source, lat / total, lon / total, alt / altTotal, (float) (acc / total), extras);
        } else {
            return create(source, lat / total, lon / total, (float) (acc / total), extras);
        }
    }

    public interface LocationBalance {
        LocationBalance BALANCED = location -> 1;
        LocationBalance FROM_EXTRA = location -> location.getExtras() == null ? 1 : location.getExtras().getDouble(EXTRA_WEIGHT, 1);

        double getWeight(Location location);
    }
}
