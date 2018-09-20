package com.wizardike.gallery;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * An activity that can be used to view all the images on a phone in a grid
 */
public class ViewAllPhotosActivity extends AppCompatActivity {
    private static final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_photos);
        // Get permission to read images from the phone's storage
        if (Build.VERSION.SDK_INT > 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_REQUEST_CODE);
        } else {
            // We have all the permissions we need, display the images
            init();
        }
    }

    private void init() {
        int threadPoolSize = Runtime.getRuntime().availableProcessors();
        if(threadPoolSize < 1) {
            threadPoolSize = 1;
        }
        Executor executor = new ThreadPoolExecutor(1,
                threadPoolSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        PhotoAdapter adapter = new PhotoAdapter(this, executor,
                new PhotoAdapter.CreatedCallback() {
                    @Override
                    public void onCreateFinished(final PhotoAdapter adapter) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final GridView photosView = findViewById(R.id.photos_view);
                                photosView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                    @Override
                                    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                                        String name = adapter.getFileName(position);
                                        if(name != null) {
                                            Intent intent = new Intent(ViewAllPhotosActivity.this, ViewOnePhotoActivity.class);
                                            intent.setAction(Intent.ACTION_VIEW);
                                            intent.setData(Uri.fromFile(new File(name)));
                                            intent.putExtra("orientation", adapter.getOrientation(position));
                                            startActivity(intent);
                                        }
                                    }
                                });
                                photosView.setAdapter(adapter);
                                //adapter.notifyDataSetChanged();
                            }
                        });
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if(grantResults.length > 0
                && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            //User didn't give permission to read images, close the app
            finish();
        } else {
            // We have all the permissions we need, display the images
            init();
        }
    }
}
