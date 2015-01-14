package org.microg.nlp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.microg.nlp.geocode.AbstractGeocodeService;
import org.microg.nlp.location.AbstractLocationService;

public class PackageReceiver extends BroadcastReceiver {
    private static final String TAG = "NlpPackageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Intent received: " + intent);
        Log.d(TAG, "Reloading location service...");
        AbstractLocationService.reloadLocationService(context);
        Log.d(TAG, "Reloading geocoding service...");
        AbstractGeocodeService.reloadLocationService(context);
    }
}
