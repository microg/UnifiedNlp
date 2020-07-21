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
import org.microg.nlp.client.UnifiedLocationClient
import org.microg.nlp.ui.model.BackendInfo
import org.microg.nlp.ui.model.BackendType
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


private fun Array<String>.without(entry: BackendInfo): Array<String> = filterNot { it == entry.unsignedComponent || it.startsWith("${entry.unsignedComponent}/") }.toTypedArray()

suspend fun BackendInfo.updateEnabled(fragment: Fragment, newValue: Boolean) {
    Log.d("USettings", "updateEnabled $signedComponent = $newValue")
    val success = try {
        if (newValue) enable(fragment) else disable(fragment)
    } catch (e: Exception) {
        false
    }
    enabled.set(if (success) newValue else false)
}

fun BackendInfo.fillDetails(context: Context) {
    appIcon.set(serviceInfo.loadIcon(context.packageManager))
    name.set(serviceInfo.loadLabel(context.packageManager).toString())
    appName.set(serviceInfo.applicationInfo.loadLabel(context.packageManager).toString())
    summary.set(serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_SUMMARY))
    aboutIntent.set(serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_ABOUT_ACTIVITY)?.let { createExternalIntent(serviceInfo.packageName, it) })
    settingsIntent.set(serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_SETTINGS_ACTIVITY)?.let { createExternalIntent(serviceInfo.packageName, it) })
    initIntent.set(serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_INIT_ACTIVITY)?.let { createExternalIntent(serviceInfo.packageName, it) })
}

fun BackendInfo.loadIntents(activity: AppCompatActivity) {
    if (aboutIntent.get() == null || settingsIntent.get() == null || initIntent.get() == null) {
        val intent = when (type) {
            BackendType.LOCATION -> Intent(ACTION_LOCATION_BACKEND)
            BackendType.GEOCODER -> Intent(ACTION_GEOCODER_BACKEND)
        }
        intent.setPackage(serviceInfo.packageName);
        intent.setClassName(serviceInfo.packageName, serviceInfo.name);
        activity.bindService(intent, object : ServiceConnection {

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                if (aboutIntent.get() == null) {
                    aboutIntent.set(when (type) {
                        BackendType.LOCATION -> LocationBackend.Stub.asInterface(service).aboutIntent
                        BackendType.GEOCODER -> GeocoderBackend.Stub.asInterface(service).aboutIntent
                    })
                }
                if (settingsIntent.get() == null) {
                    settingsIntent.set(when (type) {
                        BackendType.LOCATION -> LocationBackend.Stub.asInterface(service).settingsIntent
                        BackendType.GEOCODER -> GeocoderBackend.Stub.asInterface(service).settingsIntent
                    })
                }
                if (initIntent.get() == null) {
                    initIntent.set(when (type) {
                        BackendType.LOCATION -> LocationBackend.Stub.asInterface(service).initIntent
                        BackendType.GEOCODER -> GeocoderBackend.Stub.asInterface(service).initIntent
                    })
                }
                activity.unbindService(this)
                loaded.set(true)
            }

            override fun onServiceDisconnected(name: ComponentName) {}
        }, BIND_AUTO_CREATE)
    }
}

private fun createExternalIntent(packageName: String, activityName: String): Intent {
    val intent = Intent(Intent.ACTION_VIEW);
    intent.setPackage(packageName);
    intent.setClassName(packageName, activityName);
    return intent;
}

private suspend fun BackendInfo.enable(fragment: Fragment): Boolean {
    val initIntent = initIntent.get()
    val activity = fragment.requireActivity() as AppCompatActivity
    if (initIntent != null) {
        val success = fragment.startActivityForResultCode(initIntent) == RESULT_OK
        if (!success) {
            Log.w("USettings", "Failed to init backend $signedComponent")
            return false
        }
    }
    val client = UnifiedLocationClient[activity]
    when (type) {
        BackendType.LOCATION -> client.setLocationBackends(client.getLocationBackends().without(this) + signedComponent)
        BackendType.GEOCODER -> client.setGeocoderBackends(client.getGeocoderBackends().without(this) + signedComponent)
    }
    Log.w("USettings", "Enabled backend $signedComponent")
    return true
}

private suspend fun BackendInfo.disable(fragment: Fragment): Boolean {
    val client = UnifiedLocationClient[fragment.requireContext()]
    when (type) {
        BackendType.LOCATION -> client.setLocationBackends(client.getLocationBackends().without(this))
        BackendType.GEOCODER -> client.setGeocoderBackends(client.getGeocoderBackends().without(this))
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
