
package com.ray.callereffect;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.ray.callereffect.utils.BitmapUtil;

/***
 *  Author : yurui@palmax.cn
 *  Create at 2018/6/27 18:39
 *  description : 一个接,挂电话的自定义view
 */
public class SlideCallerView extends View {

    //滑动操作完成的临界点
    private final static int COMPLETE_LIMIT = 50;

    //滑动静止状态
    private final static int MOVE_STATE_IDEL = 0;
    //手指抬起状态
    private final static int MOVE_STATE_ACTION_UP = 1;
    //滑动操作完成状态
    private final static int MOVE_STATE_COMPLETE = 2;

    private final static int DEFAULT_BG_COLOR = Color.parseColor("#B3F0F0F0");

    private int mMoveState;
    private int mColorTheme;
    private Paint mPaint;
    private int mMaxLeft;
    private int mMinLeft;

    //背景区域
    private Rect mBackRect;
    private RectF mBackRectF;

    private Rect mLeftImageRect;
    private int mLeftOfLeftImage;
    private int mLeftBeginOfLeftImageRect;
    private Bitmap mLeftBitmap;

    private Rect mRightImageRect;
    private int mLeftOfRightImage;
    private int mLeftBeginOfRightImageRect;
    private Bitmap mRightBitmap;

    private int mEventStartX;

    private Bitmap mLeftArrowBitmap;
    private int mLeftOfLeftArrow;
    private Bitmap mRightArrowBitmap;
    private int mLeftOfRightArrow;

    private ValueAnimator mArrowAnim;
    private int mMoveMode = 0;

    //是否可以点击
    private boolean mTouchable = true;

    private SlideListener mSlideListener;

    public interface SlideListener {
        void onSlideToAnswer();

        void onSlideToHangup();
    }

