/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.location;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Locale;

/**
 * This class contains extra parameters to pass to an IGeocodeProvider
 * implementation from the Geocoder class.  Currently this contains the
 * language, country and variant information from the Geocoder's locale
 * as well as the Geocoder client's package name for geocoder server
 * logging.  This information is kept in a separate class to allow for
 * future expansion of the IGeocodeProvider interface.
 *
 * @hide
 */
public class GeocoderParams implements Parcelable {
    /**
     * This object is only constructed by the Geocoder class
     *
     * @hide
     */
    public GeocoderParams(Context context, Locale locale) {
    }

    /**
     * returns the Geocoder's locale
     */
    public Locale getLocale() {
        return null;
    }

    /**
     * returns the package name of the Geocoder's client
     */
    public String getClientPackage() {
        return null;
    }

    public static final Parcelable.Creator<GeocoderParams> CREATOR =
            new Parcelable.Creator<GeocoderParams>() {
                public GeocoderParams createFromParcel(Parcel in) {
                    return null;
                }

                public GeocoderParams[] newArray(int size) {
                    return null;
                }
            };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int flags) {
    }
}
