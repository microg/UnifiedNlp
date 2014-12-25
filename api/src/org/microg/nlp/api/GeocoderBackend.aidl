package org.microg.nlp.api;

import android.location.Location;
import android.location.Address;

interface GeocoderBackend {
    void open();
    List<Address> getFromLocation(double latitude, double longitude, int maxResults, String locale);
    List<Address> getFromLocationName(String locationName, double lowerLeftLatitude, 
        double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, 
        int maxResults, String locale);
    void close();
}
