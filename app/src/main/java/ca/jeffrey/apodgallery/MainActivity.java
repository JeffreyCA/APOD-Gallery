package ca.jeffrey.apodgallery;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.sothree.slidinguppanel.FloatingActionButtonLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO Fix ImageView scaling
// TODO Alternate views (gallery)

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

    // Date formats
    final SimpleDateFormat EXPANDED_FORMAT = new SimpleDateFormat("MMMM d, y");
    final SimpleDateFormat NUMERICAL_FORMAT = new SimpleDateFormat("y-MM-dd");
    final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat("yyMMdd");
    final float SLIDING_ANCHOR_POINT = 0.42f;
    final int DISABLED_DAYS = 155;
    final String DEFAULT_IMAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "APOD";
    final String IMAGE_EXT = ".jpg";

    // First available APOD date
    final Calendar MIN_DATE = new GregorianCalendar(2000, 0, 1);

    // NASA API key
    // final private String API_KEY = "***REMOVED***";
    AutoResizeTextView titleText;
    DocumentView description;
    FloatingActionButton fab;
    FloatingActionButtonLayout fabLayout;
    ImageView imageView;
    ImageView tomorrow;
    ImageView yesterday;
    LinearLayout mainView;
    ProgressBar progressBar;
    SlidingUpPanelLayout slidingPanel;
    TextView dateText;
    boolean tooEarly;
    Calendar[] disabledDays;
    String date;
    String today;
    String imgUrl;
    String sdUrl;
    SharedPreferences sharedPref;
    RequestQueue queue;
    private String[] disabledDayStrings = {"2000-01-05", "2000-01-06", "2000-01-08",
            "2000-02-08", "2000-02-29", "2000-03-07", "2000-03-21", "2000-03-28", "2000-05-19",
            "2000-07-28", "2000-08-17", "2000-08-29", "2000-09-06", "2000-09-29", "2000-10-13",
            "2000-11-03", "2000-11-06", "2000-11-08", "2000-11-18", "2000-12-06", "2000-12-27",
            "2000-12-28", "2001-01-22", "2001-02-14", "2001-02-19", "2001-02-20", "2001-02-23",
            "2001-02-24", "2001-02-25", "2001-02-27", "2001-03-01", "2001-03-08", "2001-04-09",
            "2001-04-28", "2001-07-06", "2001-07-11", "2001-07-31", "2001-08-08", "2001-12-03",
            "2002-01-13", "2002-02-25", "2002-03-03", "2002-03-14", "2002-05-22", "2002-05-30",
            "2002-06-21", "2002-07-09", "2002-09-16", "2002-10-15", "2002-10-31", "2002-11-18",
            "2002-11-27", "2002-12-04", "2002-12-06", "2003-02-27", "2003-02-27", "2003-03-09",
            "2003-04-21", "2003-06-18", "2003-09-29", "2003-10-07", "2003-10-17", "2003-12-29",
            "2004-01-12", "2004-03-25", "2004-03-26", "2004-03-27", "2004-08-25", "2004-09-28",
            "2004-09-28", "2005-01-29", "2005-04-09", "2005-04-09", "2005-05-22", "2005-10-16",
            "2006-03-29", "2006-05-29", "2006-07-06", "2006-07-07", "2006-08-15", "2006-11-04",
            "2006-11-18", "2006-12-01", "2006-12-16", "2007-01-18", "2007-02-03", "2007-02-21",
            "2007-03-02", "2007-03-15", "2007-03-16", "2007-03-17", "2007-05-18", "2007-05-22",
            "2007-09-29", "2008-06-21", "2008-07-22", "2008-10-16", "2008-11-25", "2008-12-31",
            "2009-01-16", "2009-02-01", "2009-03-02", "2009-04-05", "2009-04-13", "2009-05-01",
            "2009-06-29", "2009-07-09", "2009-07-11", "2009-08-10", "2009-10-17", "2009-12-14",
            "2009-12-26", "2009-12-30", "2009-12-31", "2010-01-05", "2010-01-20", "2010-01-24",
            "2010-02-10", "2010-03-06", "2010-04-08", "2010-04-09", "2010-05-10", "2010-05-26",
            "2010-06-08", "2010-07-22", "2010-07-25", "2010-08-25", "2010-08-25", "2010-11-20",
            "2010-12-15", "2011-01-20", "2011-01-23", "2011-02-01", "2011-02-22", "2011-03-07",
            "2011-06-02", "2011-06-10", "2011-06-28", "2011-07-18", "2011-07-27", "2012-01-01",
            "2012-01-09", "2012-03-12", "2012-05-23", "2013-05-01", "2013-05-03", "2013-05-03",
            "2013-11-27", "2014-01-12", "2014-01-17", "2014-02-10", "2014-12-20", "2015-03-14",
            "2015-04-23", "2015-08-01"};

    /**
     * Convert Date object to Calendar object
     *
     * @param date Date object
     *
     * @return Calendar object
     */
    public static Calendar dateToCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    public static String getHtmlTitle(String fragment) {
        int index = fragment.indexOf("-");
        return fragment.substring(index + 1).trim();
    }

    // Date Methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && getIntent()
                .getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN)) {

            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        if (myToolbar != null) {
            myToolbar.showOverflowMenu();
        }

        // Initialize preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        // Initiate image views
        imageView = (ImageView) findViewById(R.id.image);
        yesterday = (ImageView) findViewById(R.id.left_chevron);
        tomorrow = (ImageView) findViewById(R.id.right_chevron);

        // Other views
        dateText = (TextView) findViewById(R.id.date);
        description = (DocumentView) findViewById(R.id.description);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabLayout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);
        mainView = (LinearLayout) findViewById(R.id.main_view);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        titleText = (AutoResizeTextView) findViewById(R.id.title);

        tooEarly = false;
        disabledDays = new Calendar[DISABLED_DAYS];
        new setDisabledDays().execute();

        // Set scrollable description text
        if (description != null)
            description.setVerticalScrollBarEnabled(true);

        // Set date view
        today = date = EXPANDED_FORMAT.format(new Date());
        dateText.setText(date);

        // Set image
        getImageData(date);

        // No "tomorrow" image available if default day is "today"
        tomorrow.setVisibility(View.INVISIBLE);
        tomorrow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                nextDay();
            }
        });

        yesterday.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                previousDay();
            }
        });

        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /* Date Picker Library #2 (Does not allow for disabled dates, only enabled dates) */
                Calendar today = Calendar.getInstance();
                Calendar currentDate = Calendar.getInstance();
                try {
                    currentDate = dateToCalendar(EXPANDED_FORMAT.parse(date));
                }
                catch (ParseException e) {
                    e.printStackTrace();
                }

                DatePickerDialog dpd = DatePickerDialog.newInstance(MainActivity.this,
                        currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH),
                        currentDate.get(Calendar.DAY_OF_MONTH));
                dpd.setThemeDark(true);
                dpd.setMinDate(MIN_DATE);
                dpd.setMaxDate(today);
                dpd.setDisabledDays(disabledDays);
                dpd.vibrate(false);
                dpd.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        // Sliding up panel listener
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        slidingPanel.setAnchorPoint(SLIDING_ANCHOR_POINT);
        slidingPanel.setScrollableView(description);
        slidingPanel.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            // Hide FAB while expanded
            @Override
            public void onPanelExpanded(View panel) {
                fab.hide();
            }

            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            // Show FAB while collapsed
            @Override
            public void onPanelCollapsed(View panel) {
                fab.show();
                // Scroll text up so it is hidden when panel is collapsed
                description.smoothScrollTo(0, 0);
            }

            @Override
            public void onPanelAnchored(View panel) {
                fab.show();
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
            }

            @Override
            public void onPanelLayout(View panel, SlidingUpPanelLayout.PanelState state) {
            }
        });
        // Floating action button listener
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Save image to device if image is visible
                if (imageView.getDrawable() != null) {
                    saveImage(expandedToNumericalDate(date));
                    launchFullImageView(imgUrl, expandedToNumericalDate(date), true);
                }
                // No image available
                else {
                    displayImageNotAvailableToast();
                }
            }
        });
        // Display explanation of FAB
        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast.makeText(MainActivity.this, R.string.toast_fab, Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        // Set swiping gestures
        final GestureDetector gdt = new GestureDetector(MainActivity.this, new GestureListener());
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });

        mainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                gdt.onTouchEvent(event);
                return true;
            }
        });
    } // End onCreate method

    // Inflate options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        // Set colour of share icon to white (from black)
        MenuItem menuItem = menu.findItem(R.id.action_share);
        Drawable drawable = menuItem.getIcon();

        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(this, R.color.colorWhite), PorterDuff
                    .Mode.SRC_ATOP);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_share:
                shareImage(titleText.getText().toString());
                return true;
            case R.id.action_save:
                saveImage(expandedToNumericalDate(date));
                return true;
            case R.id.action_open_link:
                openLink();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set next day text
     */
    private void nextDay() {
        Glide.clear(imageView);
        progressBar.setVisibility(View.VISIBLE);

        date = getNextDay(date);
        dateText.setText(date);

        if (date.equals(today)) {
            tomorrow.setVisibility(View.INVISIBLE);
        }
        if (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED && fab
                .getVisibility() == View.GONE) {
            fab.show();
        }

        // Set image
        getImageData(date);
    }

    /**
     * Set previous day text
     */
    private void previousDay() {
        Glide.clear(imageView);
        progressBar.setVisibility(View.VISIBLE);

        // Display previous day
        date = getPreviousDay(date);
        dateText.setText(date);

        if (tomorrow.getVisibility() == View.INVISIBLE)
            tomorrow.setVisibility(View.VISIBLE);

        if (slidingPanel.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED && fab
                .getVisibility() == View.GONE)
            fab.show();

        // Set image
        getImageData(date);
    }

    /**
     * Calculate the day after the given date
     *
     * @param date formatted date
     *
     * @return the next day after the provided date
     */
    private String getNextDay(String date) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(EXPANDED_FORMAT.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            return EXPANDED_FORMAT.format(calendar.getTime());
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
    private String getPreviousDay(String date) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(EXPANDED_FORMAT.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            return EXPANDED_FORMAT.format(calendar.getTime());
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Switch to user-navigated date
     *
     * @param view        date picker dialog
     * @param year        year picked
     * @param monthOfYear month picked
     * @param dayOfMonth  day picket
     */
    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        imageView.setImageResource(0);
        progressBar.setVisibility(View.VISIBLE);

        // Create Calendar object of selected date
        Calendar pickedDate = new GregorianCalendar(year, monthOfYear, dayOfMonth);

        // Convert date into expanded format
        date = EXPANDED_FORMAT.format(pickedDate.getTime());
        dateText.setText(date);

        // Show/hide right navigation chevron
        if (tomorrow.getVisibility() == View.VISIBLE && date.equals(today)) {
            tomorrow.setVisibility(View.INVISIBLE);
        }
        else if (tomorrow.getVisibility() == View.INVISIBLE && !date.equals(today)) {
            tomorrow.setVisibility(View.VISIBLE);
        }

        // Show progress loading circle
        getImageData(date);
    }

    /**
     * Convert date format to yyyy-mm-dd
     *
     * @param date date in expanded format
     *
     * @return date in numerical format
     */
    private String expandedToNumericalDate(String date) {
        try {
            return NUMERICAL_FORMAT.format(EXPANDED_FORMAT.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Convert date format to MMMM dd, yyyy
     *
     * @param date date in numerical format
     *
     * @return date in expanded format
     */

    private String numericalToExpandedDate(String date) {
        try {
            return EXPANDED_FORMAT.format(NUMERICAL_FORMAT.parse(date));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Display toast when image is not available
     */
    private void displayImageNotAvailableToast() {
        Toast.makeText(MainActivity.this, R.string.toast_no_image, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get URL accessible in web browser
     *
     * @return browser-accessible URL
     */
    private String getFullUrl() {
        final String BASE_URL = "http://apod.nasa.gov/apod/ap";
        String shortDate;

        try {
            shortDate = SHORT_FORMAT.format(EXPANDED_FORMAT.parse(date));
            return BASE_URL + shortDate + ".html";
        }
        catch (ParseException e) {
            e.printStackTrace();
        }

        return "";
    }

    public void shareImage(String title) {
        final String IMAGE_DIRECTORY = sharedPref.getString("pref_save_location",
                DEFAULT_IMAGE_DIRECTORY);
        Intent share = new Intent(Intent.ACTION_SEND);

        // Share link if non-image content
        if (tooEarly) {
            displayImageNotAvailableToast();
        }
        else if (imageView.getDrawable() == null) {
            share.setType("text/plain");
            share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            share.putExtra(Intent.EXTRA_SUBJECT, title);
            share.putExtra(Intent.EXTRA_TEXT, getFullUrl());

            startActivity(Intent.createChooser(share, "Share link"));
        }
        // Otherwise share image
        else {
            share.setType("image/jpeg");
            share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            saveImage(expandedToNumericalDate(date));

            String path = IMAGE_DIRECTORY + expandedToNumericalDate(date) + IMAGE_EXT;
            File image = new File(path);
            Uri uri = Uri.fromFile(image);

            share.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(share, "Share image"));
        }
    }

    public void saveImage(String imageDate) {
        // Exit if no image is available
        if (imageView.getDrawable() == null) {
            displayImageNotAvailableToast();
            return;
        }

        final String DATE = imageDate;
        final String IMAGE_DIRECTORY = sharedPref.getString("pref_save_location",
                DEFAULT_IMAGE_DIRECTORY);
        ImageActivity.verifyStoragePermissions(this);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission
                .WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Load image with Glide as bitmap
            Glide.with(this).load(imgUrl).asBitmap().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {

                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                        glideAnimation) {

                    File imageDirectory = new File(IMAGE_DIRECTORY);

                    if (!imageDirectory.exists()) {
                        imageDirectory.mkdir();
                    }
                    String filename = DATE + IMAGE_EXT;
                    File image = new File(imageDirectory, filename);

                    String message = getResources().getString(R.string.toast_save_image) +
                            IMAGE_DIRECTORY + filename;

                    // Encode the file as a JPG image.
                    FileOutputStream outStream;
                    try {
                        outStream = new FileOutputStream(image);
                        resource.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

                        outStream.flush();
                        outStream.close();

                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                    catch (FileNotFoundException e) {
                        Toast.makeText(MainActivity.this, R.string.error_saving, Toast
                                .LENGTH_SHORT).show();
                    }
                    catch (IOException e) {
                        Toast.makeText(MainActivity.this, R.string.error_saving, Toast
                                .LENGTH_SHORT).show();
                    }
                }
            });
        }
        else {
            Toast.makeText(MainActivity.this, R.string.toast_storage, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        // Sliding up panel listener
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);

        if (slidingPanel != null && (slidingPanel.getPanelState() == SlidingUpPanelLayout
                .PanelState.EXPANDED)) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
        else {
            super.onBackPressed();
        }
    }

    /**
     * Get & parse image data and display image to screen
     *
     * @param response JSON object
     */
    private void onJsonResponse(JSONObject response) {
        final String IMAGE_TYPE = "image";

        boolean prefHd;
        boolean prefCopyright;
        String copyright;
        String explanation;
        String mediaType;
        String hdUrl;
        String title;

        try {
            // final String numericalDate = response.getString("date");
            explanation = response.getString("explanation");
            mediaType = response.getString("media_type");
            sdUrl = response.getString("url");
            title = response.getString("title");

            hdUrl = "";
            prefHd = sharedPref.getString("image_quality", "").equals("1");
            prefCopyright = sharedPref.getBoolean("pref_display_credit", false);

            // Check if HD image URL is included in response
            if (response.has("hdurl")) {
                hdUrl = response.getString("hdurl");
            }

            // Add copyright credits to end of description if setting allows it
            if (prefCopyright && response.has("copyright")) {
                copyright = response.getString("copyright");
                explanation += getResources().getString(R.string.title_credits) + copyright;
            }

            // Set image url depending on user preference and image availability
            if (prefHd && !hdUrl.equals("")) {
                imgUrl = hdUrl;
            }
            else {
                imgUrl = sdUrl;
            }

            // Set text
            titleText.setText(title);
            description.setText(explanation);

            if (mediaType.equals(IMAGE_TYPE)) {
                Glide.with(MainActivity.this).load(sdUrl) // Load from URL
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
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
                openNonImageContent(sdUrl);
            }
            tooEarly = false;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * GET request to NASA API
     *
     * @param date selected date
     */
    private void getImageData(String date) {
        final String mDate = date;
        // Parse date
        String apiDate = expandedToNumericalDate(date);

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap
        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());
        // Instantiate the RequestQueue with the cache and network.
        queue = new RequestQueue(cache, network);
        // Start the queue
        queue.start();

        String url = "https://api.nasa" +
                ".gov/planetary/apod?api_key=***REMOVED***" + "&date="
                + apiDate;

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

                if (error instanceof TimeoutError || error instanceof NoConnectionError) {
                    messageId = R.string.error_network;
                }
                else if (error instanceof AuthFailureError) {
                    messageId = R.string.error_auth;
                }
                else if (error instanceof ServerError) {
                    tooEarly = true;

                    if (mDate.equals(today)) {
                        messageId = R.string.error_today;
                    }
                    else {
                        messageId = -1;
                        parseHtml();
                    }
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
                if (messageId != -1) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, messageId, Toast.LENGTH_SHORT).show();
                }
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

    private void parseHtml() {
        final String EXPLANATION_HEADER = "Explanation:";
        final String YT_BASE_URL = "https://www.youtube.com/watch?v=";
        final String VM_BASE_URL = "https://vimeo.com/";
        String url = getFullUrl();
        Document doc = null;
        String contentUrl;

        Log.i("URL", url);


        if (doc == null)
            Log.i("Document", "NULL");
        // Image

        Element image = doc.select("img").first();
        Element video = doc.select("iframe[src~=(youtube\\.com|vimeo\\.com)], object[data~=" +
                "(youtube\\.com|vimeo\\.com)], embed[src~=(youtube\\.com|vimeo\\.com)]").first();
        Log.i("element gotten", "reached");
        // String imageUrl = image.absUrl("img");
        if (image != null) {
            contentUrl = image.absUrl("src");
            Glide.with(MainActivity.this).load(contentUrl) // Load from URL
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
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
        else if (video != null) {
            contentUrl = video.absUrl("src");
            System.out.print("Video URL: " + contentUrl);
            System.out.println(", ID: " + getVideoId(contentUrl));

            if (contentUrl.contains("youtu")) {
                contentUrl = YT_BASE_URL + getVideoId(contentUrl);
            }
            else if (contentUrl.contains("vimeo")) {
                contentUrl = VM_BASE_URL + getVideoId(contentUrl);
            }
        }
        // Maybe do this after loading title, explanation?
        else {
            openNonImageContent(getFullUrl());
        }

        // Parse image, links first, then remove links
        // doc.select("a").remove();
        // Title
        Element title = doc.select("title").first();
        // System.out.println("Title: " +
        String htmlTitle = getHtmlTitle(title.ownText());

        // Explanation
        String html = doc.html();
        // Some pages are badly formatted, this fixes that
        html = html.replaceAll("   <p> </p>\n  </center>", "</center>\n<p>");

        doc = Jsoup.parse(html);

        Elements elements = doc.select("p");
        String explanation = "";

        for (Element e : elements) {
            if (e.text().contains(EXPLANATION_HEADER)) {
                explanation = e.text();
                break;
            }
        }
        if (explanation.equals("")) {
            elements = doc.select("TD");
            for (Element e : elements) {
                if (e.text().contains(EXPLANATION_HEADER)) {
                    explanation = e.text();
                    break;
                }
            }
        }
        if (explanation.equals("")) {
            html = html.replaceAll("<hr>", "<p>");

            doc = Jsoup.parse(html);
            elements = doc.select("p");
            explanation = "";

            for (Element e : elements) {
                if (e.text().contains(EXPLANATION_HEADER)) {
                    explanation = e.text();
                    break;
                }
            }
        }
        titleText.setText(htmlTitle);
        description.setText(explanation);
    }

    private String getVideoId(String url) {
        final int ID_GROUP = 6;
        String videoId = "";

        if (url != null && url.trim().length() > 0) {
            String expression = "(http:|https:|)\\/\\/(player.|www.)?(vimeo\\.com|youtu(be\\" +
                    ".com|\\.be|be\\.googleapis\\.com))\\/(video\\/|embed\\/|watch\\?v=|v\\/)?" +
                    "([A-Za-z0-9._%-]*)(\\&\\S+)?";
            CharSequence input = url;
            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(input);

            if (matcher.find()) {
                String groupIndex = matcher.group(ID_GROUP);
                if (groupIndex != null) {
                    videoId = groupIndex;
                }
            }
        }
        return videoId;
    }

    /**
     * Launch activity to display image in fullscreen
     *
     * @param url URL of the image
     */
    private void launchFullImageView(String url, String numericalDate, boolean setWallpaper) {
        Intent intent = new Intent(MainActivity.this, ImageActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("date", numericalDate);
        intent.putExtra("wallpaper", setWallpaper);
        startActivity(intent);
    }

    /**
     * Open link in web browser
     */
    private void openLink() {
        String url = getFullUrl();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    /**
     * Open non-image content in suitable application
     */
    private void openNonImageContent(String url) {
        final String uri = url;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = String.format(getResources().getString(R.string.dialog_browser_desc),
                date);

        imageView.setImageResource(0);
        progressBar.setVisibility(View.GONE);

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

    // Title AsyncTask
    private class Title extends AsyncTask<String, Void, Void> {
        boolean isImage;
        String contentUrl;
        String htmlTitle;
        String explanation;

        @Override
        protected Void doInBackground(String... url) {
            final String EXPLANATION_HEADER = "Explanation:";
            final String YT_BASE_URL = "https://www.youtube.com/watch?v=";
            final String VM_BASE_URL = "https://vimeo.com/";

            Document doc;

            Log.i("URL", url[0]);
            try {
                doc = Jsoup.parse(url[0]);


                // Image

                Element image = doc.select("img").first();
                Element video = doc.select("iframe[src~=(youtube\\.com|vimeo\\.com)], " +
                        "object[data~=(youtube\\.com|vimeo\\.com)], embed[src~=(youtube\\" +
                        ".com|vimeo\\.com)]").first();

                if (image != null) {
                    isImage = true;
                    contentUrl = image.absUrl("src");
                }
                else if (video != null) {
                    isImage = false;
                    contentUrl = video.absUrl("src");

                    if (contentUrl.contains("youtu")) {
                        contentUrl = YT_BASE_URL + getVideoId(contentUrl);
                    }
                    else if (contentUrl.contains("vimeo")) {
                        contentUrl = VM_BASE_URL + getVideoId(contentUrl);
                    }
                }
                else {
                    isImage = false;
                    contentUrl = getFullUrl();
                }

                // Parse image, links first, then remove links
                // doc.select("a").remove();
                // Title
                Element title = doc.select("title").first();
                // System.out.println("Title: " +
                htmlTitle = getHtmlTitle(title.ownText());

                // Explanation
                String html = doc.html();
                // Some pages are badly formatted, this fixes that
                html = html.replaceAll("   <p> </p>\n  </center>", "</center>\n<p>");

                doc = Jsoup.parse(html);

                Elements elements = doc.select("p");

                for (Element e : elements) {
                    if (e.text().contains(EXPLANATION_HEADER)) {
                        explanation = e.text();
                        break;
                    }
                }
                if (explanation.equals("")) {
                    elements = doc.select("TD");
                    for (Element e : elements) {
                        if (e.text().contains(EXPLANATION_HEADER)) {
                            explanation = e.text();
                            break;
                        }
                    }
                }
                if (explanation.equals("")) {
                    html = html.replaceAll("<hr>", "<p>");

                    doc = Jsoup.parse(html);
                    elements = doc.select("p");
                    explanation = "";

                    for (Element e : elements) {
                        if (e.text().contains(EXPLANATION_HEADER)) {
                            explanation = e.text();
                            break;
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            titleText.setText(htmlTitle);
            description.setText(explanation);

            if (isImage) {
                Glide.with(MainActivity.this).load(contentUrl) // Load from URL
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
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
                            public boolean onResourceReady(GlideDrawable resource, String
                                    model, Target<GlideDrawable> target, boolean
                                                                   isFromMemoryCache, boolean
                                                                   isFirstResource) {
                                progressBar.setVisibility(View.GONE);
                                return false;
                            }

                        }).into(imageView);
            }
            else {
                openNonImageContent(getFullUrl());
            }
        }
    }

    /**
     * http://stackoverflow.com/questions/4098198/adding-fling-gesture-to-an-image-view-android
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private final int SWIPE_MIN_DISTANCE = 120;
        private final int SWIPE_THRESHOLD_VELOCITY = 200;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            // Right to left
            if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) >
                    SWIPE_THRESHOLD_VELOCITY) {
                // Prevent user from navigation to future days
                if (!date.equals(today))
                    nextDay();
                return false;
            }
            // Left to right
            else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) >
                    SWIPE_THRESHOLD_VELOCITY) {
                previousDay();
                return false;
            }
            return false;
        }

        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (tooEarly) {
                if (date.equals(today)) {
                    Toast.makeText(MainActivity.this, R.string.error_today, Toast.LENGTH_SHORT)
                            .show();

                }
                else {
                    Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT)
                            .show();
                }
            }
            else {
                if (imageView.getDrawable() == null) {
                    openNonImageContent(sdUrl);
                }
                else {
                    launchFullImageView(sdUrl, expandedToNumericalDate(date), false);
                }
            }
            return false;
        }

        // Touch has been long enough to indicate a long press.
        // Does not indicate motion is complete yet (no up event necessarily)
        @Override
        public void onLongPress(MotionEvent e) {
            if (imageView.getDrawable() == null) {
                Toast.makeText(MainActivity.this, R.string.toast_view_external, Toast
                        .LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(MainActivity.this, R.string.toast_view_image, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    private class setDisabledDays extends AsyncTask<Integer, Void, Void> {
        @Override
        protected Void doInBackground(Integer... params) {

            for (int i = 0; i < DISABLED_DAYS; i++) {
                disabledDays[i] = Calendar.getInstance();
                try {
                    disabledDays[i].setTime(NUMERICAL_FORMAT.parse(disabledDayStrings[i]));
                }
                catch (ParseException e) {
                    Log.i("PARSE", "EXCEPTION");
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onPostExecute(Void v) {
            disabledDayStrings = null;
        }
    }
}
