package ca.jeffrey.apodgallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ca.jeffrey.apodgallery.wallpaper.WallpaperChangeManager;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        boolean wallpaperEnabled = sharedPreferences.getBoolean("pref_daily_wallpaper", false);
        WallpaperChangeManager wallpaperChangeManager = new WallpaperChangeManager(context);
        wallpaperChangeManager.cancelAll();

        if (wallpaperEnabled && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            wallpaperChangeManager.scheduleImmediateAndRecurring();
        }
    }
}
