/*
 * SPDX-FileCopyrightText: 2022, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service.api;

import android.location.Address;
import org.microg.nlp.service.api.GeocodeRequest;
import org.microg.nlp.service.api.IAddressesCallback;
import org.microg.nlp.service.api.IStatusCallback;
import org.microg.nlp.service.api.IStringsCallback;
import org.microg.nlp.service.api.ReverseGeocodeRequest;

interface IGeocodeService {
    oneway void requestGeocode(in GeocodeRequest request, IAddressesCallback callback, in Bundle options) = 0;
    oneway void requestReverseGeocode(in ReverseGeocodeRequest request, IAddressesCallback callback, in Bundle options) = 1;
    List<Address> requestGeocodeSync(in GeocodeRequest request, in Bundle options) = 2;
    List<Address> requestReverseGeocodeSync(in ReverseGeocodeRequest request, in Bundle options) = 3;

    oneway void reloadPreferences(IStatusCallback callback, in Bundle options) = 20;
    oneway void getGeocodeBackends(IStringsCallback callback, in Bundle options) = 21;
    oneway void setGeocodeBackends(in List<String> backends, IStatusCallback callback, in Bundle options) = 22;
}
