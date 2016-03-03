/*
 * Copyright 2013-2016 microG Project Team
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

package org.microg.tools.selfcheck;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;

import org.microg.nlp.Preferences;
import org.microg.nlp.R;
import org.microg.nlp.location.AbstractLocationService;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.Context.LOCATION_SERVICE;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_PROVIDER;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public class NlpStatusChecks implements SelfCheckGroup {
    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_nlp_status);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        providerWasBound(context, collector);
        isLocationProviderSetUp(context, collector);
        if (isNetworkLocationEnabled(context, collector)) {
            isProvidingLastLocation(context, collector);
            isProvidingLocation(context, collector);
        }
    }

    private boolean providerWasBound(Context context, ResultCollector collector) {
        collector.addResult(context.getString(R.string.self_check_name_nlp_bound),
                AbstractLocationService.WAS_BOUND ? Positive : Negative, context.getString(R.string.self_check_resolution_nlp_bound));
        return AbstractLocationService.WAS_BOUND;
    }

    private boolean isLocationProviderSetUp(Context context, ResultCollector collector) {
        boolean setupLocationProvider = !TextUtils.isEmpty(new Preferences(context).getLocationBackends());
        collector.addResult(context.getString(R.string.self_check_name_nlp_setup),
                setupLocationProvider ? Positive : Negative, context.getString(R.string.self_check_resolution_nlp_setup));
        return setupLocationProvider;
    }

    private boolean isNetworkLocationEnabled(Context context, ResultCollector collector) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        boolean networkEnabled = locationManager.getProviders(true).contains(NETWORK_PROVIDER);
        collector.addResult(context.getString(R.string.self_check_name_network_enabled),
                networkEnabled ? Positive : Negative, context.getString(R.string.self_check_resolution_network_enabled));
        return networkEnabled;
    }

    private boolean isProvidingLastLocation(Context context, ResultCollector collector) {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        boolean hasKnown = location != null && location.getExtras() != null &&
                location.getExtras().containsKey(LOCATION_EXTRA_BACKEND_PROVIDER);
        collector.addResult(context.getString(R.string.self_check_name_last_location),
                hasKnown ? Positive : Unknown, context.getString(R.string.self_check_resolution_last_location));
        return hasKnown;
    }

    private void isProvidingLocation(final Context context, final ResultCollector collector) {
        final AtomicBoolean result = new AtomicBoolean(false);
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (result) {
                    try {
                        result.wait(10000);
                    } catch (InterruptedException e) {
                    }
                    collector.addResult(context.getString(R.string.self_check_name_nlp_is_providing),
                            result.get() ? Positive : Unknown, context.getString(R.string.self_check_resolution_nlp_is_providing));
                }
            }
        }).start();
        locationManager.requestSingleUpdate(NETWORK_PROVIDER, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (result) {
                    result.set(location != null && location.getExtras().containsKey(LOCATION_EXTRA_BACKEND_PROVIDER));
                    result.notifyAll();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        }, null);
    }
}
