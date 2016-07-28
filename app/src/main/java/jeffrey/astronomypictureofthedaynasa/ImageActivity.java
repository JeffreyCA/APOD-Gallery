package jeffrey.astronomypictureofthedaynasa;

/**
 * Created by Jeffrey on 7/22/2016.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;

public class ImageActivity extends Activity {
    final String IMAGE_DIRECTORY = "APOD";
    final String IMAGE_EXT = ".jpg";
    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

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

        final TouchImageView imageView = (TouchImageView) findViewById(R.id.zoom_image);
        // Set zoom to 3.5x
        imageView.setMaxZoom(3.5f);

        // Load image with Glide as bitmap
        Glide.with(ImageActivity.this).load(url).asBitmap().diskCacheStrategy
                (DiskCacheStrategy.SOURCE).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                    glideAnimation) {
                imageView.setImageBitmap(resource);

                if (setWallpaper) {
                    verifyStoragePermissions(ImageActivity.this);
                    setAsWallpaper(resource, date);
                }
            }
        });
    }

    public void setAsWallpaper(Bitmap bitmap, String imageDate) {
        File image = new File(Environment.getExternalStorageDirectory().getPath() +
                File.separator + IMAGE_DIRECTORY + File.separator + imageDate + IMAGE_EXT);

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(Uri.fromFile(image), "image/jpeg");
        intent.putExtra("mimeType", "image/jpeg");
        this.startActivity(Intent.createChooser(intent, "Set as:"));
    }
}