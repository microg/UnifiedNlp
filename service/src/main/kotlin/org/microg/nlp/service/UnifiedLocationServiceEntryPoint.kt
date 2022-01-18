/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import java.io.FileDescriptor
import java.io.PrintWriter

@Deprecated("Use LocationService or GeocodeService")
class UnifiedLocationServiceEntryPoint : LifecycleService() {
    private var root: UnifiedLocationServiceRoot = UnifiedLocationServiceRoot(this, lifecycle)

    override fun onCreate() {
        singleton = this
        super.onCreate()
        Log.d(TAG, "onCreate")
        lifecycleScope.launchWhenStarted { root.reset() }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Log.d(TAG, "onBind: $intent")
        return root.asBinder()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        root.destroy()
        singleton = null
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        writer?.println("Singleton: ${singleton != null}")
        root.dump(writer)
    }

    companion object {
        private val TAG = "ULocService"
        private var singleton: UnifiedLocationServiceEntryPoint? = null

        fun reloadPreferences() {
            singleton?.root?.reloadPreferences()
        }
    }
}
