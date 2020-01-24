/*
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Context
import android.content.SharedPreferences

class Preferences(private val context: Context) {

    private val sharedPreferences: SharedPreferences
        get() = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    var locationBackends: Array<String>
        get() = splitBackendString(sharedPreferences.getString(PREF_LOCATION_BACKENDS, null))
        set(backends) {
            sharedPreferences.edit().putString(PREF_LOCATION_BACKENDS, backends.joinToString("|")).apply()
        }

    var geocoderBackends: Array<String>
        get() = splitBackendString(sharedPreferences.getString(PREF_GEOCODER_BACKENDS, null))
        set(backends) {
            sharedPreferences.edit().putString(PREF_GEOCODER_BACKENDS, backends.joinToString("|")).apply()
        }

    companion object {
        private val PREFERENCES_NAME = "unified_nlp"
        private val PREF_LOCATION_BACKENDS = "location_backends"
        private val PREF_GEOCODER_BACKENDS = "geocoder_backends"

        private fun splitBackendString(backendString: String?): Array<String> {
            return backendString?.split("\\|".toRegex())?.dropLastWhile(String::isEmpty)?.toTypedArray()
                    ?: emptyArray()
        }
    }
}
