package org.microg.nlp.geocode;

import android.content.Intent;
import org.microg.nlp.ProviderService;

import static org.microg.nlp.api.NlpApiConstants.ACTION_RELOAD_SETTINGS;

public abstract class GeocodeService extends ProviderService<GeocodeProvider> {
    /**
     * Creates an GeocodeService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public GeocodeService(String tag) {
        super(tag);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_RELOAD_SETTINGS.equals(intent.getAction())) {
            GeocodeProvider provider = getCurrentProvider();
            if (provider != null) {
                provider.reload();
            }
        }
    }
}
