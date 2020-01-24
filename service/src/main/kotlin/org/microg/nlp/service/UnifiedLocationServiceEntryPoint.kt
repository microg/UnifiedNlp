/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class UnifiedLocationServiceEntryPoint : Service() {
    private var root: UnifiedLocationServiceRoot? = null

    @Synchronized
    fun destroy() {
        if (root != null) {
            root!!.destroy()
            root = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        destroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind: $intent")
        synchronized(this) {
            if (root == null) {
                root = UnifiedLocationServiceRoot(this)
            }
            return root!!.asBinder()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        destroy()
    }

    companion object {
        private val TAG = "ULocService"
    }
}
