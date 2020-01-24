/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service;

import android.location.Location;

interface LocationCallback {
    void onLocationUpdate(in Location location);
}
