/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.nlp.app

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import org.microg.nlp.client.UnifiedLocationClient
import org.microg.nlp.app.tools.ui.ResourceSettingsFragment
import org.microg.nlp.ui.navigate

class SettingsFragment : ResourceSettingsFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)

        findPreference<Preference>(PREF_UNIFIEDNLP)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openUnifiedNlpSettings)
            true
        }
        findPreference<Preference>(PREF_ABOUT)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            findNavController().navigate(requireContext(), R.id.openAbout)
            true
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launchWhenResumed {
            updateDetails()
        }
    }

    private suspend fun updateDetails() {
        val backendCount = UnifiedLocationClient[requireContext()].getLocationBackends().size + UnifiedLocationClient[requireContext()].getGeocoderBackends().size
        findPreference<Preference>(PREF_UNIFIEDNLP)!!.summary = resources.getQuantityString(R.plurals.pref_unifiednlp_summary, backendCount, backendCount);
    }

    companion object {
        const val PREF_ABOUT = "pref_about"
        const val PREF_UNIFIEDNLP = "pref_unifiednlp"
    }

    init {
        preferencesResource = R.xml.preferences_start
    }
}
