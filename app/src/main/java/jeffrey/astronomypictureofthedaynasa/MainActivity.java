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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.sothree.slidinguppanel.FloatingActionButtonLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    // Logging tag for listeners
    final String TAG = "LISTENER";
    final String DATE_FORMAT = "MMMM d, y";
    final String API_DATE_FORMAT = "y-MM-dd";
    // NASA API key
    final private String API_KEY = "***REMOVED***";
    String today;
    String date;

    TextView dateText;
    AutoResizeTextView titleText;
    SlidingUpPanelLayout slidingPanel;
    FloatingActionButton fab;
    FloatingActionButtonLayout fabLayout;
    ImageView imageView;
    ImageView tomorrow;
    ImageView yesterday;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        if (myToolbar != null) {
            myToolbar.showOverflowMenu();
        }

        // Initiate image views
        imageView = (ImageView) findViewById(R.id.image);
        yesterday = (ImageView) findViewById(R.id.left_chevron);
        tomorrow = (ImageView) findViewById(R.id.right_chevron);

        // Other views
        dateText = (TextView) findViewById(R.id.date);
        titleText = (AutoResizeTextView) findViewById(R.id.title);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabLayout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        // Set date view
        today = date = dateToString();
        dateText.setText(date);

        // Set image
        getImageData(date);

        // No "tomorrow" image available if default day is "today"
        tomorrow.setVisibility(View.INVISIBLE);
        tomorrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                date = nextDay(date);
                dateText.setText(date);

                if (date.equals(today))
                    tomorrow.setVisibility(View.INVISIBLE);

                // Animations are smooth enough with progress bar running behind image
                // imageView.setImageResource(0);
                // progressBar.setVisibility(View.VISIBLE); // pBar never set to INVISIBLE

                // Set image
                getImageData(date);
            }
        });

        yesterday.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Display previous date
                date = prevDay(date);
                dateText.setText(date);

                if (tomorrow.getVisibility() == View.INVISIBLE)
                    tomorrow.setVisibility(View.VISIBLE);

                // imageView.setImageResource(0);

                // Set image
                getImageData(date);
            }
        });


        // Sliding up panel listener
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        slidingPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {

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
            }

            @Override
            public void onPanelHidden(View panel) {
            }

            @Override
            public void onPanelHiddenExecuted(View panel, Interpolator interpolator, int duration) {
                // Log.i(TAG, "onPanelHiddenExecuted");
            }

            @Override
            public void onPanelShownExecuted(View panel, Interpolator interpolator, int duration) {
                // Log.i(TAG, "onPanelShownExecuted");
            }

            @Override
            public void onPanelExpandedStateY(View panel, boolean reached) {
                // Log.i(TAG, "onPanelExpandedStateY");
            }

            @Override
            public void onPanelCollapsedStateY(View panel, boolean reached) {
                // Log.i(TAG, "onPanelCollapsedStateY");
                fab.hide();
            }

            @Override
            public void onPanelLayout(View panel, SlidingUpPanelLayout.PanelState state) {
            }
        });
    } // End onCreate method


    /**
     * Convert current day to string format
     *
     * @return Today's date as string
     */
    private String dateToString() {
        return new SimpleDateFormat(DATE_FORMAT).format(new Date());
    }

    /**
     * Calculate the day after the given date
     *
     * @param date formatted date
     *
     * @return the next day after the provided date
     */
    private String nextDay(String date) {
        Calendar calendar = Calendar.getInstance();
        Date nextDate;
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);

        try {
            nextDate = format.parse(date);
            calendar.setTime(nextDate);
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            return format.format(calendar.getTime());
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calculate the day before the given date
     *
     * @param date formatted date
     *
     * @return the previous day before the provided date
     */
    private String prevDay(String date) {
        Calendar calendar = Calendar.getInstance();
        Date prevDate;
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);

        try {
            prevDate = format.parse(date);
            calendar.setTime(prevDate);
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            return format.format(calendar.getTime());
        }
        catch (ParseException e) {
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
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        // Sliding up panel listener
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if (slidingPanel != null && (slidingPanel.getPanelState() == SlidingUpPanelLayout
                .PanelState.EXPANDED)) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        else {
            super.onBackPressed();
        }
    }

    private void getImageData(String date) {
        // TODO Use locale date formatting
        // Parse date
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT);
        String apiDate = "";

        // Convert date formats to yyyy-mm-dd
        try {
            apiDate = apiFormat.format(format.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        RequestQueue queue;

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        queue = new RequestQueue(cache, network);

        // Start the queue
        queue.start();

        String url = "https://api.nasa.gov/planetary/apod?api_key=" + API_KEY + "&date=" + apiDate;

        Log.i("URL", url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url,
                null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    String date = response.getString("date");
                    String explanation = response.getString("explanation");
                    String hdUrl = response.getString("hdurl");
                    String sdUrl = response.getString("url");
                    final String title = response.getString("title");
                    String copyright = "";

                    if (response.has("copyright"))
                        copyright = response.getString("copyright");

                    boolean hdAvailable = !(hdUrl.equals(sdUrl));

                    /*
                    Log.i("REQUEST", "Date: " + date);
                    Log.i("REQUEST", "Title: " + title);
                    Log.i("REQUEST", "Explanation: " + explanation);
                    Log.i("REQUEST", "HD URL: " + hdUrl);
                    Log.i("REQUEST", "SD URL: " + sdUrl);
                    Log.i("REQUEST", "Diff URL: " + hdAvailable);
                    Log.i("REQUEST", "Copyright: " + copyright);
                    */

                    // TODO Clear cache option
                    // Load lower-resolution image by default
                    Glide.with(MainActivity.this).load(sdUrl) // Load from URL
                            .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                            .dontAnimate() // No cross-fade
                            .skipMemoryCache(true) // Use disk cache only
                            .listener(new RequestListener<String, GlideDrawable>() {
                                @Override
                                public boolean onException(Exception e, String model,
                                                           Target<GlideDrawable> target, boolean
                                                                   isFirstResource) {
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(GlideDrawable resource, String
                                        model, Target<GlideDrawable> target, boolean
                                                                       isFromMemoryCache, boolean
                                                                       isFirstResource) {
                                    Log.i("MEM_CACHE", "" + isFromMemoryCache);
                                    // progressBar.setVisibility(View.GONE);
                                    titleText.setText(title);
                                    return false;
                                }

                            }).into(imageView);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {

            // Handle Volley errors
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = "";

                imageView.setImageResource(0);

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    message = "No Internet access, check your Internet connection.";
                }
                else if (error instanceof AuthFailureError) {
                }
                else if (error instanceof ServerError) {
                    Toast.makeText(MainActivity.this, "Today's photo is not available yet.",
                            Toast.LENGTH_SHORT).show();
                }
                else if (error instanceof NetworkError) {
                }
                else if (error instanceof ParseError) {
                }

                Log.i("VOLLEY_ERROR", message);
                progressBar.setVisibility(View.INVISIBLE);

                // TODO Add error picture
            }

        }) {
            // Set headers
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            // Set caching
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }

                    final long cacheHitButRefreshed = 3 * 60 * 1000; // in 3 minutes cache will
                    // be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 24 hours this cache
                    // entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;

                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;

                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }

                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }

                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data, HttpHeaderParser
                            .parseCharset(response.headers));

                    return Response.success(new JSONObject(jsonString), cacheEntry);
                }
                catch (UnsupportedEncodingException e) {
                    return Response.error(new ParseError(e));
                }
                catch (JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }

            @Override
            protected void deliverResponse(JSONObject response) {
                super.deliverResponse(response);
            }

            @Override
            public void deliverError(VolleyError error) {
                super.deliverError(error);
            }
        };

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }
}
