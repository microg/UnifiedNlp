/*
 * Copyright (C) 2013-2017 microG Project Team
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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;

import org.microg.nlp.BuildConfig;
import org.microg.nlp.R;
import org.microg.tools.selfcheck.NlpOsCompatChecks;
import org.microg.tools.selfcheck.NlpStatusChecks;
import org.microg.tools.selfcheck.PermissionCheckGroup;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.ui.AbstractAboutFragment;
import org.microg.tools.ui.AbstractSelfCheckFragment;
import org.microg.tools.ui.AbstractSettingsFragment;

import java.io.File;
import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;
import android.preference.PreferenceManager;

import com.obsez.android.lib.filechooser.ChooserDialog;

import org.microg.nlp.util.LogToFile;

public class SettingsActivity extends AppCompatActivity {
    
    public static final String KEY_DEBUG_FILE = "debug.log.file";
    public static final String KEY_DEBUG_TO_FILE = "debug.to.file";
    public static final String KEY_DEBUG_FILE_LASTING_HOURS = "debug.file.lasting.hours";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_wrapper, new MyPreferenceFragment()).commit();
    }

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

            addPreferencesFromResource(R.xml.nlp_debug_preferences);
            initLogFileChooser();
            initLogFileLasting();
            
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
        
        private void initLogFileChooser() {

            Preference logToFileCheckbox = findPreference(KEY_DEBUG_TO_FILE);
            logToFileCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, Object value) {
                    boolean logToFile = (Boolean) value;
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    preferences.edit().putBoolean(KEY_DEBUG_TO_FILE, logToFile).apply();
                    return true;
                }
            });

            Preference buttonFileLog = findPreference(KEY_DEBUG_FILE);
            buttonFileLog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(final Preference preference) {
                    new ChooserDialog().with(getContext())
                            .withFilter(true, false)
                            .withStartFile("/mnt")
                            .withChosenListener(new ChooserDialog.Result() {
                                @Override
                                public void onChoosePath(String path, File pathFile) {
                                    String logFileName = path + "/log-unifiednlp.txt";
                                    LogToFile.logFilePathname = logFileName;
                                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                                    preferences.edit().putString(KEY_DEBUG_FILE, logFileName).apply();
                                    preference.setSummary(preferences.getString(KEY_DEBUG_FILE,""));
                                }
                            })
                            .build()
                            .show();
                    return true;
                }
            });
        }

        private void initLogFileLasting() {
            Preference logFileLasting = findPreference(KEY_DEBUG_FILE_LASTING_HOURS);
            logFileLasting.setSummary(
                    getLogFileLastingLabel(Integer.parseInt(
                            PreferenceManager.getDefaultSharedPreferences(getContext()).getString(KEY_DEBUG_FILE_LASTING_HOURS, "24"))
                    )
            );
            logFileLasting.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference logFileLasting, Object value) {
                    String logFileLastingHoursTxt = (String) value;
                    Integer logFileLastingHours = Integer.valueOf(logFileLastingHoursTxt);
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    preferences.edit().putString(KEY_DEBUG_FILE_LASTING_HOURS, logFileLastingHoursTxt).apply();
                    logFileLasting.setSummary(getString(getLogFileLastingLabel(logFileLastingHours)));
                    LogToFile.logFileHoursOfLasting = logFileLastingHours;
                    return true;
                }
            });
        }

        private int getLogFileLastingLabel(int logFileLastingValue) {
            int logFileLastingId;
            switch (logFileLastingValue) {
                case 12:
                    logFileLastingId = R.string.log_file_12_label;
                    break;
                case 48:
                    logFileLastingId = R.string.log_file_48_label;
                    break;
                case 72:
                    logFileLastingId = R.string.log_file_72_label;
                    break;
                case 168:
                    logFileLastingId = R.string.log_file_168_label;
                    break;
                case 720:
                    logFileLastingId = R.string.log_file_720_label;
                    break;
                case 24:
                default:
                    logFileLastingId = R.string.log_file_24_label;
                    break;
            }
            return logFileLastingId;
        }
    }

    public static class MySelfCheckFragment extends AbstractSelfCheckFragment {

        @Override
        protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
            if (SDK_INT > LOLLIPOP_MR1) {
                checks.add(new PermissionCheckGroup(ACCESS_COARSE_LOCATION));
            }
            checks.add(new NlpOsCompatChecks());
            checks.add(new NlpStatusChecks());
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
