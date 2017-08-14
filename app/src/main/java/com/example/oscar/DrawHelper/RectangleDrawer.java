package com.example.oscar.DrawHelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by oscar on 11-08-17.
 */

public class RectangleDrawer extends View {
    public RectangleDrawer(Context context) {
        super(context);
        init();
    }

    public RectangleDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RectangleDrawer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    ArrayList<int[]> bboxes;
    int color;
    private Paint paint;
    private Rect rect;
    private boolean paramsSeted = false;
    private boolean clearSeted = false;


    private void init()
    {
        color = Color.RED;
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.parseColor("#aaB22222"));
        rect = new Rect();
    }


    //comentariossssssssssssssssssssssssssssssssssssssssssssssssss
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(clearSeted)
        {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            clearSeted = false;
            return;
        }

        if(!this.paramsSeted)
            return;

        canvas.getClipBounds(rect);

        for (int[] bb: this.bboxes)
        {
            rect.left = bb[0];
            rect.right = bb[2];
            rect.top = bb[1];
            rect.bottom = bb[3];
            canvas.drawRect(rect, paint);
        }
        paramsSeted = false;
    }

    public void clear()
    {
        clearSeted = true;
        invalidate();
    }

    public void setParameters(ArrayList<int[]> bboxes)
    {

        this.bboxes = bboxes;
        this.paramsSeted = true;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int givenHeight = MeasureSpec.getSize(heightMeasureSpec);
        int givenWidth = MeasureSpec.getSize(widthMeasureSpec);

        switch (heightMode)
        {
            case MeasureSpec.EXACTLY:
                setMeasuredDimension(givenWidth, givenHeight);
                return;

            case MeasureSpec.AT_MOST:
                int ourHeight = getKnownHeight();
                int height = Math.min(givenHeight, ourHeight);
                setMeasuredDimension(givenWidth, height);
                return;

            case MeasureSpec.UNSPECIFIED:
                setMeasuredDimension(givenWidth, getKnownHeight());
                return;
        }
    }

    private int getKnownHeight()
    {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getContext().getResources().getDisplayMetrics());
    }


    //limpiar pantalla cuando tocan
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.i("CAMERATEST: rectangledrawrew", "se toco la pantalla");
        clear();
        return super.onTouchEvent(event);

    }

}
