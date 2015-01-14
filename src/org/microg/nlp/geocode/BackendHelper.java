package org.microg.nlp.geocode;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import org.microg.nlp.AbstractBackendHelper;
import org.microg.nlp.api.GeocoderBackend;

import java.util.List;

class BackendHelper extends AbstractBackendHelper {
    private static final String TAG = "NlpGeocodeBackendHelper";
    private GeocoderBackend backend;

    public BackendHelper(Context context, Intent serviceIntent) {
        super(TAG, context, serviceIntent);
    }

    public List<Address> getFromLocation(double latitude, double longitude, int maxResults,
            String locale) {
        try {
            return backend.getFromLocation(latitude, longitude, maxResults, locale);
        } catch (Exception e) {
            Log.w(TAG, e);
            unbind();
            return null;
        }
    }

    public List<Address> getFromLocationName(String locationName, int maxResults,
            double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude,
            double upperRightLongitude, String locale) {
        try {
            return backend.getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                    lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale);
        } catch (Exception e) {
            Log.w(TAG, e);
            unbind();
            return null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        backend = GeocoderBackend.Stub.asInterface(service);
        if (backend != null) {
            try {
                backend.open();
            } catch (Exception e) {
                Log.w(TAG, e);
                unbind();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);
        backend = null;
    }

    @Override
    public void close() throws RemoteException {
        backend.close();
    }

    @Override
    public boolean hasBackend() {
        return backend != null;
    }
}
