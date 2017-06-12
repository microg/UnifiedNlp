/*
 * Copyright (C) 2013-2017 microG Project Team
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

public class LocationServiceV2 extends AbstractLocationService {
    private static LocationProviderV2 THE_ONE;

    public LocationServiceV2() {
        super("NlpLocationServiceV2");
    }

    @Override
    protected synchronized LocationProvider getProvider() {
        if (THE_ONE == null) {
            THE_ONE = new LocationProviderV2(this);
        }
        return THE_ONE;
    }

    @Override
    protected void destroyProvider() {
        THE_ONE.destroy();
        THE_ONE = null;
    }
}
