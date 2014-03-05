package org.microg.nlp;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

public abstract class ProviderService extends IntentService {
	private Provider provider;

	/**
	 * Creates an ProviderService.  Invoked by your subclass's constructor.
	 *
	 * @param tag Used for debugging.
	 */
	public ProviderService(String tag) {
		super(tag);
	}

	@Override
	public void onCreate() {
		provider = createProvider();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return provider.getBinder();
	}

	@Override
	public void onDestroy() {
		provider = null;
	}

	/**
	 * Create a {@link org.microg.nlp.Provider}.
	 * This is most likely only called once
	 *
	 * @return a new {@link org.microg.nlp.Provider} instance
	 */
	protected abstract Provider createProvider();

	@Override
	protected void onHandleIntent(Intent intent) {
		// Default implementation is to do nothing
	}

	/**
	 * @return the currently used {@link org.microg.nlp.Provider} instance
	 */
	protected Provider getCurrentProvider() {
		return provider;
	}
}
