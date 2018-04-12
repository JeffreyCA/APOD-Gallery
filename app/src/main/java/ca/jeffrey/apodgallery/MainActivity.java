package ca.jeffrey.apodgallery;

import android.Manifest;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
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
import android.support.v4.content.FileProvider;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.anupcowkur.reservoir.Reservoir;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.crash.FirebaseCrash;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import ca.jeffrey.apodgallery.text.AutoResizeTextView;
import ca.jeffrey.apodgallery.text.TextViewEx;
import ca.jeffrey.apodgallery.wallpaper.WallpaperChangeManager;
import ca.jeffrey.apodgallery.widget.WidgetProvider;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionSpec;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.TlsVersion;

public class MainActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener, ProviderInstaller.ProviderInstallListener {

    // NASA API key
    public static final String API_KEY = "***REMOVED***";
    // Permission codes
    private static final int SAVE_PERMISSION = 100;
    private static final int SHARE_PERMISSION = 101;
    private static final int WALLPAPER_PERMISSION = 102;

    /**
     * The app version code (not the version name!) that was used on the last
     * start of the app.
     */
    private static final String LAST_APP_VERSION = "last_app_version";
    private final String DATE_PICKER_TAG = "date_picker";
    private final String PREF_SAVE_LOCATION = "pref_save_location";
    private final String DEFAULT_IMAGE_DIRECTORY = Environment.getExternalStorageDirectory()
            .getPath() + File.separator + "APOD" + File.separator;
    private final String IMAGE_EXT = ".jpg";

    // First available APOD date
    private final Calendar MIN_DATE = new GregorianCalendar(1995, 5, 20);
    // Date formats
    private final SimpleDateFormat EXPANDED_FORMAT = new SimpleDateFormat("MMMM d, y");
    private final SimpleDateFormat NUMERICAL_FORMAT = new SimpleDateFormat("y-MM-dd");
    private final SimpleDateFormat SHORT_FORMAT = new SimpleDateFormat("yyMMdd");
    // Anchor height
    private final float SLIDING_ANCHOR_POINT = 0.42f;
    private final int ERROR_DIALOG_REQUEST_CODE = 1;
    ProgressDialog dialog;
    private OkHttpClient client;
    // Member variables
    private boolean tooEarly;
    private String date;
    private String today;
    private String imgUrl;
    private String sdUrl;
    private AutoResizeTextView titleText;
    private ScrollView descriptionScroll;
    private TextViewEx description;
    private FloatingActionButton fab;
    private ImageView imageView;
    private ImageView tomorrow;
    private ImageView yesterday;
    private ProgressBar progressBar;
    private RelativeLayout dateNav;
    private RelativeLayout mainView;
    private SharedPreferences sharedPref;
    private SlidingUpPanelLayout slidingPanel;
    private TextView dateText;

    /**
     * Convert Date object to Calendar object
     *
     * @param date Date object
     * @return Calendar object
     */
    private static Calendar dateToCalendar(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal;
    }

