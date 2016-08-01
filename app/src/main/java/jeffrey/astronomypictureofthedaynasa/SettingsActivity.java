package jeffrey.astronomypictureofthedaynasa;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class SettingsActivity extends Activity implements SharedPreferences
        .OnSharedPreferenceChangeListener {
    public static final String KEY_PREF_SYNC_CONN = "pref_syncConnectionType";
    private static EditTextPreference saveDirectory;
    private static SwitchPreference displayCredits;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                Log.i("CHECKED", "" + sharedPreferences.getBoolean(key, false));
                // editor.putBoolean(key, displayCredits.isChecked());
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

            saveDirectory = (EditTextPreference) findPreference("pref_save_location");
            saveDirectory.setSummary(saveDirectory.getText());

            displayCredits = (SwitchPreference) findPreference("pref_display_credit");
        }
    }
}