/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui.model

interface BackendListEntryCallback {
    fun onEnabledChange(entry: BackendInfo?, newValue: Boolean)
    fun onOpenDetails(entry: BackendInfo?)
}
