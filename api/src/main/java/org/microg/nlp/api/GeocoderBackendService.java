/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.content.Intent;
import android.location.Address;
import android.os.Bundle;
import android.os.IBinder;

import java.util.List;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class GeocoderBackendService extends AbstractBackendService {

    private final Backend backend = new Backend();
    private boolean connected = false;

    @Override
    protected IBinder getBackend() {
        return backend;
    }

    @Override
    public void disconnect() {
        if (connected) {
            onClose();
            connected = false;
        }
    }

    /**
     * @param locale The locale, formatted as a String with underscore (eg. en_US) the resulting
     *               address should be localized in
     * @see android.location.Geocoder#getFromLocation(double, double, int)
     */
    protected abstract List<Address> getFromLocation(double latitude, double longitude, int maxResults, String locale);

    protected List<Address> getFromLocation(double latitude, double longitude, int maxResults, String locale, Bundle options) {
        return getFromLocation(latitude, longitude, maxResults, locale);
    }

    /**
     * @param locale The locale, formatted as a String with underscore (eg. en_US) the resulting
     *               address should be localized in
     * @see android.location.Geocoder#getFromLocationName(String, int, double, double, double, double)
     */
    protected abstract List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, String locale);


    protected List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, String locale, Bundle options) {
        return getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale);
    }

    private class Backend extends GeocoderBackend.Stub {

        @Override
        public void open() {
            onOpen();
            connected = true;
        }

        @Override
        public List<Address> getFromLocation(double latitude, double longitude, int maxResults, String locale) {
            return GeocoderBackendService.this
                    .getFromLocation(latitude, longitude, maxResults, locale);
        }

        @Override
        public List<Address> getFromLocationWithOptions(double latitude, double longitude, int maxResults, String locale, Bundle options) {
            return GeocoderBackendService.this.getFromLocation(latitude, longitude, maxResults, locale, options);
        }


        @Override
        public List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, String locale) {
            return GeocoderBackendService.this
                    .getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                            lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale);
        }

        @Override
        public List<Address> getFromLocationNameWithOptions(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, String locale, Bundle options) {
            return GeocoderBackendService.this.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale, options);
        }

        @Override
        public void close() {
            disconnect();
        }

        @Override
        public Intent getInitIntent() {
            return GeocoderBackendService.this.getInitIntent();
        }

        @Override
        public Intent getSettingsIntent() {
            return GeocoderBackendService.this.getSettingsIntent();
        }

        @Override
        public Intent getAboutIntent() {
            return GeocoderBackendService.this.getAboutIntent();
        }
    }
}
