package org.microg.nlp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public abstract class AbstractBackendHelper implements ServiceConnection {
    protected final Context context;
    protected final Intent serviceIntent;
    private boolean bound;
    private final String TAG;

    public AbstractBackendHelper(String tag, Context context, Intent serviceIntent) {
        TAG = tag;
        this.context = context;
        this.serviceIntent = serviceIntent;
    }

    public abstract void close() throws RemoteException;

    public abstract boolean hasBackend();

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

    public boolean bind() {
        if (!bound) {
            Log.d(TAG, "Binding to: " + serviceIntent);
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
