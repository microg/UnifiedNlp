/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.microg.nlp.api.LocationBackendService
import org.microg.nlp.api.LocationCallback
import org.microg.nlp.backend.dejavu.BackendService
import org.microg.nlp.service.api.Constants
import java.io.PrintWriter
import java.util.concurrent.CopyOnWriteArrayList


private const val TAG = "LocationFuser"

val Location.isValid: Boolean
    get() {
        if (!latitude.isFinite() || latitude > 90 || latitude < -90) return false
        if (!longitude.isFinite() || longitude > 180 || longitude < -180) return false
        if (!accuracy.isFinite() || accuracy < 0) return false
        return true
    }

class LocationFuser(private val context: Context, private val lifecycle: Lifecycle, private val receiver: LocationReceiver) : LifecycleOwner {

    companion object {
        @JvmStatic
        public val backendHelpers = CopyOnWriteArrayList<LocationBackendHelper>()
    }

    private var fusing = false
    private var lastLocationReportTime: Long = 0

    suspend fun reset() {
        unbind()
        backendHelpers.clear()
        lastLocationReportTime = 0
        for (backend in Constants.LOCATION_BACKENDS) {
            Log.d(TAG, "reset: backend: $backend")
            LogToFile.appendLog(TAG, "reset: backend $backend");
            if (backend.equals("org.microg.nlp.backend.dejavu.BackendService")) {
                backendHelpers.add(LocationBackendHelper(context, this, lifecycle, BackendService(context), null))
            }
        }
        sendMessageToActivity()
    }

