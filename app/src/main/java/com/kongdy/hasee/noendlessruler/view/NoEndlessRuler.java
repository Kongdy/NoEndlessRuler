package com.kongdy.hasee.noendlessruler.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import com.kongdy.hasee.noendlessruler.R;


/**
 * @author kongdy
 *         on 2016/8/10
 *         尺子控件
 */
public class NoEndlessRuler extends View {

    private final static String TAG = "NoEndlessRuler";

    // 滑动计算
    private Scroller scroller;

    private int mHeight;
    private int mWidth;
    private int pointerWidth;
    private int pointerHeight;
    private int rulerDistance;
    private int rulerSpace;

    private float rulerTextSize;

    private int maxValue = -1;
    private int minValue;
    private int currentValue = -1;
    private int scrollOffset; // 滑动偏移量

    private ORIENTATION mOrientation;

    /**
     * 刻度画笔
     **/
    private Paint rulerPaint;
    /**
     * 刻度数字
     **/
    private TextPaint rulerTextPaint;
    /**
     * 底部line
     **/
    private Paint labelPaint;
    /**
     * 指针
     **/
    private Bitmap pointer;

    private float lastMotionX;
    private float lastMotionY;

    private int scrollX;
    private int scrollY;

    /**
     * 滑动惯性持续时间
     */
    private static final int SCROLL_DURATION = 300;

    private GestureDetector gestureDetector;

    private final static int MESSAGE_SCROLL = 1;
    private final static int MESSAGE_JUSTIFY = 2;
    /**
     * 最小滑动阀值
     **/
    private final static int MIN_SCROLL_VALUE = 1;

    /**
     * 指针资源id
     **/
    private int pointId;

    public NoEndlessRuler(Context context) {
        super(context);
        init(null);
    }

    public NoEndlessRuler(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public NoEndlessRuler(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public NoEndlessRuler(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }


    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.NoEndlessRuler);
        int orientation = a.getInteger(R.styleable.NoEndlessRuler_ner_orientation, 2);
        pointId = a.getResourceId(R.styleable.NoEndlessRuler_ner_point_style, -1);
        a.recycle();

        if (orientation == 1) {
            mOrientation = ORIENTATION.VERTICAL;
        } else {
            mOrientation = ORIENTATION.HORIZONTAL;
        }

        rulerPaint = new Paint();
        labelPaint = new Paint();
        rulerTextPaint = new TextPaint();

        rulerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        labelPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        rulerTextPaint.setTextAlign(Paint.Align.CENTER);

        rulerPaint.setStrokeWidth(getRawSize(TypedValue.COMPLEX_UNIT_DIP, 1));
        labelPaint.setStrokeWidth(getRawSize(TypedValue.COMPLEX_UNIT_DIP, 2));
        rulerTextPaint.setTextSize(getRawSize(TypedValue.COMPLEX_UNIT_SP, 8));

        rulerPaint.setColor(Color.BLACK);
        labelPaint.setColor(Color.BLACK);
        rulerTextPaint.setColor(Color.BLACK);

        paintInit(rulerPaint);
        paintInit(labelPaint);
        paintInit(rulerTextPaint);

        scroller = new Scroller(getContext());
        scroller.setFriction(0.05f); // 摩擦力

        gestureDetector = new GestureDetector(getContext(), onGestureListener);
    }

    private void paintInit(Paint paint) {
        paint.setAntiAlias(true); // 锯齿
        paint.setFilterBitmap(true); // 滤波
        paint.setDither(true); // 防抖
        paint.setSubpixelText(true); // 像素自处理
    }

    private GestureDetector.SimpleOnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

            int mVelocityX = mOrientation == ORIENTATION.VERTICAL ? 0 : (int) -velocityX;
            int mVelocityY = mOrientation == ORIENTATION.HORIZONTAL ? 0 : (int) -velocityY;

            scroller.fling(0, 0, mVelocityX, mVelocityY, -0x7FFFFFFF, 0x7FFFFFFF, 0, 0);

