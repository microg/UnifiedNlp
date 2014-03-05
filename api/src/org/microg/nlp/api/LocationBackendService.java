package org.microg.nlp.api;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.os.RemoteException;

public abstract class LocationBackendService extends Service {

	private Backend backend = new Backend();
	private LocationCallback callback;
	private Location waiting;

	/**
	 * This method is called, whenever an app requires a location update. This can be a single or a repeated request.
	 * <p/>
	 * You may return null if your backend has no newer location available then the last one.
	 * Do not send the same {@link android.location.Location} twice, if it's not based on updated/refreshed data.
	 * <p/>
	 * You can completely ignore this method (means returning null) if you use {@link #report(android.location.Location)}.
	 *
	 * @return a new {@link android.location.Location} instance or null if not available.
	 */
	protected abstract Location update();

	/**
	 * Directly report a {@link android.location.Location} to the requesting apps. Use this if your updates are based
	 * on environment changes (eg. cell id change).
	 *
	 * @param location the new {@link android.location.Location} instance to be send
	 */
	public void report(Location location) {
		if (callback != null) {
			try {
				callback.report(location);
			} catch (RemoteException e) {
				waiting = location;
			}
		} else {
			waiting = location;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return backend;
	}


	private class Backend extends LocationBackend.Stub {
		@Override
		public void open(LocationCallback callback) throws RemoteException {
			LocationBackendService.this.callback = callback;
			if (waiting != null) {
				callback.report(waiting);
				waiting = null;
			}
		}

		@Override
		public Location update() throws RemoteException {
			return LocationBackendService.this.update();
		}

		@Override
		public void close() throws RemoteException {
			callback = null;
		}
	}
}
