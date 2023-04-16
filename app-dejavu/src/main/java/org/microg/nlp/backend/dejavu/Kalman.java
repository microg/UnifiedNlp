package org.microg.nlp.backend.dejavu;

/*
 *    DejaVu - A location provider backend for microG/UnifiedNlp
 *
 */

/**
 * Created by tfitch on 8/31/17.
 */

/*
 * This package inspired by https://github.com/villoren/KalmanLocationManager.git
 */


/**
 * Copyright (c) 2014 Renato Villone
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Changes and modifications to the original file:
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

import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * A two dimensional Kalman filter for estimating actual position from multiple
 * measurements. We cheat and use two one dimensional Kalman filters which works
 * because our two dimensions are orthogonal.
 */
class Kalman {
    private static final double ALTITUDE_NOISE = 10.0;

    private static final float MOVING_THRESHOLD = 0.7f;     // meters/sec (2.5 kph ~= 0.7 m/s)
    private static final float MIN_ACCURACY = 3.0f;         // Meters

    /**
     * Three 1-dimension trackers, since the dimensions are independent and can avoid using matrices.
     */
    private final Kalman1Dim mLatTracker;
    private final Kalman1Dim mLonTracker;
    private Kalman1Dim mAltTracker;

    /**
     *  Most recently computed mBearing. Only updated if we are moving.
     */
    private float mBearing = 0.0f;

    /**
     *  Time of last update. Used to determine how stale our position is.
     */
    private long mTimeOfUpdate;

    /**
     * Number of samples filter has used.
     */
    private long samples;

    /**
     *
     * @param location
     */

    public Kalman(Location location, double coordinateNoise) {
        final double accuracy = location.getAccuracy();
        final double coordinateNoiseDegrees = coordinateNoise * BackendService.METER_TO_DEG;
        double position, noise;
        long timeMs = location.getTime();

        // Latitude
        position = location.getLatitude();
        noise = accuracy * BackendService.METER_TO_DEG;
        mLatTracker = new Kalman1Dim(coordinateNoiseDegrees, timeMs);
        mLatTracker.setState(position, 0.0, noise);

        // Longitude
        position = location.getLongitude();
        noise = accuracy * Math.cos(Math.toRadians(location.getLatitude())) * BackendService.METER_TO_DEG;
        mLonTracker = new Kalman1Dim(coordinateNoiseDegrees, timeMs);
        mLonTracker.setState(position, 0.0, noise);

        // Altitude
        if (location.hasAltitude()) {
            position = location.getAltitude();
            noise = accuracy;
            mAltTracker = new Kalman1Dim(ALTITUDE_NOISE, timeMs);
            mAltTracker.setState(position, 0.0, noise);
        }
        mTimeOfUpdate = timeMs;
        samples = 1;
    }

    public synchronized void update(Location location) {
        if (location == null)
            return;

        // Reusable
        final double accuracy = location.getAccuracy();
        double position, noise;
        long timeMs = location.getTime();

        predict(timeMs);
        mTimeOfUpdate = timeMs;
        samples++;

        // Latitude
        position = location.getLatitude();
        noise = accuracy * BackendService.METER_TO_DEG;
        mLatTracker.update(position, noise);

        // Longitude
        position = location.getLongitude();
        noise = accuracy * Math.cos(Math.toRadians(location.getLatitude())) * BackendService.METER_TO_DEG ;
        mLonTracker.update(position, noise);

        // Altitude
        if (location.hasAltitude()) {
            position = location.getAltitude();
            noise = accuracy;
            if (mAltTracker == null) {
                mAltTracker = new Kalman1Dim(ALTITUDE_NOISE, timeMs);
                mAltTracker.setState(position, 0.0, noise);
            } else {
                mAltTracker.update(position, noise);
            }
        }
    }

    private synchronized void predict(long timeMs) {
        mLatTracker.predict(0.0, timeMs);
        mLonTracker.predict(0.0, timeMs);
        if (mAltTracker != null)
            mAltTracker.predict(0.0, timeMs);
    }

    // Allow others to override our sample count. They may want to have us report only the
    // most recent samples.
    public void setSamples(long s) {
        samples = s;
    }

    public long getSamples() {
        return samples;
    }

    public synchronized Location getLocation() {
        Long timeMs = System.currentTimeMillis();
        final Location location = new Location(BackendService.LOCATION_PROVIDER);

        predict(timeMs);
        location.setTime(timeMs);
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        location.setLatitude(mLatTracker.getPosition());
        location.setLongitude(mLonTracker.getPosition());
        if (mAltTracker != null)
            location.setAltitude(mAltTracker.getPosition());

        float accuracy = (float) (mLatTracker.getAccuracy() * BackendService.DEG_TO_METER);
        if (accuracy < MIN_ACCURACY)
            accuracy = MIN_ACCURACY;
        location.setAccuracy(accuracy);

        // Derive speed from degrees/ms in lat and lon
        double latVeolocity = mLatTracker.getVelocity() * BackendService.DEG_TO_METER;
        double lonVeolocity = mLonTracker.getVelocity() * BackendService.DEG_TO_METER *
                Math.cos(Math.toRadians(location.getLatitude()));
        float speed = (float) Math.sqrt((latVeolocity*latVeolocity)+(lonVeolocity*lonVeolocity));
        location.setSpeed(speed);

        // Compute bearing only if we are moving. Report old bearing
        // if we are below our threshold for moving.
        if (speed > MOVING_THRESHOLD) {
            mBearing = (float) Math.toDegrees(Math.atan2(latVeolocity, lonVeolocity));
        }
        location.setBearing(mBearing);

        Bundle extras = new Bundle();
        extras.putLong("AVERAGED_OF", samples);
        location.setExtras(extras);

        return location;
    }
}
