/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.nlp.location.v2

import androidx.lifecycle.LifecycleService
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.FileDescriptor
import java.io.PrintWriter

open class LocationService : LifecycleService() {
    private lateinit var provider: LocationProvider

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating system service...")
        provider = LocationProvider(this, lifecycle)
        lifecycleScope.launchWhenStarted {
            delay(5000)
            provider.connect()
        }
        Log.d(TAG, "Created system service.")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind: $intent")
        return provider.binder
    }

    override fun onDestroy() {
        runBlocking { provider.disconnect() }
        super.onDestroy()
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        if (!this::provider.isInitialized) {
            writer?.println("Not yet initialized")
        }
        provider.dump(writer)
    }

    companion object {
        private const val TAG = "LocationService"
    }
}
