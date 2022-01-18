/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Locale;

public class GeocodeRequest extends AutoSafeParcelable {
    @Field(1)
    public String locationName;
    @Field(2)
    public LatLonBounds bounds;
    @Field(3)
    public int maxResults;
    @Field(4)
    public String locale;

    private GeocodeRequest() {
    }

    public GeocodeRequest(String locationName, LatLonBounds bounds) {
        this(locationName, bounds, 1);
    }

    public GeocodeRequest(String locationName, LatLonBounds bounds, int maxResults) {
        this(locationName, bounds, maxResults, Locale.getDefault());
    }

    public GeocodeRequest(String locationName, LatLonBounds bounds, int maxResults, Locale locale) {
        this(locationName, bounds, maxResults, locale.toString());
    }

    public GeocodeRequest(String locationName, LatLonBounds bounds, int maxResults, String locale) {
        this.locationName = locationName;
        this.bounds = bounds;
        this.maxResults = maxResults;
        this.locale = locale;
    }

    public static final Creator<GeocodeRequest> CREATOR = new AutoCreator<>(GeocodeRequest.class);
}
