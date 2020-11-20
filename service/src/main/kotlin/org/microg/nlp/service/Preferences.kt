/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.io.File


class Preferences(private val context: Context) {

    private val preferences: SharedPreferences
        get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val oldPreferences: SharedPreferences
        get() = context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    private val systemDefaultPreferences: SharedPreferences?
        get() = try {
            Context::class.java.getDeclaredMethod("getSharedPreferences", File::class.java, Int::class.javaPrimitiveType).invoke(context, File("/system/etc/microg.xml"), Context.MODE_PRIVATE) as SharedPreferences
        } catch (e: java.lang.Exception) {
            null
        }

    private fun SharedPreferences.getStringSetCompat(key: String, defValues: Set<String>? = null): Set<String>? {
        if (Build.VERSION.SDK_INT >= 11) {
            try {
                val res = getStringSet(key, null)
                if (res != null) return res.filter { it.isNotEmpty() }.toSet()
            } catch (ignored: Exception) {
                // Ignore
            }
        }
        try {
            val str = getString(key, null)
            if (str != null) return str.split("\\|".toRegex()).filter { it.isNotEmpty() }.toSet()
        } catch (ignored: Exception) {
            // Ignore
        }
        return defValues
    }

    private fun SharedPreferences.Editor.putStringSetCompat(key: String, values: Set<String>): SharedPreferences.Editor {
        return if (Build.VERSION.SDK_INT >= 11) {
            putStringSet(key, values.filter { it.isNotEmpty() }.toSet())
        } else {
            putString(key, values.filter { it.isNotEmpty() }.joinToString("|"))
        }
    }

    private fun getStringSetFromAny(key: String): Set<String>? {
        migratePreference(key)
        val fromNewSettings = preferences.getStringSetCompat(key)
        if (fromNewSettings != null) return fromNewSettings
        return systemDefaultPreferences?.getStringSetCompat(key)
    }

    private fun migratePreference(key: String): Set<String>? {
        val fromOldSettings = oldPreferences.getStringSetCompat(key)
        if (fromOldSettings != null) {
            var newSettings: MutableSet<String> = mutableSetOf<String>()
            newSettings.addAll(preferences.getStringSetCompat(key).orEmpty())
            for (oldBackend in fromOldSettings) {
                // Get package name and sha1
                val parts = oldBackend.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                if (parts.size < 3) continue // skip unsigned
                val pkgName = parts[0]
                val component = parts[1]
                val oldSig = parts[2]
                if (oldSig?.length != 40) continue // skip if not sha1
                // Get matching sha256
                val sha1 = AbstractBackendHelper.firstSignatureDigest(context, pkgName, "SHA-1")
                val sha256 = AbstractBackendHelper.firstSignatureDigest(context, pkgName, "SHA-256")
                // If the system sha1 matches what we had stored
                if (oldSig == sha1) {
                    // Replace it with the sha256
                    val newBackend = "${pkgName}/${component}/${sha256}"
                    newSettings.add(newBackend)
                }
            }
             if (preferences.edit().putStringSetCompat(key, newSettings.toSet()).commit()) {
                 // Only delete the old preference once committed.
                 oldPreferences.edit().remove(key).apply()
             }
        }
        return null
    }

    var locationBackends: Set<String>
        get() = getStringSetFromAny(PREF_LOCATION_BACKENDS) ?: emptySet()
        set(backends) {
            preferences.edit().putStringSetCompat(PREF_LOCATION_BACKENDS, backends).apply()
        }

    var geocoderBackends: Set<String>
        get() = getStringSetFromAny(PREF_GEOCODER_BACKENDS) ?: emptySet()
        set(backends) {
            preferences.edit().putStringSetCompat(PREF_GEOCODER_BACKENDS, backends).apply()
        }

    companion object {
        private const val PREFERENCES_NAME = "unified_nlp"
        private const val PREF_LOCATION_BACKENDS = "location_backends"
        private const val PREF_GEOCODER_BACKENDS = "geocoder_backends"
    }
}
