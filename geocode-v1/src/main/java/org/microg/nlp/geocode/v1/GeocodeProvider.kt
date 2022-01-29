/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.nlp.geocode.v1

import android.content.Context
import android.location.Address
import android.location.GeocoderParams
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.android.location.provider.GeocodeProvider
import org.microg.nlp.client.GeocodeClient
import org.microg.nlp.service.api.GeocodeRequest
import org.microg.nlp.service.api.LatLon
import org.microg.nlp.service.api.LatLonBounds
import org.microg.nlp.service.api.ReverseGeocodeRequest

class GeocodeProvider(context: Context, lifecycle: Lifecycle) : GeocodeProvider() {
    private val client: GeocodeClient = GeocodeClient(context, lifecycle)

    override fun onGetFromLocation(latitude: Double, longitude: Double, maxResults: Int, params: GeocoderParams, addrs: MutableList<Address>): String? {
        return try {
            handleResult(addrs, client.requestReverseGeocodeSync(
                    ReverseGeocodeRequest(LatLon(latitude, longitude), maxResults, params.locale),
                    Bundle().apply { putString("packageName", params.clientPackage) }
            ))
        } catch (e: Exception) {
            Log.w(TAG, e)
            e.message
        }
    }

    override fun onGetFromLocationName(locationName: String?, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, maxResults: Int, params: GeocoderParams, addrs: MutableList<Address>): String? {
        return try {
            handleResult(addrs, client.requestGeocodeSync(
                    GeocodeRequest(locationName!!, LatLonBounds(LatLon(lowerLeftLatitude, lowerLeftLongitude), LatLon(upperRightLatitude, upperRightLongitude)), maxResults, params.locale),
                    Bundle().apply { putString("packageName", params.clientPackage) }
            ))
        } catch (e: Exception) {
            Log.w(TAG, e)
            e.message
        }
    }

    private fun handleResult(realResult: MutableList<Address>, fuserResult: List<Address>): String? {
        return if (fuserResult.isEmpty()) {
            "no result"
        } else {
            realResult.addAll(fuserResult)
            null
        }
    }

    suspend fun connect() = client.connect()
    suspend fun disconnect() = client.disconnect()

    companion object {
        private const val TAG = "GeocodeProvider"
        private const val TIMEOUT: Long = 10000
    }
}
