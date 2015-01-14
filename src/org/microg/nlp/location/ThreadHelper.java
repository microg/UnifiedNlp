/*
 * Copyright 2014-2015 µg Project Team
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
