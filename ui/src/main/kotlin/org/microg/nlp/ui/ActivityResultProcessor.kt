/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import androidx.core.util.set
import androidx.fragment.app.Fragment
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val requestCodeCounter = AtomicInteger(1)
private val continuationMap = SparseArray<(Int, Intent?) -> Unit>()

fun Fragment.startActivityForResult(intent: Intent, options: Bundle? = null, callback: (Int, Intent?) -> Unit) {
    val requestCode = requestCodeCounter.getAndIncrement()
    continuationMap[requestCode] = callback
    startActivityForResult(intent, requestCode, options)
}

suspend fun Fragment.startActivityForResultCode(intent: Intent, options: Bundle? = null): Int = suspendCoroutine { continuation ->
    startActivityForResult(intent, options) { responseCode, _ ->
        continuation.resume(responseCode)
    }
}

fun handleActivityResult(requestCode: Int, responseCode: Int, data: Intent?) {
    Log.d("ActivityResultProc", "handleActivityResult: $requestCode, $responseCode")
    try {
        continuationMap[requestCode]?.let { it(responseCode, data) }
    } catch (e: Exception) {
        Log.w("ActivityResultProc", "Error while handling activity result", e)
    }
    continuationMap.remove(requestCode)
}