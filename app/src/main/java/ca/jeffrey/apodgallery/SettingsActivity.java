package ca.jeffrey.apodgallery;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
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

import com.anupcowkur.reservoir.Reservoir;
import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;

public class SettingsActivity extends Activity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    final static String TAG_PREF_CACHE = "pref_clear_cache";
    final static String TAG_PREF_CREDIT = "pref_display_credit";
    final static String TAG_PREF_LOCATION = "pref_save_location";
    final static String TAG_PREF_QUALITY = "pref_image_quality";
    final static String TAG_PREF_VERSION = "pref_version";

    private Activity thisActivity;
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

    @Override
    protected void onPause() {
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent();
        }
        else {
            root = (LinearLayout) findViewById(android.R.id.list).getParent();
        }

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
            case TAG_PREF_LOCATION:
                String directory = saveDirectory.getText();
                if (directory.length() == 0) {
                    directory = "/sdcard/APOD/";
                }
                else if (directory.charAt(directory.length() - 1) != '/') {
                    directory += '/';
                }
                saveDirectory.setText(directory);
                editor.putString(key, directory);
                saveDirectory.setSummary(directory);
                break;
            default:
                break;
        }
        editor.apply();
    }

    /**
     * Clear app cache
     */
    private void clearApplicationCache() {
        File cacheDirectory = this.getCacheDir();
        File[] cacheFiles = cacheDirectory.listFiles();

        if (cacheDirectory.exists()) {
            for (File file : cacheFiles) {
                if (file.isFile()) {
                    deleteFile(file);
                }
            }
        }
    }

    /**
     * Delete file from device
     * @param file File to be deleted
     * @return true, if deletion is successful, otherwise false
     */
    private boolean deleteFile(File file) {
        boolean deletedAll = true;
        if (file != null) {
            if (file.isDirectory()) {
                String[] children = file.list();
                for (int i = 0; i < children.length; i++) {
                    deletedAll = deleteFile(new File(file, children[i])) && deletedAll;
                }
            }
            else {
                deletedAll = file.delete();
            }
        }
        return deletedAll;
    }

    public static class PrefsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            // Set app version number
            PreferenceScreen appVersion = (PreferenceScreen) findPreference(TAG_PREF_VERSION);
            appVersion.setSummary(BuildConfig.VERSION_NAME);

            Preference clearCache = findPreference(TAG_PREF_CACHE);
            clearCache.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    SettingsActivity instance = ((SettingsActivity) getActivity());
                    ClearCacheTask task = new ClearCacheTask(getActivity());
                    task.execute();
                    try {
                        Reservoir.clear();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    instance.clearApplicationCache();
                    return true;
                }
            });

            // Set save location summary to its contents
            saveDirectory = (EditTextPreference) findPreference(TAG_PREF_LOCATION);
            saveDirectory.setSummary(saveDirectory.getText());
        }

        /**
         * Clear image cache
         */
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
                Toast.makeText(getActivity(), R.string.toast_clear_cache, Toast.LENGTH_SHORT).show();
            }
        }
    }
}