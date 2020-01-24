/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service;

import android.location.Location;

interface AddressCallback {
    void onResult(in List<Address> location);
}
