/*
 * Copyright 2013-2015 µg Project Team
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

package org.microg.nlp.ui;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
import static android.content.pm.PackageManager.DONT_KILL_APP;
import static android.content.pm.PackageManager.GET_META_DATA;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class SettingInjectorService extends android.location.SettingInjectorService {
    private static final String TAG = "NlpSettingInjector";

    public SettingInjectorService() {
        super(TAG);
    }

    @Override
    protected String onGetSummary() {
        return "";
    }

    @Override
    protected boolean onGetEnabled() {
        return true;
    }

    public static void setLauncherIconEnabled(Context context, boolean enabled) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, SettingsLauncherActivity.class),
                enabled ? COMPONENT_ENABLED_STATE_ENABLED : COMPONENT_ENABLED_STATE_DISABLED,
                DONT_KILL_APP);
    }

    public static boolean settingsInjectionPossible(Context context) {
        try {
            Context settingsContext = context.createPackageContext("com.android.settings", CONTEXT_INCLUDE_CODE);
            ClassLoader cl = settingsContext.getClassLoader();
            Class cls = cl.loadClass("com.android.settings.location.SettingsInjector");
            Method pSi = cls.getDeclaredMethod("parseServiceInfo", ResolveInfo.class, PackageManager.class);
            pSi.setAccessible(true);
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(context, SettingInjectorService.class);
            List<ResolveInfo> ris = pm.queryIntentServices(intent, GET_META_DATA);
            ResolveInfo ri = ris.get(0);
            Object result = pSi.invoke(null, ri, pm);
            if (result != null) {
                Log.d(TAG, "Setting injection possible!");
                return true;
            }
        } catch (InvocationTargetException e) {
            Log.d(TAG, "settings injection not possible: " + e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "Can't determine if settings injection is possible");
        }
        return false;
    }
}
