package org.microg.nlp.api.sample;

import android.location.Location;
import org.microg.nlp.api.LocationBackendService;

public class SecondSampleService extends LocationBackendService {
	@Override
	protected Location update() {
		Location location = new Location("second-sample");
		location.setLatitude(13);
		location.setLongitude(13);
		location.setAccuracy(100);
		return location;
	}
}
