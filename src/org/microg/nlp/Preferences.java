package org.microg.nlp;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

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
        String defBackends = Settings.System
                .getString(context.getContentResolver(), DEFAULT_LOCATION_BACKENDS);
        return defBackends == null ? "" : defBackends;
    }

    public String getLocationBackends() {
        String s = getSharedPreferences().getString(
                context.getString(R.string.pref_location_backends), getDefaultLocationBackends());
        return s;
    }

    public String getDefaultGeocoderBackends() {
        String defBackends = Settings.System
                .getString(context.getContentResolver(), DEFAULT_GEOCODER_BACKENDS);
        return defBackends == null ? "" : defBackends;
    }

    public String getGeocoderBackends() {
        String defBackends = Settings.System
                .getString(context.getContentResolver(), DEFAULT_GEOCODER_BACKENDS);
        return getSharedPreferences().getString(
                context.getString(R.string.pref_geocoder_backends), getDefaultGeocoderBackends());
    }

    public static String[] splitBackendString(String backendString) {
        return backendString.split("\\|");
    }
}
