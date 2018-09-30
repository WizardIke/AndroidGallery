package com.wizardike.gallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.GridView;

/**
 * A GridView that can handle pinch to zoom
 */
public class PhotoGridView extends GridView {
    private ScaleGestureDetector mScaleDetector;
    private double mScaleFactor;

    public PhotoGridView(Context context) {
        super(context);
        init(context, 4);
    }

    public PhotoGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        int numColumns = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "numColumns", 4);
        init(context, numColumns);
    }

    public PhotoGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int numColumns = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "numColumns", 4);
        init(context, numColumns);
    }

    @TargetApi(21)
    public PhotoGridView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        int numColumns = attrs.getAttributeIntValue("http://schemas.android.com/apk/res/android", "numColumns", 4);
        init(context, numColumns);
    }

    private void init(Context context, int numColumns) {
        mScaleFactor = (double)numColumns;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        mScaleDetector.onTouchEvent(ev);
        return mScaleDetector.isInProgress() || super.onTouchEvent(ev);
    }

    /**
     * Handles scale events
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            double amount = detector.getScaleFactor();
            mScaleFactor /= amount;
            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(1.0, Math.min(mScaleFactor, 8.0f));

            int newNumColumns = (int)mScaleFactor;
            if(newNumColumns != getNumColumns()) {
                int position = getFirstVisiblePosition();
                setNumColumns(newNumColumns);
                setSelection(position);
            }
            invalidate();
            return true;
        }
    }

    public void setScale(double scale) {
        mScaleFactor = scale;
        super.setNumColumns((int)scale);
        invalidate();
    }

    public double getScale() {
        return mScaleFactor;
    }
}
