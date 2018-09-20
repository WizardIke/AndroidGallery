package com.wizardike.gallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class ViewOnePhotoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_one_photo);

        ImageView imageView = findViewById(R.id.main_image);

        Intent intent = getIntent();
        if(intent != null) {
            Uri photoLocation = intent.getData();
            String action = intent.getAction();
            if(Intent.ACTION_VIEW.equals(action) && photoLocation != null) {
                Bitmap bmp = BitmapFactory.decodeFile(photoLocation.getPath());
                imageView.setImageBitmap(bmp);
                int orientation = intent.getIntExtra("orientation", ExifInterface.ORIENTATION_NORMAL);
                int rotationAngle = 0; //in degrees
                switch(orientation) {
                    case ExifInterface.ORIENTATION_NORMAL:
                        rotationAngle = 0;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        rotationAngle = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        rotationAngle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        rotationAngle = 270;
                        break;
                }
                imageView.setRotation(rotationAngle);
            }
        }
    }
}