    private fun sendMessageToActivity() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(Intent("LocationGeocodeBackendUpdated"))
    }

    suspend fun unbind() {
        for (handler in backendHelpers) {
            handler.unbind()
        }
    }

    private fun unbindNow() {
        for (handler in backendHelpers) {
            handler.unbindNow()
        }
    }

    fun bind() {
        fusing = false
        for (handler in backendHelpers) {
            handler.bind()
        }
    }

    fun destroy() {
        unbindNow()
        backendHelpers.clear()
    }

    suspend fun update() {
        LogToFile.appendLog(TAG, "update2 start");
        var hasUpdates = false
        fusing = true
        for (handler in backendHelpers) {
            LogToFile.appendLog(TAG, "update handler $handler");
            if (handler.update() != null)
                hasUpdates = true
        }
        fusing = false
        if (hasUpdates)
            updateLocation()
    }

    fun updateLocation() {
        LogToFile.appendLog(TAG, "updateLocation start");
        val locations = ArrayList<Location>()
        for (handler in backendHelpers) {
            handler.lastLocation?.let { locations.add(it) }
        }
        val location = mergeLocations(locations)
        if (location != null && location.latitude.isFinite()) {
            location.provider = LocationManager.NETWORK_PROVIDER
            if (lastLocationReportTime < location.time) {
                lastLocationReportTime = location.time
                Log.v(TAG, "Fused location: $location")
                receiver.reportLocation(location)
            } else {
                Log.v(TAG, "Ignoring location update as it's older than other provider.")
            }
        }
    }

    private fun mergeLocations(locations: List<Location>): Location? {
        LogToFile.appendLog(TAG, "Merge locations: " + locations);

        val locations = locations.filter { it.isValid }.sortedWith(LocationComparator)
        if (locations.isEmpty()) return null
        if (locations.size == 1) return locations[0]
        val location = Location(locations[0])
        val backendResults = ArrayList<Location>()
        for (backendResult in locations) {
            if (locations[0] == backendResult) continue
            backendResults.add(backendResult)
        }
        if (backendResults.isNotEmpty()) {
            location.extras?.putParcelableArrayList(Constants.LOCATION_EXTRA_OTHER_BACKENDS, backendResults)
        }
        return location
    }

    fun reportLocation() {
        if (fusing)
            return
        updateLocation()
    }

    fun getLastLocationForBackend(packageName: String?, className: String?, signatureDigest: String?): Location? =
            backendHelpers.get(0)
                    /*.find {
                it.serviceIntent.`package` == packageName && it.serviceIntent.component?.className == className && (signatureDigest == null || it.signatureDigest == null || it.signatureDigest == signatureDigest)
            }?*/.lastLocation

    fun dump(writer: PrintWriter?) {
        writer?.println("${backendHelpers.size} backends:")
        for (helper in backendHelpers) {
            helper?.dump(writer)
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycle

    object LocationComparator : Comparator<Location> {
        val SWITCH_ON_FRESHNESS_CLIFF_MS: Long = 30000 // 30 seconds
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
    }
}


class LocationBackendHelper(context: Context, private val locationFuser: LocationFuser, lifecycle: Lifecycle, serviceIntent: LocationBackendService, signatureDigest: String?) : AbstractBackendHelper(TAG, context, lifecycle, serviceIntent, signatureDigest) {
    public var backend: AsyncLocationBackend = AsyncLocationBackend(serviceIntent, Callback(), serviceIntent.toString() + "-location-backend")

    private var updateWaiting: Boolean = false
    var lastLocation: Location? = null
        private set(location) {
            LogToFile.appendLog(TAG, "lastLocation setLocation $location");
            if (location == null || !location.hasAccuracy()) {
                return
            }
            var bundle = location.extras;
            if (bundle == null) {
                bundle = Bundle()
            }
            bundle?.putString(Constants.LOCATION_EXTRA_BACKEND_PROVIDER, location.provider)
            bundle?.putString(Constants.LOCATION_EXTRA_BACKEND_COMPONENT,
                    serviceIntent.toString())

            location.extras = bundle;
            location.provider = "network"
            if (!location.hasAccuracy()) {
                location.accuracy = 50000f
            }
            if (location.time <= 0) {
                location.time = System.currentTimeMillis()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                updateElapsedRealtimeNanos(location)
            }
            field = location
        }

    /**
     * Requests a location update from the backend.
     *
     * @return The location reported by the backend. This may be null if a backend cannot determine its
     * location, or if it is going to return a location asynchronously.
     */
    suspend fun update(): Location? {
        LogToFile.appendLog(TAG, "update start");
        var result: Location? = null
        if (backend != null) {
            try {
                LogToFile.appendLog(TAG, "update - backend not null");
                LogToFile.appendLog(TAG, "update - backend $backend");
                backend.open()
                LogToFile.appendLog(TAG, "update:" + backend!!.getInitIntent())
            } catch (e: java.lang.Exception) {
                LogToFile.appendLog(TAG, "exception update get initIntent", e)
            }
        }
        if (backend == null) {
            LogToFile.appendLog(TAG, "Not (yet) bound.");
            updateWaiting = true
        } else {
            updateWaiting = false
            try {
                LogToFile.appendLog(TAG, "update perform");
                result = backend?.update()
                if (result == null) {
                    LogToFile.appendLog(TAG, "Received no location from ${serviceIntent}");
                } else {
                    Log.d(TAG, "Received location from ${serviceIntent} with time ${result.time} (last was ${lastLocation?.time ?: 0})")
                    LogToFile.appendLog(TAG,"Received location from ${serviceIntent} with time ${result.time} (last was ${lastLocation?.time ?: 0})")
                            if (this.lastLocation == null || result.time > this.lastLocation!!.time) {
                        lastLocation = result
                        locationFuser.reportLocation()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                LogToFile.appendLog(TAG, e.message, e)
                unbind()
            }

        }
        return result
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private fun updateElapsedRealtimeNanos(location: Location) {
        if (location.elapsedRealtimeNanos <= 0) {
            location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    @Throws(RemoteException::class)
    public override suspend fun close() {
        Log.d(TAG, "Calling close")
        LogToFile.appendLog(TAG, "Calling close")
        backend!!.close()
    }

    public override fun hasBackend(): Boolean {
        return backend != null
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        LogToFile.appendLog(TAG, "onServiceDisconnected")
        //backend = null
    }

    override fun dump(writer: PrintWriter?) {
        super.dump(writer)
        writer?.println("    last location: ${lastLocation?.let { Location(it) }}")
    }

    private inner class Callback : LocationCallback.Stub() {
        override fun report(location: Location?) {
            LogToFile.appendLog(TAG, "report")
            val lastLocation = lastLocation
            if (location == null || lastLocation != null && location.time > 0 && location.time <= lastLocation.getTime())
                return
            this@LocationBackendHelper.lastLocation = location
            locationFuser.reportLocation()
        }
    }
}
