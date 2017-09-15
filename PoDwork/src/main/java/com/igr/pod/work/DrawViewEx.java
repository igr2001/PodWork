package com.igr.pod.work;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by igr on 12-Apr-17.
 */

public class DrawViewEx extends View {
    public static final float TOUCH_STROKE_WIDTH = 4;
    public static final int BORDER_WIDTH = 1;

    private Path mPath;
    private Paint mPaint;
    private Paint mPaintFinal;
    private Bitmap mBitmap = null;
    private Canvas mCanvas = null;

    private int mColor = Color.BLACK;
    private int mColorFinal = Color.BLACK;
    private int mColorBg = Color.WHITE;

    private boolean mIsDrawing = false;
    private boolean mIsSigned = false;

    public DrawViewEx(Context context) {
        super(context);
        InitView(context);
    }

    public DrawViewEx(Context context, AttributeSet attrs) {
        super(context, attrs);
        InitView(context);
    }

    public DrawViewEx(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        InitView(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if ( mBitmap==null ) {
            mBitmap = Bitmap.createBitmap(w-BORDER_WIDTH-BORDER_WIDTH, h-BORDER_WIDTH-BORDER_WIDTH, Bitmap.Config.ARGB_8888);
            mCanvas = new Canvas(mBitmap);
            mCanvas.drawColor(mColorBg);
        } else
            try {
                mCanvas = new Canvas(mBitmap);
                invalidate();
            } catch(IllegalStateException e) {
                mBitmap = Bitmap.createBitmap(w-BORDER_WIDTH-BORDER_WIDTH, h-BORDER_WIDTH-BORDER_WIDTH, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBitmap);
                mCanvas.drawColor(mColorBg);
            }
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(mBitmap, BORDER_WIDTH, BORDER_WIDTH, mPaint);
        if (mIsDrawing)
            mCanvas.drawPath(mPath, mPaintFinal);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if ( MainActivity.mSignViewOnly )
            return false;
        float mx = event.getX();
        float my = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsDrawing = true;
                mPath.moveTo(mx, my);
                break;
            case MotionEvent.ACTION_MOVE:
                mPath.lineTo(mx, my);
                break;
            case MotionEvent.ACTION_UP:
                mIsDrawing = false;
                mCanvas.drawPath(mPath, mPaintFinal);
                mPath.reset();
                mIsSigned = true;
                break;
            default:
                return false;
        }
        invalidate();
        return true;
    }
    // Public functions
    public boolean LoadImage(String sFileName) {
        File file = new File(sFileName);
        if ( !file.exists() )
            return false;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
//        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        mBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        mIsSigned = true;
        return true;
    }
    public boolean SaveImage(String sFileName) {
        File mFile = new File(sFileName);
        try {
            mFile.createNewFile();
            mFile.setReadable(true, false);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mFile);
                mBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            } finally {
                if (fos != null) fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    public boolean isEmpty() {
        return !mIsSigned;
    }
    public void Clear() {
        mCanvas.drawColor(mColorBg);
        invalidate();
        mIsSigned = false;
    }
    // Private functions
    private void InitView(Context context) {
        mPath = new Path();

        mPaint = new Paint(Paint.DITHER_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(TOUCH_STROKE_WIDTH);

        mPaintFinal = new Paint(Paint.DITHER_FLAG);
        mPaintFinal.setAntiAlias(true);
        mPaintFinal.setDither(true);
        mPaintFinal.setColor(mColorFinal);
        mPaintFinal.setStyle(Paint.Style.STROKE);
        mPaintFinal.setStrokeJoin(Paint.Join.ROUND);
        mPaintFinal.setStrokeCap(Paint.Cap.ROUND);
        mPaintFinal.setStrokeWidth(TOUCH_STROKE_WIDTH);
    }
}