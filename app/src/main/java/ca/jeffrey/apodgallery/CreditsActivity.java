package ca.jeffrey.apodgallery;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class CreditsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new CreditsFragment()
        ).commit();
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
        bar.setTitle(R.string.title_activity_credits);
        root.addView(bar, 0); // insert at top
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    public static class CreditsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.credits);
        }
    }
}
