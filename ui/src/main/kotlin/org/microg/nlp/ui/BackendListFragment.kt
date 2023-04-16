/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.microg.nlp.client.GeocodeClient
import org.microg.nlp.client.LocationClient
import org.microg.nlp.service.*
import org.microg.nlp.ui.databinding.BackendListBinding
import org.microg.nlp.ui.databinding.BackendListEntryBinding
import org.microg.nlp.ui.model.BackendInfo
import org.microg.nlp.ui.model.BackendListEntryCallback
import org.microg.nlp.ui.model.BackendType
import kotlin.coroutines.suspendCoroutine


class BackendListFragment : Fragment(R.layout.backend_list), BackendListEntryCallback {
    val locationAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)
    val geocoderAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BackendListBinding.inflate(inflater, container, false)
        binding.fragment = this

        lifecycleScope.launch(Dispatchers.Default) {
                LocationClient(requireContext(), lifecycle).getLocationBackends()
                GeocodeClient(requireContext(), lifecycle).getGeocodeBackends()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenStarted { updateAdapters() }
        LocalBroadcastManager.getInstance(this.requireContext()).registerReceiver(
                mMessageReceiver, IntentFilter("LocationGeocodeBackendUpdated"));
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this.requireContext()).unregisterReceiver(mMessageReceiver)
    }

    override fun onOpenDetails(entry: BackendInfo?) {
        if (entry == null) return
        findNavController().navigate(requireContext(), R.id.openBackendDetails, bundleOf(
                "type" to entry.type.get().toString(),
                "package" to entry.appName.get(),
                "name" to entry.name.get()
        ))
    }

    override fun onEnabledChange(entry: BackendInfo?, newValue: Boolean) {
        val activity = requireActivity() as AppCompatActivity
        lifecycleScope.launchWhenStarted {
            entry?.updateEnabled(this@BackendListFragment, newValue)
        }
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lifecycleScope.launch(Dispatchers.Main) {
                updateAdapters()
            }
        }
    }

    private suspend fun updateAdapters() {
        val enabledLocationBackends = Preferences(requireContext()).locationBackends.toList()
        val enabledGeocodeBackends = Preferences(requireContext()).geocoderBackends.toList()

        var locationBackends = MutableList<BackendInfo>(LocationFuser.backendHelpers.size)  { BackendInfo() }
        var counter  = 0
        for (backendHelper: LocationBackendHelper in LocationFuser.backendHelpers) {
            backendHelper.backend.open()
            locationBackends[counter].appName.set("org.microg.nlp")
            locationBackends[counter].name.set(backendHelper.backend.getBackendName())
            locationBackends[counter].summary.set(backendHelper.backend.getDescription())
            locationBackends[counter].type.set(BackendType.LOCATION)
            locationBackends[counter].initIntent.set(backendHelper.backend.getInitIntent())
            locationBackends[counter].settingsIntent.set(backendHelper.backend.getSettingsIntent())
            locationBackends[counter].aboutIntent.set(backendHelper.backend.getAboutIntent())

            locationBackends[counter].loaded.set(true)

            for (enabledLocation: String in enabledLocationBackends) {
                val enabledLocationParts = enabledLocation.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                if ((enabledLocationParts[0].equals("org.microg.nlp")) && (enabledLocationParts[1].equals(locationBackends[counter].name.get()))) {
                    locationBackends[counter].enabled.set(true)
                }
            }

            counter++
        }
        locationAdapter.setEntries(locationBackends);


        var gecodeBackends = MutableList<BackendInfo>(GeocodeFuser.backendHelpers.size)  { BackendInfo() }
        counter  = 0
        for (backendHelper: GeocodeBackendHelper in GeocodeFuser.backendHelpers) {
            backendHelper.backend.open()
            gecodeBackends[counter].appName.set("org.microg.nlp")
            gecodeBackends[counter].name.set(backendHelper.backend.getBackendName())
            gecodeBackends[counter].summary.set(backendHelper.backend.getDescription())
            gecodeBackends[counter].type.set(BackendType.GEOCODER)
            gecodeBackends[counter].initIntent.set(backendHelper.backend.getInitIntent())
            gecodeBackends[counter].settingsIntent.set(backendHelper.backend.getSettingsIntent())
            gecodeBackends[counter].aboutIntent.set(backendHelper.backend.getAboutIntent())

            gecodeBackends[counter].loaded.set(true)

            for (enabledGeocoder: String in enabledGeocodeBackends) {
                val enabledGeocoderParts = enabledGeocoder.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
                if ((enabledGeocoderParts[0].equals("org.microg.nlp")) && (enabledGeocoderParts[1].equals(gecodeBackends[counter].name.get()))) {
                    gecodeBackends[counter].enabled.set(true)
                }
            }
            counter++
        }
        geocoderAdapter.setEntries(gecodeBackends);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handleActivityResult(requestCode, resultCode, data)
    }
}

