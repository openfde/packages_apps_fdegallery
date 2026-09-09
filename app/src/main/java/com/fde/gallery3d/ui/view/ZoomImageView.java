package com.fde.gallery3d.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomImageView extends androidx.appcompat.widget.AppCompatImageView {
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;

    public ZoomImageView(Context context) {
        super(context);
        init(context);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
                scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
                setScaleX(scaleFactor);
                setScaleY(scaleFactor);
                return true;
            }
        });
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (event.getButtonState() == MotionEvent.BUTTON_SECONDARY && event.getAction() == MotionEvent.ACTION_SCROLL) {
            float vScroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
            if (vScroll < 0) {
                scaleFactor *= 1.1f;
            } else {
                scaleFactor *= 0.9f;
            }
            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));
            setScaleX(scaleFactor);
            setScaleY(scaleFactor);
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

}
