/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.microg.nlp.api.Constants
import org.microg.nlp.client.UnifiedLocationClient
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

private val TAG: String = "ULocUI"

class BackendInfo(val context: Context, val serviceInfo: ServiceInfo, val type: BackendType, val coroutineScope: CoroutineScope, enabledBackends: Array<String>) : BaseObservable() {
    val firstSignatureDigest = firstSignatureDigest(context, serviceInfo.packageName)
    val unsignedComponent: String = "${serviceInfo.packageName}/${serviceInfo.name}"
    val signedComponent: String = "${serviceInfo.packageName}/${serviceInfo.name}/$firstSignatureDigest"

    var enabled: Boolean = enabledBackends.contains(signedComponent) || enabledBackends.contains(unsignedComponent)
        @Bindable get
        set(value) {
            if (field == value) return
            field = value
            notifyPropertyChanged(BR.enabled)
            coroutineScope.launch {
                val client = UnifiedLocationClient[context]
                val withoutSelf = when (type) {
                    BackendType.LOCATION -> client.getLocationBackends()
                    BackendType.GEOCODER -> client.getGeocoderBackends()
                }.filterNot { it == unsignedComponent || it.startsWith("$unsignedComponent/") }.toTypedArray()
                val new = if (value) withoutSelf + signedComponent else withoutSelf
                try {
                    when (type) {
                        BackendType.LOCATION -> client.setLocationBackends(new)
                        BackendType.GEOCODER -> client.setGeocoderBackends(new)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to change backend state", e)
                    field = !value
                    notifyPropertyChanged(BR.enabled)
                }
            }
        }

    val appIcon: Drawable by lazy { serviceInfo.loadIcon(context.packageManager) }
    val name: CharSequence by lazy { serviceInfo.loadLabel(context.packageManager) }
    val appName: CharSequence by lazy { serviceInfo.applicationInfo.loadLabel(context.packageManager) }

    val backendSummary: String? by lazy { serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_SUMMARY) }
    val settingsActivity: String? by lazy { serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_SETTINGS_ACTIVITY) }
    val aboutActivity: String? by lazy { serviceInfo.metaData?.getString(Constants.METADATA_BACKEND_ABOUT_ACTIVITY) }

    override fun equals(other: Any?): Boolean {
        return other is BackendInfo && other.name == name && other.enabled == enabled && other.appName == appName && other.unsignedComponent == unsignedComponent && other.backendSummary == backendSummary
    }
}

enum class BackendType { LOCATION, GEOCODER }

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