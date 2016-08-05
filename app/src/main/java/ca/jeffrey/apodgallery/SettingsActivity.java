package ca.jeffrey.apodgallery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;

public class SettingsActivity extends Activity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_SYNC_CONN = "pref_syncConnectionType";
    private static Activity thisActivity;
    private static EditTextPreference saveDirectory;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisActivity = this;
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new PrefsFragment()
        ).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onPause()
     */
    @Override
    protected void onPause() {
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent
                ().getParent();
        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.view_toolbar, root,
                false);
        bar.setTitle(R.string.title_activity_settings);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SharedPreferences.Editor editor = prefs.edit();

        switch (key) {
            case "pref_save_location":
                String directory = saveDirectory.getText();
                if (directory.charAt(directory.length() - 1) != '/') {
                    directory += '/';
                    saveDirectory.setText(directory);
                }
                editor.putString(key, directory);
                saveDirectory.setSummary(directory);
                break;
            case "pref_display_credit":
                break;
            default:
                break;
        }
        editor.apply();
    }

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // Set app version number
            PreferenceScreen appVersion = (PreferenceScreen) findPreference("pref_version");
            appVersion.setSummary(BuildConfig.VERSION_NAME);

            Preference clearCache = findPreference("pref_clear_cache");
            clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    ClearCacheTask task = new ClearCacheTask(thisActivity);
                    task.execute();
                    return true;
                }
            });

            // Set save location summary to its contents
            saveDirectory = (EditTextPreference) findPreference("pref_save_location");
            saveDirectory.setSummary(saveDirectory.getText());
        }

        class ClearCacheTask extends AsyncTask<Void, Void, Long> {
            private Context context;

            public ClearCacheTask(Context context) {
                this.context = context;
            }

            @Override
            protected Long doInBackground(Void... params) {
                Glide.get(context).clearDiskCache();
                return 0L;
            }

            @Override
            protected void onPostExecute(Long result) {
                Toast.makeText(thisActivity, R.string.toast_clear_cache, Toast.LENGTH_SHORT).show();
            }
        }
    }
}