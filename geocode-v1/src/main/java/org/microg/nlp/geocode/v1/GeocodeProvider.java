/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.geocode.v1;

import android.content.Context;
import android.location.Address;
import android.location.GeocoderParams;
import android.util.Log;

import org.microg.nlp.client.UnifiedLocationClient;

import java.util.List;

public class GeocodeProvider extends com.android.location.provider.GeocodeProvider {
    private static final String TAG = "UGeocode";
    private Context context;
    private static final long TIMEOUT = 10000;

    public GeocodeProvider(Context context) {
        this.context = context;
        UnifiedLocationClient.get(context).ref();
    }

    public void onDisable() {
        UnifiedLocationClient.get(context).unref();
    }

    @Override
    public String onGetFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        try {
            return handleResult(addrs, UnifiedLocationClient.get(context).getFromLocationSync(latitude, longitude, maxResults, params.getLocale().toString(), TIMEOUT));
        } catch (Exception e) {
            Log.w(TAG, e);
            return e.getMessage();
        }
    }

    @Override
    public String onGetFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) {
        try {
            return handleResult(addrs, UnifiedLocationClient.get(context).getFromLocationNameSync(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, params.getLocale().toString(), TIMEOUT));
        } catch (Exception e) {
            Log.w(TAG, e);
            return e.getMessage();
        }
    }

    private String handleResult(List<Address> realResult, List<Address> fuserResult) {
        if (fuserResult.isEmpty()) {
            return "no result";
        } else {
            realResult.addAll(fuserResult);
            return null;
        }
    }
}
