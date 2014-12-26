package org.microg.nlp.location;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class ThreadHelper implements Runnable {
    private static final String TAG = ThreadHelper.class.getName();
    private final Context context;
    private final BackendFuser backendFuser;
    private ScheduledThreadPoolExecutor executor;
    private long time = 60000; // Initialize with 60s
    private AtomicBoolean enabled = new AtomicBoolean(false);

    public ThreadHelper(Context context, LocationProvider locationProvider) {
        this.context = context;
        backendFuser = new BackendFuser(context, locationProvider);
    }

    public void forceLocation(Location location) {
        backendFuser.forceLocation(location);
    }

    public void reload() {
        disable();
        backendFuser.reset();
        enable();
    }

    public void disable() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        backendFuser.unbind();
        enabled.set(false);
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void reset() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(this, 0, time, TimeUnit.MILLISECONDS);
    }

    public void enable() {
        if (enabled.compareAndSet(false, true)) {
            backendFuser.bind();
        }
        reset();
    }

    @Override
    public void run() {
        backendFuser.update();
    }

    public void destroy() {
        if (executor != null) {
            executor.shutdownNow();
        }
        backendFuser.destroy();
    }
}
