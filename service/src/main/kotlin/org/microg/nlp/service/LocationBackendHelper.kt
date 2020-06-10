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
import android.os.*
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_COMPONENT
import org.microg.nlp.api.Constants.LOCATION_EXTRA_BACKEND_PROVIDER
import org.microg.nlp.api.LocationCallback

class LocationBackendHelper(context: Context, private val locationFuser: LocationFuser, coroutineScope: CoroutineScope, serviceIntent: Intent, signatureDigest: String?) : AbstractBackendHelper(TAG, context, coroutineScope, serviceIntent, signatureDigest) {
    private val callback = Callback()
    private var backend: AsyncLocationBackend? = null
    private var updateWaiting: Boolean = false
    var lastLocation: Location? = null
        private set(location) {
            if (location == null || !location.hasAccuracy()) {
                return
            }
            if (location.extras == null) {
                location.extras = Bundle()
            }
            location.extras.putString(LOCATION_EXTRA_BACKEND_PROVIDER, location.provider)
            location.extras.putString(LOCATION_EXTRA_BACKEND_COMPONENT,
                    serviceIntent.component!!.flattenToShortString())
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
        var result: Location? = null
        if (backend == null) {
            Log.d(TAG, "Not (yet) bound.")
            updateWaiting = true
        } else {
            updateWaiting = false
            try {
                result = backend?.update()
                if (result == null) {
                    Log.d(TAG, "Received no location from ${serviceIntent.component!!.flattenToShortString()}")
                } else {
                    Log.d(TAG, "Received location from ${serviceIntent.component!!.flattenToShortString()} with time ${result.time} (last was ${lastLocation?.time ?: 0})")
                    if (this.lastLocation == null || result.time > this.lastLocation!!.time) {
                        lastLocation = result
                        locationFuser.reportLocation()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
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
        backend!!.close()
    }

    public override fun hasBackend(): Boolean {
        return backend != null
    }

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        super.onServiceConnected(name, service)
        backend = AsyncLocationBackend(service, name.toShortString() + "-location-backend")
        coroutineScope.launch {
            try {
                Log.d(TAG, "Calling open")
                backend!!.open(callback)
                if (updateWaiting) {
                    update()
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                unbind()
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName) {
        super.onServiceDisconnected(name)
        backend = null
    }

    private inner class Callback : LocationCallback.Stub() {
        override fun report(location: Location?) {
            val lastLocation = lastLocation
            if (location == null || lastLocation != null && location.time > 0 && location.time <= lastLocation.getTime())
                return
            this@LocationBackendHelper.lastLocation = location
            locationFuser.reportLocation()
        }
    }

    companion object {
        private val TAG = "UnifiedLocation"
    }
}
