package org.microg.nlp.geocode;

import android.content.Context;
import android.content.Intent;

import org.microg.nlp.AbstractProviderService;

import static org.microg.nlp.api.Constants.ACTION_RELOAD_SETTINGS;

public abstract class AbstractGeocodeService extends AbstractProviderService<GeocodeProvider> {
    public static void reloadLocationService(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_SETTINGS);
        intent.setClass(context, GeocodeServiceV1.class);
        context.startService(intent);
    }

    /**
     * Creates an GeocodeService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public AbstractGeocodeService(String tag) {
        super(tag);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
            GeocodeProvider provider = getProvider();
            if (provider != null) {
                provider.reload();
            }
        }
    }
}
