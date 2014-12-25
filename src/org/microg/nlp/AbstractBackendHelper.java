package org.microg.nlp;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.RemoteException;
import android.util.Log;

public abstract class AbstractBackendHelper implements ServiceConnection {
    protected final Context context;
    protected final Intent serviceIntent;
    protected boolean bound;
    private final String TAG;

    public AbstractBackendHelper(String tag, Context context, Intent serviceIntent) {
        TAG = tag;
        this.context = context;
        this.serviceIntent = serviceIntent;
    }

    public abstract void close() throws RemoteException;

    public abstract boolean hasBackend();

    public void unbind() {
        if (bound) {
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

    public boolean bind() {
        if (!bound) {
            try {
                context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                Log.w(TAG, e);
                return false;
            }
        }
        return true;
    }

}
