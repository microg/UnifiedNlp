/*
 * SPDX-FileCopyrightText: 2019, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.client

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.location.Address
import android.location.Location
import android.os.Bundle
import android.os.DeadObjectException
import android.os.IBinder
import android.os.RemoteException
import android.util.Log

import org.microg.nlp.service.AddressCallback
import org.microg.nlp.service.LocationCallback
import org.microg.nlp.service.UnifiedLocationService

import java.lang.ref.WeakReference
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicInteger

import android.content.pm.PackageManager.SIGNATURE_MATCH
import kotlinx.coroutines.*
import java.lang.RuntimeException
import java.util.concurrent.*
import kotlin.coroutines.*
import kotlin.math.min

private const val CALL_TIMEOUT = 10000L

class UnifiedLocationClient private constructor(context: Context) {
    private var bound = false
    private val serviceReferenceCount = AtomicInteger(0)
    private val options = Bundle()
    private var context: WeakReference<Context> = WeakReference(context)
    private var service: UnifiedLocationService? = null
    private val syncThreads: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.SECONDS, LinkedBlockingQueue())
    private val waitingForService = arrayListOf<Continuation<UnifiedLocationService>>()
    private var timer: Timer? = null
    private var reconnectCount = 0
    private val requests = CopyOnWriteArraySet<LocationRequest>()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            this@UnifiedLocationClient.onServiceConnected(name, service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            this@UnifiedLocationClient.onServiceDisconnected(name)
        }

        override fun onBindingDied(name: ComponentName) {
            this@UnifiedLocationClient.onBindingDied(name)
        }

        override fun onNullBinding(name: ComponentName) {
            this@UnifiedLocationClient.onNullBinding(name)

        }
    }

    var forceNextUpdate: Boolean
        get() = options.getBoolean(KEY_FORCE_NEXT_UPDATE, false)
        set(value) = options.putBoolean(KEY_FORCE_NEXT_UPDATE, value)

    var opPackageName: String?
        get() = options.getString(KEY_OP_PACKAGE_NAME)
        set(value) = options.putString(KEY_OP_PACKAGE_NAME, value)

    val isAvailable: Boolean
        get() = bound || resolve() != null

    val targetPackage: String?
        get() = resolve()?.`package`

    private fun resolve(): Intent? {
        val context = this.context.get() ?: return null
        val intent = Intent(ACTION_UNIFIED_LOCATION_SERVICE)

        val pm = context.packageManager
        var resolveInfos = pm.queryIntentServices(intent, 0)
        if (resolveInfos.size > 1) {

            // Restrict to self if possible
            val isSelf: (it: ResolveInfo) -> Boolean = {
                it.serviceInfo.packageName == context.packageName
            }
            if (resolveInfos.size > 1 && resolveInfos.any(isSelf)) {
                Log.d(TAG, "Found more than one active unified service, restricted to own package " + context.packageName)
                resolveInfos = resolveInfos.filter(isSelf)
            }

            // Restrict to package with matching signature if possible
            val isSelfSig: (it: ResolveInfo) -> Boolean = {
                it.serviceInfo.packageName == context.packageName
            }
            if (resolveInfos.size > 1 && resolveInfos.any(isSelfSig)) {
                Log.d(TAG, "Found more than one active unified service, restricted to related packages")
                resolveInfos = resolveInfos.filter(isSelfSig)
            }

            // Restrict to system if any package is system
            val isSystem: (it: ResolveInfo) -> Boolean = {
                (it.serviceInfo?.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM > 0
            }
            if (resolveInfos.size > 1 && resolveInfos.any(isSystem)) {
                Log.d(TAG, "Found more than one active unified service, restricted to system packages")
                resolveInfos = resolveInfos.filter(isSystem)
            }

            val highestPriority: ResolveInfo? = resolveInfos.maxBy { it.priority }
            intent.setPackage(highestPriority!!.serviceInfo.packageName)
            intent.setClassName(highestPriority.serviceInfo.packageName, highestPriority.serviceInfo.name)
            if (resolveInfos.size > 1) {
                Log.d(TAG, "Found more than one active unified service, picked highest priority " + intent.component)
            }
            return intent
        } else if (!resolveInfos.isEmpty()) {
            intent.setPackage(resolveInfos[0].serviceInfo.packageName)
            return intent
        } else {
            Log.w(TAG, "No service to bind to, your system does not support unified service")
            return null
        }
    }

    @Synchronized
    private fun updateBinding(): Boolean {
        Log.d(TAG, "updateBinding - current: $bound, refs: ${serviceReferenceCount.get()}, reqs: ${requests.size}, avail: $isAvailable")
        if (!bound && (serviceReferenceCount.get() > 0 || !requests.isEmpty()) && isAvailable) {
            timer = Timer("unified-client")
            bound = true
            bind()
            return true
        } else if (bound && serviceReferenceCount.get() == 0 && requests.isEmpty()) {
            timer!!.cancel()
            timer = null
            bound = false
            unbind()
            return false
        }
        return bound
    }

    @Synchronized
    private fun bindLater() {
        val timer = timer
        if (timer == null) {
            updateBinding()
            return
        }
        timer.schedule(object : TimerTask() {
            override fun run() {
                bind()
            }
        }, 1000)
    }

    @Synchronized
    private fun bind() {
        val context = this.context.get() ?: return
        if (!bound) {
            Log.w(TAG, "Tried to bind while not being bound!")
            return
        }
        if (reconnectCount > 3) {
            Log.w(TAG, "Reconnecting failed three times in a row, die out.")
            return
        }
        val intent = resolve() ?: return
        unbind()
        reconnectCount++
        Log.d(TAG, "Binding to $intent")
        bound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    @Synchronized
    private fun unbind() {
        try {
            this.context.get()?.unbindService(connection)
        } catch (ignored: Exception) {
        }

        this.service = null
    }

    @Synchronized
    fun ref() {
        Log.d(TAG, "ref: ${Exception().stackTrace[1]}")
        serviceReferenceCount.incrementAndGet()
        updateBinding()
    }

    @Synchronized
    fun unref() {
        Log.d(TAG, "unref: ${Exception().stackTrace[1]}")
        serviceReferenceCount.decrementAndGet()
        updateBinding()
    }

    suspend fun getSingleLocation(): Location = suspendCoroutine { continuation ->
        requestSingleLocation(LocationListener.wrap { continuation.resume(it) })
    }

    fun requestSingleLocation(listener: LocationListener) {
        requestLocationUpdates(listener, 0, 1)
    }

    fun requestLocationUpdates(listener: LocationListener, interval: Long) {
        requestLocationUpdates(listener, interval, Integer.MAX_VALUE)
    }

    fun requestLocationUpdates(listener: LocationListener, interval: Long, count: Int) {
        requests.removeAll(requests.filter { it.listener === listener })
        requests.add(LocationRequest(listener, interval, count))
        coroutineScope.launch {
            updateServiceInterval()
            updateBinding()
        }
    }

    fun removeLocationUpdates(listener: LocationListener) {
        coroutineScope.launch {
            removeRequests(requests.filter { it.listener === listener })
        }
    }

    private suspend fun refAndGetService(): UnifiedLocationService = suspendCoroutine { continuation -> refAndGetServiceContinued(continuation) }

    @Synchronized
    private fun refAndGetServiceContinued(continuation: Continuation<UnifiedLocationService>) {
        Log.d(TAG, "ref+get: ${Exception().stackTrace[2]}")
        serviceReferenceCount.incrementAndGet()
        waitForServiceContinued(continuation)
    }

    private suspend fun waitForService(): UnifiedLocationService = suspendCoroutine { continuation -> waitForServiceContinued(continuation) }

    @Synchronized
    private fun waitForServiceContinued(continuation: Continuation<UnifiedLocationService>) {
        val service = service
        if (service != null) {
            continuation.resume(service)
        } else {
            synchronized(waitingForService) {
                waitingForService.add(continuation)
            }
            updateBinding()
            val timer = timer
            if (timer == null) {
                synchronized(waitingForService) {
                    waitingForService.remove(continuation)
                }
                continuation.resumeWithException(RuntimeException("No timer, called waitForService when not connected"))
            } else {
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        try {
                            continuation.resumeWithException(TimeoutException())
                        } catch (e: IllegalStateException) {
                            // Resumed pretty much the same moment as timeout triggered, ignore
                        }
                    }
                }, CALL_TIMEOUT)
            }
        }
    }

    private fun <T> configureContinuationTimeout(continuation: Continuation<T>, timeout: Long) {
        if (timeout <= 0 || timeout == Long.MAX_VALUE) return
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                try {
                    Log.w(TAG, "Timeout reached")
                    continuation.resumeWithException(TimeoutException())
                } catch (ignored: Exception) {
                    // Ignore
                }
            }
        }, timeout)
    }

    private fun <T> executeSyncWithTimeout(timeout: Long = CALL_TIMEOUT, action: suspend () -> T): T {
        var result: T? = null
        val latch = CountDownLatch(1)
        var err: Exception? = null
        syncThreads.execute {
            try {
                runBlocking {
                    result = withTimeout(timeout) { action() }
                }
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

    suspend fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int, locale: String, timeout: Long = Long.MAX_VALUE): List<Address> {
        try {
            val service = refAndGetService()
            return suspendCoroutine { continuation ->
                service.getFromLocationWithOptions(latitude, longitude, maxResults, locale, options, AddressContinuation(continuation))
                configureContinuationTimeout(continuation, timeout)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request geocode", e)
            return emptyList()
        } finally {
            unref()
        }
    }

    fun getFromLocationSync(latitude: Double, longitude: Double, maxResults: Int, locale: String, timeout: Long = CALL_TIMEOUT): List<Address> = executeSyncWithTimeout(timeout) {
        getFromLocation(latitude, longitude, maxResults, locale, timeout)
    }

    suspend fun getFromLocationName(locationName: String, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String, timeout: Long = Long.MAX_VALUE): List<Address> {
        return try {
            val service = refAndGetService()
            suspendCoroutine { continuation ->
                service.getFromLocationNameWithOptions(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale, options, AddressContinuation(continuation))
                configureContinuationTimeout(continuation, timeout)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to request geocode", e)
            emptyList()
        } finally {
            unref()
        }
    }

    fun getFromLocationNameSync(locationName: String, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String, timeout: Long = CALL_TIMEOUT): List<Address> = executeSyncWithTimeout(timeout) {
        getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale, timeout)
    }

    suspend fun reloadPreferences() {
        try {
            refAndGetService().reloadPreferences()
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to handle request", e)
        } finally {
            unref()
        }
    }

    suspend fun getLocationBackends(): Array<String> {
        try {
            return refAndGetService().locationBackends
        } catch (e: Exception) {
            Log.w(TAG, "Failed to handle request", e)
            return emptyArray()
        } finally {
            unref()
        }
    }

    suspend fun setLocationBackends(backends: Array<String>) {
        try {
            refAndGetService().locationBackends = backends
        } catch (e: Exception) {
            Log.w(TAG, "Failed to handle request", e)
        } finally {
            unref()
        }
    }

    suspend fun getGeocoderBackends(): Array<String> {
        try {
            return refAndGetService().geocoderBackends
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to handle request", e)
            return emptyArray()
        } finally {
            unref()
        }
    }

    suspend fun setGeocoderBackends(backends: Array<String>) {
        try {
            refAndGetService().geocoderBackends = backends
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to handle request", e)
        } finally {
            unref()
        }
    }

    fun getLastLocationSync(timeout: Long = CALL_TIMEOUT): Location? = executeSyncWithTimeout(timeout) {
        getLastLocation()
    }

    suspend fun getLastLocation(): Location? {
        return try {
            refAndGetService().lastLocation
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to handle request", e)
            null
        } finally {
            unref()
        }
    }

    suspend fun getLastLocationForBackend(packageName: String, className: String, signatureDigest: String? = null): Location? {
        return try {
            refAndGetService().getLastLocationForBackend(packageName, className, signatureDigest)
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to handle request", e)
            null
        } finally {
            unref()
        }
    }

    private suspend fun removeRequestPendingRemoval() {
        removeRequests(requests.filter { it.needsRemoval })
    }

    private suspend fun removeRequests(removalNeeded: List<LocationRequest>) {
        if (removalNeeded.isNotEmpty()) {
            requests.removeAll(removalNeeded)
            updateServiceInterval()
            updateBinding()
        }
    }

    private suspend fun updateServiceInterval() {
        var interval: Long = Long.MAX_VALUE
        var requestSingle = false
        for (request in requests) {
            if (request.interval <= 0) {
                requestSingle = true
                continue
            }
            interval = min(interval, request.interval)
        }
        if (interval == Long.MAX_VALUE) {
            Log.d(TAG, "Disable automatic updates")
            interval = 0
        } else {
            Log.d(TAG, "Set update interval to $interval")
        }
        val service: UnifiedLocationService
        try {
            service = waitForService()
        } catch (e: Exception) {
            Log.w(TAG, e)
            return
        }
        try {
            service.setUpdateInterval(interval, options)
            if (requestSingle) {
                Log.d(TAG, "Request single update (force update: $forceNextUpdate)")
                service.requestSingleUpdate(options)
                forceNextUpdate = false
            }
        } catch (e: DeadObjectException) {
            Log.w(TAG, "Connection is dead, reconnecting")
            bind()
        } catch (e: RemoteException) {
            Log.w(TAG, "Failed to set location update interval", e)
        }
    }

    @Synchronized
    private fun onServiceConnected(name: ComponentName, binder: IBinder) {
        Log.d(TAG, "Connected to $name")
        reconnectCount = 0
        val service = UnifiedLocationService.Stub.asInterface(binder)
        this.service = service
        val continuations = arrayListOf<Continuation<UnifiedLocationService>>()
        synchronized(waitingForService) {
            continuations.addAll(waitingForService)
            waitingForService.clear()
        }
        coroutineScope.launch {
            try {
                Log.d(TAG, "Registering location callback")
                service.registerLocationCallback(object : LocationCallback.Stub() {
                    override fun onLocationUpdate(location: Location) {
                        coroutineScope.launch {
                            this@UnifiedLocationClient.onLocationUpdate(location)
                        }
                    }
                }, options)
                Log.d(TAG, "Registered location callback")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to register location callback", e)
            }
            context.get()?.resources?.getStringArray(R.array.force_location_backends)?.let { setLocationBackends(it + getLocationBackends()) }
            context.get()?.resources?.getStringArray(R.array.force_geocoder_backends)?.let { setGeocoderBackends(it + getGeocoderBackends()) }
            updateServiceInterval()
            if (continuations.size > 0) {
                Log.d(TAG, "Resuming ${continuations.size} continuations")
            }
            for (continuation in continuations) {
                try {
                    continuation.resume(service)
                } catch (e: Exception) {
                    Log.w(TAG, e)
                }
            }
        }
    }

    private suspend fun onLocationUpdate(location: Location) {
        for (request in requests) {
            request.handleLocation(location)
        }
        removeRequestPendingRemoval()
    }

    @Synchronized
    private fun onServiceDisconnected(name: ComponentName) {
        Log.d(TAG, "Disconnected from $name")
        this.service = null
    }

    private fun onBindingDied(name: ComponentName) {
        Log.d(TAG, "Connection to $name died, reconnecting")
        bind()
    }

    private fun onNullBinding(name: ComponentName) {
        Log.w(TAG, "Null binding from $name, reconnecting")
        bindLater()
    }

    interface LocationListener {
        fun onLocation(location: Location)

        companion object {
            fun wrap(listener: (Location) -> Unit): LocationListener {
                return object : LocationListener {
                    override fun onLocation(location: Location) {
                        listener(location)
                    }
                }
            }
        }
    }

    private inner class LocationRequest(val listener: LocationListener, var interval: Long, var pendingCount: Int) {
        private var lastUpdate: Long = 0

        private var failed: Boolean = false
        val needsRemoval: Boolean
            get() = pendingCount <= 0 || failed

        fun reset(interval: Long, count: Int) {
            this.interval = interval
            this.pendingCount = count
        }

        @Synchronized
        fun handleLocation(location: Location) {
            if (needsRemoval) return
            if (lastUpdate > System.currentTimeMillis()) {
                lastUpdate = System.currentTimeMillis()
            }
            if (lastUpdate <= System.currentTimeMillis() - interval / 2) {
                lastUpdate = System.currentTimeMillis()
                if (pendingCount > 0) {
                    pendingCount--
                }
                try {
                    listener.onLocation(location)
                } catch (e: Exception) {
                    Log.w(TAG, "Listener threw uncaught exception, stopping location request", e)
                    failed = true
                }

            }
        }
    }

    companion object {
        const val ACTION_UNIFIED_LOCATION_SERVICE = "org.microg.nlp.service.UnifiedLocationService"
        const val PERMISSION_SERVICE_ADMIN = "org.microg.nlp.SERVICE_ADMIN"
        const val KEY_FORCE_NEXT_UPDATE = "org.microg.nlp.FORCE_NEXT_UPDATE"
        const val KEY_OP_PACKAGE_NAME = "org.microg.nlp.OP_PACKAGE_NAME"
        private val TAG = "ULocClient"
        private var client: UnifiedLocationClient? = null

        @JvmStatic
        @Synchronized
        operator fun get(context: Context): UnifiedLocationClient {
            var client = client
            if (client == null) {
                client = UnifiedLocationClient(context)
                this.client = client
            } else {
                client.context = WeakReference(context)
            }
            return client
        }
    }
}

class AddressContinuation(private val continuation: Continuation<List<Address>>) : AddressCallback.Stub() {
    override fun onResult(addresses: List<Address>?) {
        try {
            if (addresses != null) {
                Log.d("ULocClient", "Resume with ${addresses.size} addresses")
                continuation.resume(addresses)
            } else {
                continuation.resumeWithException(NullPointerException("Service returned null"))
            }
        } catch (ignored: Exception) {
            // Ignore
        }
    }
}