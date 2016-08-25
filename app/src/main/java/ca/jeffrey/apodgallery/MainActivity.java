package ca.jeffrey.apodgallery;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.anupcowkur.reservoir.Reservoir;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// TODO Overhaul permissions management
// TODO Optimize string names, strings.xml, variables

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {
    final static int WRITE_PERMISSION = 100;
    final String TAG_PREF_LOCATION = "pref_save_location";
    // NASA API key
    final String API_KEY = "***REMOVED***";
    final String DATE_PICKER_TAG = "date_picker";
    final String DEFAULT_IMAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "APOD";
    final String IMAGE_EXT = ".jpg";
    // First available APOD date
    final Calendar MIN_DATE = new GregorianCalendar(1995, 5, 20);
    // Date formats
    final SimpleDateFormat EXPANDED_FORMAT = new SimpleDateFormat("MMMM d, y");
    final SimpleDateFormat NUMERICAL_FORMAT = new SimpleDateFormat("y-MM-dd");
    final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat("yyMMdd");
    // Anchor height
    final float SLIDING_ANCHOR_POINT = 0.42f;
    OkHttpClient client;
    // Member variables
    boolean tooEarly;
    String date;
    String today;
    String imgUrl;
    String sdUrl;

    AutoResizeTextView titleText;
    DocumentView description;
    FloatingActionButton fab;
    FloatingActionButtonLayout fabLayout;
    ImageView imageView;
    ImageView tomorrow;
    ImageView yesterday;
    RelativeLayout dateNav;
    RelativeLayout mainView;
    ProgressBar progressBar;
    SharedPreferences sharedPref;
    SlidingUpPanelLayout slidingPanel;
    TextView dateText;

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
    private static Calendar dateToCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean checkPermission(Activity a) {
        final Activity activity = a;
        final String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest
                .permission.WRITE_EXTERNAL_STORAGE};

        int currentAPIVersion = Build.VERSION.SDK_INT;

        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission
                    .WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, WRITE_PERMISSION);

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest
                        .permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(activity, R.string.toast_storage, Toast.LENGTH_SHORT).show();
                }
                else {
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission
                            .WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                        Toast.makeText(activity, R.string.toast_storage, Toast.LENGTH_SHORT).show();
                    }
                }
                return false;
            }
        }
        return true;
    }

    private String getHtmlTitle(String fragment) {
        int index = fragment.indexOf("-");
        return fragment.substring(index + 1).trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prevent multiple activities from launching
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

        // Initialize Reservoir cache
        try {
            Reservoir.init(this, 5000000);
        }
        catch (IOException e) {
            Toast.makeText(MainActivity.this, R.string.error_cache, Toast.LENGTH_SHORT).show();
        }

        client = new OkHttpClient.Builder().cache(new Cache(getCacheDir(), 10 * 1024 * 1024)) // 10M
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (isNetworkAvailable(MainActivity.this)) {
                            request = request.newBuilder().header("Cache-Control", "public, " +
                                    "max-age=" + 60).build();
                        }
                        else {
                            request = request.newBuilder().header("Cache-Control", "public, " +
                                    "only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build();
                        }
                        return chain.proceed(request);
                    }
                }).build();

        // Initiate image views
        imageView = (ImageView) findViewById(R.id.image);
        yesterday = (ImageView) findViewById(R.id.left_chevron);
        tomorrow = (ImageView) findViewById(R.id.right_chevron);

        // Other views
        dateText = (TextView) findViewById(R.id.date);
        description = (DocumentView) findViewById(R.id.description);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fabLayout = (FloatingActionButtonLayout) findViewById(R.id.fab_layout);
        mainView = (RelativeLayout) findViewById(R.id.main_view);
        dateNav = (RelativeLayout) findViewById(R.id.date_nav);
        progressBar = (ProgressBar) findViewById(R.id.progress);
        slidingPanel = (SlidingUpPanelLayout) findViewById(R.id.sliding_panel_layout);
        titleText = (AutoResizeTextView) findViewById(R.id.title);

        tooEarly = false;

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
                dpd.vibrate(false);
                dpd.show(getFragmentManager(), DATE_PICKER_TAG);
            }
        });

        // Sliding up panel listener
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
                    if (saveImage()) {
                        launchFullImageView(imgUrl, expandedToNumericalDate(date), true);
                    }
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
        // Fixes bug where tapping date nav bar would trigger fullscreen image view
        dateNav.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

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

    // Handle menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share:
                shareImage(titleText.getText().toString());
                return true;
            case R.id.action_save:
                saveImage();
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

    /**
     * Share image or media content
     *
     * @param title Title of featured content
     */
    public void shareImage(String title) {
        final String IMAGE_DIRECTORY = sharedPref.getString(TAG_PREF_LOCATION,
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

            startActivity(Intent.createChooser(share, getString(R.string.title_intent_share_link)));
        }
        // Otherwise share image
        else {
            share.setType("image/jpeg");
            share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            if (saveImage()) {
                String path = IMAGE_DIRECTORY + expandedToNumericalDate(date) + IMAGE_EXT;
                File image = new File(path);
                Uri uri = Uri.fromFile(image);

                share.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(share, getString(R.string.title_intent_share)));
            }
        }
    }

    /**
     * Save image to external storage
     */
    public boolean saveImage() {
        // Exit if no image is available
        if (imageView.getDrawable() == null) {
            displayImageNotAvailableToast();
            return false;
        }

        final String DATE = expandedToNumericalDate(date);
        final String IMAGE_DIRECTORY = sharedPref.getString(TAG_PREF_LOCATION,
                DEFAULT_IMAGE_DIRECTORY);

        boolean hasPermission = checkPermission(this);

        if (hasPermission) {
            // Load image with Glide as bitmap
            Glide.with(this).load(imgUrl).asBitmap().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(new SimpleTarget<Bitmap>() {

                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                        glideAnimation) {

                    File imageDirectory = new File(IMAGE_DIRECTORY);

                    // Make image directory if it does not exist
                    if (!imageDirectory.exists()) {
                        imageDirectory.mkdir();
                    }
                    String filename = DATE + IMAGE_EXT;
                    File image = new File(imageDirectory, filename);

                    String message = getString(R.string.toast_save_image) +
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
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == MainActivity.WRITE_PERMISSION) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, R.string.toast_permission_granted, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
            }
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
     * Handle content loading from Reservoir cache
     *
     * @param isImage     true, if the content URL is an image, otherwise false
     * @param contentUrl  URL of the content
     * @param hdImageUrl  URL of the HD image, if available
     * @param htmlTitle   Title of the featured content
     * @param explanation Explanation of the featured content
     */
    private void onHtmlResponse(boolean isImage, String contentUrl, String hdImageUrl, String
            htmlTitle, String explanation) {
        titleText.setText(htmlTitle);
        description.setText(explanation);
        sdUrl = contentUrl;

        if (isImage) {
            // Check preferences if user wants HD images saved
            if (sharedPref.getString("image_quality", "").equals("1") && !hdImageUrl.equals("")) {
                imgUrl = hdImageUrl;
            }
            else {
                imgUrl = sdUrl;
            }

            Glide.with(MainActivity.this).load(sdUrl) // Load from URL
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                    //.skipMemoryCache(true) // Use disk cache only
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model,
                                                   Target<GlideDrawable> target, boolean
                                                           isFirstResource) {
                            Toast.makeText(MainActivity.this, R.string.error_general, Toast
                                    .LENGTH_SHORT).show();
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
    }

    /**
     * Get & parse image data and display image to screen
     *
     * @param response JSON object
     */
    private void onJsonResponse(JSONObject response) throws JSONException {
        final String IMAGE_TYPE = "image";

        boolean prefHd;
        boolean prefCopyright;
        String copyright;
        String explanation;
        String mediaType;
        String hdUrl;
        String title;

        tooEarly = false;

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
            explanation += getString(R.string.title_credits) + copyright;
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
                    .skipMemoryCache(true) // Use disk cache only
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model,
                                                   Target<GlideDrawable> target, boolean
                                                           isFirstResource) {
                            Toast.makeText(MainActivity.this, R.string.error_general, Toast
                                    .LENGTH_SHORT).show();
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
    }

    /**
     * GET request to NASA API
     *
     * @param date selected date
     */
    private void getImageData(String date) {
        // Parse date
        String apiDate = expandedToNumericalDate(date);

        String url = "https://api.nasa" +
                ".gov/planetary/apod?api_key=" + API_KEY + "&date=" + apiDate;

        Glide.clear(imageView);

        // Reservoir for HTML scraping
        try {
            if (Reservoir.contains(getFullUrl())) {
                List<String> list = Reservoir.get(getFullUrl(), List.class);

                boolean isImage = list.get(0).equals("true");
                String contentUrl = list.get(1);
                String hdImageUrl = list.get(2);
                String htmlTitle = list.get(3);
                String explanation = list.get(4);

                onHtmlResponse(isImage, contentUrl, hdImageUrl, htmlTitle, explanation);
                return;
            }
        }
        catch (IOException e) {
            Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
        }

        // Regular OKHTTP request
        try {
            doJsonRequest(url);
        }
        catch (IOException e) {
            Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Check if device has internet access
     *
     * @param context Context
     *
     * @return true, if able to connect to Internet, otherwise false
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context
                .CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            // Connected
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI || activeNetwork.getType
                    () == ConnectivityManager.TYPE_MOBILE) {
                return true;
            }
        }
        // Not connected
        else {
            return false;
        }
        return false;
    }

    /**
     * Initiate JSON request via OkHttp3
     *
     * @param url Request URL
     *
     * @throws IOException Handled directly in catch statement
     */
    private void doJsonRequest(String url) throws IOException {
        // Build request
        Request request = new Request.Builder().cacheControl(new CacheControl.Builder()
                .onlyIfCached().build()).url(url).build();
        // Request call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
                Toast.makeText(MainActivity.this, R.string.error_general, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                // Run on main UI thread
                runOnUiThread(new Runnable() {
                    String res = response.body().string();

                    @Override
                    public void run() {
                        JSONObject object;
                        // Parse JSON object
                        try {
                            object = new JSONObject(res);
                            onJsonResponse(object);
                        }
                        // Error handling
                        catch (JSONException e) {
                            int code = response.code();
                            int messageId;

                            switch (code) {
                                // Server error
                                case 500:
                                    if (date.equals(today)) {
                                        messageId = R.string.error_today;
                                    }
                                    else {
                                        tooEarly = true;
                                        parseHtml();
                                        messageId = -1;
                                    }
                                    break;
                                // Client-side network error
                                case 504:
                                    messageId = R.string.error_network;
                                    break;
                                // Default server error
                                default:
                                    messageId = R.string.error_server;
                            }

                            progressBar.setVisibility(View.GONE);
                            resetText();

                            if (messageId != -1) {
                                Toast.makeText(MainActivity.this, messageId, Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Clear title text and description
     */
    private void resetText() {
        titleText.setText(R.string.title_text_image_unavailable);
        description.setText("");
    }

    /**
     * Execute retrieval of HTML of the APOD page via Jsoup scraping
     */
    private void parseHtml() {
        new GetHtmlData().execute(getFullUrl());
    }

    /**
     * Get Youtube or Vimeo video ID from URL
     *
     * @param url Video URL (Youtube or Vimeo)
     *
     * @return video ID of the link
     */
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
        String message = String.format(getString(R.string.dialog_browser_desc), date);

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

    // AsyncTask
    private class GetHtmlData extends AsyncTask<String, Void, Void> {
        List<String> bundle = new ArrayList<String>();
        boolean isImage;

        String contentUrl;
        String hdImageUrl;
        String htmlTitle;
        String explanation;

        @Override
        protected Void doInBackground(String... url) {
            final String EXPLANATION_HEADER = "Explanation:";
            final String YT_BASE_URL = "https://www.youtube.com/watch?v=";
            final String VM_BASE_URL = "https://vimeo.com/";

            Document doc = null;
            contentUrl = "";
            hdImageUrl = "";
            htmlTitle = "";
            hdImageUrl = "";
            explanation = "";

            try {
                doc = Jsoup.connect(url[0]).get();
            }
            catch (IOException e) {
                Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
            }

            // Image
            Element image = doc.select("img").first();
            Element video = doc.select("iframe[src~=(youtube\\.com|vimeo\\.com)], " +
                    "object[data~=(youtube\\.com|vimeo\\.com)], embed[src~=(youtube\\" +
                    ".com|vimeo\\.com)]").first();

            if (image != null) {
                isImage = true;
                Element hdElement = image.parent();
                contentUrl = image.absUrl("src");
                hdImageUrl = hdElement.absUrl("href");
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

            // doc.select("a").remove();
            // Title
            Element title = doc.select("title").first();

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
                //explanation = "";

                for (Element e : elements) {
                    if (e.text().contains(EXPLANATION_HEADER)) {
                        explanation = e.text();
                        break;
                    }
                }
            }

            explanation = explanation.replace(EXPLANATION_HEADER, "");
            explanation = explanation.trim();

            // 0 - is image
            if (isImage) {
                bundle.add("true");
            }
            else {
                bundle.add("false");
            }

            // 1 - URL
            bundle.add(contentUrl);
            // 2 - HD URL
            bundle.add(hdImageUrl);
            // 3 - Title
            bundle.add(htmlTitle);
            // 4 - Explanation
            bundle.add(explanation);

            try {
                Reservoir.put(url[0], bundle);
            }
            catch (IOException e) {
                Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            titleText.setText(htmlTitle);
            description.setText(explanation);

            sdUrl = contentUrl;

            if (isImage) {
                // Check preferences if user wants HD images saved
                if (sharedPref.getString("image_quality", "").equals("1") && !hdImageUrl.equals
                        ("")) {
                    imgUrl = hdImageUrl;
                }
                else {
                    imgUrl = sdUrl;
                }

                Glide.with(MainActivity.this).load(sdUrl) // Load from URL
                        .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                        //.skipMemoryCache(true) // Use disk cache only
                        .listener(new RequestListener<String, GlideDrawable>() {
                            @Override
                            public boolean onException(Exception e, String model,
                                                       Target<GlideDrawable> target, boolean
                                                               isFirstResource) {
                                Toast.makeText(MainActivity.this, R.string.error_general, Toast
                                        .LENGTH_SHORT).show();
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
                if (!date.equals(today)) {
                    nextDay();
                }
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
            boolean imageAvailable = !titleText.getText().equals(getString(R.string
                    .title_text_image_unavailable));
            if (tooEarly) {
                Toast.makeText(MainActivity.this, R.string.error_today, Toast.LENGTH_SHORT).show();
            }
            else if (progressBar.getVisibility() == View.GONE) {
                if (imageAvailable && imageView.getDrawable() == null) {
                    openNonImageContent(sdUrl);
                }
                else if (imageView.getDrawable() != null) {
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
}
