/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import java.io.PrintWriter

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun <T> Array<out T>?.isNotNullOrEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

abstract class AbstractBackendHelper(private val TAG: String, private val context: Context, private val lifecycle: Lifecycle, val serviceIntent: Intent, val signatureDigest: String?) : ServiceConnection, LifecycleOwner {
    private var bound: Boolean = false

    protected abstract suspend fun close()

    override fun getLifecycle(): Lifecycle = lifecycle

    protected abstract fun hasBackend(): Boolean

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        bound = true
        Log.d(TAG, "Bound to: $name")
    }

    override fun onServiceDisconnected(name: ComponentName) {
        bound = false
        Log.d(TAG, "Unbound from: $name")
    }

    suspend fun unbind() {
        if (bound) {
            if (hasBackend()) {
                try {
                    close()
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
            unbindNow()
        }
    }

    fun unbindNow() {
        if (bound) {
            try {
                Log.d(TAG, "Unbinding from: $serviceIntent")
                context.unbindService(this)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }

            bound = false
        }
    }

    fun bind() {
        if (!bound) {
            Log.d(TAG, "Binding to: $serviceIntent sig: $signatureDigest")
            if (serviceIntent.getPackage() == null) {
                Log.w(TAG, "Intent is not properly resolved, can't verify signature. Aborting.")
                return
            }
            val computedDigest = firstSignatureDigest(context, serviceIntent.getPackage(), "SHA-256")
            if (signatureDigest != null && signatureDigest != computedDigest) {
                Log.w(TAG, "Target signature does not match selected package ($signatureDigest != $computedDigest). Aborting.")
                return
            }
            try {
                context.bindService(serviceIntent, this, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }

        }
    }

    fun dump(writer: PrintWriter?) {
        writer?.println("  ${javaClass.simpleName} $serviceIntent bound=$bound")
    }

    companion object {
        @Suppress("DEPRECATION")
        @SuppressLint("PackageManagerGetSignatures")
        fun firstSignatureDigest(context: Context, packageName: String?, algorithm: String): String? {
            val packageManager = context.packageManager
            val info: PackageInfo?
            try {
                info = packageManager.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
            } catch (e: PackageManager.NameNotFoundException) {
                return null
            }

            if (info?.signatures.isNotNullOrEmpty()) {
                for (sig in info.signatures) {
                    digest(sig.toByteArray(), algorithm)?.let { return it }
                }
            }
            return null
        }

        private fun digest(bytes: ByteArray, algorithm: String): String? {
            try {
                val md = MessageDigest.getInstance(algorithm)
                val digest = md.digest(bytes)
                val sb = StringBuilder(2 * digest.size)
                for (b in digest) {
                    sb.append(String.format("%02x", b))
                }
                return sb.toString()
            } catch (e: NoSuchAlgorithmException) {
                return null
            }
        }
    }

}
