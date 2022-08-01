/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.content.IntentFilter
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.microg.nlp.service.api.*
import org.microg.nlp.service.api.Constants.*
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max
import kotlin.math.min

private const val TAG = "LocationService"
private const val MIN_LOCATION_INTERVAL = 2500L

class LocationService : LifecycleService() {
    private lateinit var service: LocationServiceImpl

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating userspace service...")
        service = LocationServiceImpl(this, lifecycle)
        Log.d(TAG, "Created userspace service.")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return service.asBinder()
    }

    override fun onDestroy() {
        service.destroy()
        super.onDestroy()
        Log.d(TAG, "Destroyed")
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        service.dump(writer)
    }
}

interface LocationReceiver {
    fun reportLocation(location: Location)
}

class LocationRequestInternal(private var request: LocationRequest, private val extras: Bundle) {
    val id: String
        get() = request.id
    val callingUid: Int
        get() = extras.getInt("callingUid")
    val callingPid: Int
        get() = extras.getInt("callingPid")
    val callingPackage: String?
        get() = extras.getString("callingPackage")
    val packageName: String
        get() = extras.getString("packageName")!!
    val interval: Long
        get() = request.interval
    val numUpdates: Int
        get() = request.numUpdates
    var updatesDelivered: Int = 0
        private set
    val updatesPending: Int
        get() = (numUpdates - updatesDelivered).coerceAtLeast(0)
    val listener: ILocationListener
        get() = request.listener
    val source: String
        get() = extras.getString("source") ?: "<none>"

    fun report(context: Context, location: Location) {
        if (updatesPending <= 0) throw IllegalStateException("Not waiting for updates")
        if (context.checkPermission("android.permission.ACCESS_COARSE_LOCATION", callingPid, callingUid) != PERMISSION_GRANTED) throw SecurityException("No permission to access location")
        listener.onLocation(STATUS_OK, location)
        updatesDelivered++
    }

    fun matches(other: LocationRequestInternal): Boolean {
        if (id == other.id && callingPid == other.callingPid) return true
        return false
    }

    fun adopt(requestInternal: LocationRequestInternal) {
        updatesDelivered = 0
        request = requestInternal.request
        extras.putAll(requestInternal.extras)
    }
}

