package org.microg.nlp.api.sample;

import android.location.Location;
import android.util.Log;
import org.microg.nlp.api.LocationBackendService;

public class SecondSampleService extends LocationBackendService {
	private static final String TAG = SecondSampleService.class.getName();

	@Override
	protected Location update() {
		Location location = new Location("second-sample");
		location.setLatitude(13);
		location.setLongitude(13);
		location.setAccuracy((System.currentTimeMillis() / 1000) % 100);
		Log.d(TAG, "I was asked for location and I answer: " + location);
		return location;
	}
}
