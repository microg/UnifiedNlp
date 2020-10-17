/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

import org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND
import org.microg.nlp.api.Constants.LOCATION_EXTRA_OTHER_BACKENDS
import java.util.concurrent.CopyOnWriteArrayList

class LocationFuser(private val context: Context, private val root: UnifiedLocationServiceRoot) {

    private val backendHelpers = CopyOnWriteArrayList<LocationBackendHelper>()
    private var fusing = false
    private var lastLocationReportTime: Long = 0

    suspend fun reset() {
        unbind()
        backendHelpers.clear()
        lastLocationReportTime = 0
        for (backend in Preferences(context).locationBackends) {
            Log.d(TAG, "Backend: $backend")
            val parts = backend.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            if (parts.size >= 2) {
                val intent = Intent(ACTION_LOCATION_BACKEND)
                intent.setPackage(parts[0])
                intent.setClassName(parts[0], parts[1])
                backendHelpers.add(LocationBackendHelper(context, this, root.coroutineScope, intent, if (parts.size >= 3) parts[2] else null))
            }
        }
    }

    suspend fun unbind() {
        for (handler in backendHelpers) {
            handler.unbind()
        }
    }

    fun bind() {
        fusing = false
        for (handler in backendHelpers) {
            handler.bind()
        }
    }

    suspend fun destroy() {
        unbind()
        backendHelpers.clear()
    }

    suspend fun update() {
        var hasUpdates = false
        fusing = true
        for (handler in backendHelpers) {
            if (handler.update() != null)
                hasUpdates = true
        }
        fusing = false
        if (hasUpdates)
            updateLocation()
    }

    fun updateLocation() {
        val locations = ArrayList<Location>()
        for (handler in backendHelpers) {
            handler.lastLocation?.let { locations.add(it) }
        }
        val location = mergeLocations(locations)
        if (location != null) {
            location.provider = LocationManager.NETWORK_PROVIDER
            if (lastLocationReportTime < location.time) {
                lastLocationReportTime = location.time
                Log.v(TAG, "Fused location: $location")
                root.reportLocation(location)
            } else {
                Log.v(TAG, "Ignoring location update as it's older than other provider.")
            }
        }
    }

    private fun mergeLocations(locations: List<Location>): Location? {
        Collections.sort(locations, LocationComparator.INSTANCE)
        if (locations.isEmpty()) return null
        if (locations.size == 1) return locations[0]
        val location = Location(locations[0])
        val backendResults = ArrayList<Location>()
        for (backendResult in locations) {
            if (locations[0] == backendResult) continue
            backendResults.add(backendResult)
        }
        if (!backendResults.isEmpty()) {
            location.extras.putParcelableArrayList(LOCATION_EXTRA_OTHER_BACKENDS, backendResults)
        }
        return location
    }

    fun reportLocation() {
        if (fusing)
            return
        updateLocation()
    }

    fun getLastLocationForBackend(packageName: String, className: String, signatureDigest: String?): Location? =
            backendHelpers.find {
                it.serviceIntent.`package` == packageName && it.serviceIntent.component?.className == className && (signatureDigest == null || it.signatureDigest == null || it.signatureDigest == signatureDigest)
            }?.lastLocation

    class LocationComparator : Comparator<Location> {

        /**
         * @return whether {@param lhs} is better than {@param rhs}
         */
        override fun compare(lhs: Location?, rhs: Location?): Int {
            if (lhs === rhs) return 0
            if (lhs == null) return 1
            if (rhs == null) return -1
            if (!lhs.hasAccuracy()) return 1
            if (!rhs.hasAccuracy()) return -1
            if (rhs.time > lhs.time + SWITCH_ON_FRESHNESS_CLIFF_MS) return 1
            if (lhs.time > rhs.time + SWITCH_ON_FRESHNESS_CLIFF_MS) return -1
            return (lhs.accuracy - rhs.accuracy).toInt()
        }

        companion object {
            val INSTANCE = LocationComparator()
            val SWITCH_ON_FRESHNESS_CLIFF_MS: Long = 30000 // 30 seconds
        }
    }

    companion object {
        private val TAG = "UnifiedLocation"
    }
}
