package com.step.smart.palette.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.step.smart.palette.Constant.LineType;
import com.step.smart.palette.entity.PaletteData;
import com.step.smart.palette.entity.PathEntity;
import com.step.smart.palette.utils.Preferences;

import java.util.List;

/**
 * Created by weflow on 2018/3/21.
 */

public class StrokeDrawView extends View implements PaletteSurfaceView.SyncDrawInterface {

    private PaletteView.PaletteInterface mPaletteInterface;
    private Paint mPaint;
    private Paint mEraserPaint;
    private PaletteData mPaletteData = new PaletteData();


    public StrokeDrawView(Context context) {
        this(context, null);
    }

    public StrokeDrawView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StrokeDrawView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _init(context);
    }

    private void _init(Context context) {
        if (context instanceof PaletteView.PaletteInterface) {
            mPaletteInterface = (PaletteView.PaletteInterface) context;
        }
        //画笔
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        mPaint.setStrokeWidth(5f);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setMaskFilter(new BlurMaskFilter(0.8F, BlurMaskFilter.Blur.SOLID));

        mEraserPaint = new Paint(mPaint);
        mEraserPaint.setStrokeCap(Paint.Cap.ROUND);//线冒
        mEraserPaint.setStrokeWidth(140f);
        mEraserPaint.setColor(Color.WHITE);
        mEraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));//关键代码
    }

    @Override
    protected void onDraw(Canvas canvas) {
        /*for (int i = 0; i < mPaletteData.pathList.size(); i++) {
            PathEntity p = mPaletteData.pathList.get(i);
            if (p.type == LineType.DRAW || p.type == LineType.LINE || p.type == LineType.ERASER) {
                canvas.drawPath(p.path, p.paint);
            } else if(p.type == LineType.CIRCLE) {
                canvas.drawOval(p.rect, p.paint);
            } else if(p.type == LineType.RECTANGLE) {
                canvas.drawRect(p.rect, p.paint);
            }
        }*/
        if (mBufferBitmap != null) {
            canvas.drawBitmap(mBufferBitmap, new Rect(0, 0, mBufferBitmap.getWidth(), mBufferBitmap.getHeight()), new Rect(0, 0, mBufferBitmap.getWidth(), mBufferBitmap.getHeight()), null);
        }
    }

    private Bitmap mBufferBitmap;
    private Canvas mBufferCanvas;

    public void initBuffer() {
        if (mBufferBitmap == null) {
            int screen_width = Preferences.getInt("screen_width", getWidth());
            int screen_height = Preferences.getInt("screen_height", getHeight());
            mBufferBitmap = Bitmap.createBitmap(screen_width, screen_height, Bitmap.Config.ARGB_8888);
            mBufferCanvas = new Canvas(mBufferBitmap);
        }
    }

    private void flush() {
        int size = mPaletteData.pathList.size();
        if (size < 1) {
            return;
        }
        PathEntity p = mPaletteData.pathList.get(size - 1);
        if (p.type == LineType.DRAW || p.type == LineType.LINE || p.type == LineType.ERASER) {
            mBufferCanvas.drawPath(p.path, p.paint);
        } else if (p.type == LineType.CIRCLE) {
            mBufferCanvas.drawOval(p.rect, p.paint);
        } else if (p.type == LineType.RECTANGLE) {
            mBufferCanvas.drawRect(p.rect, p.paint);
        }
        invalidate();
    }

    private void reFlush() {
        mBufferBitmap.eraseColor(Color.TRANSPARENT);
        for (int i = 0; i < mPaletteData.pathList.size(); i++) {
            PathEntity p = mPaletteData.pathList.get(i);
            if (p.type == LineType.DRAW || p.type == LineType.LINE || p.type == LineType.ERASER) {
                mBufferCanvas.drawPath(p.path, p.paint);
            } else if (p.type == LineType.CIRCLE) {
                mBufferCanvas.drawOval(p.rect, p.paint);
            } else if (p.type == LineType.RECTANGLE) {
                mBufferCanvas.drawRect(p.rect, p.paint);
            }
        }
        invalidate();
    }

    @Override
    public void syncStroke(PathEntity entity) {
        if (mBufferBitmap == null) {
            initBuffer();
        }
        if (entity == null) {
            return;
        }
        mPaletteData.pathList.add(entity);
        //invalidate();
        flush();
    }

    @Override
    public void syncEraserPoint(MotionEvent event, PathEntity entity) {
        if (mBufferBitmap == null) {
            initBuffer();
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mPaletteData.pathList.add(entity);
        }
        flush();
        //invalidate();
    }

    public void clear() {
        mPaletteData.pathList.clear();
        mPaletteData.undoList.clear();
        reFlush();
    }

    /**
     * undo
     */
    public void undo() {
        int size = mPaletteData.pathList.size();
        if (size > 0) {
            PathEntity entity = mPaletteData.pathList.remove(size - 1);
            mPaletteData.undoList.add(entity);
            reFlush();
        }
    }

    /**
     * redo
     */
    public void redo() {
        int size = mPaletteData.undoList.size();
        if (size > 0) {
            PathEntity entity = mPaletteData.undoList.remove(size - 1);
            mPaletteData.pathList.add(entity);
            reFlush();
        }
    }
}