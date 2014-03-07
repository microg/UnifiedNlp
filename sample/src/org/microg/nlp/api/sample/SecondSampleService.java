package org.microg.nlp.api.sample;

import android.location.Location;
import android.util.Log;
import org.microg.nlp.api.LocationBackendService;
import org.microg.nlp.api.LocationHelper;

public class SecondSampleService extends LocationBackendService {
	private static final String TAG = SecondSampleService.class.getName();

	@Override
	protected Location update() {
		Location location = LocationHelper.create("second-sample", 13, 13, (System.currentTimeMillis() / 1000) % 100);
		Log.d(TAG, "I was asked for location and I answer: " + location);
		return location;
	}
}
