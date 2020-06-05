/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api.sample;

import android.location.Location;
import android.util.Log;
import org.microg.nlp.api.LocationBackendService;
import org.microg.nlp.api.LocationHelper;

public class SampleBackendService extends LocationBackendService {
	private static final String TAG = SampleBackendService.class.getName();

	@Override
	protected Location update() {
		if (System.currentTimeMillis() % 60000 > 2000) {
			Log.d(TAG, "I decided not to answer now...");
			return null;
		}
		Location location = LocationHelper.create("sample", 42, 42, 42);
		Log.d(TAG, "I was asked for location and I answer: " + location);
		return location;
	}
}
