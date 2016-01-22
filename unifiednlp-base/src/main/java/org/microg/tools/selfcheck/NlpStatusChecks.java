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
import org.microg.nlp.location.AbstractLocationService;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.location.LocationManager.NETWORK_PROVIDER;
import static org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_PROVIDER;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public class NlpStatusChecks implements SelfCheckGroup {
    @Override
    public String getGroupName(Context context) {
        return "UnifiedNlp status";
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        providerWasBound(context, collector);
        isLocationProviderSetUp(context, collector);
        isProvidingLastLocation(context, collector);
        isProvidingLocation(context, collector);
    }

    private boolean providerWasBound(Context context, ResultCollector collector) {
        collector.addResult("UnifiedNlp is registered in system:", AbstractLocationService.WAS_BOUND ? Positive : Negative, "The system did not bind the UnifiedNlp service. If you just installed UnifiedNlp you should try to reboot this device.");
        return AbstractLocationService.WAS_BOUND;
    }

    private boolean isLocationProviderSetUp(Context context, ResultCollector collector) {
        boolean setupLocationProvider = !TextUtils.isEmpty(new Preferences(context).getLocationBackends());
        collector.addResult("Location backend(s) set up:", setupLocationProvider ? Positive : Negative, "Install and configure a UnifiedNlp location backend to use network-based geolocation,");
        return setupLocationProvider;
    }

    private boolean isProvidingLastLocation(Context context, ResultCollector collector) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        boolean hasKnown = location != null && location.getExtras().containsKey(LOCATION_EXTRA_BACKEND_PROVIDER);
        collector.addResult("UnifiedNlp has known location:", hasKnown ? Positive : Unknown, "UnifiedNlp has no last known location. This will cause some apps to fail.");
        return hasKnown;
    }

    private void isProvidingLocation(Context context, final ResultCollector collector) {
        final AtomicBoolean result = new AtomicBoolean(false);
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (result) {
                    try {
                        result.wait(10000);
                    } catch (InterruptedException e) {
                    }
                    collector.addResult("UnifiedNlp provides location updates:", result.get() ? Positive : Unknown, "No UnifiedNlp location was provided by the system within 10 seconds.");
                }
            }
        }).start();
        locationManager.requestSingleUpdate(NETWORK_PROVIDER, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                synchronized (result) {
                    result.set(location.getExtras().containsKey(LOCATION_EXTRA_BACKEND_PROVIDER));
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
