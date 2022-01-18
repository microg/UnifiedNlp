/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

public class Constants {
    public static final String ACTION_LOCATION_BACKEND = "org.microg.nlp.LOCATION_BACKEND";
    public static final String ACTION_GEOCODER_BACKEND = "org.microg.nlp.GEOCODER_BACKEND";
    public static final String ACTION_RELOAD_SETTINGS = "org.microg.nlp.RELOAD_SETTINGS";
    public static final String ACTION_FORCE_LOCATION = "org.microg.nlp.FORCE_LOCATION";
    public static final String PERMISSION_FORCE_LOCATION = "org.microg.permission.FORCE_COARSE_LOCATION";
    public static final String INTENT_EXTRA_LOCATION = "location";
    @Deprecated
    public static final String LOCATION_EXTRA_BACKEND_PROVIDER = "SERVICE_BACKEND_PROVIDER";
    @Deprecated
    public static final String LOCATION_EXTRA_BACKEND_COMPONENT = "SERVICE_BACKEND_COMPONENT";
    @Deprecated
    public static final String LOCATION_EXTRA_OTHER_BACKENDS = "OTHER_BACKEND_RESULTS";
    public static final String METADATA_BACKEND_SETTINGS_ACTIVITY = "org.microg.nlp.BACKEND_SETTINGS_ACTIVITY";
    public static final String METADATA_BACKEND_ABOUT_ACTIVITY = "org.microg.nlp.BACKEND_ABOUT_ACTIVITY";
    public static final String METADATA_BACKEND_INIT_ACTIVITY = "org.microg.nlp.BACKEND_INIT_ACTIVITY";
    public static final String METADATA_BACKEND_SUMMARY = "org.microg.nlp.BACKEND_SUMMARY";
    public static final String METADATA_API_VERSION = "org.microg.nlp.API_VERSION";
    public static final String API_VERSION = "3";
}
