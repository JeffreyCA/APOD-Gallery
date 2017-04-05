package ca.jeffrey.apodgallery.wallpaper;

import android.app.WallpaperManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
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
        DatabaseReference mDatabase;
        mDatabase = FirebaseDatabase.getInstance().getReference();

        mDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String sdurl = dataSnapshot.child("sdurl").getValue().toString();
                String hdurl = dataSnapshot.child("hdurl").getValue().toString();

                Log.i("hdurl", hdurl);
                Log.i("sdurl", sdurl);
                try {
                    setImageData(sdurl, hdurl);
                } catch (Exception e) {
                    FirebaseCrash.report(e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Database", "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

    private void setImageData(String sdUrl, String hdUrl) throws IOException, ExecutionException, InterruptedException {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(WallpaperChangeService.this);

        final WallpaperManager manager = WallpaperManager.getInstance(this);
        final int w = manager.getDesiredMinimumWidth();
        final int h = manager.getDesiredMinimumHeight();

        sharedPreferences.edit().putString("last_ran", today).apply();
        sharedPreferences.edit().putBoolean("non_image", false).apply();
        sharedPreferences.edit().putBoolean("today_retrieved", true).apply();

        Log.i("DesiredMinimumWidth: ", String.valueOf(w));
        Log.i("DesiredMinimumHeight: ", String.valueOf(h));
        Log.i("URL: ", sdUrl);

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

    private Bitmap toSquareBitmapCanvas(Bitmap srcBmp) {
        int dim = Math.max(srcBmp.getWidth(), srcBmp.getHeight());
        Bitmap dstBmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dstBmp);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(srcBmp, (dim - srcBmp.getWidth()) / 2, (dim - srcBmp.getHeight()) / 2, null);

        return dstBmp;
    }

    private boolean isPano(Bitmap b) {
        return b.getWidth() > (3 * b.getHeight());
    }
}
