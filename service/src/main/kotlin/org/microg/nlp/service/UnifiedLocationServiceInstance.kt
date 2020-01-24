/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.location.Location
import android.os.Bundle
import android.os.RemoteException
import android.util.Log

import android.os.Binder.getCallingUid
import org.microg.nlp.client.UnifiedLocationClient

class UnifiedLocationServiceInstance(private val root: UnifiedLocationServiceRoot) : UnifiedLocationService.Default() {
    private var callback: LocationCallback? = null
    private var interval: Long = 0
    private var singleUpdatePending = false
    private val callingPackage = root.context.packageManager.getNameForUid(getCallingUid())

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

    @Throws(RemoteException::class)
    override fun registerLocationCallback(callback: LocationCallback, options: Bundle) {
        Log.d(TAG, "registerLocationCallback[$callingPackage]")
        this.callback = callback
    }

    override fun setUpdateInterval(interval: Long, options: Bundle) {
        Log.d(TAG, "setUpdateInterval[$callingPackage] interval: $interval")
        this.interval = interval
        root.updateLocationInterval()
    }

    override fun requestSingleUpdate(options: Bundle) {
        val lastLocation = root.lastReportedLocation
        if (lastLocation == null || lastLocation.time < System.currentTimeMillis() - UnifiedLocationServiceRoot.MAX_LOCATION_AGE || options.getBoolean(UnifiedLocationClient.KEY_FORCE_NEXT_UPDATE, false)) {
            Log.d(TAG, "requestSingleUpdate[$callingPackage] requesting new location")
            singleUpdatePending = true
            root.locationFuser.update()
            root.updateLocationInterval()
        } else {
            Log.d(TAG, "requestSingleUpdate[$callingPackage] using last location ")
            try {
                this.callback!!.onLocationUpdate(lastLocation)
            } catch (e: RemoteException) {
                root.onDisconnected(this)
                throw e
            }
        }
    }

    companion object {
        private val TAG = "ULocService"
    }
}
