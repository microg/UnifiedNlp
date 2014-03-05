package org.microg.nlp.location;

import android.location.Location;
import org.microg.nlp.Provider;

public interface LocationProvider extends Provider {
	void onEnable();

	void onDisable();

	void reportLocation(Location location);
}
