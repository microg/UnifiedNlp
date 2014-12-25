package org.microg.nlp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.microg.nlp.location.LocationService;

public class PackageReceiver extends BroadcastReceiver {
    private static final String TAG = PackageReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Intent received: " + intent);
        Log.d(TAG, "Reloading location service...");
        LocationService.reloadLocationService(context);
    }
}
