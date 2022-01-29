/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.client

import android.content.Context
import android.location.Address
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import org.microg.nlp.service.api.*
import org.microg.nlp.service.api.Constants.STATUS_OK
import java.util.concurrent.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class GeocodeClient(context: Context, lifecycle: Lifecycle) : BaseClient<IGeocodeService>(context, lifecycle, { IGeocodeService.Stub.asInterface(it) }) {
    private val syncThreads: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.SECONDS, LinkedBlockingQueue())
    override val action: String
        get() = Constants.ACTION_GEOCODE

    fun requestGeocodeSync(request: GeocodeRequest, options: Bundle = defaultOptions): List<Address> = executeWithTimeout {
        withConnectedServiceSync { service ->
            service.requestGeocodeSync(request, options)
        }
    }

    suspend fun requestGeocode(request: GeocodeRequest, options: Bundle = defaultOptions): List<Address> = withService { service ->
        suspendCoroutine {
            service.requestGeocode(request, AddressesCallback(it), options)
        }
    }

    fun requestReverseGeocodeSync(request: ReverseGeocodeRequest, options: Bundle = defaultOptions): List<Address> = executeWithTimeout {
        withConnectedServiceSync { service ->
            service.requestReverseGeocodeSync(request, options)
        }
    }

    suspend fun requestReverseGeocode(request: ReverseGeocodeRequest, options: Bundle = defaultOptions): List<Address> = withService { service ->
        suspendCoroutine {
            service.requestReverseGeocode(request, AddressesCallback(it), options)
        }
    }

    suspend fun getGeocodeBackends(options: Bundle = defaultOptions): List<String> = withService { service ->
        suspendCoroutine {
            service.getGeocodeBackends(StringsCallback(it), options)
        }
    }

    suspend fun setGeocodeBackends(backends: List<String>, options: Bundle = defaultOptions): Unit = withService { service ->
        suspendCoroutine {
            service.setGeocodeBackends(backends, StatusCallback(it), options)
        }
    }

    private fun <T> executeWithTimeout(timeout: Long = CALL_TIMEOUT, action: () -> T): T {
        var result: T? = null
        val latch = CountDownLatch(1)
        var err: Exception? = null
        syncThreads.execute {
            try {
                result = action()
            } catch (e: Exception) {
                err = e
            } finally {
                latch.countDown()
            }
        }
        if (!latch.await(timeout, TimeUnit.MILLISECONDS))
            throw TimeoutException()
        err?.let { throw it }
        return result ?: throw NullPointerException()
    }

    companion object {
        private const val CALL_TIMEOUT = 10000L
    }
}

private class AddressesCallback(private val continuation: Continuation<List<Address>>) : IAddressesCallback.Stub() {
    override fun onAddresses(statusCode: Int, addresses: List<Address>?) {
        if (statusCode == STATUS_OK) {
            continuation.resume(addresses.orEmpty())
        } else {
            continuation.resumeWithException(RuntimeException("Status: $statusCode"))
        }
    }
}
