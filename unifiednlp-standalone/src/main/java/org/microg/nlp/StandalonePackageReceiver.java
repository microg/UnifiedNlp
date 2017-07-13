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

package org.microg.nlp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import org.microg.nlp.Preferences;

import org.microg.nlp.geocode.AbstractGeocodeService;
import org.microg.nlp.geocode.StandaloneAbstractGeocodeService;
import org.microg.nlp.location.AbstractLocationService;
import org.microg.nlp.location.StandaloneAbstractLocationService;

public class StandalonePackageReceiver extends BroadcastReceiver {
    private static final String TAG = "NlpPackageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Intent received: " + intent);
        String packageName = intent.getData().getSchemeSpecificPart();
        Preferences preferences = new Preferences(context);
        if (preferences.getLocationBackends().contains(packageName)) {
            Log.d(TAG, "Reloading location service for " + packageName);
            StandaloneAbstractLocationService.reloadLocationService(context);
        }
        if (preferences.getGeocoderBackends().contains(packageName)) {
            Log.d(TAG, "Reloading geocoding service for " + packageName);
            StandaloneAbstractGeocodeService.reloadGeocodeService(context);
        }
    }
}
