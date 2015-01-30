/*
 * Copyright 2014-2015 µg Project Team
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;

import org.microg.nlp.AbstractProviderService;

import static org.microg.nlp.api.Constants.ACTION_FORCE_LOCATION;
import static org.microg.nlp.api.Constants.ACTION_RELOAD_SETTINGS;
import static org.microg.nlp.api.Constants.INTENT_EXTRA_LOCATION;
import static org.microg.nlp.api.Constants.PERMISSION_FORCE_LOCATION;

public abstract class AbstractLocationService extends AbstractProviderService<LocationProvider> {
    public static void reloadLocationService(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_SETTINGS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            intent.setClass(context, LocationServiceV2.class);
        } else {
            intent.setClass(context, LocationServiceV1.class);
        }
        context.startService(intent);
    }

    /**
     * Creates an LocationService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public AbstractLocationService(String tag) {
        super(tag);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LocationProvider provider = getProvider();

        if (ACTION_FORCE_LOCATION.equals(intent.getAction())) {
            if (checkCallingPermission(PERMISSION_FORCE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {
                if (provider != null && intent.hasExtra(INTENT_EXTRA_LOCATION)) {
                    provider.forceLocation(
                            (Location) intent.getParcelableExtra(INTENT_EXTRA_LOCATION));
                }
            }
        }

        if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
            if (provider != null) {
                provider.reload();
            }
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LocationProvider provider = getProvider();
        if (provider != null) {
            provider.onDisable();
        }
        return super.onUnbind(intent);
    }
}
