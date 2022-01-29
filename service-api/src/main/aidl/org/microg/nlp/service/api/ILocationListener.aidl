/*
 * SPDX-FileCopyrightText: 2022, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import android.location.Location;

interface ILocationListener {
    oneway void onLocation(int statusCode, in Location location);
}
