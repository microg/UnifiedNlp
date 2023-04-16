package org.microg.nlp.backend.dejavu;
/*
 *    DejaVu - A location provider backend for microG/UnifiedNlp
 */

/**
 * Created by tfitch on 8/31/17.
 */

/*
 * This package inspired and largely copied from
 * https://github.com/villoren/KalmanLocationManager.git
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
 * Changes and modifications to this code:
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


class Kalman1Dim {
    private final static double TIME_SECOND = 1000.0;   // One second in milliseconds

    /**
     * Minimal time step.
     *
     * Assume 200 KPH (55.6 m/s) and a maximum accuracy of 3 meters, then there is no need
     * to update the filter any faster than 166.7 ms.
     *
     */
    private final static long TIME_STEP_MS = 150;

    /**
     * Last prediction time
     */
    private long mPredTime;

    /**
     * Time step. Computed from differences in prediction times.
     */
    private final double mt, mt2, mt2d2, mt3d2, mt4d4;

    /**
     * Process noise covariance. Computed from time step and process noise
     */
    private final double mQa, mQb, mQc, mQd;

    /**
     * Estimated state
     */
    private double mXa, mXb;

    /**
     * Estimated covariance
     */
    private double mPa, mPb, mPc, mPd;


    /**
     * Create a single dimension kalman filter.
     *
     * @param processNoise Standard deviation to calculate noise covariance from.
     * @param timeMillisec The time the filter is started.
     */
    public Kalman1Dim(double processNoise, long timeMillisec) {
        double mProcessNoise = processNoise;

        mPredTime = timeMillisec;

        mt = ((double)TIME_STEP_MS) / TIME_SECOND;
        mt2 = mt * mt;
        mt2d2 = mt2 / 2.0;
        mt3d2 = mt2 * mt / 2.0;
        mt4d4 = mt2 * mt2 / 4.0;

        // Process noise covariance
        double n2 = mProcessNoise * mProcessNoise;
        mQa = n2 * mt4d4;
        mQb = n2 * mt3d2;
        mQc = mQb;
        mQd = n2 * mt2;

        // Estimated covariance
        mPa = mQa;
        mPb = mQb;
        mPc = mQc;
        mPd = mQd;
    }

    /**
     * Reset the filter to the given state.
     * <p>
     * Should be called after creation, unless position and velocity are assumed to be both zero.
     *
     * @param position
     * @param velocity
     * @param noise
     */
    public void setState(double position, double velocity, double noise) {

        // State vector
        mXa = position;
        mXb = velocity;

        // Covariance
        double n2 = noise * noise;
        mPa = n2 * mt4d4;
        mPb = n2 * mt3d2;
        mPc = mPb;
        mPd = n2 * mt2;
    }

    /**
     * Predict state.
     *
     * @param acceleration Should be 0 unless there's some sort of control input (a gas pedal, for instance).
     * @param timeMillisec The time the prediction is for.
     */
    public void predict(double acceleration, long timeMillisec) {

        long delta_t = timeMillisec - mPredTime;
        while (delta_t > TIME_STEP_MS) {
            mPredTime = mPredTime + TIME_STEP_MS;

            // x = F.x + G.u
            mXa = mXa + mXb * mt + acceleration * mt2d2;
            mXb = mXb + acceleration * mt;

            // P = F.P.F' + Q
            double Pdt = mPd * mt;
            double FPFtb = mPb + Pdt;
            double FPFta = mPa + mt * (mPc + FPFtb);
            double FPFtc = mPc + Pdt;
            double FPFtd = mPd;

            mPa = FPFta + mQa;
            mPb = FPFtb + mQb;
            mPc = FPFtc + mQc;
            mPd = FPFtd + mQd;

            delta_t = timeMillisec - mPredTime;
        }
    }

    /**
     * Update (correct) with the given measurement.
     *
     * @param position
     * @param noise
     */
    public void update(double position, double noise) {

        double r = noise * noise;

        //  y   =  z   -   H  . x
        double y = position - mXa;

        // S = H.P.H' + R
        double s = mPa + r;
        double si = 1.0 / s;

        // K = P.H'.S^(-1)
        double Ka = mPa * si;
        double Kb = mPc * si;

        // x = x + K.y
        mXa = mXa + Ka * y;
        mXb = mXb + Kb * y;

        // P = P - K.(H.P)
        double Pa = mPa - Ka * mPa;
        double Pb = mPb - Ka * mPb;
        double Pc = mPc - Kb * mPa;
        double Pd = mPd - Kb * mPb;

        mPa = Pa;
        mPb = Pb;
        mPc = Pc;
        mPd = Pd;

    }

    /**
     * @return Estimated position.
     */
    public double getPosition() {
        return mXa;
    }

    /**
     * @return Estimated velocity.
     */
    public double getVelocity() {
        return mXb;
    }

    /**
     * @return Accuracy
     */
    public double getAccuracy() {
        return Math.sqrt(mPd / mt2);
    }
}
