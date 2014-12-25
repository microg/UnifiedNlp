package org.microg.nlp.location;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import org.microg.nlp.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.microg.nlp.api.NlpApiConstants.*;

class BackendFuser {
    private static final String TAG = BackendFuser.class.getName();

    private final List<BackendHelper> backendHelpers;
    private final LocationProvider locationProvider;
    private Location forcedLocation;
    private boolean fusing = false;

    public BackendFuser(Context context, LocationProvider provider) {
        locationProvider = provider;
        backendHelpers = new ArrayList<BackendHelper>();
        for (String backend : Preferences
                .splitBackendString(new Preferences(context).getLocationBackends())) {
            String[] parts = backend.split("/");
            if (parts.length == 2) {
                Intent intent = new Intent(ACTION_LOCATION_BACKEND);
                intent.setPackage(parts[0]);
                intent.setClassName(parts[0], parts[1]);
                backendHelpers.add(new BackendHelper(context, this, intent));
            }
        }
    }

    public void unbind() {
        for (BackendHelper handler : backendHelpers) {
            handler.unbind();
        }
    }

    public boolean bind() {
        fusing = false;
        boolean handlerBound = false;
        for (BackendHelper handler : backendHelpers) {
            if (handler.bind()) {
                handlerBound = true;
            }
        }
        return handlerBound;
    }

    public Location update() {
        fusing = true;
        for (BackendHelper handler : backendHelpers) {
            handler.update();
        }
        fusing = false;
        return getLastLocation();
    }

    public Location getLastLocation() {
        List<Location> locations = new ArrayList<Location>();
        for (BackendHelper handler : backendHelpers) {
            locations.add(handler.getLastLocation());
        }
        if (forcedLocation != null) {
            locations.add(forcedLocation);
        }
        if (locations.isEmpty()) {
            return null;
        } else {
            Location location = mergeLocations(locations);
            if (location != null) {
                location.setProvider(LocationManager.NETWORK_PROVIDER);
                locationProvider.reportLocation(location);
                Log.v(TAG, "location=" + location);
            }
            return location;
        }
    }

    private Location mergeLocations(List<Location> locations) {
        Collections.sort(locations, LocationComparator.INSTANCE);
        if (locations.isEmpty() || locations.get(0) == null)
            return null;
        if (locations.size() == 1)
            return locations.get(0);
        Location location = new Location(locations.get(0));
        ArrayList<Location> backendResults = new ArrayList<Location>();
        for (Location backendResult : locations) {
            if (locations.get(0) == backendResult)
                continue;
            if (backendResult != null)
                backendResults.add(backendResult);
        }
        if (!backendResults.isEmpty()) {
            location.getExtras()
                    .putParcelableArrayList(LOCATION_EXTRA_OTHER_BACKENDS, backendResults);
        }
        return location;
    }

    public void reportLocation() {
        if (fusing)
            return;
        getLastLocation();
    }

    public void forceLocation(Location location) {
        forcedLocation = location;
        if (forcedLocation != null) {
            Bundle extras = new Bundle();
            extras.putString(LOCATION_EXTRA_BACKEND_PROVIDER, "forced");
            location.setExtras(extras);
        }
    }

    public Location getForcedLocation() {
        return forcedLocation;
    }

    public static class LocationComparator implements Comparator<Location> {

        public static final LocationComparator INSTANCE = new LocationComparator();
        public static final long SWITCH_ON_FRESHNESS_CLIFF_MS = 30000; // 30 seconds TODO: make it a setting

        /**
         * @return whether {@param lhs} is better than {@param rhs}
         */
        @Override
        public int compare(Location lhs, Location rhs) {
            if (lhs == rhs)
                return 0;
            if (lhs == null) {
                return 1;
            }
            if (rhs == null) {
                return -1;
            }
            if (!lhs.hasAccuracy()) {
                return 1;
            }
            if (!rhs.hasAccuracy()) {
                return -1;
            }
            if (rhs.getTime() > lhs.getTime() + SWITCH_ON_FRESHNESS_CLIFF_MS) {
                return 1;
            }
            if (lhs.getTime() > rhs.getTime() + SWITCH_ON_FRESHNESS_CLIFF_MS) {
                return -1;
            }
            return (int) (lhs.getAccuracy() - rhs.getAccuracy());
        }
    }
}
