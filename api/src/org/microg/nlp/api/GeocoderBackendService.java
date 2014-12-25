package org.microg.nlp.api;

import android.location.Address;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.List;

public abstract class GeocoderBackendService extends AbstractBackendService {

    private Backend backend = new Backend();

    @Override
    protected IBinder getBackend() {
        return backend;
    }

    /**
     * @param locale The locale, formatted as a String with underscore (eg. en_US) the resulting
     *               address should be localized in
     * @see android.location.Geocoder#getFromLocation(double, double, int)
     */
    protected abstract List<Address> getFromLocation(double latitude, double longitude,
            int maxResults, String locale);

    /**
     * @param locale The locale, formatted as a String with underscore (eg. en_US) the resulting
     *               address should be localized in
     * @see android.location.Geocoder#getFromLocationName(String, int, double, double, double, double)
     */
    protected abstract List<Address> getFromLocationName(String locationName, int maxResults,
            double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude,
            double upperRightLongitude, String locale);

    private class Backend extends GeocoderBackend.Stub {

        @Override
        public void open() throws RemoteException {
            onOpen();
        }

        @Override
        public List<Address> getFromLocation(double latitude, double longitude, int maxResults,
                String locale) throws RemoteException {
            return GeocoderBackendService.this
                    .getFromLocation(latitude, longitude, maxResults, locale);
        }

        @Override
        public List<Address> getFromLocationName(String locationName, int maxResults,
                double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude,
                double upperRightLongitude, String locale) throws RemoteException {
            return GeocoderBackendService.this
                    .getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                            lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale);
        }

        @Override
        public void close() throws RemoteException {
            onClose();
            backend = null;
        }
    }
}
