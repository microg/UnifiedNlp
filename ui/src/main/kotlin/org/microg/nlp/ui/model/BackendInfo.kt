/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui.model

import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Drawable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableField

class BackendInfo(val serviceInfo: ServiceInfo, val type: BackendType, val firstSignatureDigest: String?) {
    val enabled = ObservableBoolean()
    val appIcon = ObservableField<Drawable>()
    val name = ObservableField<String>()
    val appName = ObservableField<String>()
    val summary = ObservableField<String>()

    val loaded = ObservableBoolean()

    val initIntent = ObservableField<Intent>()
    val aboutIntent = ObservableField<Intent>()
    val settingsIntent = ObservableField<Intent>()

    val unsignedComponent: String = "${serviceInfo.packageName}/${serviceInfo.name}"
    val signedComponent: String = "${serviceInfo.packageName}/${serviceInfo.name}/$firstSignatureDigest"

    override fun equals(other: Any?): Boolean {
        return other is BackendInfo && other.name == name && other.enabled == enabled && other.appName == appName && other.unsignedComponent == unsignedComponent && other.summary == summary
    }
}

enum class BackendType { LOCATION, GEOCODER }
