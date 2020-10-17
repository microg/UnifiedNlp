/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Context
import android.content.Intent
import android.location.Address
import kotlinx.coroutines.launch
import org.microg.nlp.api.Constants.ACTION_GEOCODER_BACKEND
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

class GeocodeFuser(private val context: Context, private val root: UnifiedLocationServiceRoot) {
    private val backendHelpers = CopyOnWriteArrayList<GeocodeBackendHelper>()

    suspend fun reset() {
        unbind()
        backendHelpers.clear()
        for (backend in Preferences(context).geocoderBackends) {
            val parts = backend.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size >= 2) {
                val intent = Intent(ACTION_GEOCODER_BACKEND)
                intent.setPackage(parts[0])
                intent.setClassName(parts[0], parts[1])
                backendHelpers.add(GeocodeBackendHelper(context, root.coroutineScope, intent, if (parts.size >= 3) parts[2] else null))
            }
        }
    }

    fun bind() {
        for (backendHelper in backendHelpers) {
            backendHelper.bind()
        }
    }

    suspend fun unbind() {
        for (backendHelper in backendHelpers) {
            backendHelper.unbind()
        }
    }

    suspend fun destroy() {
        unbind()
        backendHelpers.clear()
    }

    suspend fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int, locale: String): List<Address>? {
        if (backendHelpers.isEmpty())
            return null
        val result = ArrayList<Address>()
        for (backendHelper in backendHelpers) {
            val backendResult = backendHelper.getFromLocation(latitude, longitude, maxResults, locale)
            result.addAll(backendResult)
        }
        return result
    }

    suspend fun getFromLocationName(locationName: String, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String): List<Address>? {
        if (backendHelpers.isEmpty())
            return null
        val result = ArrayList<Address>()
        for (backendHelper in backendHelpers) {
            val backendResult = backendHelper.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale)
            result.addAll(backendResult)
        }
        return result
    }
}
