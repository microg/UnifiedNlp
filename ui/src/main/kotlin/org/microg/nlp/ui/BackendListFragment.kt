/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import org.microg.nlp.api.Constants.*
import org.microg.nlp.client.UnifiedLocationClient
import org.microg.nlp.ui.BackendDetailsFragment.Companion.EXTRA_NAME
import org.microg.nlp.ui.BackendDetailsFragment.Companion.EXTRA_PACKAGE
import org.microg.nlp.ui.BackendDetailsFragment.Companion.EXTRA_TYPE
import org.microg.nlp.ui.databinding.BackendListBinding
import org.microg.nlp.ui.databinding.BackendListEntryBinding

class BackendListFragment : Fragment(R.layout.backend_list) {
    val locationAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)
    val geocoderAdapter: BackendSettingsLineAdapter = BackendSettingsLineAdapter(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = BackendListBinding.inflate(inflater, container, false)
        binding.fragment = this
        lifecycleScope.launchWhenStarted { updateAdapters() }
        return binding.root
    }

    fun onBackendSelected(entry: BackendInfo) {
        val intent = Intent(BackendDetailsFragment.ACTION)
        //intent.`package` = requireContext().packageName
        intent.putExtra(EXTRA_TYPE, entry.type.name)
        intent.putExtra(EXTRA_PACKAGE, entry.serviceInfo.packageName)
        intent.putExtra(EXTRA_NAME, entry.serviceInfo.name)
        context?.packageManager?.queryIntentActivities(intent, 0)?.forEach {
            Log.d("USettings", it.activityInfo.name)
        }
        startActivity(intent)
    }

    private suspend fun updateAdapters() {
        val context = requireContext()
        locationAdapter.entries = createBackendInfoList(context, Intent(ACTION_LOCATION_BACKEND), UnifiedLocationClient[context].getLocationBackends(), BackendType.LOCATION)
        geocoderAdapter.entries = createBackendInfoList(context, Intent(ACTION_GEOCODER_BACKEND), UnifiedLocationClient[context].getGeocoderBackends(), BackendType.GEOCODER)
    }

    private fun createBackendInfoList(context: Context, intent: Intent, enabledBackends: Array<String>, type: BackendType): Array<BackendInfo> {
        val backends = context.packageManager.queryIntentServices(intent, GET_META_DATA).map { BackendInfo(context, it.serviceInfo, type, lifecycleScope, enabledBackends) }
        return backends.toTypedArray()
    }
}

class BackendSettingsLineViewHolder(val binding: BackendListEntryBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fragment: BackendListFragment, entry: BackendInfo) {
        binding.fragment = fragment
        binding.entry = entry
        binding.executePendingBindings()
    }
}

class BackendSettingsLineAdapter(val fragment: BackendListFragment) : RecyclerView.Adapter<BackendSettingsLineViewHolder>() {
    var entries: Array<BackendInfo> = emptyArray()
        set(value) {
            field = value
            notifyDataSetChanged()
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


