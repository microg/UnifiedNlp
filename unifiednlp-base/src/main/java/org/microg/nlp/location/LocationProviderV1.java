/*
 * Copyright (C) 2013-2017 microG Project Team
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
import android.location.Criteria;
import android.location.Location;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.WorkSource;
import android.util.Log;

import static android.location.LocationProvider.AVAILABLE;

public class LocationProviderV1 extends com.android.location.provider.LocationProvider
        implements LocationProvider {
    private static final String TAG = "NlpLocationProvider";

    private final ThreadHelper helper;
    private long autoTime = Long.MAX_VALUE;
    private boolean autoUpdate = false;

    public LocationProviderV1(Context context) {
        this.helper = new ThreadHelper(context, this);
    }

    @Override
    public void onDisable() {
        Log.d(TAG, "onDisable");
        helper.disable();
    }

    @Override
    public void reload() {
        helper.reload();
    }

    @Override
    public void onEnable() {
        Log.d(TAG, "onEnable");
    }

    @Override
    public boolean onRequiresNetwork() {
        return false;
    }

    @Override
    public boolean onRequiresSatellite() {
        return false;
    }

    @Override
    public boolean onRequiresCell() {
        return false;
    }

    @Override
    public boolean onHasMonetaryCost() {
        return false;
    }

    @Override
    public boolean onSupportsAltitude() {
        return true;
    }

    @Override
    public boolean onSupportsSpeed() {
        return true;
    }

    @Override
    public boolean onSupportsBearing() {
        return true;
    }

    @Override
    public int onGetPowerRequirement() {
        return Criteria.POWER_LOW;
    }

    @Override
    public boolean onMeetsCriteria(Criteria criteria) {
        if (criteria.getAccuracy() == Criteria.ACCURACY_FINE) {
            return false;
        }
        return true;
    }

    @Override
    public int onGetAccuracy() {
        return Criteria.ACCURACY_COARSE;
    }

    @Override
    public int onGetStatus(Bundle extras) {
        return AVAILABLE;
    }

    @Override
    public long onGetStatusUpdateTime() {
        return 0;
    }

    @Override
    public String onGetInternalState() {
        return null;
    }

    @Override
    public void onEnableLocationTracking(boolean enable) {
        Log.v(TAG, "onEnableLocationTracking: " + enable);
        autoUpdate = enable;
        if (autoUpdate && autoTime != Long.MAX_VALUE) helper.enable();
    }

    @Override
    public void onSetMinTime(long minTime, WorkSource ws) {
        Log.v(TAG, "onSetMinTime: " + minTime + " by " + ws);
        autoTime = Math.max(minTime, FASTEST_REFRESH_INTERVAL);
        helper.setTime(autoTime);
        if (autoUpdate) helper.enable();
    }

    @Override
    public void onUpdateNetworkState(int state, NetworkInfo info) {

    }

    @Override
    public void onUpdateLocation(Location location) {

    }

    @Override
    public boolean onSendExtraCommand(String command, Bundle extras) {
        return false;
    }

    @Override
    public void onAddListener(int uid, WorkSource ws) {

    }

    @Override
    public void onRemoveListener(int uid, WorkSource ws) {

    }

    @Override
    public void destroy() {
        helper.destroy();
    }
}
