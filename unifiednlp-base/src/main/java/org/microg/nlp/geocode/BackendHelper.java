/*
 * Copyright 2013-2016 microG Project Team
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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.microg.nlp.AbstractBackendHelper;
import org.microg.nlp.api.GeocoderBackend;

import java.util.List;

class BackendHelper extends AbstractBackendHelper {
    private static final String TAG = "NlpGeoBackendHelper";
    private GeocoderBackend backend;

    public BackendHelper(Context context, Intent serviceIntent, String signatureDigest) {
        super(TAG, context, serviceIntent, signatureDigest);
    }

    public List<Address> getFromLocation(double latitude, double longitude, int maxResults,
                                         String locale) {
        try {
            return backend.getFromLocation(latitude, longitude, maxResults, locale);
        } catch (Exception e) {
            Log.w(TAG, e);
            unbind();
            return null;
        }
    }

    public List<Address> getFromLocationName(String locationName, int maxResults,
                                             double lowerLeftLatitude, double lowerLeftLongitude,
                                             double upperRightLatitude, double upperRightLongitude,
                                             String locale) {
        try {
            return backend.getFromLocationName(locationName, maxResults, lowerLeftLatitude,
                    lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale);
        } catch (Exception e) {
            Log.w(TAG, e);
            unbind();
            return null;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        backend = GeocoderBackend.Stub.asInterface(service);
        if (backend != null) {
            try {
                backend.open();
            } catch (Exception e) {
                Log.w(TAG, e);
                unbind();
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        super.onServiceDisconnected(name);
        backend = null;
    }

    @Override
    public void close() throws RemoteException {
        backend.close();
    }

    @Override
    public boolean hasBackend() {
        return backend != null;
    }
}
