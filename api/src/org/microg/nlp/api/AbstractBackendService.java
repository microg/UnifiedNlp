package org.microg.nlp.api;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public abstract class AbstractBackendService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return getBackend();
    }

    /**
     * Called after a connection was setup
     */
    protected void onOpen() {

    }

    /**
     * Called before connection closure
     */
    protected void onClose() {

    }

    protected abstract IBinder getBackend();
}
