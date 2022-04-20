/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Context;
import android.util.Log;

@SuppressWarnings("WeakerAccess")
public class AbstractBackendHelper {
    public static final String TAG = "BackendHelper";
    protected final Context context;
    protected State state = State.DISABLED;
    protected boolean currentDataUsed = true;
    protected long lastUpdate = 0;

    public AbstractBackendHelper(Context context) {
        if (context == null)
            throw new IllegalArgumentException("context must not be null");
        this.context = context;
    }

    /**
     * Call this in {@link org.microg.nlp.api.LocationBackendService#onOpen()}.
     */
    public synchronized void onOpen() {
        if (state == State.WAITING || state == State.SCANNING) {
            Log.w(TAG, "Do not call onOpen if not closed before");
        }
        currentDataUsed = true;
        state = State.WAITING;
    }

    /**
     * Call this in {@link org.microg.nlp.api.LocationBackendService#onClose()}.
     */
    public synchronized void onClose() {
        if (state == State.DISABLED || state == State.DISABLING) {
            Log.w(TAG, "Do not call onClose if not opened before");
            return;
        }
        if (state == State.WAITING) {
            state = State.DISABLED;
        } else {
            state = State.DISABLING;
        }
    }

    /**
     * Call this in {@link org.microg.nlp.api.LocationBackendService#update()}.
     */
    public synchronized void onUpdate() {
    }

    public String[] getRequiredPermissions() {
        return new String[0];
    }

    protected enum State {DISABLED, WAITING, SCANNING, DISABLING}
}
