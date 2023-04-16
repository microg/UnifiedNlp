/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class AbstractBackendService extends Service {

    public static final String TAG = "BackendService";

    @Override
    public IBinder onBind(Intent intent) {
        return getBackend();
    }

    /**
     * Called after a connection was setup
     */
    public void onOpen() {

    }

    /**
     * Called before connection closure
     */
    protected void onClose() {

    }

    public Intent getInitIntent() {
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    public Intent getSettingsIntent() {
        return null;
    }

    @SuppressWarnings("SameReturnValue")
    public Intent getAboutIntent() {
        return null;
    }

    public String getBackendName() {
        return null;
    }

    public String getDescription() {
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        try {
            disconnect();
        } catch (Exception e) {
            Log.w(TAG, e);
        }
        return super.onUnbind(intent);
    }

    public abstract void disconnect();

    protected abstract IBinder getBackend();

    protected String getServiceApiVersion() {
        return Utils.getServiceApiVersion(this);
    }

    protected String getSelfApiVersion() {
        return Utils.getSelfApiVersion(this);
    }
}
