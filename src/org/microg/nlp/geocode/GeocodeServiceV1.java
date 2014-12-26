package org.microg.nlp.geocode;

public class GeocodeServiceV1 extends GeocodeService {
    private static final String TAG = GeocodeServiceV1.class.getName();
    private static GeocodeProviderV1 THE_ONE;

    public GeocodeServiceV1() {
        super(TAG);
    }

    @Override
    protected synchronized GeocodeProvider createProvider() {
        if (THE_ONE == null) {
            THE_ONE = new GeocodeProviderV1(this);
        }
        return THE_ONE;
    }

    @Override
    protected void destroyProvider() {
        THE_ONE.destroy();
        THE_ONE = null;
    }
}
