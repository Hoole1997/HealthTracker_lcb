package com.healthtracker.blood.suger.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.healthtracker.blood.suger.R;


public class CircularProgressView extends View {

    private Paint mBackgroundPaint; // 背景画笔
    private Paint mProgressPaint; // 进度画笔
    private RectF mArcBounds; // 圆弧边界
    private int[] mProgressColors; // 进度颜色数组
    private int mCurrentProgress; // 当前进度
    private int mTargetProgress; // 目标进度

    // 动画更新监听器
    public class ProgressAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private ValueAnimator mAnimator;

        public ProgressAnimatorUpdateListener(ValueAnimator animator) {
            mAnimator = animator;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animator) {
            CircularProgressView.this.mCurrentProgress = ((Integer) animator.getAnimatedValue()).intValue();
            if (CircularProgressView.this.getContext() instanceof Activity) {
                Activity activity = (Activity) CircularProgressView.this.getContext();
                if (activity.isFinishing() || activity.isDestroyed()) {
                    if (mAnimator != null) {
                        mAnimator.cancel();
                    }
                    CircularProgressView.this.mCurrentProgress = CircularProgressView.this.mTargetProgress;
                    return;
                }
            }
            CircularProgressView.this.invalidate();
        }
    }

    // 动画监听器
    public class ProgressAnimatorListener extends AnimatorListenerAdapter {

        private boolean mIsAutoPlay;

        public ProgressAnimatorListener(boolean isAutoPlay) {
            mIsAutoPlay = isAutoPlay;
        }

        @Override
        public void onAnimationEnd(Animator animator) {
            super.onAnimationEnd(animator);

        }
    }

    public CircularProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * 设置进度
     * @param progress 目标进度
     * @param duration 动画时长
     * @param autoPlay 是否自动播放动画
     * @param startProgress 动画起始进度
     * @param isAutoReset 是否自动重置进度
     */
    public void setProgress(int progress, long duration, boolean autoPlay, int startProgress, boolean isAutoReset) {
        // 根据实际情况进行处理
        // ...
    }

    /**
     * 获取当前进度
     * @return 当前进度值
     */
    public int getProgress() {
        return this.mCurrentProgress;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(mArcBounds, 0.0f, 360.0f, false, mBackgroundPaint);
        canvas.drawArc(mArcBounds, 275.0f, (mCurrentProgress * 360) / 100, false, mProgressPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredWidth = (getMeasuredWidth() - getPaddingLeft()) - getPaddingRight();
        int measuredHeight = (getMeasuredHeight() - getPaddingTop()) - getPaddingBottom();
        int strokeWidth = (int) ((Math.min(measuredWidth, measuredHeight)) - (mBackgroundPaint.getStrokeWidth() > mProgressPaint.getStrokeWidth() ? mBackgroundPaint : mProgressPaint).getStrokeWidth());
        int paddingLeft = getPaddingLeft() + ((measuredWidth - strokeWidth) / 2);
        int paddingTop = getPaddingTop() + ((measuredHeight - strokeWidth) / 2);
        mArcBounds = new RectF(paddingLeft, paddingTop, paddingLeft + strokeWidth, paddingTop + strokeWidth);
        int[] colors = mProgressColors;
        if (colors == null || colors.length <= 1) {
            return;
        }
        mProgressPaint.setShader(new LinearGradient(0.0f, 0.0f, getMeasuredWidth(), 0.0f, mProgressColors, null, Shader.TileMode.MIRROR));
    }

    /**
     * 设置背景颜色
     * @param color 背景颜色资源ID
     */
    public void setBackground(int color) {
        this.mBackgroundPaint.setColor(ContextCompat.getColor(getContext(), color));
        invalidate();
    }

    /**
     * 设置背景宽度
     * @param width 背景宽度
     */
    public void setBackgroundWidth(int width) {
        this.mBackgroundPaint.setStrokeWidth(width);
        invalidate();
    }

    /**
     * 设置进度颜色
     * @param color 进度颜色资源ID
     */
    public void setProgressColor(int color) {
        this.mProgressPaint.setColor(ContextCompat.getColor(getContext(), color));
        this.mProgressPaint.setShader(null);
        invalidate();
    }

    /**
     * 设置进度宽度
     * @param width 进度宽度
     */
    public void setProgressWidth(int width) {
        this.mProgressPaint.setStrokeWidth(width);
        invalidate();
    }

    /**
     * 设置进度
     * @param progress 进度值
     */
    public void setProgress(int progress) {
        this.mCurrentProgress = progress;
        this.mTargetProgress = progress;
        invalidate();
    }

    public CircularProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray obtainStyledAttributes = context.obtainStyledAttributes(attrs, R.styleable.CircularProgressView);
        Paint backgroundPaint = new Paint();
        this.mBackgroundPaint = backgroundPaint;
        backgroundPaint.setStyle(Paint.Style.STROKE);
        this.mBackgroundPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mBackgroundPaint.setAntiAlias(true);
        this.mBackgroundPaint.setDither(true);
        this.mBackgroundPaint.setStrokeWidth(obtainStyledAttributes.getDimension(R.styleable.CircularProgressView_backWidth, 6.0f));
        this.mBackgroundPaint.setColor(obtainStyledAttributes.getColor(R.styleable.CircularProgressView_backColor, Color.YELLOW));
        Paint progressPaint = new Paint();
        this.mProgressPaint = progressPaint;
        progressPaint.setStyle(Paint.Style.STROKE);
        this.mProgressPaint.setStrokeCap(Paint.Cap.ROUND);
        this.mProgressPaint.setAntiAlias(true);
        this.mProgressPaint.setDither(true);
        this.mProgressPaint.setStrokeWidth(obtainStyledAttributes.getDimension(R.styleable.CircularProgressView_progWidth, 6.0f));
        this.mProgressPaint.setColor(obtainStyledAttributes.getColor(R.styleable.CircularProgressView_progColor, Color.BLUE));
        int startColor = obtainStyledAttributes.getColor(R.styleable.CircularProgressView_progStartColor, Color.WHITE);
        int endColor = obtainStyledAttributes.getColor(R.styleable.CircularProgressView_progFirstColor, Color.RED);
        if (startColor != -1 && endColor != -1) {
            this.mProgressColors = new int[]{startColor, endColor};
        } else {
            this.mProgressColors = null;
        }
        this.mCurrentProgress = obtainStyledAttributes.getInteger(R.styleable.CircularProgressView_progress, 0);
        obtainStyledAttributes.recycle();
    }

    /**
     * 设置进度颜色
     * @param colors 进度颜色资源ID数组
     */
    public void setProgressColors(int[] colors) {
        if (colors == null || colors.length < 2) {
            return;
        }
        this.mProgressColors = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            this.mProgressColors[i] = ContextCompat.getColor(getContext(), colors[i]);
        }
        this.mProgressPaint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, getMeasuredWidth(), this.mProgressColors, null, Shader.TileMode.MIRROR));
        invalidate();
    }
}

