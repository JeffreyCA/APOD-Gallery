package jeffrey.astronomypictureofthedaynasa;

/**
 * Created by Jeffrey on 7/22/2016.
 */

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

public class FullImageActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        // Receive image url from intent
        Bundle extras = getIntent().getExtras();
        String url = extras.getString("url");

        final TouchImageView imageView = (TouchImageView) findViewById(R.id.zoom_image);
        // Set zoom to 3.5x
        imageView.setMaxZoom(3.5f);

        // Load image with Glide as bitmap
        Glide.with(FullImageActivity.this).load(url).asBitmap().diskCacheStrategy
                (DiskCacheStrategy.SOURCE).centerCrop().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap>
                    glideAnimation) {
                imageView.setImageBitmap(resource);
            }
        });
    }
}