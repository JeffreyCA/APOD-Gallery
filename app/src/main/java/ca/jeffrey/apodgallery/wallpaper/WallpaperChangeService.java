package ca.jeffrey.apodgallery.wallpaper;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import ca.jeffrey.apodgallery.MainActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WallpaperChangeService extends GcmTaskService {
    public static final String TAG_TASK_DAILY = "tag_task_daily";
    public static final String TAG_TASK_ONEOFF = "tag_oneoff";
    public static final String TAG_TASK_MINUTELY = "tag_minutely";

    String today;

    @Override
    public int onRunTask(TaskParams taskParams) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        today = new SimpleDateFormat("y-MM-dd").format(new Date());

        boolean nonImage;
        String lastRan;
        boolean todayRetrieved;

        switch (taskParams.getTag()) {
            case TAG_TASK_DAILY:
                 nonImage = sharedPreferences.getBoolean("non_image", false);
                 lastRan = sharedPreferences.getString("last_ran", "");
                todayRetrieved = sharedPreferences.getBoolean("today_retrieved", false);

                // Already up-to-date or no image available
                if (lastRan.equals(today) && (todayRetrieved || nonImage)) {
                    return GcmNetworkManager.RESULT_SUCCESS;
                }

                getImageData();

                return GcmNetworkManager.RESULT_SUCCESS;
            case TAG_TASK_ONEOFF:
                nonImage = sharedPreferences.getBoolean("non_image", false);
                lastRan = sharedPreferences.getString("last_ran", "");
                todayRetrieved = sharedPreferences.getBoolean("today_retrieved", false);

                // Already up-to-date or no image available
                if (lastRan.equals(today) && (todayRetrieved || nonImage)) {
                    return GcmNetworkManager.RESULT_SUCCESS;
                }

                getImageData();

                return GcmNetworkManager.RESULT_SUCCESS;
            default:
                return GcmNetworkManager.RESULT_FAILURE;
        }
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
    }

    private void getImageData() {
        String url = "https://api.nasa.gov/planetary/apod?api_key=" + MainActivity.API_KEY +
                "&date=" + today;

        Log.i("getImageData", "reached");

        doJsonRequest(url);
    }

    private void doJsonRequest(final String url) {
        OkHttpClient client;
        // Initialize OkHttp client and cache
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).build();

        // Build request
        Request request = new Request.Builder().url(url).build();
        // Request call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
                FirebaseCrash.log(url);
                FirebaseCrash.report(new Exception("Daily Wallpaper - onFailure"));
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                String res = response.body().string();

                try {
                    JSONObject object = new JSONObject(res);
                    onJsonResponse(object);
                } catch (JSONException je) {
                    SharedPreferences sharedPreferences = PreferenceManager
                            .getDefaultSharedPreferences(WallpaperChangeService.this);

                    sharedPreferences.edit().putBoolean("non_image", false).apply();
                    sharedPreferences.edit().putString("last_ran", today).apply();
                    sharedPreferences.edit().putBoolean("today_retrieved", false).apply();
                }
                // Error handling
                catch (Exception e) {
                    FirebaseCrash.log(url);
                    FirebaseCrash.report(new Exception("Daily Wallpaper - OtherException"));
                }
            }
        });
    }

    private void onJsonResponse(JSONObject response) throws JSONException, ExecutionException, InterruptedException, IOException {
        final String IMAGE_TYPE = "image";
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(WallpaperChangeService.this);

        String mediaType;
        String sdUrl;
        String hdUrl;

        mediaType = response.getString("media_type");
        sdUrl = response.getString("url").replaceAll("http://", "https://");
        hdUrl = response.getString("hdurl").replaceAll("http://", "https://");

        if (mediaType.equals(IMAGE_TYPE)) {
            final WallpaperManager manager = WallpaperManager.getInstance(this);
            final int w = manager.getDesiredMinimumWidth();
            final int h = manager.getDesiredMinimumHeight();

            sharedPreferences.edit().putString("last_ran", today).apply();
            sharedPreferences.edit().putBoolean("non_image", false).apply();
            sharedPreferences.edit().putBoolean("today_retrieved", true).apply();

            Log.i("DesiredMinimumWidth: ", String.valueOf(w));
            Log.i("DesiredMinimumHeight: ", String.valueOf(h));
            Log.i("URL: ", sdUrl);

            // sdUrl = "https://apod.nasa.gov/apod/image/1702/ElNidoEcliptic500La.jpg";

            Bitmap original = Glide.with(this).load(sdUrl).asBitmap()
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.RESULT)
                    .into(w, h).get();
            Bitmap processed;


            if (isPano(original)) {
                if (isEvieLauncher()) {
                    processed = toSquareBitmapCanvas(original);
                } else {
                    processed = scaleBitmap(original, "autofill", manager);
                }
            } else {
                processed = original;
            }

            manager.setBitmap(processed);
            original.recycle();
        } else {
            sharedPreferences.edit().putString("last_ran", today).apply();
            sharedPreferences.edit().putBoolean("non_image", true).apply();
        }
    }

    boolean isEvieLauncher() {
        final String EVIE_PACKAGE_NAME = "is.shortcut";

        PackageManager localPackageManager = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        String packageName = localPackageManager.resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;

        return packageName.equals(EVIE_PACKAGE_NAME);
    }

    // Scale Bitmap to fit to max height, width, autofit, or autofill
    public Bitmap scaleBitmap(Bitmap sampleBitmap,
                              final String setWallAspect,
                              final WallpaperManager wallpaperManager) {
        final double heightBm = sampleBitmap.getHeight(); // height of Bitmap in memory
        final double widthBm = sampleBitmap.getWidth(); // width of Bitmap in memory
        final double heightDh = wallpaperManager.getDesiredMinimumHeight(); // desired device height
        final double widthDh = wallpaperManager.getDesiredMinimumWidth(); // desired device width
        double factor = 1.0;
        double width = 0;
        double height = 0;
        // If statement to decide, based on the options, how to position the Bitmap as the wallpaper
        if (setWallAspect.equals("height")) { // keep height of Bitmap, may result in black bars
            factor = heightDh / heightBm * 1;
            height = heightDh;
            width = Math.round(widthBm * factor);
        } else if (setWallAspect.equals("width")) { // keep width of Bitmap, may result in black bars
            factor = widthDh / widthBm * 1;
            width = widthDh;
            height = Math.round(heightBm * factor);
        } else if (setWallAspect.equals("autofit")) { // fit entire Bitmap on screen, may result in black bars
            if (heightBm >= widthBm) {
                factor = heightDh / heightBm * 1;
                height = heightDh;
                width = Math.round(widthBm * factor);
            } else {
                factor = widthDh / widthBm * 1;
                width = widthDh;
                height = Math.round(heightBm * factor);
            }
        } else if (setWallAspect.equals("autofill")) { // fill entire screen with Bitmap
            if (heightBm >= widthBm) {
                factor = widthDh / widthBm * 1;
                width = widthDh;
                height = Math.round(heightBm * factor);
            } else {
                factor = heightDh / heightBm * 1;
                height = heightDh;
                width = Math.round(widthBm * factor);
            }
        }
        sampleBitmap = Bitmap.createScaledBitmap(sampleBitmap, (int) width,
                (int) height, true);
        return sampleBitmap;
    }

    private Bitmap toSquareBitmapThumbnail(Bitmap bitmap) {
        int dimension = Math.min(bitmap.getWidth(), bitmap.getHeight());
        return ThumbnailUtils.extractThumbnail(bitmap, dimension, dimension);
    }

    private Bitmap toSquareBitmapCanvas(Bitmap srcBmp) {
        int dim = Math.max(srcBmp.getWidth(), srcBmp.getHeight());
        Bitmap dstBmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dstBmp);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(srcBmp, (dim - srcBmp.getWidth()) / 2, (dim - srcBmp.getHeight()) / 2, null);

        return dstBmp;
    }

    private Bitmap toSquareBitmap(Bitmap bitmap) {
        if (bitmap.getWidth() >= bitmap.getHeight()) {
            return Bitmap.createBitmap(
                    bitmap,
                    bitmap.getWidth() / 2 - bitmap.getHeight() / 2,
                    0,
                    bitmap.getHeight(),
                    bitmap.getHeight()
            );

        } else {

            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    bitmap.getHeight() / 2 - bitmap.getWidth() / 2,
                    bitmap.getWidth(),
                    bitmap.getWidth()
            );
        }
    }

    private boolean isPano(Bitmap b) {
        return b.getWidth() > (3 * b.getHeight());
    }
}
