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

package org.microg.nlp.standalone.selfcheck;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.text.TextUtils;

import org.microg.nlp.Preferences;
import org.microg.nlp.standalone.R;

import android.os.IBinder;
import org.microg.tools.selfcheck.SelfCheckGroup;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public class StandaloneNlpStatusChecks extends Service implements SelfCheckGroup {
    
    private static ResultCollector collector;
    private static Context context;
    private static Location mLastLocation;
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
        isLocationProviderSetUp();
        isProvidingLocation();
    }

    private boolean isLocationProviderSetUp() {
        boolean setupLocationProvider = !TextUtils.isEmpty(new Preferences(context).getLocationBackends());
        collector.addResult(context.getString(R.string.self_check_name_nlp_setup),
                setupLocationProvider ? Positive : Negative, context.getString(R.string.self_check_resolution_nlp_setup));
        return setupLocationProvider;
    }

    private void isProvidingLocation() {
        try {
            updateNetworkLocation();
        } catch (SecurityException e) {
            collector.addResult(context.getString(R.string.self_check_name_last_location), Unknown, context.getString(R.string.self_check_loc_perm_missing));
        }
    }
    
    private void updateNetworkLocation() {
        mLastLocation = null;
        mLastAddress = null;
        Intent sendIntent = new Intent("android.intent.action.START_LOCATION_UPDATE");
        sendIntent.setPackage("org.microg.nlp");
        sendIntent.putExtra("destinationPackageName", "org.microg.nlp");
        context.startService(sendIntent);
    }
}