class BackendSettingsLineViewHolder(val binding: BackendListEntryBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fragment: BackendListFragment, entry: BackendInfo?) {
        binding.callbacks = fragment
        binding.entry = entry
        binding.executePendingBindings()
    }
}

class BackendSettingsLineAdapter(val fragment: BackendListFragment) : RecyclerView.Adapter<BackendSettingsLineViewHolder>() {
    private val entries: MutableList<BackendInfo?> = arrayListOf()

    fun addOrUpdateEntry(entry: BackendInfo?) {
        if (entry == null) {
            if (entries.contains(null)) return
            entries.add(entry)
            notifyItemInserted(entries.size - 1)
        } else {
            val oldIndex = entries.indexOfFirst { it?.name == entry.name }
            if (oldIndex != -1) {
                if (entries[oldIndex] == entry) return
                entries.removeAt(oldIndex)
            }
            val targetIndex = when (val i = entries.indexOfFirst { it == null || it.name.get().toString() > entry.name.get().toString() }) {
                -1 -> entries.size
                else -> i
            }
            entries.add(targetIndex, entry)
            when (oldIndex) {
                targetIndex -> notifyItemChanged(targetIndex)
                -1 -> notifyItemInserted(targetIndex)
                else -> notifyItemMoved(oldIndex, targetIndex)
            }
        }
    }

    fun removeEntry(entry: BackendInfo?) {
        val index = entries.indexOfFirst { it == entry || it?.name == entry?.name }
        entries.removeAt(index)
        notifyItemRemoved(index)
    }

    fun setEntries(entries: MutableList<BackendInfo>) {
        val oldEntries = this.entries.toTypedArray()
        for (oldEntry in oldEntries) {
            if (!entries.any { it == oldEntry || it?.name == oldEntry?.name }) {
                removeEntry(oldEntry)
            }
        }
        entries.forEach { addOrUpdateEntry(it) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BackendSettingsLineViewHolder {
        return BackendSettingsLineViewHolder(BackendListEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: BackendSettingsLineViewHolder, position: Int) {
        holder.bind(fragment, entries[position])
    }

    override fun getItemCount(): Int {
        return entries.size
    }
}

fun NavController.navigate(context: Context, @IdRes resId: Int, args: Bundle? = null) {
    navigate(resId, args, if (context.systemAnimationsEnabled) navOptions {
        anim {
            enter = androidx.navigation.ui.R.anim.nav_default_enter_anim
            exit = androidx.navigation.ui.R.anim.nav_default_exit_anim
            popEnter = androidx.navigation.ui.R.anim.nav_default_pop_enter_anim
            popExit = androidx.navigation.ui.R.anim.nav_default_pop_exit_anim
        }
    } else null)
}

val Context.systemAnimationsEnabled: Boolean
    get() {
        val duration: Float
        val transition: Float
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            duration = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            transition = Settings.Global.getFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
        } else {
            duration = Settings.System.getFloat(contentResolver, Settings.System.ANIMATOR_DURATION_SCALE, 1f)
            transition = Settings.System.getFloat(contentResolver, Settings.System.TRANSITION_ANIMATION_SCALE, 1f)
        }
        return duration != 0f && transition != 0f
    }
