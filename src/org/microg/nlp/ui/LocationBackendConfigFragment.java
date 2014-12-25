package org.microg.nlp.ui;

import android.content.Intent;
import android.content.pm.ServiceInfo;
import org.microg.nlp.Preferences;
import org.microg.nlp.location.LocationService;

import java.util.List;
import java.util.Map;

import static org.microg.nlp.api.NlpApiConstants.ACTION_LOCATION_BACKEND;

public class LocationBackendConfigFragment extends AbstractBackendConfigFragment {
    private static final String TAG = LocationBackendConfigFragment.class.getName();

    public LocationBackendConfigFragment() {
        super(TAG);
    }

    @Override
    protected Map<ServiceInfo, AbstractBackendConfigFragment.KnownBackend> queryKnownBackends() {
        return intentToKnownBackendMap(getActivity(), new Intent(ACTION_LOCATION_BACKEND));
    }

    @Override
    protected List<ServiceInfo> queryActiveBackends() {
        return backendStringToServiceInfos(new Preferences(getActivity()).getLocationBackends(),
                getKnownBackends().keySet());
    }

    @Override
    protected void saveActiveBackends(List<ServiceInfo> activeBackends) {
        new Preferences(getActivity())
                .setLocationBackends(serviceInfosToBackendString(activeBackends));
        LocationService.reloadLocationService(getActivity());
    }

}
