/*
 * Copyright 2013-2016 microG Project Team
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

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;

import org.microg.nlp.standalone.BuildConfig;
import org.microg.nlp.standalone.R;
import org.microg.nlp.standalone.selfcheck.StandaloneNlpStatusChecks;
import org.microg.tools.selfcheck.PermissionCheckGroup;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.ui.AbstractAboutFragment;
import org.microg.tools.ui.AbstractSelfCheckFragment;
import org.microg.tools.ui.AbstractSettingsFragment;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import org.microg.nlp.standalone.service.UnifiedNlpLocationManager;

public class StandaloneSettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, new MyPreferenceFragment()).commit();
        /*Intent intent1 = new Intent(this, UnifiedNlpLocationManager.class);
        intent1.setAction(IUnifiedNlpLocationManager.class.getName());
        bindService(intent1, mConnection, Context.BIND_AUTO_CREATE);*/
    }
    
    /*private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                IBinder service) {            
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            
        }
    };*/

    private static boolean isUnifiedNlpAppRelease(Context context) {
        int resId = context.getResources().getIdentifier("is_unifiednlp_app", "bool", context.getPackageName());
        return resId != 0 && context.getResources().getBoolean(resId);
    }

    public static class MyPreferenceFragment extends AbstractSettingsFragment {
        @Override
        public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
            if (isUnifiedNlpAppRelease(getContext())) {
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
            if (isUnifiedNlpAppRelease(getContext())) {
                addPreferencesFromResource(R.xml.nlp_about_preferences);

                findPreference(getString(R.string.pref_about_title))
                        .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                getFragmentManager().beginTransaction()
                                        .addToBackStack("root")
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .replace(R.id.content_wrapper, new MyAboutFragment())
                                        .commit();
                                return true;
                            }
                        });
            }
        }
    }

    public static class MySelfCheckFragment extends AbstractSelfCheckFragment {

        @Override
        protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
            if (SDK_INT > LOLLIPOP_MR1) {
                checks.add(new PermissionCheckGroup(ACCESS_COARSE_LOCATION));
            }
            checks.add(new StandaloneNlpStatusChecks());   
        }
    }

    public static class MyAboutFragment extends AbstractAboutFragment {

        @Override
        protected String getSummary() {
            String packageName = getContext().getPackageName();
            if (packageName.equals("com.google.android.gms")) {
                return getString(R.string.nlp_version_default);
            } else if (packageName.equals("com.google.android.location")) {
                return getString(R.string.nlp_version_legacy);
            } else if (packageName.equals("org.microg.nlp")) {
                return getString(R.string.nlp_version_custom);
            }
            return null;
        }

        @Override
        protected String getSelfVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @Override
        protected void collectLibraries(List<Library> libraries) {
            libraries.add(new Library("org.microg.nlp.api", "microG UnifiedNlp Api", "Apache License 2.0 by microG Team"));
        }
    }
}
