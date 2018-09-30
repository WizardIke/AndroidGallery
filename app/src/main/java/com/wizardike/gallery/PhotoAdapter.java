package com.wizardike.gallery;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Loads images that are on the phone as needed and wraps them in Views.
 */
public class PhotoAdapter extends BaseAdapter {
    private String[] imageLocations;
    private int[] imageOrientations;
    private final Activity activity;
    private final Executor executor;
    private int thumbnailSize = 200;
    private LruCache<String, Bitmap> memoryCache;

    /**
     * The type of function that will be called when a PhotoAdapter finishes loading.
     */
    interface CreatedCallback {
        void onCreateFinished(PhotoAdapter adapter);
    }

    /**
     * Creates a PhotoAdapter.
     * @param activity The Activity that will use the PhotoAdapter.
     * @param executor An Executor to load images on.
     * @param callback A function the will be called when the PhotoAdapter has been fully created and
     *                 is ready to use.
     */
    PhotoAdapter(Activity activity, Executor executor, final CreatedCallback callback) {
        this.activity = activity;
        this.executor = executor;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                //Get all the image file names from a content provider
                Pair<int[], String[]> imageDescriptions = getImageDescriptions();
                imageOrientations = imageDescriptions.first;
                imageLocations = imageDescriptions.second;

                // Get max available VM memory, exceeding this amount will throw an
                // OutOfMemory exception. Stored in kilobytes as LruCache takes an
                // int in its constructor.
                final int maxMemory = (int)(Runtime.getRuntime().maxMemory() / 1024);

                // Use 1/4th of the available memory for this memory cache or enough memory for all images.
                final int cacheSize = Math.min(maxMemory / 4, (imageLocations.length * 270 * 270 * 4) / 1024);

                memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                    @Override
                    protected int sizeOf(String key, Bitmap bitmap) {
                        // The cache size will be measured in kilobytes rather than
                        // number of items.
                        return bitmap.getByteCount() / 1024;
                    }
                };

