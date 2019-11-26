/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import org.microg.nlp.api.LocationCallback;
import android.content.Intent;
import android.location.Location;

interface LocationBackend {
    void open(LocationCallback callback);
    Location update();
    void close();
    Intent getInitIntent();
    Intent getSettingsIntent();
    Intent getAboutIntent();
    Location updateWithOptions(in Bundle options);
}
