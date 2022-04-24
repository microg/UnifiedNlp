/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.GET_META_DATA
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import org.microg.nlp.client.GeocodeClient
import org.microg.nlp.client.LocationClient
import org.microg.nlp.service.api.LatLon
import org.microg.nlp.service.api.ReverseGeocodeRequest
import org.microg.nlp.ui.databinding.BackendDetailsBinding
import org.microg.nlp.ui.model.BackendDetailsCallback
import org.microg.nlp.ui.model.BackendInfo
import org.microg.nlp.ui.model.BackendType
import org.microg.nlp.ui.model.BackendType.GEOCODER
import org.microg.nlp.ui.model.BackendType.LOCATION
import java.util.*

class BackendDetailsFragment : Fragment(R.layout.backend_details), BackendDetailsCallback {

    fun Double.toStringWithDigits(digits: Int): String {
        val s = this.toString()
        val i = s.indexOf('.')
        if (i <= 0 || s.length - i - 1 < digits) return s
        if (digits == 0) return s.substring(0, i)
        return s.substring(0, s.indexOf('.') + digits + 1)
    }

    fun Float.toStringWithDigits(digits: Int): String {
        val s = this.toString()
        val i = s.indexOf('.')
        if (i <= 0 || s.length - i - 1 < digits) return s
        if (digits == 0) return s.substring(0, i)
        return s.substring(0, s.indexOf('.') + digits + 1)
    }

    @ColorInt
    fun Context.resolveColor(@AttrRes resid: Int): Int? {
        val typedValue = TypedValue()
        if (!theme.resolveAttribute(resid, typedValue, true)) return null
        val colorRes = if (typedValue.resourceId != 0) typedValue.resourceId else typedValue.data
        return ContextCompat.getColor(this, colorRes)
    }

    val switchBarEnabledColor: Int
        get() = context?.resolveColor(androidx.appcompat.R.attr.colorControlActivated) ?: Color.RED

    val switchBarDisabledColor: Int
        get() {
            val color = context?.resolveColor(android.R.attr.textColorSecondary) ?: Color.RED
            return Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        }

    val switchBarTrackTintColor: ColorStateList
        get() {
            val color = context?.resolveColor(android.R.attr.textColorPrimaryInverse)
                    ?: Color.RED
            val withAlpha = Color.argb(50, Color.red(color), Color.green(color), Color.blue(color))
            return ColorStateList(arrayOf(emptyArray<Int>().toIntArray()), arrayOf(withAlpha).toIntArray())
        }

