/*
 * Copyright 2013-2015 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.nlp.ui;

import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.microg.nlp.R;
import org.microg.tools.selfcheck.NlpOsCompatChecks;
import org.microg.tools.selfcheck.NlpStatusChecks;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.ui.AbstractSelfCheckFragment;

import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, new MyPreferenceFragment()).commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (!getContext().getPackageName().equals("com.google.android.gms")
                    || getResources().getIdentifier("is_gmscore", "bool", "com.google.android.gms") == 0) {
                addPreferencesFromResource(R.xml.nlp_setup_preferences);

                findPreference(getString(R.string.self_check_title))
                        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                getFragmentManager().beginTransaction()
                                        .addToBackStack("root")
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .replace(R.id.content_wrapper, new MySelfCheckFragment())
                                        .commit();
                                return true;
                            }
                        });
            }
            addPreferencesFromResource(R.xml.nlp_preferences);
        }
    }

    public static class MySelfCheckFragment extends AbstractSelfCheckFragment {

        @Override
        protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
            checks.add(new NlpOsCompatChecks());
            checks.add(new NlpStatusChecks());
        }
    }
}
