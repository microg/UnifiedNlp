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

package org.microg.nlp.geocode;

public class GeocodeServiceV1 extends AbstractGeocodeService {
    private static final String TAG = "NlpGeocodeService";
    private static GeocodeProviderV1 THE_ONE;

    public GeocodeServiceV1() {
        super(TAG);
    }

    @Override
    protected synchronized GeocodeProvider getProvider() {
        if (THE_ONE == null) {
            THE_ONE = new GeocodeProviderV1(this);
        }
        return THE_ONE;
    }

    @Override
    protected void destroyProvider() {
        THE_ONE.destroy();
        THE_ONE = null;
    }
}
