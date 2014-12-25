package org.microg.nlp.location;

public class LocationServiceV2 extends LocationService {
    private static final String TAG = LocationServiceV2.class.getName();

    public LocationServiceV2() {
        super(TAG);
    }

    @Override
    protected LocationProvider createProvider() {
        return new LocationProviderV2(this);
    }
}