    // OnCreate
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isTaskRoot() && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER) && getIntent()
                .getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
            finish();
            return;
        }

        // Initialize toolbar
        Toolbar myToolbar = findViewById(R.id.toolbar);
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
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, R.string.error_cache, Toast.LENGTH_SHORT).show();
        }

        // Initialize OkHttp client and cache
        client = getNewHttpClient();

        // Initiate image views
        imageView = findViewById(R.id.image);
        tomorrow = findViewById(R.id.right_chevron);
        yesterday = findViewById(R.id.left_chevron);

        // Other views
        dateText = findViewById(R.id.date);
        description = findViewById(R.id.description);
        descriptionScroll = findViewById(R.id.description_scroll);
        fab = findViewById(R.id.fab);
        mainView = findViewById(R.id.main_view);
        dateNav = findViewById(R.id.date_nav);
        progressBar = findViewById(R.id.progress);
        slidingPanel = findViewById(R.id.sliding_panel_layout);
        titleText = findViewById(R.id.title);

        tooEarly = false;

        // Set date view
        today = date = EXPANDED_FORMAT.format(new Date());
        dateText.setText(date);

        Bundle extras = getIntent().getExtras();

        if (getIntent().hasExtra("widget")) {
            initializeListeners();

            String dateString = extras.getString("widget");

            try {
                date = EXPANDED_FORMAT.format(NUMERICAL_FORMAT.parse(dateString));
            } catch (ParseException e) {

            }

            dateText.setText(date);
            updateDateNavButtons();

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getImageData(date);
            } else {
                dialog = ProgressDialog.show(this, getString(R.string.dialog_ciphers_title),
                        getString(R.string.dialog_ciphers_body), true);
                ProviderInstaller.installIfNeededAsync(this, this);
            }
        } else {
            refreshWidgets();

            switch (checkAppStart()) {
                case NORMAL:
                    initializeListeners();
                    getImageData(date);
                    break;
                case FIRST_TIME_VERSION:
                    // Remove obsolete preference keys
                    sharedPref.edit().remove("non_image").apply();
                    sharedPref.edit().remove("today_retrieved").apply();
                    sharedPref.edit().remove("last_ran").apply();
                    // sharedPref.edit().putString(PREF_SAVE_LOCATION, DEFAULT_IMAGE_DIRECTORY).apply();

                    displayMinorChangesDialog();

                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        initializeListeners();
                        getImageData(date);
                    } else {
                        dialog = ProgressDialog.show(this, getString(R.string.dialog_ciphers_title),
                                getString(R.string.dialog_ciphers_body), true);
                        ProviderInstaller.installIfNeededAsync(this, this);
                    }

                    // Restart daily wallpaper changer
                    if (sharedPref.getBoolean("pref_daily_wallpaper", false)) {
                        refreshTasks();
                    }

                    break;
                case FIRST_TIME:
                    displayMajorChangesDialog();
                    sharedPref.edit().putString(PREF_SAVE_LOCATION, DEFAULT_IMAGE_DIRECTORY).apply();

                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        initializeListeners();
                        getImageData(date);
                    } else {
                        dialog = ProgressDialog.show(this, getString(R.string.dialog_ciphers_title),
                                getString(R.string.dialog_ciphers_body), true);
                        ProviderInstaller.installIfNeededAsync(this, this);
                    }

                    break;
                default:
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        initializeListeners();
                        getImageData(date);
                    } else {
                        dialog = ProgressDialog.show(this, getString(R.string.dialog_ciphers_title),
                                getString(R.string.dialog_ciphers_body), true);
                        ProviderInstaller.installIfNeededAsync(this, this);
                    }
                    break;
            }
        }
        // Set image
    } // End onCreate method

    @Override
    public void onProviderInstalled() {
        dialog.dismiss();
        initializeListeners();
        getImageData(date);
    }

    @Override
    public void onProviderInstallFailed(int errorCode, Intent intent) {
        dialog.dismiss();

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show();
                onProviderInstalled();
            } else {
                displayGoogleServicesDialog();
            }
        }
    }

    public OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, null, null);
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                client.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }

        return client;
    }

    private OkHttpClient getNewHttpClient() {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .cache(new Cache(getCacheDir(), 10 * 1024 * 1024)) // 10M
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        if (isNetworkAvailable()) {
                            request = request.newBuilder().header("Cache-Control", "public, " +
                                    "max-age=" + 60).build();
                        } else {
                            request = request.newBuilder().header("Cache-Control", "public, " +
                                    "only-if-cached, max-stale=" + 60 * 60 * 24 * 7).build();
                        }
                        return chain.proceed(request);
                    }
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS);

        return enableTls12OnPreLollipop(client).build();
    }

    private void openGooglePlay() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    private void refreshTasks() {
        WallpaperChangeManager wallpaperChangeManager = new WallpaperChangeManager(this);
        wallpaperChangeManager.cancelAll();
        wallpaperChangeManager.scheduleImmediateAndRecurring();

        Toast.makeText(this, R.string.toast_reset_task, Toast.LENGTH_SHORT).show();
    }

    private void refreshWidgets() {
        Intent intent = new Intent(getApplicationContext(), WidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager man = AppWidgetManager.getInstance(getApplication());

        int ids[] = man.getAppWidgetIds(new ComponentName(getApplication(), WidgetProvider.class));
        for (int id : ids) {
            man.notifyAppWidgetViewDataChanged(id, R.id.stack_view);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }

    private void displayMinorChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.dialog_whats_new_title) + BuildConfig.VERSION_NAME)
                .setMessage(R.string.change_latest)
                .setNegativeButton(R.string.label_review, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openGooglePlay();
                    }
                })
                .setPositiveButton(R.string.label_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setCancelable(false);
        builder.create().show();
    }

    private void displayMajorChangesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getString(R.string.dialog_whats_new_title) + BuildConfig.VERSION_NAME)
                .setMessage(R.string.change_latest)
                .setPositiveButton(R.string.label_dismiss, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // displayWallpaperFeaturesDialog();
                    }
                });

        builder.setCancelable(false);
        builder.create().show();
    }

    private void displayWallpaperFeaturesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.dialog_wallpaper_title)
                .setMessage(R.string.wallpaper_summary)
                .setNegativeButton(R.string.label_settings, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(intent);
                    }
                })
                .setPositiveButton(R.string.label_not_now, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setCancelable(false);
        builder.create().show();
    }

    private void displayGoogleServicesDialog() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.dialog_gservices_title)
                .setMessage(R.string.dialog_gservices_body)
                .setNegativeButton(R.string.label_ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        onProviderInstalled();
                    }
                })
                .setPositiveButton(R.string.label_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")));
                        } catch (ActivityNotFoundException e) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms")));
                        }
                    }
                }).show();
    }

    private void initializeListeners() {
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
                Calendar today = Calendar.getInstance();
                Calendar currentDate = Calendar.getInstance();
                try {
                    currentDate = dateToCalendar(EXPANDED_FORMAT.parse(date));
                } catch (ParseException e) {
                    FirebaseCrash.report(e);
                    FirebaseCrash.log(date);
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
                descriptionScroll.smoothScrollTo(0, 0);
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
                if (imageAvailable()) {
                    int androidVersion = Build.VERSION.SDK_INT;
                    if (androidVersion >= Build.VERSION_CODES.M) {
                        if (!checkPermission()) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE}, WALLPAPER_PERMISSION);
                        } else {
                            setAsWallpaper();
                        }
                    } else {
                        setAsWallpaper();
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
    }

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
    public void onBackPressed() {
        // Sliding up panel listener
        slidingPanel = findViewById(R.id.sliding_panel_layout);

        if (slidingPanel != null && (slidingPanel.getPanelState() == SlidingUpPanelLayout
                .PanelState.EXPANDED || slidingPanel.getPanelState() == SlidingUpPanelLayout
                .PanelState.ANCHORED)) {
            slidingPanel.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Glide.clear(imageView);
        progressBar.setVisibility(View.VISIBLE);
        getImageData(date);
    }

    private void updateDateNavButtons() {
        // Show/hide right navigation chevron
        if (tomorrow.getVisibility() == View.VISIBLE && date.equals(today)) {
            tomorrow.setVisibility(View.INVISIBLE);
        } else if (tomorrow.getVisibility() == View.INVISIBLE && !date.equals(today)) {
            tomorrow.setVisibility(View.VISIBLE);
        }
    }

    // Handle menu item selection
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                today = EXPANDED_FORMAT.format(new Date());
                updateDateNavButtons();

                Glide.clear(imageView);
                progressBar.setVisibility(View.VISIBLE);
                getImageData(date);
                return true;
            case R.id.action_share:
                share();
                return true;
            case R.id.action_save:
                saveImageMenu();
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
     * Finds out started for the first time (ever or in the current version).<br/>
     * <br/>
     * Note: This method is <b>not idempotent</b> only the first call will
     * determine the proper result. Any subsequent calls will only return
     * {@link AppStart#NORMAL} until the app is started again. So you might want
     * to consider caching the result!
     *
     * @return the type of app start
     */
    public AppStart checkAppStart() {
        PackageInfo pInfo;
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);
        AppStart appStart = AppStart.NORMAL;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            int lastVersionCode = sharedPreferences
                    .getInt(LAST_APP_VERSION, -1);
            int currentVersionCode = pInfo.versionCode;
            appStart = checkAppStart(currentVersionCode, lastVersionCode);
            // Update version in preferences
            sharedPreferences.edit()
                    .putInt(LAST_APP_VERSION, currentVersionCode).apply();
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("Logger",
                    "Unable to determine current app version from pacakge manager. Defensively assuming normal app start.");
        }
        return appStart;
    }

    public AppStart checkAppStart(int currentVersionCode, int lastVersionCode) {
        if (lastVersionCode == -1) {
            return AppStart.FIRST_TIME;
        } else if (lastVersionCode < currentVersionCode) {
            return AppStart.FIRST_TIME_VERSION;
        } else if (lastVersionCode > currentVersionCode) {
            Log.w("Logger", "Current version code (" + currentVersionCode
                    + ") is less then the one recognized on last startup ("
                    + lastVersionCode
                    + "). Defenisvely assuming normal app start.");
            return AppStart.NORMAL;
        } else {
            return AppStart.NORMAL;
        }
    }

    /**
     * Display permission granted toast
     *
     * @param requestCode  permission request code
     * @param permissions  array of permissions asked
     * @param grantResults results if permissions were granted or revoked
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case SAVE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImage();
                    return;
                } else {
                    Toast.makeText(MainActivity.this, R.string.toast_storage, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case SHARE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    shareImage();
                    return;
                } else {
                    Toast.makeText(MainActivity.this, R.string.toast_storage, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            case WALLPAPER_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setAsWallpaper();
                    return;
                } else {
                    Toast.makeText(MainActivity.this, R.string.toast_storage, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Check required permissions are granted
     *
     * @return true, if permissions were granted, otherwise false
     */
    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    // Date Methods

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
     * @return the next day after the provided date
     */
    private String getNextDay(String date) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(EXPANDED_FORMAT.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            return EXPANDED_FORMAT.format(calendar.getTime());
        } catch (ParseException e) {
            FirebaseCrash.report(e);
            FirebaseCrash.log(date);
        }
        return null;
    }

    /**
     * Calculate the day before the given date
     *
     * @param date formatted date
     * @return the previous day before the provided date
     */
    private String getPreviousDay(String date) {
        Calendar calendar = Calendar.getInstance();

        try {
            calendar.setTime(EXPANDED_FORMAT.parse(date));
            calendar.add(Calendar.DAY_OF_YEAR, -1);

            return EXPANDED_FORMAT.format(calendar.getTime());
        } catch (ParseException e) {
            FirebaseCrash.report(e);
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
        } else if (tomorrow.getVisibility() == View.INVISIBLE && !date.equals(today)) {
            tomorrow.setVisibility(View.VISIBLE);
        }

        // Show progress loading circle
        getImageData(date);
    }

    /**
     * Convert date format to yyyy-mm-dd
     *
     * @param date date in expanded format
     * @return date in numerical format
     */
    private String expandedToNumericalDate(String date) {
        try {
            return NUMERICAL_FORMAT.format(EXPANDED_FORMAT.parse(date));
        } catch (ParseException e) {
            FirebaseCrash.report(e);
        }

        return "";
    }

    /**
     * Get URL accessible in web browser
     *
     * @return browser-accessible URL
     */
    private String getFullUrl() {
        final String BASE_URL = "https://apod.nasa.gov/apod/ap";
        String shortDate;

        try {
            shortDate = SHORT_FORMAT.format(EXPANDED_FORMAT.parse(date));
            return BASE_URL + shortDate + ".html";
        } catch (ParseException e) {
            FirebaseCrash.report(e);
        }

        return "";
    }

    /**
     * Get title of featured content from HTML source code
     *
     * @param fragment Portion of source code pertaining to <title> tag
     * @return title of the featured content
     */
    private String getHtmlTitle(String fragment) {
        int index = fragment.indexOf("-");
        return fragment.substring(index + 1).trim();
    }

    /**
     * Display toast when image is not available
     */
    private void displayImageNotAvailableToast() {
        Toast.makeText(MainActivity.this, R.string.toast_no_image, Toast.LENGTH_SHORT).show();
    }

    /**
     * Determine if ImageView is empty
     *
     * @return true, if ImageView is not empty, otherwise false
     */
    public boolean imageAvailable() {
        return imageView.getDrawable() != null;
    }

    private void saveImageMenu() {
        if (imageAvailable()) {
            int androidVersion = Build.VERSION.SDK_INT;
            if (androidVersion >= Build.VERSION_CODES.M) {
                if (!checkPermission()) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, SAVE_PERMISSION);
                } else {
                    saveImage();
                }
            } else {
                saveImage();
            }
        }
        // No image available
        else {
            displayImageNotAvailableToast();
        }
    }

    /**
     * Save image to external storage
     */
    private void saveImage() {
        final String DATE = expandedToNumericalDate(date);
        final String IMAGE_DIRECTORY = sharedPref.getString(SettingsActivity.TAG_PREF_LOCATION,
                DEFAULT_IMAGE_DIRECTORY);

        // Load image with Glide as bitmap
        Glide.with(this).load(imgUrl).asBitmap().diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into(new SimpleTarget<Bitmap>() {

                    @Override
                    public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                            glideAnimation) {
                        boolean writeDirectory;
                        File imageDirectory = new File(IMAGE_DIRECTORY);
                        Log.i("LOC", IMAGE_DIRECTORY);

                        // Make image directory if it does not exist
                        if (imageDirectory.exists()) {
                            writeDirectory = true;
                        } else {
                            writeDirectory = imageDirectory.mkdir();
                        }

                        if (writeDirectory) {
                            String filename = DATE + IMAGE_EXT;
                            File image = new File(imageDirectory, filename);

                            String message = getString(R.string.toast_save_image) + IMAGE_DIRECTORY + filename;

                            // Encode the file as a JPG image.
                            FileOutputStream outStream;
                            try {
                                outStream = new FileOutputStream(image);
                                resource.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

                                outStream.flush();
                                outStream.close();

                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                            } catch (FileNotFoundException e) {
                                Toast.makeText(MainActivity.this, R.string.error_saving, Toast
                                        .LENGTH_SHORT).show();

                            } catch (IOException e) {
                                Toast.makeText(MainActivity.this, R.string.error_saving, Toast
                                        .LENGTH_SHORT).show();
                            }
                            refreshWidgets();
                        } else {
                            Toast.makeText(MainActivity.this, R.string.error_saving, Toast
                                    .LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setAsWallpaper() {
        saveImage();
        launchFullImageView(imgUrl, expandedToNumericalDate(date), true);
    }

    /**
     * Share image or media content
     */
    private void share() {
        String title = titleText.getText().toString();

        // Share link if non-image content
        if (tooEarly) {
            displayImageNotAvailableToast();
        } else if (!imageAvailable()) {
            shareText(title);
        }
        // Otherwise share image
        else {
            shareImagePermission();
        }
    }

    private void shareText(String title) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        share.putExtra(Intent.EXTRA_TEXT, getFullUrl());

        startActivity(Intent.createChooser(share, getString(R.string.title_intent_share_link)));
    }

    private void shareImage() {
        final String IMAGE_DIRECTORY = sharedPref.getString(SettingsActivity.TAG_PREF_LOCATION, DEFAULT_IMAGE_DIRECTORY);
        Intent share = new Intent(Intent.ACTION_SEND);

        share.setType("image/jpeg");
        share.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        saveImage();
        String path = IMAGE_DIRECTORY + expandedToNumericalDate(date) + IMAGE_EXT;
        File image = new File(path);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            share.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName()
                    + ".provider", image);
        } else {
            uri = Uri.fromFile(image);
        }

        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(share, getString(R.string.title_intent_share)));
    }

    private void shareImagePermission() {
        int androidVersion = Build.VERSION.SDK_INT;

        if (androidVersion >= Build.VERSION_CODES.M) {
            if (!checkPermission()) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE}, SHARE_PERMISSION);
            } else {
                shareImage();
            }
        } else {
            shareImage();
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
        description.setText(explanation, true);
        sdUrl = contentUrl.replaceAll("http://", "https://");

        if (isImage) {
            // Check preferences if user wants HD images saved
            if (sharedPref.getString(SettingsActivity.TAG_PREF_QUALITY, "").equals("1") &&
                    !hdImageUrl.equals("")) {
                imgUrl = hdImageUrl;
            } else {
                imgUrl = sdUrl;
            }

            Glide.with(this).load(sdUrl) // Load from URL
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                    //.skipMemoryCache(true) // Use disk cache only
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model,
                                                   Target<GlideDrawable> target, boolean
                                                           isFirstResource) {

                            MainActivity.this.runOnUiThread(new Runnable() {
                                public void run() {
                                    resetText();
                                    Toast.makeText(MainActivity.this, R.string.error_network, Toast
                                            .LENGTH_SHORT).show();
                                }
                            });

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
        } else {
            openNonImageContent(sdUrl);
        }
    }

    // HTTP Request Methods

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
        sdUrl = response.getString("url").replaceAll("http://", "https://");
        title = response.getString("title");
        hdUrl = "";
        prefHd = sharedPref.getString("image_quality", "").equals("1");
        prefCopyright = sharedPref.getBoolean("pref_display_credit", false);

        // Check if HD image URL is included in response
        if (response.has("hdurl")) {
            hdUrl = response.getString("hdurl").replaceAll("http://", "https://");
        }

        // Add copyright credits to end of description if setting allows it
        // Add extra line breaks as workaround for text getting cut off
        if (prefCopyright && response.has("copyright")) {
            copyright = response.getString("copyright");
            explanation += "\n\n" + getString(R.string.title_credits) + copyright + "\n\n";
        } else if (!response.has("copyright")) {
            explanation += "\n\n";
        }

        // Set image url depending on user preference and image availability
        if (prefHd && !hdUrl.equals("")) {
            imgUrl = hdUrl;
        } else {
            imgUrl = sdUrl;
        }

        // Set text
        titleText.setText(title);
        description.setText(explanation, true);

        if (mediaType.equals(IMAGE_TYPE)) {
            Glide.with(this).load(sdUrl) // Load from URL
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE) // Or .RESULT
                    // .skipMemoryCache(true) // Use disk cache only
                    .listener(new RequestListener<String, GlideDrawable>() {
                        @Override
                        public boolean onException(Exception e, String model,
                                                   Target<GlideDrawable> target, boolean
                                                           isFirstResource) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resetText();
                                    Toast.makeText(MainActivity.this, R.string.error_network, Toast
                                            .LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                            });

                            return false;
                        }

                        @Override
                        public boolean onResourceReady(GlideDrawable resource, String model,
                                                       Target<GlideDrawable> target, boolean
                                                               isFromMemoryCache, boolean
                                                               isFirstResource) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                            return false;
                        }

                    }).into(imageView);
        } else {
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

        String url = "https://api.nasa.gov/planetary/apod?api_key=" + API_KEY + "&date=" + apiDate;

        Glide.clear(imageView);
        titleText.setText("Loading...");
        tooEarly = false;

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
        } catch (IOException e) {
            Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
        }

        // Regular OKHTTP request
        doJsonRequest(url);
    }

    /**
     * Check if device has internet access
     *
     * @return true, if able to connect to Internet, otherwise false
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    /**
     * Initiate JSON request via OkHttp3
     *
     * @param url Request URL
     * @throws IOException Handled directly in catch statement
     */
    private void doJsonRequest(String url) {
        // Build request
        Request request = new Request.Builder().cacheControl(new CacheControl.Builder()
                .onlyIfCached().build()).url(url).build();
        // Request call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
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
                            Date d = EXPANDED_FORMAT.parse(date);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(d);
                            int year = cal.get(Calendar.YEAR);

                            if (year > 1997) {
                                object = new JSONObject(res);
                                onJsonResponse(object);
                            } else {
                                parseHtml();
                            }
                        } catch (ParseException pe) {
                            FirebaseCrash.report(pe);
                        }

                        // Error handling
                        catch (JSONException e) {
                            e.printStackTrace();
                            int code = response.code();
                            int messageId;

                            progressBar.setVisibility(View.GONE);

                            switch (code) {
                                // Server error
                                case 400:
                                    if (date.equals(today)) {
                                        tooEarly = true;
                                        messageId = R.string.error_today;
                                        resetText();
                                        break;
                                    }
                                case 500:
                                    if (date.equals(today)) {
                                        tooEarly = true;
                                        messageId = R.string.error_today;
                                        resetText();
                                    } else {
                                        parseHtml();
                                        messageId = -1;
                                    }
                                    break;
                                // Client-side network error
                                case 504:
                                    messageId = R.string.error_network;
                                    resetText();
                                    break;
                                // Default server error
                                default:
                                    messageId = R.string.error_server;
                                    resetText();
                            }


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
     * @return video ID of the link
     */
    private String getVideoId(String url) {
        final int ID_GROUP = 6;
        String videoId = "";

        if (url != null && url.trim().length() > 0) {
            String expression = "(http:|https:|)\\/\\/(player.|www.)?(vimeo\\.com|youtu(be\\" +
                    ".com|\\.be|be\\.googleapis\\.com))\\/(video\\/|embed\\/|watch\\?v=|v\\/)?" +
                    "([A-Za-z0-9._%-]*)(\\&\\S+)?";

            Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(url);

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

    /**
     * Distinguishes different kinds of app starts: <li>
     * <ul>
     * First start ever ({@link #FIRST_TIME})
     * </ul>
     * <ul>
     * First start in this version ({@link #FIRST_TIME_VERSION})
     * </ul>
     * <ul>
     * Normal app start ({@link #NORMAL})
     * </ul>
     *
     * @author schnatterer
     */
    public enum AppStart {
        FIRST_TIME, FIRST_TIME_VERSION, NORMAL
    }

    // AsyncTask
    private class GetHtmlData extends AsyncTask<String, Void, Boolean> {
        List<String> bundle = new ArrayList<>();
        boolean isImage;

        String contentUrl;
        String hdImageUrl;
        String htmlTitle;
        String explanation;

        @Override
        protected Boolean doInBackground(String... url) {
            FirebaseCrash.log("Date: " + date);

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
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                    }
                });
                return false;
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
            } else if (video != null) {
                isImage = false;
                contentUrl = video.absUrl("src");

                if (contentUrl.contains("youtu")) {
                    contentUrl = YT_BASE_URL + getVideoId(contentUrl);
                } else if (contentUrl.contains("vimeo")) {
                    contentUrl = VM_BASE_URL + getVideoId(contentUrl);
                }
            } else {
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
            } else {
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
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, R.string.error_server, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            titleText.setText(htmlTitle);
            description.setText(explanation, true);

            sdUrl = contentUrl.replaceAll("http://", "https://");

            if (isImage) {
                // Check preferences if user wants HD images saved
                if (sharedPref.getString(SettingsActivity.TAG_PREF_QUALITY, "").equals("1") &&
                        !hdImageUrl.equals("")) {
                    imgUrl = hdImageUrl;
                } else {
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

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        resetText();
                                        Toast.makeText(MainActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
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
            } else {
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
            } else if (progressBar.getVisibility() == View.GONE) {
                if (imageAvailable && imageView.getDrawable() == null) {
                    openNonImageContent(sdUrl);
                } else if (imageView.getDrawable() != null) {
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
            } else {
                Toast.makeText(MainActivity.this, R.string.toast_view_image, Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }
}
