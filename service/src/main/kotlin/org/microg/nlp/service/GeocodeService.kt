/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Address
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.microg.nlp.service.api.*
import org.microg.nlp.service.api.Constants.*
import java.io.FileDescriptor
import java.io.PrintWriter

private const val TAG = "GeocodeService"

class GeocodeService : LifecycleService() {
    private lateinit var service: GeocodeServiceImpl

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Creating userspace service...")
        service = GeocodeServiceImpl(this, lifecycle)
        Log.d(TAG, "Created userspace service.")
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return service.asBinder()
    }

    override fun onDestroy() {
        service.destroy()
        super.onDestroy()
        Log.d(TAG, "Destroyed")
    }

    override fun dump(fd: FileDescriptor?, writer: PrintWriter?, args: Array<out String>?) {
        service.dump(writer)
    }
}

class GeocodeServiceImpl(private val context: Context, private val lifecycle: Lifecycle) : IGeocodeService.Stub(), LifecycleOwner {
    private val fuser = GeocodeFuser(context, lifecycle)

    init {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "Preparing GeocodeFuser...")
            fuser.reset()
            fuser.bind()
            Log.d(TAG, "Finished preparing GeocodeFuser")
        }
    }

    private fun getCallingPackage(): String? {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val callingPid = getCallingPid()
        if (manager != null && callingPid > 0) {
            manager.runningAppProcesses.find { it.pid == callingPid }?.pkgList?.firstOrNull()?.let { return it }
        }
        return context.packageManager.getPackagesForUid(getCallingUid())?.firstOrNull()
    }

    private fun processOptions(options: Bundle?): Bundle {
        val options = options ?: Bundle()
        options.putString("callingPackage", getCallingPackage())
        if (!options.containsKey("packageName")) {
            options.putString("packageName", options.getString("packageName"))
        } else if (context.checkCallingPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED) {
            val claimedPackageName = options.getString("packageName")
            if (context.packageManager.getPackagesForUid(getCallingUid())?.any { it == claimedPackageName } != true) {
                options.putString("packageName", options.getString("packageName"))
            }
        }
        options.putInt("callingUid", getCallingUid())
        options.putInt("callingPid", getCallingPid())
        return options
    }

    private fun Bundle.checkPermission(permission: String): Int {
        return context.checkPermission(permission, getInt("callingPid"), getInt("callingUid"))
    }

    override fun requestGeocode(request: GeocodeRequest?, callback: IAddressesCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (request == null)
                return@launchWhenStarted callback.onAddresses(STATUS_INVALID_ARGS, null)
            if (extras.checkPermission("android.permission.INTERNET") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onAddresses(STATUS_PERMISSION_ERROR, null)
            callback.onAddresses(STATUS_OK, fuser.getFromLocationName(
                    request.locationName,
                    request.maxResults,
                    request.bounds.lowerLeft.latitude,
                    request.bounds.lowerLeft.longitude,
                    request.bounds.upperRight.latitude,
                    request.bounds.upperRight.longitude,
                    request.locale
            ))
        }
    }

    override fun requestGeocodeSync(request: GeocodeRequest?, options: Bundle?): List<Address> {
        val extras = processOptions(options)
        if (request == null || extras.getString("packageName") == null) throw IllegalArgumentException()
        if (extras.checkPermission("android.permission.INTERNET") != PERMISSION_GRANTED) throw SecurityException("Missing INTERNET permission")
        return fuser.getFromLocationNameSync(
            request.locationName,
            request.maxResults,
            request.bounds.lowerLeft.latitude,
            request.bounds.lowerLeft.longitude,
            request.bounds.upperRight.latitude,
            request.bounds.upperRight.longitude,
            request.locale
        ).orEmpty()
    }

    override fun requestReverseGeocode(request: ReverseGeocodeRequest?, callback: IAddressesCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (request == null)
                return@launchWhenStarted callback.onAddresses(STATUS_INVALID_ARGS, null)
            if (extras.checkPermission("android.permission.INTERNET") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onAddresses(STATUS_PERMISSION_ERROR, null)
            callback.onAddresses(STATUS_OK, fuser.getFromLocation(request.location.latitude, request.location.longitude, request.maxResults, request.locale))
        }
    }

    override fun requestReverseGeocodeSync(request: ReverseGeocodeRequest?, options: Bundle?): List<Address> {
        val extras = processOptions(options)
        if (request == null || extras.getString("packageName") == null) throw IllegalArgumentException()
        if (extras.checkPermission("android.permission.INTERNET") != PERMISSION_GRANTED) throw SecurityException("Missing INTERNET permission")
        return fuser.getFromLocationSync(request.location.latitude, request.location.longitude, request.maxResults, request.locale).orEmpty()
    }

    override fun reloadPreferences(callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStatus(STATUS_PERMISSION_ERROR)
            fuser.reset()
            fuser.bind()
            callback.onStatus(Constants.STATUS_OK)
        }
    }

    override fun getGeocodeBackends(callback: IStringsCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStrings(STATUS_PERMISSION_ERROR, null)
            callback.onStrings(Constants.STATUS_OK, Preferences(context).geocoderBackends.toList())
        }
    }

    override fun setGeocodeBackends(backends: MutableList<String>?, callback: IStatusCallback?, options: Bundle?) {
        val extras = processOptions(options)
        if (callback == null || extras.getString("packageName") == null) return
        lifecycleScope.launchWhenStarted {
            if (extras.checkPermission("org.microg.nlp.SERVICE_ADMIN") != PERMISSION_GRANTED)
                return@launchWhenStarted callback.onStatus(STATUS_PERMISSION_ERROR)
            if (backends == null)
                return@launchWhenStarted callback.onStatus(STATUS_INVALID_ARGS)
            Preferences(context).geocoderBackends = backends.toSet()
            callback.onStatus(Constants.STATUS_OK)
        }
    }

    fun dump(writer: PrintWriter?) {
        fuser.dump(writer)
    }

    fun destroy() {
        fuser.destroy()
    }

    override fun getLifecycle(): Lifecycle = lifecycle
}

