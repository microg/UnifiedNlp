package org.microg.nlp.geocode;

import org.microg.nlp.ProviderService;

public abstract class GeocodeService extends ProviderService<GeocodeProvider> {
    /**
     * Creates an GeocodeService.  Invoked by your subclass's constructor.
     *
     * @param tag Used for debugging.
     */
    public GeocodeService(String tag) {
        super(tag);
    }
}
