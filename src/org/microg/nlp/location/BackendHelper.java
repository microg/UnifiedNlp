package org.microg.nlp.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.*;
import android.util.Log;
import com.android.location.provider.LocationProviderBase;
import org.microg.nlp.api.LocationBackend;
import org.microg.nlp.api.LocationCallback;

public class BackendHelper {
	private static final String TAG = BackendHelper.class.getName();
	private final Context context;
	private final LocationProvider provider;
	private final Intent serviceIntent;
	private final Connection connection = new Connection();
	private final Callback callback = new Callback();
	private LocationBackend backend;
	private boolean updateWaiting;
	private Location lastLocation;
	private boolean bound;

	public BackendHelper(Context context, LocationProvider provider, Intent serviceIntent) {
		this.context = context;
		this.provider = provider;
		this.serviceIntent = serviceIntent;
	}

	public boolean bind() {
		if (!bound) {
			try {
				context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
			} catch (Exception e) {
				Log.w(TAG, e);
				return false;
			}
		}
		return true;
	}

	public void unbind() {
		if (bound) {
			try {
				backend.close();
				context.unbindService(connection);
			} catch (Exception e) {
				Log.w(TAG, e);
			}
		}
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
				provider.reportLocation(lastLocation);
			} catch (RemoteException e) {
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
		if (location.getExtras() == null) {
			location.setExtras(new Bundle());
		}
		location.getExtras().putString("SERVICE_BACKEND_PROVIDER", location.getProvider());
		location.getExtras().putString("SERVICE_BACKEND_PACKAGE", serviceIntent.getPackage());
		location.setProvider("network");
		if (!location.hasAccuracy()) {
			location.setAccuracy(50000);
		}
		if (location.getTime() <= 0) {
			location.setTime(System.currentTimeMillis());
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			if (location.getElapsedRealtimeNanos() <= 0) {
				location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
			}
		}
		location.getExtras().putParcelable(LocationProviderBase.EXTRA_NO_GPS_LOCATION, new Location(location));
		lastLocation = location;
		Log.v(TAG, "location=" + lastLocation);
		return lastLocation;
	}

	private class Connection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			bound = true;
			backend = LocationBackend.Stub.asInterface(service);
			try {
				backend.open(callback);
				if (updateWaiting) {
					update();
				}
			} catch (RemoteException e) {
				Log.w(TAG, e);
				unbind();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			backend = null;
			bound = false;
		}
	}

	private class Callback extends LocationCallback.Stub {
		@Override
		public void report(Location location) throws RemoteException {
			provider.reportLocation(setLastLocation(location));
		}
	}
}
