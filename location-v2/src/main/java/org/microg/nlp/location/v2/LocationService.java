package org.microg.nlp.location.v2;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class LocationService extends Service {
    private static final String TAG = "ULocation";
    private LocationProvider provider;

    @Override
    public synchronized IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: "+intent);
        if (provider == null) {
            provider = new LocationProvider(this);
        }
        return provider.getBinder();
    }

    @Override
    public synchronized boolean onUnbind(Intent intent) {
        if (provider != null) {
            provider.unsetRequest();
        }
        return super.onUnbind(intent);
    }
}
