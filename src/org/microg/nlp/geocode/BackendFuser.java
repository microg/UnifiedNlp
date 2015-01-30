/*
 * Copyright 2014-2015 µg Project Team
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

package org.microg.nlp.geocode;

import android.content.Context;
import android.content.Intent;
import android.location.Address;

import org.microg.nlp.Preferences;

import java.util.ArrayList;
import java.util.List;

import static org.microg.nlp.api.Constants.ACTION_GEOCODER_BACKEND;

class BackendFuser {
    private final List<BackendHelper> backendHelpers = new ArrayList<>();
    private final Context context;

    public BackendFuser(Context context) {
        this.context = context;
        reset();
    }

    public void bind() {
        for (BackendHelper backendHelper : backendHelpers) {
            backendHelper.bind();
        }
    }

    public void unbind() {
        for (BackendHelper backendHelper : backendHelpers) {
            backendHelper.unbind();
        }
    }

    public List<Address> getFromLocation(double latitude, double longitude, int maxResults,
            String locale) {
        if (backendHelpers.isEmpty())
            return null;
        ArrayList<Address> result = new ArrayList<>();
        for (BackendHelper backendHelper : backendHelpers) {
            List<Address> backendResult = backendHelper
                    .getFromLocation(latitude, longitude, maxResults, locale);
            if (backendResult != null) {
                result.addAll(backendResult);
            }
        }
        return result;
    }

    public List<Address> getFromLocationName(String locationName, int maxResults,
            double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude,
            double upperRightLongitude, String locale) {
        if (backendHelpers.isEmpty())
            return null;
        ArrayList<Address> result = new ArrayList<>();
        for (BackendHelper backendHelper : backendHelpers) {
            List<Address> backendResult = backendHelper
                    .getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                            lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale);
            if (backendResult != null) {
                result.addAll(backendResult);
            }
        }
        return result;
    }

    public void reset() {
        unbind();
        backendHelpers.clear();
        for (String backend : Preferences
                .splitBackendString(new Preferences(context).getGeocoderBackends())) {
            String[] parts = backend.split("/");
            if (parts.length == 2) {
                Intent intent = new Intent(ACTION_GEOCODER_BACKEND);
                intent.setPackage(parts[0]);
                intent.setClassName(parts[0], parts[1]);
                backendHelpers.add(new BackendHelper(context, intent));
            }
        }
    }
}
