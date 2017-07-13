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

package org.microg.nlp.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import static org.microg.nlp.api.Constants.ACTION_RELOAD_SETTINGS;
import org.microg.nlp.standalone.service.UnifiedNlpLocationManager;

public abstract class StandaloneAbstractLocationService extends AbstractLocationService {
    
    public StandaloneAbstractLocationService(String tag) {
        super(tag);
    }
    
    public static ComponentName reloadLocationService(Context context) {
        Intent intent = new Intent(ACTION_RELOAD_SETTINGS);
        intent.setClass(context, UnifiedNlpLocationManager.class);
        return context.startService(intent);
    }
}
