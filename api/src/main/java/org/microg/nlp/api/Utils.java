/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

@SuppressWarnings("WeakerAccess")
public class Utils {

    /**
     * Bring a mac address to the form 01:23:45:AB:CD:EF
     *
     * @param mac address to be well-formed
     * @return well-formed mac address
     */
    public static String wellFormedMac(String mac) {
        int HEX_RADIX = 16;
        int[] bytes = new int[6];
        String[] splitAtColon = mac.split(":");
        if (splitAtColon.length == 6) {
            for (int i = 0; i < 6; ++i) {
                bytes[i] = Integer.parseInt(splitAtColon[i], HEX_RADIX);
            }
        } else {
            String[] splitAtLine = mac.split("-");
            if (splitAtLine.length == 6) {
                for (int i = 0; i < 6; ++i) {
                    bytes[i] = Integer.parseInt(splitAtLine[i], HEX_RADIX);
                }
            } else if (mac.length() == 12) {
                for (int i = 0; i < 6; ++i) {
                    bytes[i] = Integer.parseInt(mac.substring(i * 2, (i + 1) * 2), HEX_RADIX);
                }
            } else if (mac.length() == 17) {
                for (int i = 0; i < 6; ++i) {
                    bytes[i] = Integer.parseInt(mac.substring(i * 3, (i * 3) + 2), HEX_RADIX);
                }
            } else {
                throw new IllegalArgumentException("Can't read this string as mac address");

            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; ++i) {
            String hex = Integer.toHexString(bytes[i]);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            if (sb.length() != 0)
                sb.append(":");
            sb.append(hex);
        }
        return sb.toString();
    }

    public static String getPackageApiVersion(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return applicationInfo.metaData == null ? null
                : applicationInfo.metaData.getString(Constants.METADATA_API_VERSION);
    }

    public static String getServiceApiVersion(Context context) {
        String apiVersion = getPackageApiVersion(context, "com.google.android.gms");
        if (apiVersion == null)
            apiVersion = getPackageApiVersion(context, "com.google.android.location");
        if (apiVersion == null) apiVersion = getPackageApiVersion(context, "org.microg.nlp");
        return apiVersion;
    }

    public static String getSelfApiVersion(Context context) {
        String apiVersion = getPackageApiVersion(context, context.getPackageName());
        if (!Constants.API_VERSION.equals(apiVersion)) {
            Log.w("VersionUtil", "You did not specify the currently used api version in your manifest.\n" +
                    "When using gradle + aar, this should be done automatically, if not, add the\n" +
                    "following to your <application> tag\n" +
                    "<meta-data android:name=\"" + Constants.METADATA_API_VERSION +
                    "\" android:value=\"" + Constants.API_VERSION + "\" />");
            apiVersion = Constants.API_VERSION;
        }
        return apiVersion;
    }
}
