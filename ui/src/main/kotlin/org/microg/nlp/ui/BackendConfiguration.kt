/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.Service.BIND_AUTO_CREATE
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.microg.nlp.api.Constants
import org.microg.nlp.api.Constants.ACTION_GEOCODER_BACKEND
import org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND
import org.microg.nlp.api.GeocoderBackend
import org.microg.nlp.api.LocationBackend
import org.microg.nlp.client.GeocodeClient
import org.microg.nlp.client.LocationClient
import org.microg.nlp.ui.model.BackendInfo
import org.microg.nlp.ui.model.BackendType
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private const val TAG = "USettings"

suspend fun BackendInfo.updateEnabled(fragment: Fragment, newValue: Boolean) {
    val success = try {
        if (newValue) enable(fragment) else disable(fragment)
    } catch (e: Exception) {
        false
    }
    enabled.set(if (success) newValue else false)
}

private suspend fun BackendInfo.enable(fragment: Fragment): Boolean {
    val initIntent = initIntent.get()
    val activity = fragment.requireActivity() as AppCompatActivity
    if (initIntent != null) {
        Log.d(TAG, "enable: $initIntent")
        val success = fragment.startActivityForResultCode(initIntent) == RESULT_OK
        if (!success) {
            Log.w("USettings", "Failed to init backend $initIntent")
            return false
        }
    }
    when(type.get()) {
        BackendType.LOCATION -> {
            val client = LocationClient(activity, activity.lifecycle)
            client.setLocationBackends(client.getLocationBackends())
        }
        BackendType.GEOCODER -> {
            val client = GeocodeClient(activity, activity.lifecycle)
            client.setGeocodeBackends(client.getGeocodeBackends())
        }
    }
    Log.w("USettings", "Enabled backend $initIntent")
    return true
}

private suspend fun BackendInfo.disable(fragment: Fragment): Boolean {
    val activity = fragment.requireActivity() as AppCompatActivity
    when(type.get()) {
        BackendType.LOCATION -> {
            val client = LocationClient(activity, activity.lifecycle)
            client.setLocationBackends(client.getLocationBackends())
        }
        BackendType.GEOCODER -> {
            val client = GeocodeClient(activity, activity.lifecycle)
            client.setGeocodeBackends(client.getGeocodeBackends())
        }
    }
    return true
}


@Suppress("DEPRECATION")
@SuppressLint("PackageManagerGetSignatures")
fun firstSignatureDigest(context: Context, packageName: String?): String? {
    val packageManager = context.packageManager
    val info: PackageInfo?
    try {
        info = packageManager.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
    } catch (e: PackageManager.NameNotFoundException) {
        return null
    }

    if (info?.signatures?.isNotEmpty() == true) {
        for (sig in info.signatures) {
            sha256sum(sig.toByteArray())?.let { return it }
        }
    }
    return null
}

private fun sha256sum(bytes: ByteArray): String? {
    try {
        val md = MessageDigest.getInstance("SHA-256")
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
