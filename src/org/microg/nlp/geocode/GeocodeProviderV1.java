package org.microg.nlp.geocode;

import android.location.Address;
import android.location.GeocoderParams;
import com.android.location.provider.GeocodeProvider;

import java.util.List;

public class GeocodeProviderV1 extends GeocodeProvider implements org.microg.nlp.geocode.GeocodeProvider {
	@Override
	public String onGetFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params,
	                                List<Address> addresses) {
		return null;
	}

	@Override
	public String onGetFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude,
	                                    double upperRightLatitude, double upperRightLongitude, int maxResults,
	                                    GeocoderParams params, List<Address> addresses) {
		return null;
	}
}
