package ca.jeffrey.apodgallery;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

public class MyTaskService extends GcmTaskService {
    public static final String TAG_TASK_ONEOFF = "tag_oneoff";
    public static final String TAG_TASK_MINUTELY = "tag_minutely";

    public MyTaskService() {

    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        switch (taskParams.getTag()) {
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

            case TAG_TASK_MINUTELY:
                SharedPreferences sharedPreferences = PreferenceManager
                        .getDefaultSharedPreferences(this);
                int count = sharedPreferences.getInt(TAG_TASK_MINUTELY, 0) + 1;
                sharedPreferences.edit().putInt(TAG_TASK_MINUTELY, count).apply();
                Log.i(TAG_TASK_MINUTELY, "Count: " + count);

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
