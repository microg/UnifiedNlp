/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.nlp.geocode.v1

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class GeocodeService : LifecycleService() {
    private lateinit var provider: GeocodeProvider

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating system service...")
        provider = GeocodeProvider(this, lifecycle)
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

    override fun onUnbind(intent: Intent): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        runBlocking { provider.disconnect() }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "GeocodeService"
    }
}
