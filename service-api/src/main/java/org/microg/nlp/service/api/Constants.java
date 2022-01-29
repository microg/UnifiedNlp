/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

public final class Constants {
    public static final int STATUS_OK = 0;
    public static final int STATUS_NOT_IMPLEMENTED = 1;
    public static final int STATUS_PERMISSION_ERROR = 2;
    public static final int STATUS_INVALID_ARGS = 3;

    public static final String ACTION_LOCATION = "org.microg.nlp.service.LOCATION";
    public static final String ACTION_GEOCODE = "org.microg.nlp.service.GEOCODE";

    public static final String LOCATION_EXTRA_BACKEND_PROVIDER = "org.microg.nlp.extra.SERVICE_BACKEND_PROVIDER";
    public static final String LOCATION_EXTRA_BACKEND_COMPONENT = "org.microg.nlp.extra.SERVICE_BACKEND_COMPONENT";
    public static final String LOCATION_EXTRA_OTHER_BACKENDS = "org.microg.nlp.extra.OTHER_BACKEND_RESULTS";
}
