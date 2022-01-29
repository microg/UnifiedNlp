/*
 * SPDX-FileCopyrightText: 2022, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import android.location.Location;
import org.microg.nlp.service.api.ILocationListener;
import org.microg.nlp.service.api.IStatusCallback;
import org.microg.nlp.service.api.IStringsCallback;
import org.microg.nlp.service.api.LocationRequest;

interface ILocationService {
    oneway void getLastLocation(ILocationListener listener, in Bundle options) = 0;
    oneway void getLastLocationForBackend(String packageName, String className, String signatureDigest, ILocationListener listener, in Bundle options) = 1;

    oneway void updateLocationRequest(in LocationRequest request, IStatusCallback callback, in Bundle options) = 10;
    oneway void cancelLocationRequestByListener(ILocationListener listener, IStatusCallback callback, in Bundle options) = 11;
    oneway void cancelLocationRequestById(String id, IStatusCallback callback, in Bundle options) = 12;
    oneway void forceLocationUpdate(IStatusCallback callback, in Bundle options) = 13;

    oneway void reloadPreferences(IStatusCallback callback, in Bundle options) = 20;
    oneway void getLocationBackends(IStringsCallback callback, in Bundle options) = 21;
    oneway void setLocationBackends(in List<String> backends, IStatusCallback callback, in Bundle options) = 22;
}
