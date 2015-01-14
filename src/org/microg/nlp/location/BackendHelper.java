package org.microg.nlp.location;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.*;
import android.util.Log;
import com.android.location.provider.LocationProviderBase;
import org.microg.nlp.AbstractBackendHelper;
import org.microg.nlp.api.LocationBackend;
import org.microg.nlp.api.LocationCallback;

import static org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_PROVIDER;
import static org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_COMPONENT;

class BackendHelper extends AbstractBackendHelper {
    private static final String TAG = "NlpLocationBackendHelper";
    private final BackendFuser backendFuser;
    private final Callback callback = new Callback();
    private LocationBackend backend;
    private boolean updateWaiting;
    private Location lastLocation;

    public BackendHelper(Context context, BackendFuser backendFuser, Intent serviceIntent) {
        super(TAG, context, serviceIntent);
        this.backendFuser = backendFuser;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public Location update() {
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.");
            updateWaiting = true;
        } else {
            updateWaiting = false;
            try {
                setLastLocation(backend.update());
                backendFuser.reportLocation();
            } catch (Exception e) {
                Log.w(TAG, e);
                unbind();
            }
        }
        return lastLocation;
    }

    private Location setLastLocation(Location location) {
        if (location == null) {
            return lastLocation;
        }
        if (!location.hasAccuracy()) {
            return lastLocation;
        }
        if (location.getExtras() == null) {
            location.setExtras(new Bundle());
        }
        location.getExtras().putString(LOCATION_EXTRA_BACKEND_PROVIDER, location.getProvider());
        location.getExtras().putString(LOCATION_EXTRA_BACKEND_COMPONENT,
                serviceIntent.getComponent().flattenToShortString());
        if (!location.hasAccuracy()) {
            location.setAccuracy(50000);
        }
        if (location.getTime() <= 0) {
            location.setTime(System.currentTimeMillis());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            updateElapsedRealtimeNanos(location);
        }
        location.getExtras()
                .putParcelable(LocationProviderBase.EXTRA_NO_GPS_LOCATION, new Location(location));
        lastLocation = location;
        return lastLocation;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void updateElapsedRealtimeNanos(Location location) {
        if (location.getElapsedRealtimeNanos() <= 0) {
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        }
    }

    @Override
    public void close() throws RemoteException {
        backend.close();
    }

    @Override
    public boolean hasBackend() {
        return backend != null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        backend = LocationBackend.Stub.asInterface(service);
        if (backend != null) {
            try {
                backend.open(callback);
                if (updateWaiting) {
                    update();
                }
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

    private class Callback extends LocationCallback.Stub {
        @Override
        public void report(Location location) throws RemoteException {
            setLastLocation(location);
            backendFuser.reportLocation();
        }
    }
}
