package org.microg.nlp.geocode;

import org.microg.nlp.Provider;

public class GeocodeServiceV1 extends GeocodeService {
	private static final String TAG = GeocodeServiceV1.class.getName();

	public GeocodeServiceV1() {
		super(TAG);
	}

	@Override
	protected Provider createProvider() {
		return new GeocodeProviderV1();
	}
}
