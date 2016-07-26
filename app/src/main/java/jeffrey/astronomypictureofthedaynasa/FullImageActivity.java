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
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FullImageActivity extends Activity {

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
        else {

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
        Glide.with(FullImageActivity.this).load(url).asBitmap().diskCacheStrategy
                (DiskCacheStrategy.SOURCE).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                    glideAnimation) {
                imageView.setImageBitmap(resource);

                if (setWallpaper) {
                    verifyStoragePermissions(FullImageActivity.this);
                    setAsWallpaper(resource, date);
                }
            }
        });
    }

    public void setAsWallpaper(Bitmap bitmap, String imageDate) {
        File imageDirectory = new File(Environment.getExternalStorageDirectory().getPath() +
                "/AOPD");

        if (!imageDirectory.exists()) {
            imageDirectory.mkdir();
        }
        String filename = imageDate + ".jpg";
        String message = getResources().getString(R.string.toast_save_image) + filename;
        File image = new File(imageDirectory, filename);

        // Encode the file as a JPG image.
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(image);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);

            outStream.flush();
            outStream.close();

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            Toast.makeText(this, R.string.error_saving + image.getPath(), Toast.LENGTH_SHORT)
                    .show();
        }

        Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setDataAndType(Uri.fromFile(image), "image/jpeg");
        intent.putExtra("mimeType", "image/jpeg");
        this.startActivity(Intent.createChooser(intent, "Set as:"));
    }
}