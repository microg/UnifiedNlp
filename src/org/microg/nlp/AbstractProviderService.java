package org.microg.nlp;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;

public abstract class AbstractProviderService<T extends Provider> extends IntentService {
    /**
     * Creates an ProviderService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public AbstractProviderService(String tag) {
        super(tag);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return getProvider().getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        destroyProvider();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Create a new {@link Provider} or return the existing one.
     * <p/>
     * This might be called more than once, the implementation has to ensure that only one
     * {@link Provider} is returned.
     *
     * @return a new or existing {@link Provider} instance
     */
    protected abstract T getProvider();

    /**
     * Destroy the active {@link Provider}.
     *
     * After this has been called, the {@link Provider} instance, that was active before should no
     * longer be returned with {@link #getProvider()}.
     */
    protected abstract void destroyProvider();

    @Override
    protected void onHandleIntent(Intent intent) {
        // Default implementation is to do nothing
    }
}
