package com.wizardike.gallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

/**
 * An activity that can be used to view one image
 */
public class ViewOnePhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_one_photo);

        ImageView imageView = findViewById(R.id.main_image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Intent intent = getIntent();
        if(intent != null) {
            Uri photoLocation = intent.getData();
            String action = intent.getAction();
            if(Intent.ACTION_VIEW.equals(action) && photoLocation != null) {
                Bitmap bmp = BitmapFactory.decodeFile(photoLocation.getPath());
                imageView.setImageBitmap(bmp);
                int orientation = intent.getIntExtra("orientation", 0);
                imageView.setRotation(orientation);
            }
        }
    }
}