    private lateinit var binding: BackendDetailsBinding
    private lateinit var locationClient: LocationClient
    private lateinit var geocodeClient: GeocodeClient

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = BackendDetailsBinding.inflate(inflater, container, false)
        binding.callbacks = this
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.switchWidget.trackTintList = switchBarTrackTintColor
        locationClient = LocationClient(requireContext(), lifecycle)
        geocodeClient = GeocodeClient(requireContext(), lifecycle)
        lifecycleScope.launchWhenStarted { initContent(createBackendInfo()) }
    }

    override fun onPause() {
        super.onPause()
    }

    private suspend fun initContent(entry: BackendInfo?) {
        binding.entry = entry
        binding.executePendingBindings()
        updateContent(entry)
    }

    private var updateInProgress = false
    private suspend fun updateContent(entry: BackendInfo?) {
        if (entry == null) return
        if (!entry.loaded.get()) {
            entry.fillDetails(requireContext())
            entry.loadIntents(requireActivity() as AppCompatActivity)
        }
        if (entry.type == LOCATION && entry.enabled.get()) {
            if (updateInProgress) {
                Log.d(TAG, "Location update still in progress")
                return
            }
            locationClient.connect()
            Log.d(TAG, "Connected to location client")
            updateInProgress = true
            try {
                val locationTemp = locationClient.getLastLocationForBackend(
                    entry.serviceInfo.packageName,
                    entry.serviceInfo.name,
                    entry.firstSignatureDigest
                )
                val location = when (locationTemp) {
                    null -> {
                        delay(500L) // Wait short time to ensure backend was activated
                        Log.d(TAG, "Location was not available, requesting once")
                        locationClient.forceLocationUpdate()
                        val secondAttempt = locationClient.getLastLocationForBackend(
                            entry.serviceInfo.packageName,
                            entry.serviceInfo.name,
                            entry.firstSignatureDigest
                        )
                        if (secondAttempt == null) {
                            Log.d(TAG, "Location still not available, waiting or giving up")
                            delay(WAIT_FOR_RESULT)
                            locationClient.getLastLocationForBackend(
                                entry.serviceInfo.packageName,
                                entry.serviceInfo.name,
                                entry.firstSignatureDigest
                            )
                        } else {
                            secondAttempt
                        }
                    }
                    else -> locationTemp
                } ?: return
                var locationString =
                    "${location.latitude.toStringWithDigits(6)}, ${location.longitude.toStringWithDigits(6)}"

                Log.d(TAG, "Location reported is $locationString, trying to gather address")
                val address = geocodeClient.requestReverseGeocode(
                    ReverseGeocodeRequest(
                        LatLon(
                            location.latitude,
                            location.longitude
                        )
                    )
                ).singleOrNull()
                if (address != null) {
                    val addressLine = StringBuilder()
                    var i = 0
                    addressLine.append(address.getAddressLine(i))
                    while (addressLine.length < 10 && address.maxAddressLineIndex > i) {
                        i++
                        addressLine.append(", ")
                        addressLine.append(address.getAddressLine(i))
                    }
                    locationString = addressLine.toString()
                }
                binding.lastLocationString = locationString
                binding.executePendingBindings()
            } finally {
                locationClient.disconnect()
                updateInProgress = false
            }
        } else {
            Log.d(TAG, "Location is not available for this backend (type: ${entry.type}, enabled ${entry.enabled.get()}")
            binding.lastLocationString = ""
            binding.executePendingBindings()
        }
    }

    override fun onAboutClicked(entry: BackendInfo?) {
        entry?.aboutIntent?.get()?.let { requireContext().startActivity(it) }
    }

    override fun onConfigureClicked(entry: BackendInfo?) {
        entry?.settingsIntent?.get()?.let { requireContext().startActivity(it) }
    }

    private suspend fun createBackendInfo(): BackendInfo? {
        val type = BackendType.values().find { it.name == arguments?.getString("type") }
                ?: return null
        val packageName = arguments?.getString("package") ?: return null
        val name = arguments?.getString("name") ?: return null
        val serviceInfo = context?.packageManager?.getServiceInfo(ComponentName(packageName, name), GET_META_DATA)
                ?: return null
        val enabledBackends = when (type) {
            LOCATION -> locationClient.getLocationBackends()
            GEOCODER -> geocodeClient.getGeocodeBackends()
        }
        val info = BackendInfo(serviceInfo, type, firstSignatureDigest(requireContext(), packageName))
        info.enabled.set(enabledBackends.contains(info.signedComponent) || enabledBackends.contains(info.unsignedComponent))
        return info
    }

    override fun onEnabledChange(entry: BackendInfo?, newValue: Boolean) {
        if (entry?.enabled?.get() == newValue) return
        Log.d(TAG, "onEnabledChange: ${entry?.signedComponent} = $newValue")
        entry?.enabled?.set(newValue)
        lifecycleScope.launchWhenStarted {
            entry?.updateEnabled(this@BackendDetailsFragment, newValue)
            initContent(entry)
        }
    }

    override fun onAppClicked(entry: BackendInfo?) {
        if (entry == null) return
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", entry.serviceInfo.packageName, null)
        intent.data = uri
        requireContext().startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val TAG = "USettings"
        private const val WAIT_FOR_RESULT = 5000L
    }
}
