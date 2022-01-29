/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import android.os.Parcel;
import android.os.Parcelable;

public class LatLon implements Parcelable {
    private double latitude;
    private double longitude;

    public LatLon(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public static final Creator<LatLon> CREATOR = new Creator<LatLon>() {
        @Override
        public LatLon createFromParcel(Parcel source) {
            return new LatLon(source.readDouble(), source.readDouble());
        }

        @Override
        public LatLon[] newArray(int size) {
            return new LatLon[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}
