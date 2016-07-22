package jeffrey.astronomypictureofthedaynasa;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
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
import com.android.volley.DefaultRetryPolicy;
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
import com.bluejamesbond.text.DocumentView;
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

// TODO Clear cache option
// TODO Set as wallpaper
// TODO Date Picker
// TODO Share image/save on device

public class MainActivity extends AppCompatActivity {

    // Logging tag for listeners
    final String TAG = "LISTENER";
    final String DATE_FORMAT = "MMMM d, y";
    final String API_DATE_FORMAT = "y-MM-dd";
    // NASA API key
    final private String API_KEY = "***REMOVED***";

    AutoResizeTextView titleText;
    DocumentView description;
    FloatingActionButton fab;
    FloatingActionButtonLayout fabLayout;
    ImageView imageView;
    ImageView tomorrow;
    ImageView yesterday;
    ProgressBar progressBar;
    SlidingUpPanelLayout slidingPanel;
    TextView dateText;

    String date;
    String today;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize toolbar
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
        description = (DocumentView) findViewById(R.id.description);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabLayout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        titleText = (AutoResizeTextView) findViewById(R.id.title);

        // Set scrollable description text
        if (description != null)
            description.setVerticalScrollBarEnabled(true);

        // Set date view
        today = date = dateToString();
        dateText.setText(date);

        // Set image
        getImageData(date);

        // No "tomorrow" image available if default day is "today"
        tomorrow.setVisibility(View.INVISIBLE);
        tomorrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Glide.clear(imageView);
                progressBar.setVisibility(View.VISIBLE);

                // Display next date
                date = nextDay(date);
                dateText.setText(date);

                if (date.equals(today))
                    tomorrow.setVisibility(View.INVISIBLE);
                if (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED &&
                        fab.getVisibility() == View.GONE)
                    fab.show();

                // Set image
                getImageData(date);
            }
        });

        yesterday.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Glide.clear(imageView);
                progressBar.setVisibility(View.VISIBLE);

                // Display previous date
                date = prevDay(date);
                dateText.setText(date);

                if (tomorrow.getVisibility() == View.INVISIBLE)
                    tomorrow.setVisibility(View.VISIBLE);

                if (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED &&
                        fab.getVisibility() == View.GONE)
                    fab.show();

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
                // Scroll text up so it is hidden when panel is collapsed
                description.scrollTo(0, 0);
            }

            @Override
            public void onPanelAnchored(View panel) {
            }

            @Override
            public void onPanelHidden(View panel) {
            }

            @Override
            public void onPanelHiddenExecuted(View panel, Interpolator interpolator, int duration) {
            }

            @Override
            public void onPanelShownExecuted(View panel, Interpolator interpolator, int duration) {
            }

            @Override
            public void onPanelExpandedStateY(View panel, boolean reached) {
            }

            @Override
            public void onPanelCollapsedStateY(View panel, boolean reached) {
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

    private String expandedToNumericalDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT);

        // Convert date formats to yyyy-mm-dd
        try {
            return apiFormat.format(format.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    private String numericalToExpandedDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        SimpleDateFormat apiFormat = new SimpleDateFormat(API_DATE_FORMAT);

        // Convert date formats to MMMM dd, yyyy
        try {
            return format.format(apiFormat.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
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

    private void onJsonResponse(JSONObject response) {
        try {
            final String IMAGE_TYPE = "image";

            String date = response.getString("date");
            String explanation = response.getString("explanation");

            final String sdUrl = response.getString("url");
            String mediaType = response.getString("media_type");
            final String title = response.getString("title");
            String hdUrl = "";
            String copyright = "";

            if (response.has("copyright"))
                copyright = response.getString("copyright");
            else if (response.has("hdurl"))
                hdUrl = response.getString("hdurl");

            boolean hdAvailable = !(hdUrl.equals(sdUrl));

            imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    launchFullImageView(sdUrl);
                }
            });

            // Load lower-resolution image by default
            titleText.setText(title);
            description.setText(explanation);

            if (mediaType.equals(IMAGE_TYPE)) {
                Glide.with(MainActivity.this).load(sdUrl) // Load from URL
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                        .centerCrop()
                        //.dontAnimate() // No cross-fade
                        .skipMemoryCache(true) // Use disk cache only
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model,
                                                       Target<GlideDrawable> target, boolean
                                                               isFirstResource) {

                                return false;
                            }

                            @Override
                            public boolean onResourceReady(GlideDrawable resource, String model,
                                                           Target<GlideDrawable> target, boolean
                                                                   isFromMemoryCache, boolean
                                                                   isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                        }).into(imageView);
            }
            else {
                openInBrowserDialog(date, sdUrl);
            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void getImageData(String date) {
        // Parse date
        String apiDate = expandedToNumericalDate(date);

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
                onJsonResponse(response);
            }
        }, new Response.ErrorListener() {
            // Handle Volley errors
            @Override
            public void onErrorResponse(VolleyError error) {
                int messageId;
                progressBar.setVisibility(View.GONE);

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    messageId = R.string.error_internet;
                }
                else if (error instanceof AuthFailureError) {
                    messageId = R.string.error_auth;
                }
                else if (error instanceof ServerError) {
                    messageId = R.string.error_server;
                }
                else if (error instanceof NetworkError) {
                    messageId = R.string.error_network;
                }
                else if (error instanceof ParseError) {
                    messageId = R.string.error_parse;
                }
                else {
                    messageId = R.string.error_general;
                }

                // Display long toast message
                Toast.makeText(MainActivity.this, messageId, Toast.LENGTH_LONG).show();
            }

        }) {
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

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000, 0, DefaultRetryPolicy
                .DEFAULT_BACKOFF_MULT));

        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest);
    }

    /**
     * Launch activity to display image in fullscreen
     *
     * @param url URL of the image
     */
    public void launchFullImageView(String url) {
        Intent intent = new Intent(MainActivity.this, FullImageActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    private void openInBrowserDialog(String date, String url) {
        final String uri = url;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = String.format(getResources().getString(R.string.dialog_browser_desc),
                numericalToExpandedDate(date));

        imageView.setImageResource(0);
        progressBar.setVisibility(View.GONE);
        fab.hide();

        builder.setTitle(R.string.dialog_browser_title);
        builder.setMessage(message);

        builder.setPositiveButton(R.string.action_yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                // Open Link in browser
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(browserIntent);
            }
        });

        builder.setNegativeButton(R.string.action_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }
}