    public SlideCallerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mSlideListener = null;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SlideCallerView);
        mTouchable = typedArray.getBoolean(R.styleable.SlideCallerView_touchable, true);
        mColorTheme = typedArray.getColor(R.styleable.SlideCallerView_bgColor, DEFAULT_BG_COLOR);
        typedArray.recycle();
    }

    public SlideCallerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlideCallerView(Context context) {
        this(context, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureDimension(280, widthMeasureSpec);
        int height = measureDimension(140, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        initDrawingVal();
    }

    public void setTouchable(boolean touchable) {
        mTouchable = touchable;
    }

    public void initDrawingVal() {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        mBackRectF = new RectF();
        mLeftImageRect = new Rect();
        mBackRect = new Rect(0, 0, width, height);
        mMinLeft = 0;
        mMaxLeft = width - height;
        mLeftOfLeftImage = 0;
        mLeftBeginOfLeftImageRect = mLeftOfLeftImage;

        initDrawingBitmaps(height);
    }

    private void initDrawingBitmaps(final int diameter) {
        mLeftBitmap = BitmapUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.greencall, diameter, diameter);
        mRightBitmap = BitmapUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.redcall, diameter, diameter);
        mLeftArrowBitmap = BitmapUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.arrow_left, ((int) (diameter / 2f + 0.5f)), ((int) (diameter / 2f + 0.5f)));
        mRightArrowBitmap = BitmapUtil.decodeSampledBitmapFromResource(getResources(), R.drawable.arrow_right, diameter, diameter);
        mRightImageRect = new Rect(mMaxLeft, 0, mMaxLeft + diameter, getHeight());
        mLeftOfRightImage = mMaxLeft;
        mLeftBeginOfRightImageRect = mMaxLeft;
        mLeftOfLeftArrow = mMinLeft + mMinLeft + diameter + 30;
        mLeftOfRightArrow = mMaxLeft - mRightArrowBitmap.getWidth() - 30;
        animateArrow(diameter);
    }

    private void animateArrow(final int diameter) {
        final int startX = 0;
        final int endX = 30;
        final int initLeftOfLeftArrow = mLeftOfLeftArrow;
        final int initLeftOfRightArrow = mLeftOfRightArrow;
        mArrowAnim = ValueAnimator.ofInt(startX,
                endX);
        mArrowAnim.addUpdateListener(new AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLeftOfLeftArrow = initLeftOfLeftArrow + (Integer) animation.getAnimatedValue();
                mLeftOfRightArrow = initLeftOfRightArrow - (int) animation.getAnimatedValue();
                invalidateView(mMinLeft + diameter + 30, 0, mMaxLeft - 30, getHeight());
            }
        });
        mArrowAnim.setDuration(400);
        mArrowAnim.setRepeatMode(ValueAnimator.REVERSE);
        mArrowAnim.setRepeatCount(ValueAnimator.INFINITE);
        mArrowAnim.start();
    }

    public int measureDimension(int defaultSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = defaultSize; // UNSPECIFIED
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int radius;
        radius = mBackRect.height() / 2;
        mBackRectF.set(mBackRect);
        mPaint.setColor(mColorTheme);
        canvas.drawRoundRect(mBackRectF, radius, radius, mPaint);
        if (mLeftBitmap != null && (mMoveMode == 1 || mMoveMode == 0)) {
            mLeftImageRect.set(mLeftOfLeftImage, 0, mLeftOfLeftImage
                    + mBackRect.height(), mBackRect.height());
            canvas.drawBitmap(mLeftBitmap, null, mLeftImageRect, null);
        }
        if (mRightBitmap != null && (mMoveMode == 2 || mMoveMode == 0)) {
            mRightImageRect.set(mLeftOfRightImage, 0, mLeftOfRightImage
                    + mBackRect.height(), mBackRect.height());
            canvas.drawBitmap(mRightBitmap, null, mRightImageRect, null);
        }

        if (mLeftArrowBitmap != null && mMoveMode == 0 && mMoveState == MOVE_STATE_IDEL) {
            canvas.drawBitmap(mLeftArrowBitmap, mLeftOfRightArrow, radius - mLeftArrowBitmap.getHeight() / 2, null);
        }

        if (mRightArrowBitmap != null && mMoveMode == 0 && mMoveState == MOVE_STATE_IDEL) {
            canvas.drawBitmap(mRightArrowBitmap, mLeftOfLeftArrow, radius - mRightArrowBitmap.getHeight() / 2, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mTouchable || mMoveState == MOVE_STATE_COMPLETE)
            return super.onTouchEvent(event);
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mMoveMode = 0;
                if (mLeftImageRect.contains((int) event.getX(), (int) event.getY())) {
                    mMoveMode = 1;
                } else if (mRightImageRect.contains((int) event.getX(), (int) event.getY())) {
                    mMoveMode = 2;
                }
                mEventStartX = (int) event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMoveMode == 0) {
                    break;
                }
                int eventLastX = (int) event.getX();
                int diffX = eventLastX - mEventStartX;
                int tempX;
                if (mMoveMode == 1) {
                    tempX = diffX + mLeftBeginOfLeftImageRect;
                } else {
                    tempX = diffX + mLeftBeginOfRightImageRect;
                }
                if (tempX >= mMinLeft && tempX <= mMaxLeft) {
                    if (mMoveMode == 1) {
                        mLeftOfLeftImage = tempX;
                        if (Math.abs(mLeftOfLeftImage - mMaxLeft) < COMPLETE_LIMIT) {
                            if (mSlideListener != null) {
                                mSlideListener.onSlideToAnswer();
                            }
                            mMoveState = MOVE_STATE_ACTION_UP;
                            break;
                        }
                    } else {
                        mLeftOfRightImage = tempX;
                        if (Math.abs(mLeftOfRightImage - mMinLeft) < COMPLETE_LIMIT) {
                            if (mSlideListener != null) {
                                mSlideListener.onSlideToHangup();
                            }
                            mMoveState = MOVE_STATE_ACTION_UP;
                            break;
                        }
                    }
                    invalidateView();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mMoveMode == 0)
                    break;
                mLeftBeginOfLeftImageRect = mLeftOfLeftImage;
                mLeftBeginOfRightImageRect = mLeftOfRightImage;
                moveToDest();
                break;
            default:
                break;
        }
        return true;
    }

    private void invalidateView() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    /**
     * 重绘箭头区域，局部重绘节省内存
     *
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    private void invalidateView(int left, int top, int right, int bottom) {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            invalidate(left, top, right, bottom);
        } else {
            postInvalidate(left, top, right, bottom);
        }
    }

    public void setSlideListener(SlideListener listener) {
        this.mSlideListener = listener;
    }

    private void moveToDest() {

        final boolean toRight;
        final int moveMode = mMoveMode;
        if (moveMode == 1) {
            toRight = (mLeftBeginOfLeftImageRect > mMaxLeft / 2);
        } else {
            toRight = (mLeftBeginOfRightImageRect > mMaxLeft / 2);
        }

        ValueAnimator toDestAnim = ValueAnimator.ofInt(moveMode == 1 ? mLeftBeginOfLeftImageRect : mLeftBeginOfRightImageRect,
                toRight ? mMaxLeft : mMinLeft);
        toDestAnim.setDuration(500);
        toDestAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        toDestAnim.start();
        toDestAnim.addUpdateListener(new AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (moveMode == 1) {
                    mLeftOfLeftImage = (Integer) animation.getAnimatedValue();
                } else {
                    mLeftOfRightImage = (Integer) animation.getAnimatedValue();
                }
                invalidateView();
            }
        });
        toDestAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (toRight) {

                    if (moveMode == 1) {
                        mLeftBeginOfLeftImageRect = mMaxLeft;
                        if (mSlideListener != null && mMoveState != MOVE_STATE_ACTION_UP) {
                            mSlideListener.onSlideToAnswer();
                        }
                        mMoveState = MOVE_STATE_COMPLETE;
                    } else {
                        mLeftBeginOfRightImageRect = mMaxLeft;
                    }

                } else {

                    if (moveMode == 1) {
                        mLeftBeginOfLeftImageRect = mMinLeft;
                    } else {
                        mLeftBeginOfRightImageRect = mMinLeft;
                        if (mSlideListener != null && mMoveState != MOVE_STATE_ACTION_UP) {
                            mSlideListener.onSlideToHangup();
                        }
                        mMoveState = MOVE_STATE_COMPLETE;
                    }

                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mArrowAnim != null && mArrowAnim.isStarted()) {
            mArrowAnim.cancel();
            mArrowAnim = null;
        }
    }
}
