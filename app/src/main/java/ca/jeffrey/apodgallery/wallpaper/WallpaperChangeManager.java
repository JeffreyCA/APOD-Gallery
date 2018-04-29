package ca.jeffrey.apodgallery.wallpaper;

import android.content.Context;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;

public class WallpaperChangeManager {
    private static final int IMMEDIATE = 5;
    private static final int PERIOD = 3600 * 8;
    private static final int FLEX = 3600 * 2;

    private FirebaseJobDispatcher dispatcher;

    public WallpaperChangeManager(Context context) {
        dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
    }

    public void cancelAll() {
        dispatcher.cancelAll();
    }

    public void scheduleImmediateAndRecurring() {
        Job immediateTask = dispatcher.newJobBuilder()
                .setService(WallpaperChangeService.class)
                .setTag(WallpaperChangeService.TAG_TASK_ONEOFF)
                .setTrigger(Trigger.executionWindow(0, IMMEDIATE))
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        Job periodicTask = dispatcher.newJobBuilder()
                .setService(WallpaperChangeService.class)
                .setTag(WallpaperChangeService.TAG_TASK_DAILY)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(PERIOD - FLEX, PERIOD))
                .setReplaceCurrent(true)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build();

        dispatcher.mustSchedule(immediateTask);
        dispatcher.mustSchedule(periodicTask);
    }
}
