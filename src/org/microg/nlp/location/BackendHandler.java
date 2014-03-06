package org.microg.nlp.location;

import android.location.Location;

public interface BackendHandler {
	void unbind();

	boolean bind();

	Location update();

	Location getLastLocation();
}
