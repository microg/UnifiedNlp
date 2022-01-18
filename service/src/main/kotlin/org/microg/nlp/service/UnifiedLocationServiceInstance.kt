/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Binder.getCallingPid
import android.os.Binder.getCallingUid
import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.lifecycleScope
import org.microg.nlp.client.UnifiedLocationClient.Companion.KEY_FORCE_NEXT_UPDATE
import org.microg.nlp.client.UnifiedLocationClient.Companion.KEY_OP_PACKAGE_NAME
import org.microg.nlp.client.UnifiedLocationClient.Companion.PERMISSION_SERVICE_ADMIN
import java.io.PrintWriter

@Deprecated("Use LocationService or GeocodeService")
class UnifiedLocationServiceInstance(private val root: UnifiedLocationServiceRoot) : UnifiedLocationService.Default() {
    private var callback: LocationCallback? = null
    private var interval: Long = 0
    private var singleUpdatePending = false
    val callingPackage = root.context.getCallingPackage()

    private var opPackage: String? = null
    private val debugPackageString: String?
        get() {
            if (opPackage == callingPackage || opPackage == null) return callingPackage
            return "$callingPackage for $opPackage"
        }

    private fun Context.getCallingPackage(): String? {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val callingPid = getCallingPid()
        if (manager != null && callingPid > 0) {
            manager.runningAppProcesses.find { it.pid == callingPid }?.pkgList?.singleOrNull()?.let { return it }
        }
        return packageManager.getPackagesForUid(getCallingUid())?.singleOrNull()
    }

    fun reportLocation(location: Location) {
        try {
            if (callback != null) {
                callback!!.onLocationUpdate(location)
            }
            if (singleUpdatePending) {
                singleUpdatePending = false
                root.updateLocationInterval()
            }
        } catch (e: RemoteException) {
            root.onDisconnected(this)
        }
    }

    fun getInterval(): Long {
        // TODO: Do not report interval if client should no longer receive
        return if (singleUpdatePending) UnifiedLocationServiceRoot.MIN_LOCATION_INTERVAL else interval
    }

    override fun registerLocationCallback(callback: LocationCallback, options: Bundle) {
        if (root.context.checkCallingPermission(PERMISSION_SERVICE_ADMIN) == PERMISSION_GRANTED && options.containsKey(KEY_OP_PACKAGE_NAME)) {
            opPackage = options.getString(KEY_OP_PACKAGE_NAME)
        }
        Log.d(TAG, "registerLocationCallback[$callingPackage]")
        this.callback = callback
    }

    override fun setUpdateInterval(interval: Long, options: Bundle) {
        if (root.context.checkCallingPermission(PERMISSION_SERVICE_ADMIN) == PERMISSION_GRANTED && options.containsKey(KEY_OP_PACKAGE_NAME)) {
            opPackage = options.getString(KEY_OP_PACKAGE_NAME)
        }
        Log.d(TAG, "setUpdateInterval[$debugPackageString] interval: $interval")
        this.interval = interval
        root.updateLocationInterval()
    }

    override fun requestSingleUpdate(options: Bundle) {
        if (root.context.checkCallingPermission(PERMISSION_SERVICE_ADMIN) == PERMISSION_GRANTED && options.containsKey(KEY_OP_PACKAGE_NAME)) {
            opPackage = options.getString(KEY_OP_PACKAGE_NAME)
        }
        val lastLocation = root.lastReportedLocation
        if (lastLocation == null || lastLocation.time < System.currentTimeMillis() - UnifiedLocationServiceRoot.MAX_LOCATION_AGE || options.getBoolean(KEY_FORCE_NEXT_UPDATE, false)) {
            Log.d(TAG, "requestSingleUpdate[$debugPackageString] requesting new location")
            singleUpdatePending = true
            root.lifecycleScope.launchWhenStarted {
                root.locationFuser.update()
                root.updateLocationInterval()
            }
        } else if (callback != null) {
            Log.d(TAG, "requestSingleUpdate[$debugPackageString] using last location ")
            try {
                this.callback!!.onLocationUpdate(lastLocation)
            } catch (e: RemoteException) {
                root.onDisconnected(this)
                throw e
            }
        }
    }

    fun dump(writer: PrintWriter?) {
        writer?.println("$debugPackageString: interval $interval, single $singleUpdatePending")
    }

    companion object {
        private val TAG = "ULocService"
    }
}
