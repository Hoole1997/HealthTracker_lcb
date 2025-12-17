package com.app.raise.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import com.app.raise.R

class StarCheckView : View {
    private var initBitmap: Bitmap? = null
    private var starBitmap: Bitmap? = null
    private var defaultBitmap: Bitmap? = null
    private var onStarBitmap: Bitmap? = null
    private var bitmapPaint: Paint? = null
    private var isCheck = false
    private var starAnimator: ValueAnimator? = null
    private var bgAnimator: ValueAnimator? = null
    private var grayStarAnimator: ValueAnimator? = null
    private var animationEndListener: AnimationEndListener? = null
    private var position = 0

    private val bgPaint: Paint = Paint().apply {
        isAntiAlias = true
    }
    private val gradientColors = intArrayOf(1728043553, 1728043553, -855647711)
    private var radialGradient: RadialGradient? = null
    private var currentRadius: Float = 0f

    constructor(context: Context?) : super(context) {
        this.init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        this.init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        this.init()
    }

    private fun init() {
        this.defaultBitmap = loadBitmapFromRes(R.drawable.lib_rate_star)
        this.starBitmap = this.defaultBitmap
        this.onStarBitmap = loadBitmapFromRes(R.drawable.lib_rate_star_on)
        this.bitmapPaint = Paint()
        bgPaint.style = Paint.Style.FILL_AND_STROKE
    }

    private fun loadBitmapFromRes(@DrawableRes resId: Int): Bitmap? {
        val drawable = AppCompatResources.getDrawable(context, resId) ?: return null
        return drawableToBitmap(drawable)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) return drawable.bitmap

        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: this.width.takeIf { it > 0 } ?: return null
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: this.height.takeIf { it > 0 } ?: return null

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    @Synchronized
    fun setInitStarDrawable(@DrawableRes res: Int) {
        if (this.initBitmap == null || initBitmap!!.isRecycled) {
            this.initBitmap = loadBitmapFromRes(res)
        }
        this.starBitmap = this.initBitmap
        this.postInvalidate()
    }

    @Synchronized
    fun setStarDrawableToDefault() {
        this.starBitmap = this.defaultBitmap
        this.postInvalidate()
    }

    fun isCheck(): Boolean {
        return this.isCheck
    }

    fun setCheck(isCheck: Boolean) {
        this.setCheck(isCheck, false)
    }

    fun setPosition(position: Int) {
        this.position = position
    }

    fun setOnAnimationEnd(animationEndListener: AnimationEndListener?) {
        this.animationEndListener = animationEndListener
    }

    fun setCheck(isCheck: Boolean, isAnimation: Boolean) {
        this.isCheck = isCheck
        if (isCheck && isAnimation) {
            this.startAnimation()
        } else {
            if (this.starAnimator != null) {
                starAnimator!!.cancel()
                this.starAnimator = null
            }

            if (this.grayStarAnimator != null) {
                grayStarAnimator!!.cancel()
                this.grayStarAnimator = null
            }

            if (this.bgAnimator != null) {
                bgAnimator!!.cancel()
                this.bgAnimator = null
            }

            this.postInvalidate()
        }
    }

    private fun startAnimation() {
        this.starAnimator = ValueAnimator.ofFloat(*floatArrayOf(0.4f, 1.0f))
        starAnimator?.addUpdateListener { this@StarCheckView.invalidate() }
        starAnimator?.setDuration(1200L)
        starAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                this@StarCheckView.starAnimator = null
            }
        })
        starAnimator?.interpolator = OvershootInterpolator(2.0f)
        starAnimator?.start()
        this.grayStarAnimator = ValueAnimator.ofFloat(*floatArrayOf(1.0f, 0.4f))
        grayStarAnimator?.setDuration(400L)
        grayStarAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                this@StarCheckView.grayStarAnimator = null
            }
        })
        grayStarAnimator?.interpolator = OvershootInterpolator(2.0f)
        grayStarAnimator?.start()
        this.bgAnimator = ValueAnimator.ofFloat(*floatArrayOf(0.4f, 1.2f))
        bgAnimator?.setDuration(1200L)
        bgAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (this@StarCheckView.animationEndListener != null) {
                    animationEndListener!!.onAnimationEnd(animation)
                }

                this@StarCheckView.bgAnimator = null
            }
        })
        bgAnimator?.interpolator = AccelerateDecelerateInterpolator()
        bgAnimator?.start()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = this.width
        val height = this.height

        if (width > 0 && height > 0) {
            val centerX = width / 2.0f
            val centerY = height / 2.0f

            bgAnimator?.let { animator ->
                val value = animator.animatedValue as Float
                val radius = if (width > height) {
                    height / 2.0f * value
                } else {
                    width / 2.0f * value
                }

                val alphaColor = ((1.2f - value) / 1.2f * 255.0f).toInt() * 2
                bgPaint.alpha = alphaColor

                if (radius != currentRadius) {
                    radialGradient = RadialGradient(
                        centerX,
                        centerY,
                        radius,
                        gradientColors,
                        null,
                        Shader.TileMode.CLAMP
                    )
                    bgPaint.shader = radialGradient
                    currentRadius = radius
                }

                canvas.drawCircle(centerX, centerY, radius, bgPaint)
            }

            var grayStartAlpha = 255
            var startAlpha = 255
            var isCanvasSave = false
            var value: Float
            if (this.grayStarAnimator != null) {
                value = grayStarAnimator!!.animatedValue as Float
                grayStartAlpha = (grayStartAlpha.toFloat() * value).toInt()
                isCanvasSave = true
                canvas.save()
                canvas.scale(value, value, centerX, centerY)
            }

            if (!this.isCheck) {
                this.drawBitmap(canvas, this.starBitmap, grayStartAlpha)
            }

            if (isCanvasSave) {
                canvas.restore()
            }

            if (this.starAnimator != null) {
                value = starAnimator!!.animatedValue as Float
                startAlpha = (startAlpha.toFloat() * value).toInt()
                canvas.scale(value, value, centerX, centerY)
            }

            if (this.isCheck) {
                this.drawBitmap(canvas, this.onStarBitmap, startAlpha)
            }
        }
    }

    private fun drawBitmap(canvas: Canvas?, bitmap: Bitmap?, alpha: Int) {
        var mAlpha = alpha
        if (bitmap != null && canvas != null) {
            if (mAlpha > 255) {
                mAlpha = 255
            }

            val left = (this.width - bitmap.width) / 2
            val top = (this.height - bitmap.height) / 2
            bitmapPaint!!.alpha = mAlpha
            canvas.drawBitmap(bitmap, left.toFloat(), top.toFloat(), this.bitmapPaint)
        }
    }

    interface AnimationEndListener {
        fun onAnimationEnd(animation: Animator?)
    }
}