package org.microg.nlp.geocode;

public class GeocodeServiceV1 extends GeocodeService {
    private static final String TAG = GeocodeServiceV1.class.getName();

    public GeocodeServiceV1() {
        super(TAG);
    }

    @Override
    protected GeocodeProvider createProvider() {
        return new GeocodeProviderV1(this);
    }
}
