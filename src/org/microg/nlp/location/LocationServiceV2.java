package org.microg.nlp.location;

public class LocationServiceV2 extends AbstractLocationService {
    private static final String TAG = "NlpLocationService";
    private static LocationProviderV2 THE_ONE;

    public LocationServiceV2() {
        super(TAG);
    }

    @Override
    protected synchronized LocationProvider getProvider() {
        if (THE_ONE == null) {
            THE_ONE = new LocationProviderV2(this);
        }
        return THE_ONE;
    }

    @Override
    protected void destroyProvider() {
        THE_ONE.destroy();
        THE_ONE = null;
    }
}
