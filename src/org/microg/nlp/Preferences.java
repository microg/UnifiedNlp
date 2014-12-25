package org.microg.nlp;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

public class Preferences {
    private static final String DEFAULT_LOCATION_BACKENDS = "default_location_backends";
    private static final String LOCATION_BACKENDS = "location_backends";
    private static final String DEFAULT_GEOCODER_BACKENDS = "default_geocoder_backends";
    private static final String GEOCODER_BACKENDS = "geocoder_backends";
    private final Context context;

    public Preferences(Context context) {
        this.context = context;
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences("config", Context.MODE_PRIVATE);
    }

    public String getLocationBackends() {
        String defBackends = Settings.System
                .getString(context.getContentResolver(), DEFAULT_LOCATION_BACKENDS);
        String s = getSharedPreferences()
                .getString(LOCATION_BACKENDS, defBackends == null ? "" : defBackends);
        Log.d("nlpPreferences", "location_backends: "+s);
        return s;
    }

    public void setLocationBackends(String backends) {
        getSharedPreferences().edit().putString(LOCATION_BACKENDS, backends).commit();
    }

    public String getGeocoderBackends() {
        String defBackends = Settings.System
                .getString(context.getContentResolver(), DEFAULT_GEOCODER_BACKENDS);
        return getSharedPreferences()
                .getString(GEOCODER_BACKENDS, defBackends == null ? "" : defBackends);
    }

    public void setGeocoderBackends(String backends) {
        getSharedPreferences().edit().putString(GEOCODER_BACKENDS, backends).commit();
    }
    
    public static String[] splitBackendString(String backendString) {
        return backendString.split("\\|");
    }
}
