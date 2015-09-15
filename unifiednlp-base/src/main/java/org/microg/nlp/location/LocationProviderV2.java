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

package org.microg.nlp.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.os.WorkSource;
import android.util.Log;

import com.android.location.provider.LocationProviderBase;
import com.android.location.provider.ProviderPropertiesUnbundled;
import com.android.location.provider.ProviderRequestUnbundled;

import static android.location.LocationProvider.AVAILABLE;

class LocationProviderV2 extends LocationProviderBase implements LocationProvider {
    private static final String TAG = "NlpLocationProvider";
    private static final ProviderPropertiesUnbundled props = ProviderPropertiesUnbundled.create(
            false, // requiresNetwork
            false, // requiresSatellite
            false, // requiresCell
            false, // hasMonetaryCost
            true, // supportsAltitude
            true, // supportsSpeed
            true, // supportsBearing
            Criteria.POWER_LOW, // powerRequirement
            Criteria.ACCURACY_COARSE); // accuracy
    private final ThreadHelper helper;

    public LocationProviderV2(Context context) {
        super(TAG, props);
        this.helper = new ThreadHelper(context, this);
    }

    @Override
    public void onDisable() {
        Log.d(TAG, "onDisable");
        helper.disable();
    }

    @Override
    public void forceLocation(Location location) {
        helper.forceLocation(location);
    }

    @Override
    public void reload() {
        helper.reload();
    }

    @Override
    public void destroy() {
        helper.destroy();
    }

    @Override
    public void onEnable() {
        Log.d(TAG, "onEnable");
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
    public void onSetRequest(ProviderRequestUnbundled requests, WorkSource source) {
        Log.v(TAG, "onSetRequest: " + requests + " by " + source);

        long autoTime = requests.getInterval();
        boolean autoUpdate = requests.getReportLocation();

        if (autoTime < 1500) {
            // Limit to 1.5s
            autoTime = 1500;
        }
        Log.v(TAG, "using autoUpdate=" + autoUpdate + " autoTime=" + autoTime);

        if (autoUpdate) {
            helper.setTime(autoTime);
            helper.enable();
        } else {
            helper.disable();
        }
    }

}
