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
    final private String API_KEY = "***REMOVED***";
    String today;
    String current_date;
    TextView date;
    FloatingActionButton fab;
    FloatingActionButtonLayout fab_layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        myToolbar.showOverflowMenu();
        setSupportActionBar(myToolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab_layout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);

        ImageView imageView = (ImageView) findViewById(R.id.image);
        Glide.with(this).load("http://apod.nasa.gov/apod/image/1607/ayiomamitis-star-trails-marathon-oinoe-2016.jpg").into(imageView);

        date = (TextView) findViewById(R.id.date);
        today = current_date = dateToString();
        date.setText(current_date);

        ImageView yesterday = (ImageView) findViewById(R.id.left_chevron);
        final ImageView tomorrow = (ImageView) findViewById(R.id.right_chevron);
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
                current_date = prevDay(current_date);
                date.setText(current_date);

                if (tomorrow.getVisibility() == View.INVISIBLE)
                    tomorrow.setVisibility(View.VISIBLE);
            }
        });

        final String TAG = "LISTENER";
        SlidingUpPanelLayout sliding_panel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        sliding_panel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            FloatingActionButton fabe = (FloatingActionButton) findViewById(R.id.fab);

            @Override
            public void onPanelExpanded(View panel) {
                Log.i(TAG, "onPanelExpanded");
                fabe.hide();
            }

            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelCollapsed(View panel) {
                Log.i(TAG, "onPanelCollapsed");
                fabe.show();
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
    }

    private String dateToString() {
        return new SimpleDateFormat("MMMM d, y").format(new Date());
    }

    private String nextDay(String date) {
        final Calendar calendar = Calendar.getInstance();
        final Date next_date;
        final SimpleDateFormat format = new SimpleDateFormat("MMMM d, y");

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

    private String prevDay(String date) {
        final Calendar calendar = Calendar.getInstance();
        final Date next_date;
        final SimpleDateFormat format = new SimpleDateFormat("MMMM d, y");

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

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
