package org.microg.nlp.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import org.microg.nlp.Preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.microg.nlp.api.NlpApiConstants.ACTION_GEOCODER_BACKEND;
import static org.microg.nlp.api.NlpApiConstants.ACTION_LOCATION_BACKEND;

public class GeocodeBackendConfigFragment extends AbstractBackendConfigFragment {
    private static final String TAG = GeocodeBackendConfigFragment.class.getName();
    
    public GeocodeBackendConfigFragment() {
        super(TAG);
    }

    @Override
    protected Map<ServiceInfo, KnownBackend> queryKnownBackends() {
        return intentToKnownBackendMap(getActivity(), new Intent(ACTION_GEOCODER_BACKEND));
    }

    @Override
    protected List<ServiceInfo> queryActiveBackends() {
        return backendStringToServiceInfos(new Preferences(getActivity()).getGeocoderBackends(),
                getKnownBackends().keySet());
    }

    @Override
    protected void saveActiveBackends(List<ServiceInfo> activeBackends) {
        new Preferences(getActivity()).setGeocoderBackends(serviceInfosToBackendString(activeBackends));
    }
}
