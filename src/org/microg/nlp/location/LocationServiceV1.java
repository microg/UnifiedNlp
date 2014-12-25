package org.microg.nlp.location;

public class LocationServiceV1 extends LocationService {
    private static final String TAG = LocationServiceV1.class.getName();

    public LocationServiceV1() {
        super(TAG);
    }

    @Override
    protected LocationProvider createProvider() {
        return new LocationProviderV1(this);
    }
}
