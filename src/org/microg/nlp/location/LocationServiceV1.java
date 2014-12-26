package org.microg.nlp.location;

public class LocationServiceV1 extends LocationService {
    private static final String TAG = LocationServiceV1.class.getName();
    private static LocationProviderV1 THE_ONE;

    public LocationServiceV1() {
        super(TAG);
    }

    @Override
    protected synchronized LocationProvider createProvider() {
        if (THE_ONE == null) {
            THE_ONE = new LocationProviderV1(this);
        }
        return THE_ONE;
    }

    @Override
    protected void destroyProvider() {
        THE_ONE.destroy();
        THE_ONE = null;
    }
}
