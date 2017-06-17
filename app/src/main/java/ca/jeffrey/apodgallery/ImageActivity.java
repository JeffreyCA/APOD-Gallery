package ca.jeffrey.apodgallery;

/**
 * Created by Jeffrey on 7/22/2016.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;

import ca.jeffrey.apodgallery.image.TouchImageView;

public class ImageActivity extends Activity {
    private final float MAX_ZOOM = 3.5f;
    private final String DEFAULT_IMAGE_DIRECTORY = Environment.getExternalStorageDirectory().getPath() +
      File.separator + "APOD";
    private final String IMAGE_EXT = ".jpg";

    private SharedPreferences sharedPref;

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
        // Set max zoom
        imageView.setMaxZoom(MAX_ZOOM);

        // Load image with Glide as bitmap
        Glide.with(ImageActivity.this).load(url).asBitmap().diskCacheStrategy(DiskCacheStrategy
                .SOURCE).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                    glideAnimation) {
                imageView.setImageBitmap(resource);

                if (setWallpaper) {
                    setAsWallpaper(date);
                }
            }
        });
    }

    /**
     * Set saved image as device wallpaper
     *
     * @param imageDate date of featured image
     */
    private void setAsWallpaper(String imageDate) {
        final String IMAGE_DIRECTORY = sharedPref.getString(SettingsActivity.TAG_PREF_LOCATION,
                DEFAULT_IMAGE_DIRECTORY);

        File image = new File(IMAGE_DIRECTORY + imageDate + IMAGE_EXT);
        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        Uri uri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            uri = FileProvider.getUriForFile(this, this.getApplicationContext().getPackageName()
                    + ".provider", image);
        }
        else {
            uri = Uri.fromFile(image);
        }

        intent.setDataAndType(uri, "image/*");
        intent.putExtra("mimeType", "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string
                .title_intent_wallpaper)));
    }
}