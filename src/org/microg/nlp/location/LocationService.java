package org.microg.nlp.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import org.microg.nlp.Provider;
import org.microg.nlp.ProviderService;

public abstract class LocationService extends ProviderService {
	public static final String ACTION_RELOAD_SETTINGS = "org.microg.nlp.RELOAD_SETTINGS";

	public static void reloadLocationService(Context context) {
		Intent intent = new Intent(LocationService.ACTION_RELOAD_SETTINGS);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			intent.setClass(context, LocationServiceV2.class);
		} else {
			// TODO
		}
		context.startService(intent);
	}

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

		if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
			Provider provider = getCurrentProvider();
			if (provider != null) {
				provider.reload();
			}
		}
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Provider provider = getCurrentProvider();
		if (provider instanceof LocationProvider) {
			((LocationProvider) provider).onDisable();
		}
		return super.onUnbind(intent);
	}
}
