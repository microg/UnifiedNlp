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

class BackendInfo() {
    val enabled = ObservableBoolean()
    val appIcon = ObservableField<Drawable>()
    var name = ObservableField<String>()
    var appName = ObservableField<String>()
    var summary = ObservableField<String>()
    var type = ObservableField<BackendType>()

    val loaded = ObservableBoolean()

    val initIntent = ObservableField<Intent>()
    val aboutIntent = ObservableField<Intent>()
    val settingsIntent = ObservableField<Intent>()

    override fun equals(other: Any?): Boolean {
        return other is BackendInfo && other.name == name && other.enabled == enabled && other.appName == appName && other.summary == summary
    }
}

enum class BackendType { LOCATION, GEOCODER }
