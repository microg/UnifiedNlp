/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import static org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND;
import static org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_PROVIDER;
import static org.microg.nlp.api.Constants.LOCATION_EXTRA_OTHER_BACKENDS;

class BackendFuser {
    private static final String TAG = "NlpLocationBackendFuser";

    private final List<BackendHelper> backendHelpers = new ArrayList<BackendHelper>();
    private final LocationProvider locationProvider;
    private final Context context;
    private Location forcedLocation;
    private boolean fusing = false;

    public BackendFuser(Context context, LocationProvider locationProvider) {
        this.locationProvider = locationProvider;
        this.context = context;
        reset();
    }

    public void reset() {
        unbind();
        backendHelpers.clear();
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

    public void bind() {
        fusing = false;
        for (BackendHelper handler : backendHelpers) {
            handler.bind();
        }
    }

    public void update() {
        Boolean hasUpdates = false;
        fusing = true;
        for (BackendHelper handler : backendHelpers) {
            if (handler.update() != null)
                hasUpdates = true;
        }
        fusing = false;
        if (hasUpdates)
            updateLocation();
    }

    void updateLocation() {
        List<Location> locations = new ArrayList<Location>();
        for (BackendHelper handler : backendHelpers) {
            locations.add(handler.getLastLocation());
        }
        if (forcedLocation != null) {
            locations.add(forcedLocation);
        }
        if (!locations.isEmpty()) {
            Location location = mergeLocations(locations);
            if (location != null) {
                location.setProvider(LocationManager.NETWORK_PROVIDER);
                locationProvider.reportLocation(location);
                Log.v(TAG, "location=" + location);
            }
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
        updateLocation();
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

    public void destroy() {
        unbind();
        backendHelpers.clear();
    }

    public static class LocationComparator implements Comparator<Location> {

        public static final LocationComparator INSTANCE = new LocationComparator();
        public static final long SWITCH_ON_FRESHNESS_CLIFF_MS = 30000; // 30 seconds

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
