/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.client

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_ABOVE_CLIENT
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.nlp.service.api.Constants
import org.microg.nlp.service.api.ILocationListener
import org.microg.nlp.service.api.IStatusCallback
import org.microg.nlp.service.api.IStringsCallback
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "BaseClient"

abstract class BaseClient<I>(val context: Context, private val lifecycle: Lifecycle, val asInterface: (IBinder) -> I) : LifecycleOwner {
    private val callbackThreads: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.SECONDS, LinkedBlockingQueue())
    private var persistedConnectionCounter = AtomicInteger(0)
    private val serviceConnectionMutex = Mutex()
    private var persistedServiceConnection: ContinuedServiceConnection? = null
    val defaultOptions = Bundle()

    abstract val action: String

    val intent: Intent?
        get() = resolveIntent(context, action)

    val isAvailable: Boolean
        get() = intent != null

    var packageName: String
        get() = defaultOptions.getString("packageName") ?: context.packageName
        set(value) = defaultOptions.putString("packageName", value)

    init {
        packageName = context.packageName
    }

    val isConnectedUnsafe: Boolean
        get() = persistedServiceConnection != null && persistedConnectionCounter.get() > 0

    suspend fun isConnected(): Boolean = serviceConnectionMutex.withLock {
        return persistedServiceConnection != null && persistedConnectionCounter.get() > 0
    }

    suspend fun connect() {
        serviceConnectionMutex.withLock {
            if (persistedServiceConnection == null) {
                val intent = intent ?: throw IllegalStateException("$action service is not available")
                persistedServiceConnection = suspendCoroutine<ContinuedServiceConnection> { continuation ->
                    context.bindService(intent, ContinuedServiceConnection(continuation), BIND_AUTO_CREATE or BIND_ABOVE_CLIENT)
                }
            }
            persistedConnectionCounter.incrementAndGet()
        }
    }

    suspend fun disconnect() {
        serviceConnectionMutex.withLock {
            if (persistedConnectionCounter.decrementAndGet() <= 0) {
                persistedServiceConnection?.let { context.unbindService(it) }
                persistedServiceConnection = null
                persistedConnectionCounter.set(0)
            }
        }
    }

    fun <T> withConnectedServiceSync(v:  (I) -> T): T {
        try {
            if (persistedConnectionCounter.incrementAndGet() <= 1) {
                throw IllegalStateException("Service not connected")
            }
            val service = persistedServiceConnection?.service ?: throw RuntimeException("No binder returned")
            return v(asInterface(service))
        } finally {
            persistedConnectionCounter.decrementAndGet()
        }
    }

    suspend fun <T> withService(v: suspend (I) -> T): T {
        connect()
        val service = persistedServiceConnection?.service ?: throw RuntimeException("No binder returned")
        return try {
            v(asInterface(service))
        } finally {
            disconnect()
        }
    }

    override fun getLifecycle(): Lifecycle = lifecycle
}

internal class ContinuedServiceConnection(private val continuation: Continuation<ContinuedServiceConnection>) : ServiceConnection {
    var service: IBinder? = null
    private var continued: Boolean = false

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        this.service = service
        if (!continued) {
            continued = true
            continuation.resume(this)
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        if (!continued) {
            continued = true
            continuation.resumeWithException(RuntimeException("Disconnected"))
        }
    }

    override fun onBindingDied(name: ComponentName?) {
        if (!continued) {
            continued = true
            continuation.resumeWithException(RuntimeException("Binding diead"))
        }
    }

    override fun onNullBinding(name: ComponentName?) {
        if (!continued) {
            continued = true
            continuation.resume(this)
        }
    }
}

internal class StringsCallback(private val continuation: Continuation<List<String>>) : IStringsCallback.Stub() {
    override fun onStrings(statusCode: Int, strings: MutableList<String>) {
        if (statusCode == Constants.STATUS_OK) {
            continuation.resume(strings)
        } else {
            continuation.resumeWithException(RuntimeException("Status: $statusCode"))
        }
    }
}

internal class StatusCallback(private val continuation: Continuation<Unit>) : IStatusCallback.Stub() {
    override fun onStatus(statusCode: Int) {
        if (statusCode == Constants.STATUS_OK) {
            continuation.resume(Unit)
        } else {
            continuation.resumeWithException(RuntimeException("Status: $statusCode"))
        }
    }
}
