package ca.jeffrey.apodgallery;

import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyTaskService extends GcmTaskService {
    public static final String TAG_TASK_DAILY = "tag_task_daily";
    public static final String TAG_TASK_ONEOFF = "tag_oneoff";
    public static final String TAG_TASK_MINUTELY = "tag_minutely";

    private void getImageData() {
        String url = "https://api.nasa.gov/planetary/apod?api_key=" + MainActivity.API_KEY;
        Log.i("getImageData", "reached");
        doJsonRequest(url);
    }

    private void doJsonRequest(String url) {
        OkHttpClient client;
        // Initialize OkHttp client and cache
        client = new OkHttpClient.Builder().cache(new Cache(getCacheDir(), 10 * 1024 * 1024)) // 10M
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        request = request.newBuilder().header("Cache-Control", "public, " +
                                "max-age=" + 60).build();
                        return chain.proceed(request);
                    }
                })
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).build();

        // Build request
        Request request = new Request.Builder().cacheControl(new CacheControl.Builder()
                .onlyIfCached().build()).url(url).build();
        // Request call
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                String res = response.body().string();

                // Parse JSON object

                try {
                    JSONObject object = new JSONObject(res);
                    onJsonResponse(object);

                }
                // Error handling
                catch (JSONException e) {
                    e.printStackTrace();

                    int code = response.code();

                    switch (code) {
                        // Server error
                        case 400:
                            // Too early
                        case 500:
                            // Too early
                            // /break;
                        // Client-side network error
                        case 504:
                            // break;
                        // Default server error
                        default:
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static final double multiplier = 1.50;

    // Load Bitmap from file
    public Bitmap loadBitmap(final String filename,
                             final WallpaperManager wallpaperManager) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // switching it on
        BitmapFactory.decodeFile(filename, options);
        options.inSampleSize = calculateInSampleSize(options, wallpaperManager); // Load Bitmap in to memory with the option save as much memory as possible, very important!
        options.inPreferQualityOverSpeed = true; // Beauty over speed in case of wallpapers
        options.inJustDecodeBounds = false; // switching it off
        options.inMutable = true; // This to make sure to use less memory, due to Bitmap not being mutable
        return BitmapFactory.decodeFile(filename, options);
    }

    // Calculate best sample-size to load Bitmap in to memory
    public int calculateInSampleSize(final BitmapFactory.Options options,
                                     final WallpaperManager wallpaperManager) {
        final int rawHeight = options.outHeight; // height of source Bitmap
        final int rawWidth = options.outWidth; // width of source Bitmap
        final int reqHeight = (int) (wallpaperManager.getDesiredMinimumHeight() * multiplier); // desired device height * multiplier
        final int reqWidth = (int) (wallpaperManager.getDesiredMinimumWidth() * multiplier); // desired device width * multiplier
        int inSampleSize = 1;
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            inSampleSize = 2;
        }
        if (rawHeight > reqHeight * 2 || rawWidth > reqWidth * 2) {
            inSampleSize = 4;
        }
        return inSampleSize;
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

    // Crop or inflate bitmap to desired device height and/or width
    public Bitmap prepareBitmap(final Bitmap sampleBitmap,
                                final WallpaperManager wallpaperManager) {
        Bitmap changedBitmap = null;
        final int heightBm = sampleBitmap.getHeight();
        final int widthBm = sampleBitmap.getWidth();
        final int heightDh = wallpaperManager.getDesiredMinimumHeight();
        final int widthDh = wallpaperManager.getDesiredMinimumWidth();

        if (widthDh > widthBm || heightDh > heightBm) {
            final int xPadding = Math.max(0, widthDh - widthBm) / 2;
            final int yPadding = Math.max(0, heightDh - heightBm) / 2;
            changedBitmap = Bitmap.createBitmap(widthDh, heightDh,
                    Bitmap.Config.ARGB_8888);
            final int[] pixels = new int[widthBm * heightBm];
            sampleBitmap.getPixels(pixels, 0, widthBm, 0, 0, widthBm, heightBm);
            changedBitmap.setPixels(pixels, 0, widthBm, xPadding, yPadding,
                    widthBm, heightBm);
        } else if (widthBm > widthDh || heightBm > heightDh) {
            changedBitmap = Bitmap.createBitmap(widthDh, heightDh,
                    Bitmap.Config.ARGB_8888);
            int cutLeft = 0;
            int cutTop = 0;
            int cutRight = 0;
            int cutBottom = 0;
            final Rect desRect = new Rect(0, 0, widthDh, heightDh);
            Rect srcRect = new Rect();
            if (widthBm > widthDh) { // crop width (left and right)
                cutLeft = (widthBm - widthDh) / 2;
                cutRight = (widthBm - widthDh) / 2;
                srcRect = new Rect(cutLeft, 0, widthBm - cutRight, heightBm);
            } else if (heightBm > heightDh) { // crop height (top and bottom)
                cutTop = (heightBm - heightDh) / 2;
                cutBottom = (heightBm - heightDh) / 2;
                srcRect = new Rect(0, cutTop, widthBm, heightBm - cutBottom);
            }
            final Canvas canvas = new Canvas(changedBitmap);
            canvas.drawBitmap(sampleBitmap, srcRect, desRect, null);
        } else {
            changedBitmap = sampleBitmap;
        }
        return changedBitmap;
    }



    private void onJsonResponse(JSONObject response) throws JSONException, ExecutionException, InterruptedException, IOException {
        final String IMAGE_TYPE = "image";
        String mediaType;
        final String sdUrl;
        String hdUrl;

        mediaType = response.getString("media_type");
        sdUrl = response.getString("url").replaceAll("http://", "https://");
        hdUrl = response.getString("hdurl").replaceAll("http://", "https://");

        if (mediaType.equals(IMAGE_TYPE)) {
            final WallpaperManager manager = WallpaperManager.getInstance(this);
            int w = manager.getDesiredMinimumWidth();
            int h = manager.getDesiredMinimumHeight();
            Log.i("width: ", String.valueOf(w));
            Log.i("height: ", String.valueOf(h));
            Log.i("URL: ", sdUrl);
            Bitmap result = Glide.with(this).load(sdUrl).asBitmap().override(w, h).into(w, h).get();
            Bitmap cropped = prepareBitmap(result, manager);
            Bitmap scaled = Bitmap.createScaledBitmap(result, w, h, true);
            manager.setBitmap(cropped);
        }
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        switch (taskParams.getTag()) {
            case TAG_TASK_DAILY:
                int count = sharedPreferences.getInt(TAG_TASK_MINUTELY, 0) + 1;
                sharedPreferences.edit().putInt(TAG_TASK_MINUTELY, count).apply();
                getImageData();
                return GcmNetworkManager.RESULT_SUCCESS;
            case TAG_TASK_ONEOFF:
                String url = "http://androidwalls.net/wp-content/uploads/2017/01/San%20Francisco%20Golden%20Gate%20Bridge%20Fog%20Lights%20Android%20Wallpaper.jpg";
                try {
                    Log.i("ONE_OFF", "Reached");
                    // Bitmap result = Glide.with(this).load(url)
                    //         .asBitmap().into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
                    // WallpaperManager manager = WallpaperManager.getInstance(this);
                    // manager.setBitmap(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return GcmNetworkManager.RESULT_SUCCESS;
            default:
                return GcmNetworkManager.RESULT_FAILURE;
        }
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
    }
}
