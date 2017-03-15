package ca.jeffrey.apodgallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        boolean wallpaperEnabled = sharedPreferences.getBoolean("pref_daily_wallpaper", false);
        clearAllTasks(context);

        if (wallpaperEnabled && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            setPeriodicTask(context);
        }
    }

    private void setPeriodicTask(Context context) {
        final int PERIOD = 3600 * 8;
        final int FLEX = 3600 * 2;

        GcmNetworkManager gcmNetworkManager = GcmNetworkManager.getInstance(context);

        PeriodicTask task = new PeriodicTask.Builder()
                .setTag(MyTaskService.TAG_TASK_DAILY)
                .setService(MyTaskService.class)
                .setPeriod(PERIOD)
                .setFlex(FLEX)
                .setPersisted(true)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)  // not needed, default
                .setUpdateCurrent(true) // not needed, you know this is 1st time
                .build();
        gcmNetworkManager.schedule(task);

        OneoffTask immediateTask = new OneoffTask.Builder()
                .setService(MyTaskService.class)
                .setTag(MyTaskService.TAG_TASK_ONEOFF)
                .setExecutionWindow(0, 5)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .build();

        gcmNetworkManager.schedule(immediateTask);
    }

    private void clearAllTasks(Context context) {
        GcmNetworkManager
                .getInstance(context)
                .cancelAllTasks(MyTaskService.class);
    }
}
