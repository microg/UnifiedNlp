/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.nlp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public abstract class AbstractBackendHelper implements ServiceConnection {
    private final Context context;
    protected final Intent serviceIntent;
    private boolean bound;
    private final String TAG;
    private final String signatureDigest;

    protected AbstractBackendHelper(String tag, Context context, Intent serviceIntent) {
        this(tag, context, serviceIntent, null);
    }

    protected AbstractBackendHelper(String tag, Context context, Intent serviceIntent, String signatureDigest) {
        TAG = tag;
        this.context = context;
        this.serviceIntent = serviceIntent;
        this.signatureDigest = signatureDigest;
    }

    protected abstract void close() throws RemoteException;

    protected abstract boolean hasBackend();

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        bound = true;
        Log.d(TAG, "Bound to: " + name);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        bound = false;
        Log.d(TAG, "Unbound from: " + name);
    }

    public void unbind() {
        if (bound) {
            Log.d(TAG, "Unbinding from: " + serviceIntent);
            if (hasBackend()) {
                try {
                    close();
                } catch (Exception e) {
                    Log.w(TAG, e);
                }
            }
            try {
                context.unbindService(this);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
            bound = false;
        }
    }

    public void bind() {
        if (!bound) {
            Log.d(TAG, "Binding to: " + serviceIntent);
            if (signatureDigest != null) {
                if (serviceIntent.getPackage() == null) {
                    Log.w(TAG, "Intent is not properly resolved, can't verify signature. Aborting.");
                    return;
                }
                if (!signatureDigest.equals(Preferences.firstSignatureDigest(context, serviceIntent.getPackage()))) {
                    Log.w(TAG, "Target signature does not match selected package. Aborting.");
                    return;
                }
            }
            try {
                context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                Log.w(TAG, e);
            }
        }
    }

}
