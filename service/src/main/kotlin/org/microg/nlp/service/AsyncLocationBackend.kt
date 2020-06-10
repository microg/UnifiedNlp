/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.content.Intent
import android.location.Location
import android.os.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.nlp.api.LocationBackend
import org.microg.nlp.api.LocationCallback
import java.lang.Exception
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AsyncLocationBackend(binder: IBinder, name: String = "location-backend-thread") : Thread(name) {
    private lateinit var looper: Looper
    private lateinit var handler: Handler
    private val mutex = Mutex(true)
    private val backend = LocationBackend.Stub.asInterface(binder)

    override fun run() {
        Looper.prepare()
        looper = Looper.myLooper()!!
        handler = Handler(looper)
        handler.post {
            mutex.unlock()
        }
        Looper.loop()
    }

    suspend fun updateWithOptions(options: Bundle?): Location = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.updateWithOptions(options))
                } catch (e: Exception) {
                    Result.failure<Location>(e)
                }
                it.resumeWith(result)
            }
        }
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

    suspend fun open(callback: LocationCallback) {
        start()
        mutex.withLock {
            suspendCoroutine<Unit> {
                handler.post {
                    val result = try {
                        backend.open(callback)
                        Result.success(Unit)
                    } catch (e: Exception) {
                        Result.failure<Unit>(e)
                    }
                    it.resumeWith(result)
                }
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

    suspend fun update(): Location = mutex.withLock {
        suspendCoroutine {
            handler.post {
                val result = try {
                    Result.success(backend.update())
                } catch (e: Exception) {
                    Result.failure<Location>(e)
                }
                it.resumeWith(result)
            }
        }
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
}