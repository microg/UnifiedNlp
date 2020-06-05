package org.microg.nlp.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager.GET_META_DATA
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.microg.nlp.client.UnifiedLocationClient
import org.microg.nlp.ui.BackendType.GEOCODER
import org.microg.nlp.ui.BackendType.LOCATION
import org.microg.nlp.ui.databinding.BackendDetailsBinding
import java.util.*

class BackendDetailsFragment : Fragment(R.layout.backend_details) {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BackendDetailsBinding.inflate(inflater, container, false)
        binding.fragment = this
        binding.switchWidget.trackTintList = switchBarTrackTintColor
        lifecycleScope.launchWhenStarted {
            val entry = createBackendInfo()
            binding.entry = entry
            binding.executePendingBindings()
            if (entry?.type == LOCATION) {
                val client = UnifiedLocationClient[entry.context]

                val location = client.getLastLocationForBackend(entry.serviceInfo.packageName, entry.serviceInfo.name, entry.firstSignatureDigest)
                        ?: return@launchWhenStarted
                var locationString = "${location.latitude.toStringWithDigits(6)}, ${location.longitude.toStringWithDigits(6)}"

                val address = client.getFromLocation(location.latitude, location.longitude, 1, Locale.getDefault().toString()).singleOrNull()
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
            }
        }
        return binding.root
    }

    fun onBackendEnabledChanged(entry: BackendInfo) {
        entry.enabled = !entry.enabled
    }

    private fun createExternalIntent(packageName: String, activityName: String): Intent {
        val intent = Intent(ACTION_VIEW);
        intent.setPackage(packageName);
        intent.setClassName(packageName, activityName);
        return intent;
    }

    private fun startExternalActivity(packageName: String, activityName: String) {
        requireContext().startActivity(createExternalIntent(packageName, activityName))
    }

    fun onAboutClicked(entry: BackendInfo) {
        entry.aboutActivity?.let { activityName -> startExternalActivity(entry.serviceInfo.packageName, activityName) }
    }

    fun onConfigureClicked(entry: BackendInfo) {
        entry.settingsActivity?.let { activityName -> startExternalActivity(entry.serviceInfo.packageName, activityName) }
    }

    private suspend fun createBackendInfo(): BackendInfo? {
        val activity = activity ?: return null
        val intent = activity.intent ?: return null
        val type = BackendType.values().find { it.name == intent.getStringExtra(EXTRA_TYPE) }
                ?: return null
        val packageName = intent.getStringExtra(EXTRA_PACKAGE) ?: return null
        val name = intent.getStringExtra(EXTRA_NAME) ?: return null
        val serviceInfo = activity.packageManager.getServiceInfo(ComponentName(packageName, name), GET_META_DATA)
                ?: return null
        val enabledBackends = when (type) {
            GEOCODER -> UnifiedLocationClient[activity].getGeocoderBackends()
            LOCATION -> UnifiedLocationClient[activity].getLocationBackends()
        }
        return BackendInfo(activity, serviceInfo, type, lifecycleScope, enabledBackends)
    }

    companion object {
        val ACTION = "org.microg.nlp.ui.BACKEND_DETAILS"
        val EXTRA_TYPE = "org.microg.nlp.ui.BackendDetailsFragment.type"
        val EXTRA_PACKAGE = "org.microg.nlp.ui.BackendDetailsFragment.package"
        val EXTRA_NAME = "org.microg.nlp.ui.BackendDetailsFragment.name"
    }
}