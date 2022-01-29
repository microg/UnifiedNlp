/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Locale;

public class ReverseGeocodeRequest extends AutoSafeParcelable {
    @Field(1)
    public LatLon location;
    @Field(2)
    public int maxResults;
    @Field(3)
    public String locale;

    private ReverseGeocodeRequest() {
    }

    public ReverseGeocodeRequest(LatLon location) {
        this(location, 1);
    }

    public ReverseGeocodeRequest(LatLon location, int maxResults) {
        this(location, maxResults, Locale.getDefault());
    }

    public ReverseGeocodeRequest(LatLon location, int maxResults, Locale locale) {
        this(location, maxResults, locale.toString());
    }

    public ReverseGeocodeRequest(LatLon location, int maxResults, String locale) {
        this.location = location;
        this.maxResults = maxResults;
        this.locale = locale;
    }

    public static final Creator<ReverseGeocodeRequest> CREATOR = new AutoCreator<>(ReverseGeocodeRequest.class);
}
