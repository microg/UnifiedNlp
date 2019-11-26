/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Intent;
import android.location.Location;
import android.location.Address;

interface GeocoderBackend {
    void open();
    List<Address> getFromLocation(double latitude, double longitude, int maxResults, String locale);
    List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, 
        double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, 
        String locale);
    void close();
    Intent getInitIntent();
    Intent getSettingsIntent();
    Intent getAboutIntent();
    List<Address> getFromLocationWithOptions(double latitude, double longitude, int maxResults,
        String locale, in Bundle options);
    List<Address> getFromLocationNameWithOptions(String locationName, int maxResults,
        double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude,
        double upperRightLongitude, String locale, in Bundle options);
}
