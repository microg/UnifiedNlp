/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui.model

interface BackendDetailsCallback {
    fun onEnabledChange(entry: BackendInfo?, newValue: Boolean)
    fun onAppClicked(entry: BackendInfo?)
    fun onAboutClicked(entry: BackendInfo?)
    fun onConfigureClicked(entry: BackendInfo?)
}
