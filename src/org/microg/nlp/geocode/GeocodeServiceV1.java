package org.microg.nlp.geocode;

public class GeocodeServiceV1 extends AbstractGeocodeService {
    private static final String TAG = "NlpGeocodeService";
    private static GeocodeProviderV1 THE_ONE;

    public GeocodeServiceV1() {
        super(TAG);
    }

    @Override
    protected synchronized GeocodeProvider getProvider() {
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
