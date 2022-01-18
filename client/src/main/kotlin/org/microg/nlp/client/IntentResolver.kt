/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.client

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.util.Log

internal fun resolveIntent(context: Context, action: String): Intent? {
    val intent = Intent(action)

    val pm = context.packageManager
    var resolveInfos = pm.queryIntentServices(intent, 0)
    if (resolveInfos.size > 1) {

        // Restrict to self if possible
        val isSelf: (it: ResolveInfo) -> Boolean = {
            it.serviceInfo.packageName == context.packageName
        }
        if (resolveInfos.size > 1 && resolveInfos.any(isSelf)) {
            Log.d("IntentResolver", "Found more than one service for $action, restricted to own package " + context.packageName)
            resolveInfos = resolveInfos.filter(isSelf)
        }

        // Restrict to package with matching signature if possible
        val isSelfSig: (it: ResolveInfo) -> Boolean = {
            it.serviceInfo.packageName == context.packageName
        }
        if (resolveInfos.size > 1 && resolveInfos.any(isSelfSig)) {
            Log.d("IntentResolver", "Found more than one service for $action, restricted to related packages")
            resolveInfos = resolveInfos.filter(isSelfSig)
        }

        // Restrict to system if any package is system
        val isSystem: (it: ResolveInfo) -> Boolean = {
            (it.serviceInfo?.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM > 0
        }
        if (resolveInfos.size > 1 && resolveInfos.any(isSystem)) {
            Log.d("IntentResolver", "Found more than one service for $action, restricted to system packages")
            resolveInfos = resolveInfos.filter(isSystem)
        }

        val highestPriority: ResolveInfo? = resolveInfos.maxByOrNull { it.priority }
        intent.setPackage(highestPriority!!.serviceInfo.packageName)
        intent.setClassName(highestPriority.serviceInfo.packageName, highestPriority.serviceInfo.name)
        if (resolveInfos.size > 1) {
            Log.d("IntentResolver", "Found more than one service for $action, picked highest priority " + intent.component)
        }
        return intent
    } else if (!resolveInfos.isEmpty()) {
        intent.setPackage(resolveInfos[0].serviceInfo.packageName)
        return intent
    } else {
        Log.w("IntentResolver", "No service to bind to, your system does not support unified service")
        return null
    }
}
