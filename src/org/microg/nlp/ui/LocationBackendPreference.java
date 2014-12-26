package org.microg.nlp.ui;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import org.microg.nlp.Preferences;
import org.microg.nlp.R;
import org.microg.nlp.location.LocationService;

import static org.microg.nlp.api.NlpApiConstants.ACTION_LOCATION_BACKEND;

public class LocationBackendPreference extends AbstractBackendPreference {
    public LocationBackendPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogTitle(R.string.configure_location_backends);
        setTitle(R.string.configure_location_backends);
    }

    @Override
    protected void onValueChanged() {
        LocationService.reloadLocationService(getContext());
    }

    @Override
    protected Intent buildBackendIntent() {
        return new Intent(ACTION_LOCATION_BACKEND);
    }

    @Override
    protected String defaultValue() {
        return new Preferences(getContext()).getDefaultLocationBackends();
    }
}
