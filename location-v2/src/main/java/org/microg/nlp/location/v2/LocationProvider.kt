/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.nlp.location.v2

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationProvider
import android.os.Bundle
import android.os.SystemClock
import android.os.WorkSource
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.android.location.provider.LocationProviderBase
import com.android.location.provider.ProviderPropertiesUnbundled
import com.android.location.provider.ProviderRequestUnbundled
import kotlinx.coroutines.launch
import org.microg.nlp.client.LocationClient
import org.microg.nlp.service.api.Constants.STATUS_OK
import org.microg.nlp.service.api.ILocationListener
import org.microg.nlp.service.api.LocationRequest
import java.io.FileDescriptor
import java.io.PrintWriter
import java.util.*

class LocationProvider(private val context: Context, private val lifecycle: Lifecycle) : LocationProviderBase(TAG, ProviderPropertiesUnbundled.create(false, false, false, false, true, true, true, Criteria.POWER_LOW, Criteria.ACCURACY_COARSE)), LifecycleOwner {
    private val client: LocationClient = LocationClient(context, lifecycle)
    private val id = UUID.randomUUID().toString()
    private var statusUpdateTime = SystemClock.elapsedRealtime()
    private val listener = object : ILocationListener.Stub() {
        override fun onLocation(statusCode: Int, location: Location?) {
            if (statusCode == STATUS_OK && location != null) {
                val reportableLocation = Location(location)
                for (key in reportableLocation.extras.keySet().toList()) {
                    if (key?.startsWith("org.microg.nlp.") == true) {
                        reportableLocation.extras.remove(key)
                    }
                }
                Log.d(TAG, "reportLocation: $reportableLocation")
                reportLocation(reportableLocation)
            }
        }
    }
    private var opPackageName: String? = null
    private var opPackageNames: Set<String> = emptySet()
    private var autoTime = Long.MAX_VALUE
    private var autoUpdate = false

    init {
        client.defaultOptions.putString("source", "LocationProvider")
        client.defaultOptions.putString("requestId", id)
    }

    override fun onEnable() {
        Log.d(TAG, "onEnable")
        statusUpdateTime = SystemClock.elapsedRealtime()
    }

    override fun onDisable() {
        Log.d(TAG, "onDisable")
        unsetRequest()
        statusUpdateTime = SystemClock.elapsedRealtime()
    }

    override fun onSetRequest(requests: ProviderRequestUnbundled, source: WorkSource) {
        Log.v(TAG, "onSetRequest: $requests by $source")
        opPackageName = null
        try {
            val namesField = WorkSource::class.java.getDeclaredField("mNames")
            namesField.isAccessible = true
            val names = namesField[source] as Array<String>
            if (names != null) {
                opPackageNames = setOfNotNull(*names)
                for (name in names) {
                    if (!EXCLUDED_PACKAGES.contains(name)) {
                        opPackageName = name
                        break
                    }
                }
                if (opPackageName == null && names.isNotEmpty()) opPackageName = names[0]
            } else {
                opPackageNames = emptySet()
            }
        } catch (ignored: Exception) {
        }
        autoTime = requests.interval.coerceAtLeast(FASTEST_REFRESH_INTERVAL)
        autoUpdate = requests.reportLocation
        Log.v(TAG, "using autoUpdate=$autoUpdate autoTime=$autoTime")
        lifecycleScope.launch {
            updateRequest()
        }
    }

    suspend fun updateRequest() {
        if (client.isConnected()) {
            if (autoUpdate) {
                client.packageName = opPackageName ?: context.packageName
                client.updateLocationRequest(LocationRequest(listener, autoTime, Int.MAX_VALUE, id))
            } else {
                client.cancelLocationRequestById(id)
            }
        }
    }

    fun unsetRequest() {
        lifecycleScope.launch {
            client.cancelLocationRequestById(id)
        }
    }

    override fun onGetStatus(extras: Bundle?): Int {
        return LocationProvider.AVAILABLE
    }

    override fun onGetStatusUpdateTime(): Long {
        return statusUpdateTime
    }

    override fun onSendExtraCommand(command: String?, extras: Bundle?): Boolean {
        Log.d(TAG, "onSendExtraCommand: $command, $extras")
        return false
    }

    override fun onDump(fd: FileDescriptor?, pw: PrintWriter?, args: Array<out String>?) {
        dump(pw)
    }

    fun dump(writer: PrintWriter?) {
        writer?.println("ID: $id")
        writer?.println("connected: ${client.isConnectedUnsafe}")
        writer?.println("active: $autoUpdate")
        writer?.println("interval: $autoTime")
        writer?.println("${opPackageNames.size} sources:")
        for (packageName in opPackageNames) {
            writer?.println("  $packageName")
        }
    }

    suspend fun connect() {
        Log.d(TAG, "Connecting to userspace service...")
        client.connect()
        updateRequest()
        Log.d(TAG, "Connected to userspace service.")
    }
    suspend fun disconnect() = client.disconnect()

    override fun getLifecycle(): Lifecycle = lifecycle

    companion object {
        private val EXCLUDED_PACKAGES = listOf("android", "com.android.location.fused", "com.google.android.gms")
        private const val FASTEST_REFRESH_INTERVAL: Long = 2500
        private const val TAG = "LocationProvider"
    }
}
