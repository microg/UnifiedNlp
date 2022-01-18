/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import org.microg.safeparcel.AutoSafeParcelable;

public class LatLonBounds extends AutoSafeParcelable {
    @Field(1)
    public LatLon lowerLeft;
    @Field(2)
    public LatLon upperRight;

    public LatLonBounds(LatLon lowerLeft, LatLon upperRight) {
        this.lowerLeft = lowerLeft;
        this.upperRight = upperRight;
    }

    public static final Creator<LatLonBounds> CREATOR = new AutoCreator<>(LatLonBounds.class);
}
