/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import org.microg.nlp.api.Constants.ACTION_GEOCODER_BACKEND
import org.microg.nlp.api.Constants.ACTION_LOCATION_BACKEND
import org.microg.nlp.client.GeocodeClient
import org.microg.nlp.client.LocationClient
import org.microg.nlp.ui.databinding.BackendListBinding
import org.microg.nlp.ui.databinding.BackendListEntryBinding
import org.microg.nlp.ui.model.BackendInfo
import org.microg.nlp.ui.model.BackendListEntryCallback
import org.microg.nlp.ui.model.BackendType

class BackendListFragment : Fragment(R.layout.backend_list), BackendListEntryCallback {
    val locationAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)
    val geocoderAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BackendListBinding.inflate(inflater, container, false)
        binding.fragment = this
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenStarted { updateAdapters() }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onOpenDetails(entry: BackendInfo?) {
        if (entry == null) return
        findNavController().navigate(requireContext(), R.id.openBackendDetails, bundleOf(
                "type" to entry.type.name,
                "package" to entry.serviceInfo.packageName,
                "name" to entry.serviceInfo.name
        ))
    }

    override fun onEnabledChange(entry: BackendInfo?, newValue: Boolean) {
        val activity = requireActivity() as AppCompatActivity
        lifecycleScope.launchWhenStarted {
            entry?.updateEnabled(this@BackendListFragment, newValue)
        }
    }

    private suspend fun updateAdapters() {
        val activity = requireActivity() as AppCompatActivity
        locationAdapter.setEntries(createBackendInfoList(activity, Intent(ACTION_LOCATION_BACKEND), LocationClient(activity, lifecycle).getLocationBackends(), BackendType.LOCATION))
        geocoderAdapter.setEntries(createBackendInfoList(activity, Intent(ACTION_GEOCODER_BACKEND), GeocodeClient(activity, lifecycle).getGeocodeBackends(), BackendType.GEOCODER))
    }

    private fun createBackendInfoList(activity: AppCompatActivity, intent: Intent, enabledBackends: List<String>, type: BackendType): Array<BackendInfo?> {
        val backends = activity.packageManager.queryIntentServices(intent, GET_META_DATA).map {
            val info = BackendInfo(it.serviceInfo, type, firstSignatureDigest(activity, it.serviceInfo.packageName))
            if (enabledBackends.contains(info.signedComponent) || enabledBackends.contains(info.unsignedComponent)) {
                info.enabled.set(true)
            }
            info.fillDetails(activity)
            lifecycleScope.launchWhenStarted {
                info.loadIntents(activity)
            }
            info
        }.sortedBy { it.name.get() }
        if (backends.isEmpty()) return arrayOf(null)
        return backends.toTypedArray()
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
            val oldIndex = entries.indexOfFirst { it?.unsignedComponent == entry.unsignedComponent }
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
        val index = entries.indexOfFirst { it == entry || it?.unsignedComponent == entry?.unsignedComponent }
        entries.removeAt(index)
        notifyItemRemoved(index)
    }

    fun setEntries(entries: Array<BackendInfo?>) {
        val oldEntries = this.entries.toTypedArray()
        for (oldEntry in oldEntries) {
            if (!entries.any { it == oldEntry || it?.unsignedComponent == oldEntry?.unsignedComponent }) {
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
