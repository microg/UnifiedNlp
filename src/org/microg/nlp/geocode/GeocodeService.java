package org.microg.nlp.geocode;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import org.microg.nlp.ProviderService;

public abstract class GeocodeService extends ProviderService {
	/**
	 * Creates an GeocodeService.  Invoked by your subclass's constructor.
	 *
	 * @param tag Used for debugging.
	 */
	public GeocodeService(String tag) {
		super(tag);
	}
}
