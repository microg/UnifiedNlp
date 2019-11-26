/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Context;

@SuppressWarnings("unused")
@Deprecated
public class VersionUtils {

    public static String getPackageApiVersion(Context context, String packageName) {
        return Utils.getPackageApiVersion(context, packageName);
    }

    public static String getServiceApiVersion(Context context) {
        return Utils.getServiceApiVersion(context);
    }

    public static String getSelfApiVersion(Context context) {
        return Utils.getSelfApiVersion(context);
    }
}
