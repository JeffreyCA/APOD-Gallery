package ca.jeffrey.apodgallery;

import android.app.WallpaperManager;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
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
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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


    private Bitmap anotherScaler(Bitmap bitmap) {
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

    private void onJsonResponse(JSONObject response) throws JSONException, ExecutionException, InterruptedException, IOException {
        final String IMAGE_TYPE = "image";
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
            
            Log.i("DesiredMinimumWidth: ", String.valueOf(w));
            Log.i("DesiredMinimumHeight: ", String.valueOf(h));
            Log.i("URL: ", sdUrl);

            Bitmap original = Glide.with(this).load(sdUrl).asBitmap().into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get();
            Bitmap desired = Glide.with(this).load(sdUrl).asBitmap().into(w, h).get();

            // Works for all, may degrade quality
            Bitmap square = anotherScaler(original);

            // Works for most (not panorama)
            Bitmap autofill = scaleBitmap(original, "autofill", manager);
            Bitmap autofit = scaleBitmap(original, "autofit", manager);
            manager.setBitmap(original);

            Log.i("OG width: ", String.valueOf(original.getWidth()));
            Log.i("OG height: ", String.valueOf(original.getHeight()));
            Log.i("Desired width: ", String.valueOf(desired.getWidth()));
            Log.i("Desired height: ", String.valueOf(desired.getHeight()));
            Log.i("Autofill width: ", String.valueOf(autofill.getWidth()));
            Log.i("Autofill height: ", String.valueOf(autofill.getHeight()));
            Log.i("Autofill width: ", String.valueOf(autofit.getWidth()));
            Log.i("Autofill height: ", String.valueOf(autofit.getHeight()));

            /*
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                public void run() {
                    Glide.with(MyTaskService.this)
                            .load("https://apod.nasa.gov/apod/image/1702/ssc2017-trappist1_1024.jpg")
                            .asBitmap()
                            // .override(w, h)
                            .into(new SimpleTarget<Bitmap>(w, h) {
                                @Override
                                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                    try {
                                        manager.setBitmap(resource);
                                        Log.i("Final width: ", String.valueOf(resource.getWidth()));
                                        Log.i("Final height: ", String.valueOf(resource.getHeight()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                }
            });
            */

        }
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        switch (taskParams.getTag()) {
            case TAG_TASK_DAILY:
                int count = sharedPreferences.getInt(TAG_TASK_DAILY, 0) + 1;
                sharedPreferences.edit().putInt(TAG_TASK_DAILY, count).apply();
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
