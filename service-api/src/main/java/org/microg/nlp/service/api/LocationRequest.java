/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import org.microg.safeparcel.AutoSafeParcelable;

import java.util.UUID;

public class LocationRequest extends AutoSafeParcelable {
    @Field(1)
    public ILocationListener listener;
    @Field(2)
    public long interval;
    @Field(3)
    public int numUpdates;
    @Field(4)
    public String id;

    private LocationRequest() {
    }

    public LocationRequest(ILocationListener listener, long interval) {
        this(listener, interval, Integer.MAX_VALUE, UUID.randomUUID().toString());
    }

    public LocationRequest(ILocationListener listener, long interval, int numUpdates) {
        this(listener, interval, numUpdates, UUID.randomUUID().toString());
    }

    public LocationRequest(ILocationListener listener, long interval, int numUpdates, String id) {
        this.listener = listener;
        this.interval = interval;
        this.numUpdates = numUpdates;
        this.id = id;
    }

    public static final Creator<LocationRequest> CREATOR = new AutoCreator<>(LocationRequest.class);
}
