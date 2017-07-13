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

package org.microg.nlp.ui;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.AttributeSet;

import org.microg.nlp.Preferences;
import org.microg.nlp.R;
import org.microg.nlp.api.GeocoderBackend;
import org.microg.nlp.api.LocationBackend;
import org.microg.nlp.ui.LocationBackendPreference;

import static org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND;
import org.microg.nlp.location.StandaloneAbstractLocationService;

public class StandaloneLocationBackendPreference extends LocationBackendPreference {
    public StandaloneLocationBackendPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onValueChanged() {
        StandaloneAbstractLocationService.reloadLocationService(getContext());
    }
}
