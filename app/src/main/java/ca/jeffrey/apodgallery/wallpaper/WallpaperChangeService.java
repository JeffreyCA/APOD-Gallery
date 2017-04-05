package ca.jeffrey.apodgallery.wallpaper;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

public class WallpaperChangeService extends GcmTaskService {
    public static final String TAG_TASK_DAILY = "tag_task_daily";
    public static final String TAG_TASK_ONEOFF = "tag_oneoff";

    private String today;
    private DatabaseReference database;

    private String getLatestImageDate() {
        final String[] date = {today};

        database.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                date[0] = dataSnapshot.child("date").getValue().toString();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.log(databaseError.toString());
            }
        });

        return date[0];
    }

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
    }

    private void getImageData() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String sdUrl = dataSnapshot.child("sdurl").getValue().toString();
                final String hdUrl = dataSnapshot.child("hdurl").getValue().toString();

                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setImageData(sdUrl, hdUrl);
                            Log.i("APOD Wallpaper", "Set");
                        } catch (Exception e) {
                            FirebaseCrash.report(e);
                        }
                    }
                };
                AsyncTask.execute(runnable);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                FirebaseCrash.log(databaseError.toString());
            }
        });
    }


    private void setImageData(String sdUrl, String hdUrl) throws IOException, ExecutionException, InterruptedException {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(WallpaperChangeService.this);

        final WallpaperManager manager = WallpaperManager.getInstance(this);
        final int w = manager.getDesiredMinimumWidth();
        final int h = manager.getDesiredMinimumHeight();

        if (!sdUrl.equals(sharedPreferences.getString("last_url", ""))) {
            sharedPreferences.edit().putString("last_ran", today).apply();
            sharedPreferences.edit().putString("last_url", sdUrl).apply();

            // Log.i("DesiredMinimumWidth: ", String.valueOf(w));
            // Log.i("DesiredMinimumHeight: ", String.valueOf(h));
            // Log.i("URL: ", sdUrl);

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
        }
    }

    private boolean isEvieLauncher() {
        final String EVIE_PACKAGE_NAME = "is.shortcut";

        PackageManager localPackageManager = getPackageManager();
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        String packageName = localPackageManager.resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY).activityInfo.packageName;

        return packageName.equals(EVIE_PACKAGE_NAME);
    }

    private boolean isPano(Bitmap b) {
        return b.getWidth() > (3 * b.getHeight());
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

    private Bitmap toSquareBitmapCanvas(Bitmap srcBmp) {
        int dim = Math.max(srcBmp.getWidth(), srcBmp.getHeight());
        Bitmap dstBmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dstBmp);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(srcBmp, (dim - srcBmp.getWidth()) / 2, (dim - srcBmp.getHeight()) / 2, null);

        return dstBmp;
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this);

        today = new SimpleDateFormat("y-MM-dd").format(new Date());
        database = FirebaseDatabase.getInstance().getReference();

        String lastRan;
        boolean todayRetrieved;

        switch (taskParams.getTag()) {
            case TAG_TASK_DAILY:
                lastRan = sharedPreferences.getString("last_ran", "");

                // Already up-to-date or no image available
                if (lastRan.equals(getLatestImageDate())) {
                    return GcmNetworkManager.RESULT_SUCCESS;
                }

                getImageData();
                return GcmNetworkManager.RESULT_SUCCESS;
            case TAG_TASK_ONEOFF:
                lastRan = sharedPreferences.getString("last_ran", "");

                // Already up-to-date or no image available
                if (lastRan.equals(getLatestImageDate())) {
                    return GcmNetworkManager.RESULT_SUCCESS;
                }

                getImageData();
                return GcmNetworkManager.RESULT_SUCCESS;
            default:
                return GcmNetworkManager.RESULT_FAILURE;
        }
    }
}
