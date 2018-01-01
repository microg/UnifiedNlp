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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;

import org.microg.nlp.AbstractProviderService;

import java.lang.reflect.Method;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static org.microg.nlp.api.Constants.ACTION_FORCE_LOCATION;
import static org.microg.nlp.api.Constants.ACTION_RELOAD_SETTINGS;
import static org.microg.nlp.api.Constants.INTENT_EXTRA_LOCATION;
import static org.microg.nlp.api.Constants.PERMISSION_FORCE_LOCATION;

public abstract class AbstractLocationService extends AbstractProviderService<LocationProvider> {
    public static ComponentName reloadLocationService(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_SETTINGS);
        setIntentTarget(context, intent);
        return context.startService(intent);
    }

    private static void setIntentTarget(Context context, Intent intent) {
        if (SDK_INT >= JELLY_BEAN_MR1) {
            intent.setClass(context, LocationServiceV2.class);
        } else {
            intent.setClass(context, LocationServiceV1.class);
        }
    }

    public static boolean WAS_BOUND = false;

    /**
     * Creates an LocationService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public AbstractLocationService(String tag) {
        super(tag);
    }

    @Override
    public IBinder onBind(Intent intent) {
        WAS_BOUND = true;
        updateLauncherIcon();
        return super.onBind(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        LocationProvider provider = getProvider();

        if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
            if (provider != null) {
                provider.reload();
            } else {
                Log.d(TAG, "Cannot reload settings, provider not ready");
            }
        }

        updateLauncherIcon();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LocationProvider provider = getProvider();
        if (provider != null) {
            provider.onDisable();
        }
        updateLauncherIcon();
        return super.onUnbind(intent);
    }

    @SuppressWarnings("unchecked")
    private void updateLauncherIcon() {
        try {
            Class cls = Class.forName("org.microg.nlp.ui.SettingsLauncherActivity");
            Method updateLauncherIcon = cls.getDeclaredMethod("updateLauncherIcon", Context.class);
            updateLauncherIcon.invoke(null, this);
        } catch (Exception ignored) {
            // This package does not come with a settings launcher icon
        }
    }
}
