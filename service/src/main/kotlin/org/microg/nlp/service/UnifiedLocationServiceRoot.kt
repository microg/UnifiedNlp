/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.location.Address
import android.location.Location
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.microg.nlp.client.UnifiedLocationClient.Companion.PERMISSION_SERVICE_ADMIN
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.math.max
import kotlin.math.min

class UnifiedLocationServiceRoot(private val service: UnifiedLocationServiceEntryPoint, val coroutineScope: CoroutineScope) : UnifiedLocationService.Stub() {
    private val instances = HashMap<Int, UnifiedLocationServiceInstance>()
    private val geocoderThreads: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val timer: Timer = Timer("location-requests")
    private var timerTask: TimerTask? = null
    private var lastTime: Long = 0
    val locationFuser: LocationFuser = LocationFuser(service, this)
    val geocodeFuser: GeocodeFuser = GeocodeFuser(service, this)
    var lastReportedLocation: Location? = null
        private set
    private var interval: Long = 0

    val context: Context
        get() = service

    init {
        coroutineScope.launch { reset() }
    }

    val instance: UnifiedLocationServiceInstance
        @Synchronized get() {
            checkLocationPermission()
            val instance = instances[Binder.getCallingPid()] ?: UnifiedLocationServiceInstance(this)
            instances[Binder.getCallingPid()] = instance
            return instance
        }

    @Synchronized
    fun reportLocation(location: Location) {
        for (instance in ArrayList(instances.values)) {
            instance.reportLocation(location)
        }
        lastReportedLocation = location
    }

    @Synchronized
    fun onDisconnected(instance: UnifiedLocationServiceInstance) {
        var instancePid: Int? = null
        for (pid in instances.keys) {
            if (instances[pid] === instance) instancePid = pid
        }
        if (instancePid != null) {
            instances.remove(instancePid)
        }
    }

    fun destroy() {
        coroutineScope.launch {
            locationFuser.destroy()
            geocodeFuser.destroy()
        }
    }

    @Synchronized
    fun updateLocationInterval() {
        var interval: Long = Long.MAX_VALUE
        val sb = StringBuilder()
        for (instance in ArrayList(instances.values)) {
            val implInterval = instance.getInterval()
            if (implInterval <= 0) continue
            interval = min(interval, implInterval)
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append("${instance.callingPackage}:${implInterval}ms")
        }
        interval = max(interval, MIN_LOCATION_INTERVAL)

        if (this.interval == interval) return
        this.interval = interval

        timerTask?.cancel()
        timerTask = null

        if (interval < Long.MAX_VALUE) {
            Log.d(TAG, "Set merged location interval to $interval ($sb)")

            val timerTask = object : TimerTask() {
                override fun run() {
                    coroutineScope.launch {
                        lastTime = System.currentTimeMillis()
                        locationFuser.update()
                    }
                }
            }
            timer.scheduleAtFixedRate(timerTask, min(interval, max(0, interval - (System.currentTimeMillis() - lastTime))), interval)
            this.timerTask = timerTask
        } else {
            Log.d(TAG, "Disable location updates")
        }
    }

    private fun checkLocationPermission() {
        if (Binder.getCallingUid() == Process.myUid()) return // Always except self
        service.enforceCallingPermission(ACCESS_COARSE_LOCATION, "coarse location permission required")
    }

    private fun checkAdminPermission() {
        if (Binder.getCallingUid() == Process.myUid()) return // Always except self
        service.enforceCallingPermission(PERMISSION_SERVICE_ADMIN, "coarse location permission required")
    }

    override fun registerLocationCallback(callback: LocationCallback, options: Bundle) {
        instance.registerLocationCallback(callback, options)
    }

    override fun setUpdateInterval(interval: Long, options: Bundle) {
        instance.setUpdateInterval(interval, options)
    }

    override fun requestSingleUpdate(options: Bundle) {
        instance.requestSingleUpdate(options)
    }

    override fun getFromLocationWithOptions(latitude: Double, longitude: Double, maxResults: Int, locale: String, options: Bundle, callback: AddressCallback) {
        coroutineScope.launch {
            val res = try {
                geocodeFuser.getFromLocation(latitude, longitude, maxResults, locale).orEmpty()
            } catch (e: Exception) {
                Log.w(TAG, e)
                emptyList<Address>()
            }
            try {
                callback.onResult(res)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun getFromLocationNameWithOptions(locationName: String, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String, options: Bundle, callback: AddressCallback) {
        coroutineScope.launch {
            val res = try {
                geocodeFuser.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale).orEmpty()
            } catch (e: Exception) {
                Log.w(TAG, e)
                emptyList<Address>()
            }
            try {
                callback.onResult(res)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    private fun <E> Set<E>.contentEquals(other: Set<E>): Boolean {
        if (other.size != size) return false
        return containsAll(other)
    }

    override fun getLocationBackends(): Array<String> {
        return Preferences(service).locationBackends.toTypedArray()
    }

    override fun setLocationBackends(backends: Array<String>) {
        checkAdminPermission();
        if (Preferences(service).locationBackends.contentEquals(backends.toSet())) return
        Preferences(service).locationBackends = backends.toSet()
        reloadPreferences()
    }

    override fun getGeocoderBackends(): Array<String> {
        return Preferences(service).geocoderBackends.toTypedArray()
    }

    override fun setGeocoderBackends(backends: Array<String>) {
        checkAdminPermission();
        if (Preferences(service).geocoderBackends.contentEquals(backends.toSet())) return
        Preferences(service).geocoderBackends = backends.toSet()
        reloadPreferences()
    }

    override fun reloadPreferences() {
        checkAdminPermission();
        coroutineScope.launch {
            reset()
        }
    }

    override fun getLastLocation(): Location? {
        checkLocationPermission()
        return lastReportedLocation
    }

    override fun getLastLocationForBackend(packageName: String, className: String, signatureDigest: String?): Location? {
        checkLocationPermission()
        return locationFuser.getLastLocationForBackend(packageName, className, signatureDigest)
    }

    @Synchronized
    suspend fun reset() {
        locationFuser.reset()
        locationFuser.bind()
        geocodeFuser.reset()
        geocodeFuser.bind()
    }

    companion object {
        private val TAG = "ULocService"
        val MIN_LOCATION_INTERVAL = 2500L
        val MAX_LOCATION_AGE = 3600000L
    }
}