class LocationServiceImpl(private val context: Context, private val lifecycle: Lifecycle) : ILocationService.Stub(), LifecycleOwner, LocationReceiver {
    private val packageFilter: IntentFilter = IntentFilter().apply {
        addAction(ACTION_PACKAGE_CHANGED)
        addAction(ACTION_PACKAGE_REMOVED)
        addAction(ACTION_PACKAGE_REPLACED)
        addAction(ACTION_PACKAGE_RESTARTED)
        addDataScheme("package")
    }
    private val packageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Package updated, binding")
            fuser.bind()
        }
    }
    private val requests = arrayListOf<LocationRequestInternal>()
    private val fuser = LocationFuser(context, lifecycle, this)
    private var lastLocation: Location? = null
    private var interval: Long = 0
    private val timer: Timer = Timer("location-requests")
    private var timerTask: TimerTask? = null
    private var lastTime: Long = 0

    init {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "Preparing LocationFuser...")
            fuser.reset()
            fuser.bind()
            fuser.update()
            Log.d(TAG, "Finished preparing LocationFuser")
            context.registerReceiver(packageReceiver, packageFilter)
        }
    }

    private fun updateLocationInterval() {
        var interval: Long = Long.MAX_VALUE
        var requestNow = false
        synchronized(requests) {
            for (request in requests) {
                if (request.interval == 0L && request.updatesPending == 1) requestNow = true
                if (request.interval <= 0 || request.updatesPending <= 0) continue
                interval = min(interval, request.interval)
            }
        }
        interval = max(interval, MIN_LOCATION_INTERVAL)

        if (this.interval == interval) return
        this.interval = interval

        synchronized(timer) {
            timerTask?.cancel()
            timerTask = null

            if (interval < Long.MAX_VALUE) {
                Log.d(TAG, "Set merged location interval to $interval")

                val timerTask = object : TimerTask() {
                    override fun run() {
                        lifecycleScope.launchWhenStarted {
                            lastTime = SystemClock.elapsedRealtime()
                            fuser.update()
                            Log.d(TAG, "Triggered update")
                        }
                    }
                }
                val delay = if (requestNow) {
                    0
                } else {
                    (interval - (SystemClock.elapsedRealtime() - lastTime)).coerceIn(0, interval)
                }
                timer.scheduleAtFixedRate(timerTask, delay, interval)
                this.timerTask = timerTask
            } else {
                Log.d(TAG, "Disable location updates")
            }
        }
    }

    private fun getCallingPackage(): String? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val callingPid = getCallingPid()
        if (manager != null && callingPid > 0) {
            manager.runningAppProcesses.find { it.pid == callingPid }?.pkgList?.firstOrNull()?.let { return it }
        }
        return context.packageManager.getPackagesForUid(getCallingUid())?.firstOrNull()
    }

    private fun processOptions(options: Bundle?): Bundle {
        val options = options ?: Bundle()
        val callingPackage = getCallingPackage()
        options.putString("callingPackage", callingPackage)
        if (!options.containsKey("packageName")) {
            options.putString("packageName", callingPackage)
        } else if (context.checkCallingPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED && context.packageName != callingPackage) {
            val claimedPackageName = options.getString("packageName")
            if (context.packageManager.getPackagesForUid(getCallingUid())?.any { it == claimedPackageName } != true) {
                Log.d(TAG, "$callingPackage invalidly claimed package name $claimedPackageName, ignoring")
                options.putString("packageName", callingPackage)
            }
        }
        options.putInt("callingUid", getCallingUid())
        options.putInt("callingPid", getCallingPid())
        return options
    }

    private fun Bundle.checkPermission(permission: String): Int {
        return context.checkPermission(permission, getInt("callingPid"), getInt("callingUid"))
    }

    override fun getLastLocation(listener: ILocationListener?, options: Bundle?) {
        val extras = processOptions(options)
        if (listener == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("android.permission.ACCESS_COARSE_LOCATION") != PERMISSION_GRANTED)
                return@launchWhenStarted listener.onLocation(STATUS_PERMISSION_ERROR, null)
            listener.onLocation(STATUS_OK, lastLocation)
        }
    }

    override fun getLastLocationForBackend(packageName: String?, className: String?, signatureDigest: String?, listener: ILocationListener?, options: Bundle?) {
        val extras = processOptions(options)
        if (listener == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("android.permission.ACCESS_COARSE_LOCATION") != PERMISSION_GRANTED)
                return@launchWhenStarted listener.onLocation(STATUS_PERMISSION_ERROR, null)
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted listener.onLocation(STATUS_PERMISSION_ERROR, null)
            listener.onLocation(STATUS_OK, fuser.getLastLocationForBackend(packageName, className, signatureDigest))
        }
    }

    override fun updateLocationRequest(request: LocationRequest?, callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("android.permission.ACCESS_COARSE_LOCATION") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStatus(STATUS_PERMISSION_ERROR)
            if (request == null)
                return@launchWhenStarted callback.onStatus(STATUS_INVALID_ARGS)
            val requestInternal = LocationRequestInternal(request, extras)
            synchronized(requests) {
                requests.find { it.matches(requestInternal) }?.adopt(requestInternal) ?: requests.add(requestInternal)
            }
            updateLocationInterval()
            callback.onStatus(STATUS_OK)
        }
    }

    override fun cancelLocationRequestByListener(listener: ILocationListener?, callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (listener == null)
                return@launchWhenStarted callback.onStatus(STATUS_INVALID_ARGS)
            synchronized(requests) {
                requests.removeAll { it.listener == listener }
            }
            updateLocationInterval()
            callback.onStatus(STATUS_OK)
        }
    }

    override fun cancelLocationRequestById(id: String?, callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (id == null)
                return@launchWhenStarted callback.onStatus(STATUS_INVALID_ARGS)
            synchronized(requests) {
                requests.removeAll { it.id == id && it.callingPid == extras.getInt("callingPid") }
            }
            updateLocationInterval()
            callback.onStatus(STATUS_OK)
        }
    }

    override fun forceLocationUpdate(callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStatus(STATUS_PERMISSION_ERROR)
            fuser.update()
            callback.onStatus(STATUS_OK)
        }
    }

    override fun reloadPreferences(callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStatus(STATUS_PERMISSION_ERROR)
            fuser.reset()
            fuser.bind()
            callback.onStatus(STATUS_OK)
        }
    }

    override fun getLocationBackends(callback: IStringsCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStrings(STATUS_PERMISSION_ERROR, null)
            callback.onStrings(STATUS_OK, Preferences(context).locationBackends.toList())
        }
    }

    override fun setLocationBackends(backends: MutableList<String>?, callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStatus(STATUS_PERMISSION_ERROR)
            if (backends == null)
                return@launchWhenStarted callback.onStatus(STATUS_INVALID_ARGS)
            Preferences(context).locationBackends = backends.toSet()
            fuser.reset()
            fuser.bind()
            callback.onStatus(STATUS_OK)
        }
    }

    override fun reportLocation(location: Location) {
        val newLocation = Location(location)
        if (!newLocation.isValid) return
        this.lastLocation = newLocation
        val requestsToDelete = hashSetOf<LocationRequestInternal>()
        synchronized(requests) {
            for (request in ArrayList(requests)) {
                try {
                    request.report(context, newLocation)
                    if (request.updatesPending <= 0) requestsToDelete.add(request)
                } catch (e: Exception) {
                    Log.w(TAG, "Removing request due to error: ", e)
                    requestsToDelete.add(request)
                }
            }
            requests.removeAll(requestsToDelete)
        }
        updateLocationInterval()
    }

    fun dump(writer: PrintWriter?) {
        writer?.println("last location: $lastLocation")
        writer?.println("interval: $interval")
        synchronized(requests) {
            writer?.println("${requests.size} requests:")
            for (request in requests) {
                writer?.println("  ${request.id} package=${request.packageName} source=${request.source} interval=${request.interval} pending=${request.updatesPending}")
            }
        }
        fuser.dump(writer)
    }

    fun destroy() {
        context.unregisterReceiver(packageReceiver)
        fuser.destroy()
    }

    override fun getLifecycle(): Lifecycle = lifecycle
}
