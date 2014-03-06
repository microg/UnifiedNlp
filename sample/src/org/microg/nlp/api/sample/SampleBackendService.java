package org.microg.nlp.api.sample;

import android.location.Location;
import android.util.Log;
import org.microg.nlp.api.LocationBackendService;

public class SampleBackendService extends LocationBackendService {
	private static final String TAG = SampleBackendService.class.getName();

	@Override
	protected Location update() {
		if (System.currentTimeMillis()%60000>2000) {
			Log.d(TAG, "I decided not to answer now...");
			return null;
		}
		Location location = new Location("sample");
		location.setLatitude(42);
		location.setLongitude(42);
		location.setAccuracy(42);
		Log.d(TAG, "I was asked for location and I answer: " + location);
		return location;
	}
}
