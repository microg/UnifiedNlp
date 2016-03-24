/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.nlp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;

public class Preferences {
    private static final String DEFAULT_LOCATION_BACKENDS = "default_location_backends";
    private static final String DEFAULT_GEOCODER_BACKENDS = "default_geocoder_backends";
    private final Context context;

    public Preferences(Context context) {
        this.context = context;
    }

    private SharedPreferences getSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public String getDefaultLocationBackends() {
        String defBackends = Settings.Secure.getString(context.getContentResolver(), DEFAULT_LOCATION_BACKENDS);
        return defBackends == null ? "" : defBackends;
    }

    public String getLocationBackends() {
        return getSharedPreferences().getString(context.getString(R.string.pref_location_backends),
                getDefaultLocationBackends());
    }

    public String getDefaultGeocoderBackends() {
        String defBackends = Settings.Secure.getString(context.getContentResolver(), DEFAULT_GEOCODER_BACKENDS);
        return defBackends == null ? "" : defBackends;
    }

    public String getGeocoderBackends() {
        return getSharedPreferences().getString(context.getString(R.string.pref_geocoder_backends),
                getDefaultGeocoderBackends());
    }

    public static String[] splitBackendString(String backendString) {
        return backendString.split("\\|");
    }
}
