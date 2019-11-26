/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class LocationBackendService extends AbstractBackendService {

    private LocationCallback callback;
    private Location waiting;

    /**
     * Called, whenever an app requires a location update. This can be a single or a repeated request.
     * <p/>
     * You may return null if your backend has no newer location available then the last one.
     * Do not send the same {@link android.location.Location} twice, if it's not based on updated/refreshed data.
     * <p/>
     * You can completely ignore this method (means returning null) if you use {@link #report(android.location.Location)}.
     *
     * @return a new {@link android.location.Location} instance or null if not available.
     */
    @SuppressWarnings("SameReturnValue")
    protected Location update() {
        return null;
    }

    protected Location update(Bundle options) {
        return update();
    }

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
            } catch (android.os.DeadObjectException e) {
                waiting = location;
                callback = null;
            } catch (RemoteException e) {
                waiting = location;
            }
        } else {
            waiting = location;
        }
    }

    /**
     * @return true if we're an actively connected backend, false if not
     */
    public boolean isConnected() {
        return callback != null;
    }

    @Override
    protected IBinder getBackend() {
        return new Backend();
    }

    @Override
    public void disconnect() {
        if (callback != null) {
            onClose();
            callback = null;
        }
    }

    private class Backend extends LocationBackend.Stub {
        @Override
        public void open(LocationCallback callback) throws RemoteException {
            LocationBackendService.this.callback = callback;
            if (waiting != null) {
                callback.report(waiting);
                waiting = null;
            }
            onOpen();
        }

        @Override
        public Location update() {
            return LocationBackendService.this.update();
        }

        @Override
        public Location updateWithOptions(Bundle options) {
            return LocationBackendService.this.update(options);
        }

        @Override
        public void close() {
            disconnect();
        }

        @Override
        public Intent getInitIntent() {
            return LocationBackendService.this.getInitIntent();
        }

        @Override
        public Intent getSettingsIntent() {
            return LocationBackendService.this.getSettingsIntent();
        }

        @Override
        public Intent getAboutIntent() {
            return LocationBackendService.this.getAboutIntent();
        }
    }
}
