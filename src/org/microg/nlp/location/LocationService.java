package org.microg.nlp.location;

import android.content.Intent;
import org.microg.nlp.Provider;
import org.microg.nlp.ProviderService;

public abstract class LocationService extends ProviderService {
	/**
	 * Creates an LocationService.  Invoked by your subclass's constructor.
	 *
	 * @param tag Used for debugging.
	 */
	public LocationService(String tag) {
		super(tag);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		/*
		 * There is an undocumented way to send locations via intent in Google's LocationService.
		 * This intent based location is not secure, that's why it's not active here,
		 * but maybe we will add it in the future, could be a nice debugging feature :)
		 */

		/*
		Location location = intent.getParcelableExtra("location");
		if (nlprovider != null && location != null) {
			nlprovider.reportLocation(location);
		}
		*/
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Provider provider = getCurrentProvider();
		if (provider instanceof LocationProvider) {
			((LocationProvider) provider).onDisable();
		}
		return false;
	}
}
