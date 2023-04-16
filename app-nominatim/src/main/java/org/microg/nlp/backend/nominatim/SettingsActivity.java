package org.microg.nlp.backend.nominatim;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_AppCompat);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().show();
        }

        // Display the fragment as the main content.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new PrefsFragment())
                .commit();
    }

    public static class PrefsFragment extends PreferenceFragmentCompat {
        public static final String catApiKeyToken = "cat_api_preference";
        public static final String apiChoiceToken = "api_server_choice";
        public static final String mapQuestApiKeyToken = "api_preference";

        private SharedPreferences shPref;
        private SharedPreferences.OnSharedPreferenceChangeListener listener;

        private Preference mApiChoicePref;
        private Preference mCatAPIKeyPref;
        private Preference mMapQuestApiKeyPref;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            shPref = getPreferenceManager().getSharedPreferences();

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            mApiChoicePref = findPreference(apiChoiceToken);
            mCatAPIKeyPref = findPreference(catApiKeyToken);
            mMapQuestApiKeyPref = findPreference(mapQuestApiKeyToken);

            refreshPrefs();

            // Need explicit reference.
            // See :
            // http://stackoverflow.com/a/3104265
            listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    updatePreference(findPreference(key), key);
                }
            };
            shPref.registerOnSharedPreferenceChangeListener(listener);

            mApiChoicePref.setSummary(shPref.getString(apiChoiceToken, "OSM"));
            mMapQuestApiKeyPref.setSummary(shPref.getString(mapQuestApiKeyToken, ""));
        }

        private void refreshPrefs() {
            String apiServer = shPref.getString(apiChoiceToken, "OSM");
            if (apiServer.equals("OSM")) {
                getPreferenceScreen().removePreference(mCatAPIKeyPref);
            } else {
                getPreferenceScreen().addPreference(mCatAPIKeyPref);
            }
        }

        private void updatePreference(Preference preference, String key) {
            refreshPrefs();

            if (preference == null) {
                return;
            }

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntry());
                return;
            }

            preference.setSummary(shPref.getString(key, "Default"));
        }
    }
}
