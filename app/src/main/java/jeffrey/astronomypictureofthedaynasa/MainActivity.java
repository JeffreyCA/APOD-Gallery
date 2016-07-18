package jeffrey.astronomypictureofthedaynasa;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sothree.slidinguppanel.FloatingActionButtonLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // NASA API key
    final private String API_KEY = "***REMOVED***";
    // Logging tag for listeners
    final String TAG = "LISTENER";

    String today;
    String current_date;

    TextView date;
    SlidingUpPanelLayout sliding_panel;
    FloatingActionButton fab;
    FloatingActionButtonLayout fab_layout;
    ImageView image_view;
    ImageView tomorrow;
    ImageView yesterday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.showOverflowMenu();
        setSupportActionBar(myToolbar);

        // Initiate image views
         image_view = (ImageView) findViewById(R.id.image);
         yesterday = (ImageView) findViewById(R.id.left_chevron);
         tomorrow = (ImageView) findViewById(R.id.right_chevron);

        // Other views
        date = (TextView) findViewById(R.id.date);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab_layout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);

        // Retrieve image from server
        Glide.with(this).load("http://apod.nasa.gov/apod/image/1607/ayiomamitis-star-trails-marathon-oinoe-2016.jpg").into(image_view);

        // Set date view
        today = current_date = dateToString();
        date.setText(current_date);

        // No "tomorrow" image available if default day is "today"
        tomorrow.setVisibility(View.INVISIBLE);
        tomorrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                current_date = nextDay(current_date);
                date.setText(current_date);

                if (current_date.equals(today))
                    tomorrow.setVisibility(View.INVISIBLE);
            }
        });

        yesterday.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Display previous date
                current_date = prevDay(current_date);
                date.setText(current_date);

                if (tomorrow.getVisibility() == View.INVISIBLE)
                    tomorrow.setVisibility(View.VISIBLE);
            }
        });


        // Sliding up panel listener
        sliding_panel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        sliding_panel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

            // Hide FAB while expanded
            @Override
            public void onPanelExpanded(View panel) {
                Log.i(TAG, "onPanelExpanded");
                fab.hide();
            }

            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            // Show FAB while collapsed
            @Override
            public void onPanelCollapsed(View panel) {
                Log.i(TAG, "onPanelCollapsed");
                fab.show();
            }

            @Override
            public void onPanelAnchored(View panel) {
                Log.i(TAG, "onPanelAnchored");
            }

            @Override
            public void onPanelHidden(View panel) {
                Log.i(TAG, "onPanelHidden");
            }

            @Override
            public void onPanelHiddenExecuted(View panel, Interpolator interpolator, int duration) {
                Log.i(TAG, "onPanelHiddenExecuted");
            }

            @Override
            public void onPanelShownExecuted(View panel, Interpolator interpolator, int duration) {
                Log.i(TAG, "onPanelShownExecuted");
            }

            @Override
            public void onPanelExpandedStateY(View panel, boolean reached) {
                Log.i(TAG, "onPanelExpandedStateY");
            }

            @Override
            public void onPanelCollapsedStateY(View panel, boolean reached) {
                Log.i(TAG, "onPanelCollapsedStateY");
            }

            @Override
            public void onPanelLayout(View panel, SlidingUpPanelLayout.PanelState state) {
                Log.i(TAG, "onPanelLayout");
            }
        });

    } // End onCreate method

    /**
     * Convert current day to string format
     * @return Today's date as string
     */
    private String dateToString() {
        return new SimpleDateFormat("MMMM d, y").format(new Date());
    }

    /**
     * Calculate the day after the given date
     * @param date formatted date
     * @return the next day after the provided date
     */
    private String nextDay(String date) {
        Calendar calendar = Calendar.getInstance();
        Date next_date;
        SimpleDateFormat format = new SimpleDateFormat("MMMM d, y");

        try {
            next_date = format.parse(date);
            calendar.setTime(next_date);
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            return format.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculate the day before the given date
     * @param date formatted date
     * @return the previous day before the provided date
     */
    private String prevDay(String date) {
        Calendar calendar = Calendar.getInstance();
        Date next_date;
        SimpleDateFormat format = new SimpleDateFormat("MMMM d, y");

        try {
            next_date = format.parse(date);
            calendar.setTime(next_date);
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            return format.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Inflate options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Manage menu selection
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_settings:
                // editNote(info.id);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

	/*@Override
	public void onBackPressed() {
        if (mLayout != null &&
                (mLayout.getPanelState() == PanelState.EXPANDED || mLayout.getPanelState() == PanelState.ANCHORED)) {
            mLayout.setPanelState(PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }*/
}
