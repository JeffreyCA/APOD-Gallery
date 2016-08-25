package ca.jeffrey.apodgallery;

/**
 * Created by Jeffrey on 7/22/2016.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;

public class ImageActivity extends Activity {
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    final String DEFAULT_IMAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath() +
            File.separator + "APOD";
    final String IMAGE_EXT = ".jpg";

    SharedPreferences sharedPref;

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission
                .WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        // Receive image url from intent
        Bundle extras = getIntent().getExtras();
        String url = extras.getString("url");
        final String date = extras.getString("date");
        final boolean setWallpaper = extras.getBoolean("wallpaper");

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        final TouchImageView imageView = (TouchImageView) findViewById(R.id.zoom_image);
        // Set zoom to 3.5x
        imageView.setMaxZoom(3.5f);

        // Load image with Glide as bitmap
        Glide.with(ImageActivity.this).load(url).asBitmap().diskCacheStrategy(DiskCacheStrategy
                .SOURCE).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                    glideAnimation) {
                imageView.setImageBitmap(resource);

                if (setWallpaper) {
                    boolean hasPermission = MainActivity.checkPermission(ImageActivity.this);

                    if (hasPermission) {
                        setAsWallpaper(date);
                    }
                }
            }
        });
    }

    /**
     * Set saved image as device wallpaper
     *
     * @param imageDate date of featured image
     */
    public void setAsWallpaper(String imageDate) {
        final String IMAGE_DIRECTORY = sharedPref.getString("pref_save_location",
                DEFAULT_IMAGE_DIRECTORY);

        File image = new File(IMAGE_DIRECTORY + imageDate + IMAGE_EXT);
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(Uri.fromFile(image), "image/jpeg");
        intent.putExtra("mimeType", "image/jpeg");
        this.startActivity(Intent.createChooser(intent, "Set as:"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        if (requestCode == MainActivity.WRITE_PERMISSION) {
            for (int i = 0, len = permissions.length; i < len; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    // Toast.makeText(this, R.string.toast_permission_granted, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
        }
    }
}