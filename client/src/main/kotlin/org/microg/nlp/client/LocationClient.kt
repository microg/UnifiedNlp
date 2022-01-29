/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.client

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.nlp.service.api.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class LocationClient(context: Context, lifecycle: Lifecycle) : BaseClient<ILocationService>(context, lifecycle, { ILocationService.Stub.asInterface(it) }) {
    private val requests = hashSetOf<LocationRequest>()
    private val requestsMutex = Mutex(false)

    override val action: String
        get() = Constants.ACTION_LOCATION

    suspend fun getLastLocation(options: Bundle = defaultOptions): Location? = withService { service ->
        suspendCoroutine {
            service.getLastLocation(SingleLocationListener(it), options)
        }
    }

    suspend fun getLastLocationForBackend(componentName: ComponentName, options: Bundle = defaultOptions) = getLastLocationForBackend(componentName.packageName, componentName.className, null, options)

    suspend fun getLastLocationForBackend(packageName: String, className: String, signatureDigest: String? = null, options: Bundle = defaultOptions): Location? = withService { service ->
        suspendCoroutine {
            service.getLastLocationForBackend(packageName, className, signatureDigest, SingleLocationListener(it), options)
        }
    }

    private suspend fun <T> withRequestService(v: suspend (ILocationService) -> T): T {
        return requestsMutex.withLock {
            try {
                if (requests.isEmpty()) connect()
                withService(v)
            } finally {
                if (requests.isEmpty()) disconnect()
            }
        }
    }

    suspend fun updateLocationRequest(request: LocationRequest, options: Bundle = defaultOptions): Unit = withRequestService { service ->
        suspendCoroutine<Unit> {
            service.updateLocationRequest(request, StatusCallback(it), options)
        }
        requests.removeAll { it.id == request.id }
        requests.add(request)
    }

    suspend fun cancelLocationRequestByListener(listener: ILocationListener, options: Bundle = defaultOptions): Unit = withRequestService { service ->
        suspendCoroutine<Unit> {
            service.cancelLocationRequestByListener(listener, StatusCallback(it), options)
        }
        requests.removeAll { it.listener == listener }
    }

    suspend fun cancelLocationRequestById(id: String, options: Bundle = defaultOptions): Unit = withRequestService { service ->
        suspendCoroutine<Unit> {
            service.cancelLocationRequestById(id, StatusCallback(it), options)
        }
        requests.removeAll { it.id == id }
    }

    suspend fun forceLocationUpdate(options: Bundle = defaultOptions): Unit = withService { service ->
        suspendCoroutine {
            service.forceLocationUpdate(StatusCallback(it), options)
        }
    }

    suspend fun getLocationBackends(options: Bundle = defaultOptions): List<String> = withService { service ->
        suspendCoroutine {
            service.getLocationBackends(StringsCallback(it), options)
        }
    }

    suspend fun setLocationBackends(backends: List<String>, options: Bundle = defaultOptions): Unit = withService { service ->
        suspendCoroutine {
            service.setLocationBackends(backends, StatusCallback(it), options)
        }
    }
}

private class SingleLocationListener(private val continuation: Continuation<Location?>) : ILocationListener.Stub() {
    override fun onLocation(statusCode: Int, location: Location?) {
        if (statusCode == Constants.STATUS_OK) {
            continuation.resume(location)
        } else {
            continuation.resumeWithException(RuntimeException("Status: $statusCode"))
        }
    }
}
