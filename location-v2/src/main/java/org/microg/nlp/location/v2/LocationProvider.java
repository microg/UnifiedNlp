/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.location.v2;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.os.WorkSource;
import android.util.Log;

import com.android.location.provider.LocationProviderBase;
import com.android.location.provider.ProviderPropertiesUnbundled;
import com.android.location.provider.ProviderRequestUnbundled;

import org.microg.nlp.client.UnifiedLocationClient;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static android.location.LocationProvider.AVAILABLE;

public class LocationProvider extends LocationProviderBase implements UnifiedLocationClient.LocationListener {
    private static final List<String> EXCLUDED_PACKAGES = Arrays.asList("android", "com.android.location.fused", "com.google.android.gms");
    private static final long FASTEST_REFRESH_INTERVAL = 30000;
    private static final String TAG = "ULocation";
    private Context context;

    public LocationProvider(Context context) {
        super(TAG, ProviderPropertiesUnbundled.create(false, false, false, false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE));
        this.context = context;
    }

    @Override
    public void onEnable() {
        UnifiedLocationClient.get(context).ref();
    }

    @Override
    public void onDisable() {
        UnifiedLocationClient.get(context).unref();
    }

    @Override
    public void onSetRequest(ProviderRequestUnbundled requests, WorkSource source) {
        Log.v(TAG, "onSetRequest: " + requests + " by " + source);
        String opPackageName = null;
        try {
            Field namesField = WorkSource.class.getDeclaredField("mNames");
            namesField.setAccessible(true);
            String[] names = (String[]) namesField.get(source);
            if (names != null) {
                for (String name : names) {
                    if (!EXCLUDED_PACKAGES.contains(name)) {
                        opPackageName = name;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        long autoTime = Math.max(requests.getInterval(), FASTEST_REFRESH_INTERVAL);
        boolean autoUpdate = requests.getReportLocation();

        Log.v(TAG, "using autoUpdate=" + autoUpdate + " autoTime=" + autoTime);

        if (autoUpdate) {
            UnifiedLocationClient.get(context).setOpPackageName(opPackageName);
            UnifiedLocationClient.get(context).requestLocationUpdates(this, autoTime);
        } else {
            UnifiedLocationClient.get(context).removeLocationUpdates(this);
        }
    }

    public void unsetRequest() {
        UnifiedLocationClient.get(context).removeLocationUpdates(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int onGetStatus(Bundle extras) {
        return AVAILABLE;
    }

    @Override
    public long onGetStatusUpdateTime() {
        return 0;
    }

    @Override
    public void onLocation(Location location) {
        reportLocation(location);
    }
}
