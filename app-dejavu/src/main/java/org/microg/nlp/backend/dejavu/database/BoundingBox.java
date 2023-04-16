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
 * Created by tfitch on 9/28/17.
 */

import android.location.Location;

import org.microg.nlp.backend.dejavu.BackendService;

public class BoundingBox {
    private double north;
    private double south;
    private double east;
    private double west;
    private double center_lat;
    private double center_lon;
    private double radius;
    private double radius_ns;
    private double radius_ew;

    public BoundingBox() {
        reset();
    }

    public BoundingBox(Location loc) {
        reset();
        update(loc);
    }

    public BoundingBox(double lat, double lon, float radius) {
        reset();
        update(lat, lon, radius);
    }

    public BoundingBox(Database.EmitterInfo info) {
        reset();
        update(info.latitude, info.longitude, info.radius_ns, info.radius_ew);
    }

    /**
     * Expand, if needed, the bounding box to include the coverage area
     * implied by a location.
     * @param loc A record describing the coverage of an RF emitter.
     */
    private boolean update(Location loc) {
        return update(loc.getLatitude(), loc.getLongitude(), loc.getAccuracy());
    }

    /**
     * Expand bounding box to include an emitter at a lat/lon with a
     * specified radius.
     *
     * @param lat The center latitude for the coverage area.
     * @param lon The center longitude for the coverage area.
     * @param radius The radius of the coverage area.
     */
    private boolean update(double lat, double lon, float radius) {
        return update(lat, lon, radius, radius);
    }

    /**
     * Expand bounding box to include an emitter at a lat/lon with a
     * specified radius.
     *
     * @param lat The center latitude for the coverage area.
     * @param lon The center longitude for the coverage area.
     * @param radius_ns The distance from the center to the north (or south) edge.
     * @param radius_ew The distance from the center to the east (or west) edge.
     */
    private boolean update(double lat, double lon, float radius_ns, float radius_ew) {
        double locNorth = lat + (radius_ns * BackendService.METER_TO_DEG);
        double locSouth = lat - (radius_ns * BackendService.METER_TO_DEG);
        double cosLat = Math.cos(Math.toRadians(lat));
        double locEast = lon + (radius_ew * BackendService.METER_TO_DEG) * cosLat;
        double locWest = lon - (radius_ew * BackendService.METER_TO_DEG) * cosLat;

        // Can't just "update(locNorth, locWest) || update(locSouth, locEast)"
        // because we need the second update to be called even if the first
        // returns true.
        boolean rslt = update(locNorth, locWest);
        if (update(locSouth, locEast))
            rslt = true;
        return rslt;
    }

    /**
     * Update the bounding box to include a point at the specified lat/lon
     * @param lat The latitude to be included in the bounding box
     * @param lon The longitude to be included in the bounding box
     */
    public boolean update(double lat, double lon) {
        boolean rslt = false;

        if (lat > north) {
            north = lat;
            rslt = true;
        }
        if (lat < south) {
            south = lat;
            rslt = true;
        }
        if (lon > east) {
            east = lon;
            rslt = true;
        }
        if (lon < west) {
            west = lon;
            rslt = true;
        }

        if (rslt) {
            center_lat = (north + south)/2.0;
            center_lon = (east + west)/2.0;

            radius_ns = (float)((north - center_lat) * BackendService.DEG_TO_METER);
            double cosLat = Math.max(Math.cos(Math.toRadians(center_lat)),BackendService.MIN_COS);
            radius_ew = (float)(((east - center_lon) * BackendService.DEG_TO_METER) / cosLat);

            radius = Math.sqrt(radius_ns*radius_ns + radius_ew*radius_ew);
        }

        return rslt;
    }

    public double getNorth() {
        return north;
    }

    public double getSouth() {
        return south;
    }

    public double getEast() {
        return east;
    }

    public double getWest() {
        return west;
    }

    public double getCenter_lat() { return center_lat; }

    public double getCenter_lon() { return center_lon; }

    public double getRadius() { return radius; }

    public double getRadius_ns() { return radius_ns; }

    public double getRadius_ew() { return radius_ew; }

    @Override
    public String toString() {
        return "(" + north + "," + west + "," + south + "," + east + "," + center_lat + "," + center_lon + "," + radius_ns+ "," + radius_ew+ "," + radius + ")";
    }

    private void reset() {
        north = -91.0;      // Impossibly south
        south = 91.0;       // Impossibly north
        east = -181.0;      // Impossibly west
        west = 181.0;       // Impossibly east
        center_lat = 0.0;   // Center at "null island"
        center_lon = 0.0;
        radius = 0.0;       // No coverage radius
        radius_ns = 0.0;
        radius_ew = 0.0;
    }

}
