/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Intent
import android.location.Address
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.nlp.api.GeocoderBackend
import java.util.concurrent.*
import kotlin.coroutines.suspendCoroutine

class AsyncGeocoderBackend(binder: IBinder, name: String = "geocoder-backend-thread") : Thread(name) {
    private val syncThreads: ThreadPoolExecutor = ThreadPoolExecutor(1, Runtime.getRuntime().availableProcessors(), 1, TimeUnit.SECONDS, LinkedBlockingQueue())
    private lateinit var looper: Looper
    private lateinit var handler: Handler
    private val mutex = Mutex(true)
    private val backend = GeocoderBackend.Stub.asInterface(binder)

    override fun run() {
        Looper.prepare()
        looper = Looper.myLooper()!!
        handler = Handler(looper)
        handler.post {
            mutex.unlock()
        }
        Looper.loop()
    }

    suspend fun open() {
        start()
        mutex.withLock {
            suspendCoroutine<Unit> {
                handler.post {
                    val result = try {
                        backend.open()
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure<Unit>(e)
                    }
                    it.resumeWith(result)
                }
            }
        }
    }

    suspend fun getFromLocation(latitude: Double, longitude: Double, maxResults: Int, locale: String?): List<Address> = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.getFromLocation(latitude, longitude, maxResults, locale))
                } catch (e: Exception) {
                    Result.failure<List<Address>>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    fun getFromLocationSync(latitude: Double, longitude: Double, maxResults: Int, locale: String?): List<Address> = executeWithTimeout {
        backend.getFromLocation(latitude, longitude, maxResults, locale);
    }

    suspend fun getFromLocationName(locationName: String?, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String?): List<Address> = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale))
                } catch (e: Exception) {
                    Result.failure<List<Address>>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    fun getFromLocationNameSync(locationName: String?, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String?): List<Address> = executeWithTimeout {
        backend.getFromLocationName(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale)
    }

    suspend fun close() {
        mutex.withLock {
            suspendCoroutine<Unit> {
                handler.post {
                    val result = try {
                        backend.close()
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure<Unit>(e)
                    }
                    it.resumeWith(result)
                }
            }
        }
        looper.quit()
    }

    suspend fun getSettingsIntent(): Intent = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.settingsIntent)
                } catch (e: Exception) {
                    Result.failure<Intent>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    suspend fun getInitIntent(): Intent = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.initIntent)
                } catch (e: Exception) {
                    Result.failure<Intent>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    suspend fun getAboutIntent(): Intent = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.aboutIntent)
                } catch (e: Exception) {
                    Result.failure<Intent>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    suspend fun getFromLocationWithOptions(latitude: Double, longitude: Double, maxResults: Int, locale: String?, options: Bundle?): List<Address> = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.getFromLocationWithOptions(latitude, longitude, maxResults, locale, options))
                } catch (e: Exception) {
                    Result.failure<List<Address>>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    fun getFromLocationWithOptionsSync(latitude: Double, longitude: Double, maxResults: Int, locale: String?, options: Bundle?): List<Address> = executeWithTimeout {
        backend.getFromLocationWithOptions(latitude, longitude, maxResults, locale, options)
    }

    suspend fun getFromLocationNameWithOptions(locationName: String?, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String?, options: Bundle?): List<Address> = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.getFromLocationNameWithOptions(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale, options))
                } catch (e: Exception) {
                    Result.failure<List<Address>>(e)
                }
                it.resumeWith(result)
            }
        }
    }

    fun getFromLocationNameWithOptionsSync(locationName: String?, maxResults: Int, lowerLeftLatitude: Double, lowerLeftLongitude: Double, upperRightLatitude: Double, upperRightLongitude: Double, locale: String?, options: Bundle?): List<Address> = executeWithTimeout {
        backend.getFromLocationNameWithOptions(locationName, maxResults, lowerLeftLatitude, lowerLeftLongitude, upperRightLatitude, upperRightLongitude, locale, options)
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
