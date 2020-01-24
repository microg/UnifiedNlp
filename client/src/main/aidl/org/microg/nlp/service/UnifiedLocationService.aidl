/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service;

import android.location.Location;
import org.microg.nlp.service.AddressCallback;
import org.microg.nlp.service.LocationCallback;

interface UnifiedLocationService {
    void registerLocationCallback(LocationCallback callback, in Bundle options);
    void setUpdateInterval(long interval, in Bundle options);
    void requestSingleUpdate(in Bundle options);

    void getFromLocationWithOptions(double latitude, double longitude, int maxResults,
        String locale, in Bundle options, AddressCallback callback);
    void getFromLocationNameWithOptions(String locationName, int maxResults,
        double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude,
        double upperRightLongitude, String locale, in Bundle options, AddressCallback callback);

    void reloadPreferences();
    String[] getLocationBackends();
    void setLocationBackends(in String[] backends);
    String[] getGeocoderBackends();
    void setGeocoderBackends(in String[] backends);

    Location getLastLocation();
    Location getLastLocationForBackend(String packageName, String className, String signatureDigest);
}