                //signal that the adapter has finished loaded
                callback.onCreateFinished(PhotoAdapter.this);
            }
        });
    }

    /**
     * Reloads all the images if they have changed since the adapter whose last created or recreated.
     * @param callback A function that will be called on the ui thread when the adapter has finished recreating
     */
    public void reCreate(final CreatedCallback callback) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                final Pair<int[], String[]> imageDescriptions = getImageDescriptions();
                final boolean equal = Arrays.equals(imageDescriptions.first, imageOrientations)
                        && Arrays.equals(imageDescriptions.second, imageLocations);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(equal) {
                            //the data hasn't changed
                            callback.onCreateFinished(PhotoAdapter.this);
                        } else {
                            //the data has changed
                            PhotoAdapter.this.imageOrientations = imageDescriptions.first;
                            PhotoAdapter.this.imageLocations = imageDescriptions.second;
                            memoryCache.evictAll();
                            PhotoAdapter.this.notifyDataSetChanged();
                            callback.onCreateFinished(PhotoAdapter.this);
                        }
                    }
                });
            }
        });
    }

    /**
     * Gets the orientation and locations of all images on the phone
     * @return A pair containing orientations and locations
     */
    private Pair<int[], String[]> getImageDescriptions() {
        String[] imageLocations;
        int[] imageOrientations = null;

        ContentResolver contentResolver = PhotoAdapter.this.activity.getContentResolver();
        final String[] projection = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.ORIENTATION };
        final Cursor cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC");
        if(cursor != null) {
            imageLocations = new String[cursor.getCount()];
            imageOrientations = new int[cursor.getCount()];
            int i = 0;
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            final int orientationColumn = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
            //Iterate over the returned image names and add them to an array
            if (cursor.moveToFirst()) {

                do {
                    final String data = cursor.getString(dataColumn);
                    final int orientation = cursor.getInt(orientationColumn);
                    imageLocations[i] = data;
                    imageOrientations[i] = orientation;
                    ++i;
                } while (cursor.moveToNext());
            }
            cursor.close();
        } else {
            imageLocations = new String[]{};
        }

        return Pair.create(imageOrientations, imageLocations);
    }

    /**
     * Stores the current information about an item in the adapter.
     */
    private class ViewHolder {
        int position;
        ImageView image;
        ProgressBar progressBar;
    }

    public String getFileName(int position) {
        return imageLocations[position];
    }

    public int getOrientation(int position) {
        return imageOrientations[position];
    }

    /**
     * Gets the number of images in the adapter.
     * The adapter must have finished loaded.
     */
    @Override
    public int getCount() {
        return imageLocations.length;
    }


    /**
     * @return The file name of the image at position i
     */
    @Override
    public Object getItem(int i) {
        return imageLocations[i];
    }

    /**
     * Get a unique id for item at position i
     */
    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * Returns a view that displays the image at position i.
     * The if the image isn't already loaded, it will be loaded in the background and a progress bar
     * will be displayed.
     */
    @Override
    public View getView(final int i, View convertView, ViewGroup viewGroup) {
        Log.d("PhotoAdapter", "View Requested: " + i);
        final ViewHolder vh = convertView == null ? new ViewHolder() : (ViewHolder) convertView.getTag();
        if (convertView == null) {
            // if it's not recycled, inflate it from xml
            convertView = activity.getLayoutInflater().inflate(R.layout.photo_layout, viewGroup, false);
            // find the image and progress bar
            vh.image = convertView.findViewById(R.id.photo);
            vh.progressBar = convertView.findViewById(R.id.progress_bar);
            // and set the tag to it
            convertView.setTag(vh);
        }
        // set it's position
        vh.position = i;
        // and erase the image so we don't see old photos, also start the progress bar
        vh.image.setImageBitmap(null);
        vh.image.setVisibility(View.INVISIBLE);
        vh.progressBar.setVisibility(View.VISIBLE);
        vh.image.setRotation(0);

        //Try to load the image from the cache
        Bitmap cachedImage = memoryCache.get(imageLocations[i]);
        if(cachedImage != null) {
            setImage(vh.image, vh.progressBar, cachedImage, imageOrientations[i]);
            return convertView;
        }

        // load the image from file
        final String[] imageLocations = this.imageLocations;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                // get the string for the url
                if(vh.position != i) {
                    return;
                }
                // decode the jpeg into a bitmap
                final Bitmap bmp = getThumbnail(imageLocations[i], thumbnailSize);

                // set the bitmap (might be null)
                // must be run on the ui thread as it alters a view and uses a non-thread safe cache
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(imageLocations == PhotoAdapter.this.imageLocations) {
                            //Cache the image
                            if (memoryCache.get(imageLocations[i]) == null) {
                                memoryCache.put(imageLocations[i], bmp);
                            }
                            if (vh.position == i) {
                                setImage(vh.image, vh.progressBar, bmp, imageOrientations[i]);
                            }
                        }
                    }
                });
            }
        });
        return convertView;
    }

    /**
     * Adds an image to a ImageView and stops it's progress bar.
     * @param imageView The image view to display the image on
     * @param progressBar The progress bar to hide
     * @param bitmap The image to display
     * @param orientation The orientation of the image
     */
    private static void setImage(ImageView imageView, ProgressBar progressBar, Bitmap bitmap, int orientation) {
        imageView.setImageBitmap(bitmap);
        imageView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        imageView.setRotation(orientation);
    }

    /**
     * Loads and returns a scales down version of an image while trying not to use large amounts of memory.
     * @param filename The file name of an image file
     * @param desiredSize The pixel width and height of the returned image
     * @return An image of desiredSize size
     */
    private static Bitmap getThumbnail(String filename, int desiredSize) {
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true; // obtain the size of the image, without loading it in memory
        BitmapFactory.decodeFile(filename, bitmapOptions);
        // find the best scaling factor for the desired dimensions
        float scale = (float)Math.min(bitmapOptions.outWidth, bitmapOptions.outHeight) / (float)desiredSize;
        int sampleSize = (int)Math.ceil(scale);
        //round down to power of 2
        sampleSize |= (sampleSize >> 1);
        sampleSize |= (sampleSize >> 2);
        sampleSize |= (sampleSize >> 4);
        sampleSize |= (sampleSize >> 8);
        sampleSize |= (sampleSize >> 16);
        sampleSize -= (sampleSize >> 1);

        bitmapOptions.inSampleSize = sampleSize; // this value must be a power of 2,
        bitmapOptions.inJustDecodeBounds = false; // now we want to load the image
        Bitmap thumbnail = BitmapFactory.decodeFile(filename, bitmapOptions);
        return ThumbnailUtils.extractThumbnail(thumbnail, desiredSize, desiredSize);
    }

}
