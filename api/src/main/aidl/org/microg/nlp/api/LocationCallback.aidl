/*
 * SPDX-FileCopyrightText: 2013, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.api;

import android.location.Location;

interface LocationCallback {
    void report(in Location location);
}