            return true;
        }
    };

    /**
     * 滑动处理
     */
    private Handler slideHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            scroller.computeScrollOffset();
            int mScrollX = scroller.getCurrX();
            int mScrollY = scroller.getCurrY();
            int deltaX = mScrollX - scrollX;
            int deltaY = mScrollY - scrollY;
            scrollX = mScrollX;
            scrollY = mScrollY;

            doScroll(deltaX, deltaY);

            scrollOffset = deltaX > deltaY ? deltaX : deltaY;

            if (Math.abs(mScrollX - scroller.getFinalX()) < MIN_SCROLL_VALUE &&
                    Math.abs(mScrollY - scroller.getFinalY()) < MIN_SCROLL_VALUE) {
                lastMotionX = scroller.getFinalX();
                lastMotionY = scroller.getFinalY();
                scroller.forceFinished(true);
            }

            if (!scroller.isFinished()) {
                setNextMessage(MESSAGE_SCROLL);
            } else if (msg.what == MESSAGE_SCROLL) {
                setNextMessage(MESSAGE_JUSTIFY);
                justify();
            } else {
                finished();
            }
            return true;
        }
    });

    private void justify() {
        if (needScroll()) {
            return;
        }
        if (Math.abs(scrollOffset) > MIN_SCROLL_VALUE) {
            int scrollX;
            int scrollY;
            if (scrollOffset < -rulerSpace / 2) {
                scrollX = mOrientation == ORIENTATION.VERTICAL ? 0 : scrollOffset + rulerSpace;
                scrollY = mOrientation == ORIENTATION.HORIZONTAL ? 0 : scrollOffset + rulerSpace;
            } else if (scrollOffset > rulerSpace / 2) {
                scrollX = mOrientation == ORIENTATION.VERTICAL ? 0 : scrollOffset - rulerSpace;
                scrollY = mOrientation == ORIENTATION.HORIZONTAL ? 0 : scrollOffset - rulerSpace;
            } else {
                scrollX = mOrientation == ORIENTATION.VERTICAL ? 0 : scrollOffset;
                scrollY = mOrientation == ORIENTATION.HORIZONTAL ? 0 : scrollOffset;
            }
            scrollXY(scrollX, scrollY, 0);
        }
    }


    private void finished() {
        if (needScroll()) {
            return;
        }
        scrollOffset = 0;
        invalidate();
    }


    /**
     * 开始滑动
     *
     * @param deltaX
     * @param deltaY
     */
    private void doScroll(int deltaX, int deltaY) {
        if (deltaX == 0 && deltaY == 0) {
            return;
        }
        if (mOrientation == ORIENTATION.VERTICAL) {
            scrollOffset += deltaY;
        } else {
            scrollOffset += deltaX;
        }

        invalidate();
    }

    private void setNextMessage(int MESSAGE) {
        clearMessage();
        slideHandler.sendEmptyMessage(MESSAGE);
    }

    private void clearMessage() {
        slideHandler.removeMessages(MESSAGE_SCROLL);
        slideHandler.removeMessages(MESSAGE_JUSTIFY);
    }


    private void scrollX(int distance, int duration) {
        scroller.forceFinished(true);
        scroller.startScroll(0, 0, distance, 0, duration);
        setNextMessage(MESSAGE_SCROLL);
    }

    private void scrollY(int distance, int duration) {
        scroller.forceFinished(true);
        scroller.startScroll(0, 0, 0, distance, duration);
        setNextMessage(MESSAGE_SCROLL);
    }

    private void scrollXY(int distanceX, int distanceY, int duration) {
        scroller.forceFinished(true);
        scroller.startScroll(0, 0, distanceX, distanceY, duration);
        setNextMessage(MESSAGE_SCROLL);
    }

    /**
     * 是否可以继续滑动
     *
     * @return
     */
    private boolean needScroll() {
        int outRange = 0;
        if (currentValue < minValue) {
            outRange = (minValue - currentValue) * rulerSpace;
        } else if (currentValue > maxValue) {
            outRange = (currentValue - minValue) * rulerSpace;
        }
        if (0 != outRange) {
            scrollOffset = 0;
            if (mOrientation == ORIENTATION.VERTICAL) {
                scrollY(-outRange, 100);
            } else {
                scrollX(-outRange, 100);
            }
            return false;
        }

        return true;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w - getPaddingLeft() - getPaddingRight();
        mHeight = h - getPaddingBottom() - getPaddingTop();
        initProperty();
    }

    private void initProperty() {

        float defaultMinDistance = getRawSize(TypedValue.COMPLEX_UNIT_DIP, 2);
        float defaultMaxDistance = mOrientation == ORIENTATION.VERTICAL ? 2 * mHeight / 3 : 2 * mWidth / 3;

        pointerWidth = (int) (mOrientation == ORIENTATION.VERTICAL ? defaultMaxDistance : defaultMinDistance);
        pointerHeight = (int) (mOrientation == ORIENTATION.HORIZONTAL ? defaultMaxDistance : defaultMinDistance);

        // 设置pointer样式
        if (pointId == -1) {
            Paint tempPaint = new Paint();
            tempPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            tempPaint.setColor(Color.rgb(250, 124, 0)); // 默认橘黄色
            paintInit(tempPaint);
            Bitmap result = Bitmap.createBitmap(pointerWidth, pointerHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result);
            canvas.drawRect(0, 0, pointerWidth, pointerHeight, tempPaint);
            pointer = result;
        } else {
            pointer = BitmapFactory.decodeResource(getResources(), pointId);

            // TODO: 2016/8/10 do some size fit
//                     int outWidth = pointer.getWidth();
//            int outHeight = pointer.getHeight();
//
//            float scale;
//            float offsetX = 0;
//            float offsetY = 0;
//
//            if(outWidth/pointerWidth > outHeight/pointerHeight) {
//                scale = outWidth/pointerWidth;
//                offsetX = (pointerWidth-outWidth*scale)*0.5f;
//            } else {
//                scale = outHeight/pointerHeight;
//                offsetY = (pointerHeight-outHeight*scale)*0.5f;
//            }
//
//            Matrix pointerMatrix = new Matrix();
//            pointerMatrix.set(null);
//
//            pointerMatrix.postScale(scale,scale);
//            pointerMatrix.postTranslate(offsetX,offsetY);

            //pointer.
        }

        // 计算尺寸
        if (maxValue == -1) {
            maxValue = 200;
        }
        // 分割线间隔默认为4dp
        rulerSpace = (int) getRawSize(TypedValue.COMPLEX_UNIT_DIP, 4);
        //rulerSpace = (mOrientation == ORIENTATION.VERTICAL?mHeight:mWidth)/(maxValue/2);
        rulerDistance = (mOrientation == ORIENTATION.VERTICAL ? mWidth : mHeight) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.saveLayer(0, 0, getMeasuredWidth(), getMeasuredHeight(), rulerPaint, Canvas.ALL_SAVE_FLAG);

        if (mOrientation == ORIENTATION.VERTICAL) {
            drawVertical(canvas);
        } else {
            drawHorizontal(canvas);
        }
        drawPointer(canvas);
        canvas.restore();
    }

    /**
     * 绘制指针
     *
     * @param canvas
     */
    private void drawPointer(Canvas canvas) {

    }

    /**
     * 画水平尺子
     */
    private void drawHorizontal(Canvas canvas) {
        canvas.drawLine(0, mHeight, mWidth, mHeight, labelPaint);
        int tempValue = minValue;
        while (tempValue < maxValue) {
            int startX = scrollOffset+tempValue * rulerSpace;
            if (tempValue % 5 == 0 || tempValue == 0) {
                canvas.drawLine(startX, mHeight - labelPaint.getStrokeWidth() / 2, startX,
                        mHeight - (rulerDistance * 4) / 3 - labelPaint.getStrokeWidth() / 2, rulerPaint);
                canvas.drawText(String.valueOf(tempValue), startX, rulerTextPaint.getFontSpacing(),
                        rulerTextPaint);
            } else {
                canvas.drawLine(startX, mHeight - labelPaint.getStrokeWidth() / 2, startX,
                        mHeight - rulerDistance - labelPaint.getStrokeWidth() / 2, rulerPaint);
            }
            tempValue += 1;
        }
    }

    /**
     * 画垂直尺子
     */
    private void drawVertical(Canvas canvas) {
        canvas.drawLine(0, 0, 0, mHeight, labelPaint);
        int tempValue = minValue;
        while (tempValue < maxValue) {
            int startY = scrollOffset+tempValue * rulerSpace;
            if (tempValue % 5 == 0 || tempValue == 0) {
                canvas.drawLine(labelPaint.getStrokeWidth() / 2, startY, labelPaint.getStrokeWidth() / 2 +
                        (rulerDistance * 4) / 3, startY, rulerPaint);
                canvas.drawText(String.valueOf(tempValue), mWidth, startY, rulerTextPaint);
            } else {
                canvas.drawLine(labelPaint.getStrokeWidth() / 2, startY, labelPaint.getStrokeWidth() / 2 +
                        rulerDistance, startY, rulerPaint);
            }
            tempValue += 1;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastMotionX = event.getX();
                lastMotionY = event.getY();
                clearMessage();
                break;
            case MotionEvent.ACTION_MOVE:
                if(getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                int delayY = 0;
                int delayX = 0;
                if (mOrientation == ORIENTATION.VERTICAL) {
                    final float motionY = event.getY();
                    delayY = (int) (lastMotionY - motionY);
                    lastMotionY = motionY;
                } else {
                    final float motionX = event.getX();
                    delayX = (int) (lastMotionX - motionX);
                    lastMotionX = motionX;
                }
                doScroll(delayX,delayY);
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if(getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }
        if(!gestureDetector.onTouchEvent(event) && event.getAction() == MotionEvent.ACTION_UP){
            justify();
        }
        return true;
    }


    public float getRawSize(int unit, float value) {
        DisplayMetrics metrics = getContext().getResources()
                .getDisplayMetrics();
        return TypedValue.applyDimension(unit, value, metrics);
    }

    public Paint getRulerPaint() {
        return rulerPaint;
    }

    public TextPaint getRulerTextPaint() {
        return rulerTextPaint;
    }

    public Paint getLabelPaint() {
        return labelPaint;
    }

    /**
     * 方向枚举
     *
     * @author wangk
     */
    public static enum ORIENTATION {
        VERTICAL, HORIZONTAL
    }

}
