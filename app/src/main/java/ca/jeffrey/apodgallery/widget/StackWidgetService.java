package ca.jeffrey.apodgallery.widget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.google.firebase.crash.FirebaseCrash;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import ca.jeffrey.apodgallery.R;
import ca.jeffrey.apodgallery.SettingsActivity;

public class StackWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    final static String TAG_MAX_IMAGES = "pref_max_images";
    final static int DEFAULT_COUNT = 10;
    private int mCount;
    private List<WidgetItem> mWidgetItems = new ArrayList<>();
    private Context mContext;
    private int mAppWidgetId;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        mCount = intent.getIntExtra(TAG_MAX_IMAGES, DEFAULT_COUNT);

        int filesAvailable = countFiles();

        if (filesAvailable == -1) {
            mCount = 0;
        } else if (filesAvailable < mCount) {
            mCount = filesAvailable;
        }
    }

    private int countFiles() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        // Get save location from preferences
        String directory_path = sharedPreferences.getString(SettingsActivity.TAG_PREF_LOCATION,
                Environment.getExternalStorageDirectory().getPath() + "/APOD/");

        File directory = new File(directory_path);
        directory.mkdirs();

        File[] list = directory.listFiles();

        if (list == null) {
            return -1;
        }

        int count = 0;

        for (File f: list){
            String name = f.getName();
            if (name.endsWith(".jpg"))
                count++;
        }
        return count;
    }

    public void onCreate() {
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        // Get save location from preferences
        String directory_path = sharedPreferences.getString(SettingsActivity.TAG_PREF_LOCATION,
                Environment.getExternalStorageDirectory().getPath() + "/APOD/");

        // Replace with preferences value
        File file = new File(directory_path);
        File[] list = file.listFiles();

        mWidgetItems.clear();

        if (list != null) {
            for (File f : list) {
                String name = f.getName();
                if (name.endsWith(".jpg")) {
                    name = name.replaceAll(".jpg", "");
                    mWidgetItems.add(new WidgetItem(name));
                }
            }
        }
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            FirebaseCrash.report(e);
        }
    }
    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
        mWidgetItems.clear();
    }
    public int getCount() {
        return mCount;
    }

    public RemoteViews getViewAt(int position) {
        // position will always range from 0 to getCount() - 1.
        // We construct a remote views item based on our widget item xml file, and set the
        // text based on the position.
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.layout_item);
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        // Get save location from preferences
        final String IMAGE_DIRECTORY = sharedPreferences.getString(SettingsActivity.TAG_PREF_LOCATION,
                Environment.getExternalStorageDirectory().getPath() + "/APOD/");

        final String EXT = ".jpg";

        if (mWidgetItems.size() > position) {
            WidgetItem item = mWidgetItems.get(position);
            rv.setTextViewText(R.id.widget_text, item.getFormattedDate());
            try {
                File f = new File(IMAGE_DIRECTORY + mWidgetItems.get(position).getDate() + EXT);
                Bitmap b = BitmapFactory.decodeStream(new FileInputStream(f));
                rv.setImageViewBitmap(R.id.widget_image, b);
            } catch (Exception e) {
                FirebaseCrash.report(e);
            }
        }

        // Next, we set a fill-intent which will be used to fill-in the pending intent template
        // which is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putInt(WidgetProvider.EXTRA_ITEM, position);
        if (mWidgetItems.size() > position) {
            extras.putString(WidgetProvider.EXTRA_DATE, mWidgetItems.get(position).getDate());
        } else {
            extras.putString(WidgetProvider.EXTRA_DATE, "");
        }

        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.stackWidgetItem, fillInIntent);
        // You can do heaving lifting in here, synchronously. For example, if you need to
        // process an image, fetch something from the network, etc., it is ok to do it here,
        // synchronously. A loading view will show up in lieu of the actual contents in the
        // interim.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            FirebaseCrash.report(e);
        }
        // Return the remote views object.
        return rv;
    }
    public RemoteViews getLoadingView() {
        // You can create a custom loading view (for instance when getViewAt() is slow.) If you
        // return null here, you will get the default loading view.
        return null;
    }
    public int getViewTypeCount() {
        return 1;
    }
    public long getItemId(int position) {
        return position;
    }
    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        final String TAG_MAX_IMAGES = "pref_max_images";
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);

        mCount = sharedPreferences.getInt(TAG_MAX_IMAGES, DEFAULT_COUNT);

        int filesAvailable = countFiles();

        if (filesAvailable == -1) {
            mCount = 0;
        } else if (filesAvailable < mCount) {
            mCount = filesAvailable;
            onCreate();
        }
    }
}
