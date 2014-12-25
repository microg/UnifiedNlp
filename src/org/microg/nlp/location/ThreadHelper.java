package org.microg.nlp.location;

import android.content.Context;
import android.location.Location;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class ThreadHelper implements Runnable {
    private final Context context;
    private final LocationProvider locationProvider;
    private BackendFuser backendFuser;
    private ScheduledThreadPoolExecutor executor;
    private long time = 5000; // Initialize with 5s
    private boolean enabled;

    public ThreadHelper(Context context, LocationProvider locationProvider) {
        this.context = context;
        this.locationProvider = locationProvider;
        updateBackendHandler();
    }

    private void updateBackendHandler() {
        BackendFuser old = backendFuser;
        backendFuser = new BackendFuser(context, locationProvider);
        if (old != null) {
            backendFuser.forceLocation(old.getForcedLocation());
        }
    }

    public void forceLocation(Location location) {
        backendFuser.forceLocation(location);
    }

    public void reload() {
        disable();
        updateBackendHandler();
        enable();
    }

    public void disable() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (enabled) {
            backendFuser.unbind();
            enabled = false;
        }
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
        if (!enabled) {
            backendFuser.bind();
            enabled = true;
        }
        reset();
    }

    @Override
    public void run() {
        backendFuser.update();
    }
}
