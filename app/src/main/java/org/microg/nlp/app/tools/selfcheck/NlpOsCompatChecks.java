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

package org.microg.nlp.app.tools.selfcheck;

import android.content.Context;

import org.microg.nlp.app.R;

import java.util.Arrays;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.JELLY_BEAN;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static android.os.Build.VERSION_CODES.KITKAT;
import static android.os.Build.VERSION_CODES.M;

public class NlpOsCompatChecks implements SelfCheckGroup {

    public static final String CONFIG_NL_PROVIDER = "config_networkLocationProvider";
    public static final String CONFIG_NL_PROVIDER_PACKAGE_NAME = "config_networkLocationProviderPackageName";
    public static final String CONFIG_ENABLE_NL_OVERLAY = "config_enableNetworkLocationOverlay";
    public static final String CONFIG_NL_PROVIDER_PACKAGE_NAMES = "config_locationProviderPackageNames";

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_nlpcompat);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        checkSystemIsSupported(context, collector);
        checkSystemIsConfigured(context, collector);
    }

    private boolean checkSystemIsSupported(Context context, ResultCollector collector) {
        boolean isSupported = (SDK_INT >= KITKAT && SDK_INT <= M);
        collector.addResult(context.getString(R.string.self_check_name_system_supported),
                isSupported ? Result.Positive : Result.Unknown, context.getString(R.string.self_check_resolution_system_supported));
        return isSupported;
    }

    private boolean checkSystemIsConfigured(Context context, ResultCollector collector) {
        // 2.3+ com.android.internal.R.string.config_networkLocationProvider
        // 4.1+ com.android.internal.R.string.config_networkLocationProviderPackageName
        // 4.2+ com.android.internal.R.array.config_locationProviderPackageNames
        // 4.3+ com.android.internal.R.array.config_locationProviderPackageNames /
        //      com.android.internal.R.string.config_networkLocationProviderPackageName /
        //      com.android.internal.R.bool.config_enableNetworkLocationOverlay
        boolean systemMatchesPackage = false;
        if (SDK_INT < JELLY_BEAN) {
            systemMatchesPackage |= context.getPackageName().equals(getResourceString(context, CONFIG_NL_PROVIDER));
        } else {
            boolean overlay = getResourceBool(context, CONFIG_ENABLE_NL_OVERLAY);
            if (SDK_INT < JELLY_BEAN_MR1 || (SDK_INT > JELLY_BEAN_MR1 && !overlay)) {
                systemMatchesPackage |= context.getPackageName().equals(getResourceString(context, CONFIG_NL_PROVIDER_PACKAGE_NAME));
            }
            if (SDK_INT == JELLY_BEAN_MR1 || (SDK_INT > JELLY_BEAN_MR1 && overlay)) {
                systemMatchesPackage |= Arrays.asList(getResourceArray(context, CONFIG_NL_PROVIDER_PACKAGE_NAMES)).contains(context.getPackageName());
            }
        }
        collector.addResult(context.getString(R.string.self_check_name_nlp_package_name),
                systemMatchesPackage ? Result.Positive : Result.Negative, context.getString(R.string.self_check_resolution_nlp_package_name));
        return systemMatchesPackage;
    }

    private String[] getResourceArray(Context context, String identifier) {
        try {
            int resId = context.getResources().getIdentifier(identifier, "array", "android");
            if (resId == 0)
                resId = context.getResources().getIdentifier(identifier, "array", "com.android.internal");
            return context.getResources().getStringArray(resId);
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean getResourceBool(Context context, String identifier) {
        try {
            int resId = context.getResources().getIdentifier(identifier, "bool", "android");
            if (resId == 0)
                resId = context.getResources().getIdentifier(identifier, "bool", "com.android.internal");
            return context.getResources().getBoolean(resId);
        } catch (Exception e) {
            return false;
        }
    }

    private String getResourceString(Context context, String identifier) {
        try {
            int resId = context.getResources().getIdentifier(identifier, "string", "android");
            if (resId == 0)
                resId = context.getResources().getIdentifier(identifier, "string", "com.android.internal");
            return context.getString(resId);
        } catch (Exception e) {
            return null;
        }
    }
}
