package org.microg.nlp;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

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

    public String[] getLocationBackends() {
        String defBackends = Settings.System.getString(context.getContentResolver(), DEFAULT_LOCATION_BACKENDS);
        String backends = getSharedPreferences().getString(LOCATION_BACKENDS, defBackends);
        return backends.split("\\|");
    }
    
    public void setLocationBackends(String backends) {
        getSharedPreferences().edit().putString(LOCATION_BACKENDS, backends).commit();
    }
    
    public String[] getGeocoderBackends() {
        String defBackends = Settings.System.getString(context.getContentResolver(), DEFAULT_GEOCODER_BACKENDS);
        String backends = getSharedPreferences().getString(GEOCODER_BACKENDS, defBackends);
        return backends.split("\\|");
    }
}
