/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.util.Log

class PackageChangedReceiver : BroadcastReceiver() {

    private fun isProtectedAction(action: String) = when (action) {
        ACTION_PACKAGE_CHANGED, ACTION_PACKAGE_REMOVED, ACTION_PACKAGE_REPLACED, ACTION_PACKAGE_RESTARTED -> true
        else -> false
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Intent received: $intent")
        if (intent.action?.let { isProtectedAction(it) } != true) return

        val packageName = intent.data!!.schemeSpecificPart
        val preferences = Preferences(context)
        for (backend in preferences.locationBackends) {
            if (backend.startsWith("$packageName/")) {
                Log.d(TAG, "Reloading location service for $packageName")
                UnifiedLocationServiceEntryPoint.reloadPreferences()
                return
            }
        }
        for (backend in preferences.geocoderBackends) {
            if (backend.startsWith("$packageName/")) {
                Log.d(TAG, "Reloading geocoding service for $packageName")
                UnifiedLocationServiceEntryPoint.reloadPreferences()
                return
            }
        }
    }

    companion object {
        private const val TAG = "UnifiedService"
    }
}
