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

package org.microg.tools.selfcheck;

import android.app.Service;
import android.content.Context;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;

import org.microg.nlp.Preferences;
import org.microg.nlp.R;
import org.microg.nlp.location.AbstractLocationService;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.Context.LOCATION_SERVICE;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import static android.location.LocationManager.NETWORK_PROVIDER;
import android.os.IBinder;
import org.microg.nlp.api.Constants;
import static org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_COMPONENT;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public class NlpStatusChecks extends Service implements SelfCheckGroup {
    
    private static Location mLastLocation;
    private static ResultCollector collector;
    private static Context context;
    private static Address mLastAddress;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int ret = super.onStartCommand(intent, flags, startId);
        if ("android.intent.action.LOCATION_UPDATE".equals(intent.getAction()) && (intent.getExtras() != null)) {
            mLastLocation = (Location) intent.getExtras().getParcelable("location");
            mLastAddress = (Address) intent.getExtras().getParcelable("addresses");
            collector.addResult(getBaseContext().getString(org.microg.nlp.R.string.self_check_name_nlp_is_providing),
                            (mLastLocation != null) ? Positive : Unknown,
                            getBaseContext().getString(org.microg.nlp.R.string.self_check_resolution_nlp_is_providing));
            if (mLastLocation == null) {
                collector.addResult(
                        getBaseContext().getString(R.string.self_check_geocoder_no_location),
                        Negative,
                        getBaseContext().getString(R.string.self_check_geocoder_verify_backend));
                return ret;
            }

            collector.addResult(
                    getBaseContext().getString(R.string.self_check_name_nlp_geocoder_is_providing_addresses),
                    (mLastAddress != null) ? Positive : Negative,
                    getBaseContext().getString(R.string.self_check_resolution_nlp_geocoder_no_address));
        }
        return ret;
    }
    
    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_nlp_status);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        this.collector = collector;
        this.context = context;
        SharedPreferences mSharedPreferences = context.getSharedPreferences(Constants.APP_SETTINGS_NAME,
                Context.MODE_PRIVATE);
        boolean osSupportsUnlp = mSharedPreferences.getBoolean(Constants.OS_SUPPORTS_UNLP, false);
        if (osSupportsUnlp) {
            providerWasBound();
        }
        isLocationProviderSetUp();
        if (isNetworkLocationEnabled()) {
            if (osSupportsUnlp) {
                isProvidingLastLocation();
            }
            isProvidingLocation(!osSupportsUnlp);
            if (osSupportsUnlp) {
                isGeocoderProvideAddress();
            }
        }
    }

    private boolean providerWasBound() {
        collector.addResult(context.getString(R.string.self_check_name_nlp_bound),
                AbstractLocationService.WAS_BOUND ? Positive : Negative, context.getString(R.string.self_check_resolution_nlp_bound));
        return AbstractLocationService.WAS_BOUND;
    }

    private boolean isLocationProviderSetUp() {
        boolean setupLocationProvider = !TextUtils.isEmpty(new Preferences(context).getLocationBackends());
        collector.addResult(context.getString(R.string.self_check_name_nlp_setup),
                setupLocationProvider ? Positive : Negative, context.getString(R.string.self_check_resolution_nlp_setup));
        return setupLocationProvider;
    }

    private boolean isNetworkLocationEnabled() {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        boolean networkEnabled = locationManager.getProviders(true).contains(NETWORK_PROVIDER);
        collector.addResult(context.getString(R.string.self_check_name_network_enabled),
                networkEnabled ? Positive : Negative, context.getString(R.string.self_check_resolution_network_enabled));
        return networkEnabled;
    }

    private boolean isProvidingLastLocation() {
        LocationManager locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            boolean hasKnown = location != null && location.getExtras() != null &&
                    location.getExtras().containsKey(LOCATION_EXTRA_BACKEND_COMPONENT);
            collector.addResult(context.getString(R.string.self_check_name_last_location),
                    hasKnown ? Positive : Unknown, context.getString(R.string.self_check_resolution_last_location));

            if (hasKnown) {
                mLastLocation = location;
            }

            return hasKnown;
        } catch (SecurityException e) {
            collector.addResult(context.getString(R.string.self_check_name_last_location), Unknown, context.getString(R.string.self_check_loc_perm_missing));
            return false;
        }
    }

    private void isProvidingLocation(final boolean checkByIntent) {
        if (checkByIntent) {
            try {
                updateNetworkLocation();
            } catch (SecurityException e) {
                collector.addResult(context.getString(R.string.self_check_name_last_location), Unknown, context.getString(R.string.self_check_loc_perm_missing));
            }
            return;
        }
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
                            result.get() ? Positive : Unknown,
                            context.getString(R.string.self_check_resolution_nlp_is_providing));
                }
            }
        }).start();
        try {
            locationManager.requestSingleUpdate(NETWORK_PROVIDER, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    synchronized (result) {
                        result.set(location != null && location.getExtras() != null &&
                                location.getExtras().containsKey(LOCATION_EXTRA_BACKEND_COMPONENT));
                        result.notifyAll();
                        mLastLocation = location;
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
        } catch (SecurityException e) {
            collector.addResult(context.getString(R.string.self_check_name_last_location), Unknown, context.getString(R.string.self_check_loc_perm_missing));
        }
    }

    private void isGeocoderProvideAddress() {
        if (mLastLocation == null) {
            collector.addResult(
                    context.getString(R.string.self_check_geocoder_no_location),
                    Negative,
                    context.getString(R.string.self_check_geocoder_verify_backend));
            return;
        }

        final AtomicBoolean result = new AtomicBoolean(false);
        final AtomicBoolean timeout = new AtomicBoolean(true);

        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (result) {
                    try {
                        result.wait(10000);
                    } catch (InterruptedException ignored) {
                    }

                    if (timeout.get()) {
                        collector.addResult(
                                context.getString(R.string.self_check_name_nlp_geocoder_is_providing_addresses),
                                Unknown,
                                context.getString(R.string.self_check_resolution_nlp_geocoder_no_address_timeout));
                    } else {
                        collector.addResult(
                                context.getString(R.string.self_check_name_nlp_geocoder_is_providing_addresses),
                                result.get() ? Positive : Negative,
                                context.getString(R.string.self_check_resolution_nlp_geocoder_no_address));
                    }
                }
            }
        }).start();

        // Threaded Geocoder Request
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (result) {
                    try {
                        geocoder.getFromLocation(
                                mLastLocation.getLatitude(),
                                mLastLocation.getLongitude(),
                                1);
                        result.set(true);
                    } catch (IOException e) {
                        result.set(false);
                    }
                    timeout.set(false);
                    result.notifyAll();
                }
            }
        }).start();
    }
    
    private void updateNetworkLocation() {
        mLastLocation = null;
        mLastAddress = null;
        Intent sendIntent = new Intent("android.intent.action.START_LOCATION_UPDATE");
        sendIntent.setPackage("org.microg.nlp");
        sendIntent.putExtra("destinationPackageName", "org.microg.nlp");
        sendIntent.putExtra("resolveAddress", true);
        context.startService(sendIntent);
    }
}
