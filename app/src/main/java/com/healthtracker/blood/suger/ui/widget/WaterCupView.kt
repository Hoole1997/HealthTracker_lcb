package com.healthtracker.blood.suger.ui.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.healthtracker.blood.suger.R
import kotlin.math.min
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 自定义控件：喝水状态杯子
 * - 底层绘制杯子图片 `R.mipmap.ic_cup`
 * - 盖上水波纹（Canvas 绘制的两层正弦波填充）
 * - 最上层覆盖杯子遮罩 `R.mipmap.ic_cup_cover` 以遮挡超出杯沿的波纹
 */
class WaterCupView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 配置参数（可通过 XML attrs 覆盖）
    private var maxCups: Int = 8
    private var currentCups: Int = 0
    private var waterColor: Int = Color.parseColor("#4DB6F9")
    private var waveAmplitudePx: Float = dp(8f)
    private var waveSpeedPxPerSec: Float = dp(60f) // 每秒位移像素
    private var cupTopInsetPx: Float = dp(24f) // 杯内水面顶部预留（避免水漫出杯沿）
    private var cupBottomInsetPx: Float = dp(12f) // 底部预留
    private var cupSideInsetPx: Float = dp(10f) // 左右预留，避免触碰杯壁/白边
    private var stepMl: Int = 100 // 水位变化的最小触发单位（毫升）
    private var enableBreathing: Boolean = true // 是否开启振幅呼吸动画

    // 位图资源
    private var cupBitmapSrc: Bitmap? = null
    private var coverBitmapSrc: Bitmap? = null
    private var cupBitmap: Bitmap? = null
    private var coverBitmap: Bitmap? = null

    // 动画相关
    private var waveShiftPx: Float = 0f
    private var waveAnimator: ValueAnimator? = null
    private var levelAnimator: ValueAnimator? = null
    private var animatedLevelRatio: Float = 0f // 动画过渡中显示的水位比例（0..1）
    private var amplitudeAnimator: ValueAnimator? = null // 振幅呼吸动画
    private var amplitudeBreath: Float = 1f // 呼吸因子（0.9..1.15）

    // 绘制对象
    private val baseWaterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = waterColor
    }
    private val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = waterColor
    }
    private val waterPaint2 = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = adjustAlpha(waterColor, 0.6f)
    }
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val waterSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(2f)
        color = adjustAlpha(Color.WHITE, 0.9f)
    }

    private val wavePath = Path()
    private val wavePath2 = Path()
    private val waveSurfacePath = Path()

    // 计算用缓存
    private var cupDrawLeft = 0f
    private var cupDrawTop = 0f
    private var cupDrawWidth = 0f
    private var cupDrawHeight = 0f
    private var waveLengthPx = 0f

    init {
        // 读取自定义属性
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.WaterCupView)
            maxCups = a.getInt(R.styleable.WaterCupView_maxCups, maxCups)
            currentCups = a.getInt(R.styleable.WaterCupView_currentCups, currentCups)
            waterColor = a.getColor(R.styleable.WaterCupView_waterColor, waterColor)
            waveAmplitudePx = a.getDimension(R.styleable.WaterCupView_waveAmplitude, waveAmplitudePx)
            val speedDpPerSec = a.getFloat(R.styleable.WaterCupView_cupWaveSpeed, 60f)
            waveSpeedPxPerSec = dp(speedDpPerSec)
            cupTopInsetPx = a.getDimension(R.styleable.WaterCupView_cupTopInset, cupTopInsetPx)
            cupBottomInsetPx = a.getDimension(R.styleable.WaterCupView_cupBottomInset, cupBottomInsetPx)
            cupSideInsetPx = a.getDimension(R.styleable.WaterCupView_cupSideInset, cupSideInsetPx)
            a.recycle()
        }

        // 初始化位图资源
        try {
            cupBitmapSrc = BitmapFactory.decodeResource(resources, R.mipmap.ic_cup)
            coverBitmapSrc = BitmapFactory.decodeResource(resources, R.mipmap.ic_cup_cover)
        } catch (_: Throwable) {
            // 忽略：若资源不存在，控件仍可不崩溃运行（只绘制波纹）
        }

        animatedLevelRatio = if (maxCups > 0) currentCups / maxCups.toFloat() else 0f

        // 同步水颜色到第二层
        baseWaterPaint.color = waterColor
        waterPaint.color = waterColor
        waterPaint2.color = adjustAlpha(waterColor, 0.6f)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // 当水位为 0 时不启动波纹动画
        updateWaveAnimationState()
    }

    override fun onDetachedFromWindow() {
        stopWaveAnimation()
        stopAmplitudeAnimation()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val srcW = cupBitmapSrc?.width ?: dp(120f).roundToInt()
        val srcH = cupBitmapSrc?.height ?: dp(160f).roundToInt()

        val desiredW = srcW
        val desiredH = srcH

        val w = resolveSize(desiredW, widthMeasureSpec)
        val h = resolveSize(desiredH, heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 将杯子与遮罩缩放到视图可用范围内（居中）
        val srcCup = cupBitmapSrc
        val srcCover = coverBitmapSrc
        if (srcCup != null && srcCover != null && w > 0 && h > 0) {
            val scale = min(w / srcCup.width.toFloat(), h / srcCup.height.toFloat())
            val targetW = (srcCup.width * scale).roundToInt().coerceAtLeast(1)
            val targetH = (srcCup.height * scale).roundToInt().coerceAtLeast(1)
            cupBitmap = Bitmap.createScaledBitmap(srcCup, targetW, targetH, true)
            coverBitmap = Bitmap.createScaledBitmap(srcCover, targetW, targetH, true)

            cupDrawWidth = targetW.toFloat()
            cupDrawHeight = targetH.toFloat()
            cupDrawLeft = (w - targetW) / 2f
            cupDrawTop = (h - targetH) / 2f
            waveLengthPx = (cupDrawWidth - 2f * cupSideInsetPx).coerceAtLeast(1f)
        } else {
            // 无位图时，波长取视图宽度
            cupDrawWidth = w.toFloat()
            cupDrawHeight = h.toFloat()
            cupDrawLeft = 0f
            cupDrawTop = 0f
            waveLengthPx = (w.toFloat() - 2f * cupSideInsetPx).coerceAtLeast(1f)
        }

        // 根据尺寸调整波动动画时长（位移一个波长所需时间）
        restartWaveAnimationIfRunning()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cupW = cupDrawWidth
        val cupH = cupDrawHeight
        val left = cupDrawLeft
        val top = cupDrawTop

        // 1) 绘制杯子底图
        cupBitmap?.let { canvas.drawBitmap(it, left, top, imagePaint) }

        // 2) 计算水位区域
        val waveBottom = top + cupH - cupBottomInsetPx
        val waveAreaHeight = (cupH - cupTopInsetPx - cupBottomInsetPx).coerceAtLeast(0f)
        val levelY = waveBottom - animatedLevelRatio.coerceIn(0f, 1f) * waveAreaHeight

        // 3) 若饮水量为 0（比例为 0），不绘制水波纹
        if (animatedLevelRatio > 0f) {
            val shiftRad = (waveShiftPx / waveLengthPx) * (Math.PI.toFloat() * 2f)
            val innerLeft = left + cupSideInsetPx
            val innerWidth = (cupW - 2f * cupSideInsetPx).coerceAtLeast(1f)
            // 动态振幅：随水位与呼吸动画变化
            val amplitude1 = waveAmplitudePx * amplitudeBreath * (0.6f + 0.4f * animatedLevelRatio)
            val amplitude2 = amplitude1 * 0.55f

            // 先绘制底层矩形水体，确保底部是水平的
            canvas.drawRect(innerLeft, levelY, innerLeft + innerWidth, waveBottom, baseWaterPaint)

            // 再绘制仅在水面上的波纹，底边闭合到 levelY，保证底部是直线
            // 设置渐变填充，使水面与底部颜色层次更自然
            waterPaint.shader = LinearGradient(
                innerLeft,
                levelY - amplitude1,
                innerLeft,
                waveBottom,
                adjustAlpha(waterColor, 0.95f),
                adjustAlpha(waterColor, 0.65f),
                Shader.TileMode.CLAMP
            )
            waterPaint2.shader = LinearGradient(
                innerLeft,
                levelY - amplitude2,
                innerLeft,
                waveBottom,
                adjustAlpha(waterColor, 0.7f),
                adjustAlpha(waterColor, 0.4f),
                Shader.TileMode.CLAMP
            )

            buildWavePath(wavePath, innerLeft, innerWidth, levelY, levelY, amplitude1, shiftRad)
            canvas.drawPath(wavePath, waterPaint)

            buildWavePath(wavePath2, innerLeft, innerWidth, levelY, levelY, amplitude2, shiftRad + Math.PI.toFloat())
            canvas.drawPath(wavePath2, waterPaint2)

            // 水面高光描边：仅描绘波面线条，增强动感
            buildWaveSurfacePath(waveSurfacePath, innerLeft, innerWidth, levelY, amplitude1 * 0.85f, shiftRad)
            canvas.drawPath(waveSurfacePath, waterSurfacePaint)
        }

        // 4) 绘制遮罩图片（盖在最上层）
        coverBitmap?.let { canvas.drawBitmap(it, left, top, imagePaint) }
    }

    // 构建从左到右的波纹填充 Path
    private fun buildWavePath(
        path: Path,
        left: Float,
        width: Float,
        levelY: Float,
        bottomY: Float,
        amplitude: Float,
        phase: Float
    ) {
        path.reset()
        var x = 0f
        val step = 4f // 绘制步进，越小越平滑
        path.moveTo(left, levelY)
        while (x <= width) {
            val y = levelY + amplitude * sin((x / waveLengthPx) * (Math.PI.toFloat() * 2f) + phase)
            path.lineTo(left + x, y)
            x += step
        }
        path.lineTo(left + width, bottomY)
        path.lineTo(left, bottomY)
        path.close()
    }

    // 构建水面高光线 Path（不闭合，仅顶部波面）
    private fun buildWaveSurfacePath(
        path: Path,
        left: Float,
        width: Float,
        levelY: Float,
        amplitude: Float,
        phase: Float
    ) {
        path.reset()
        var x = 0f
        val step = 4f
        var started = false
        while (x <= width) {
            val y = levelY + amplitude * sin((x / waveLengthPx) * (Math.PI.toFloat() * 2f) + phase)
            if (!started) {
                path.moveTo(left + x, y)
                started = true
            } else {
                path.lineTo(left + x, y)
            }
            x += step
        }
    }

    // 启动波纹位移动画（循环）
    private fun startWaveAnimation() {
        if (waveAnimator?.isRunning == true) return
        val durationMs = computeWaveDurationMs()
        waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                waveShiftPx = fraction * waveLengthPx
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    private fun restartWaveAnimationIfRunning() {
        if (waveAnimator?.isRunning == true) {
            stopWaveAnimation()
            startWaveAnimation()
        }
    }

    private fun stopWaveAnimation() {
        waveAnimator?.cancel()
        waveAnimator = null
    }

    private fun computeWaveDurationMs(): Long {
        val pxPerSec = waveSpeedPxPerSec.coerceAtLeast(1f)
        val seconds = (waveLengthPx / pxPerSec).coerceAtLeast(0.8f) // 最短 0.8s 一周期
        return (seconds * 1000L).toLong()
    }

    // 根据当前水位比例决定是否需要运行波纹动画
    private fun updateWaveAnimationState() {
        if (animatedLevelRatio <= 0f) {
            stopWaveAnimation()
            stopAmplitudeAnimation()
        } else {
            startWaveAnimation()
            startAmplitudeAnimation()
        }
    }

    // 启动/停止振幅呼吸动画
    private fun startAmplitudeAnimation() {
        if (!enableBreathing) return
        if (amplitudeAnimator?.isRunning == true) return
        amplitudeAnimator = ValueAnimator.ofFloat(0.9f, 1.15f).apply {
            duration = 2200L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                amplitudeBreath = animator.animatedValue as Float
                if (animatedLevelRatio > 0f) {
                    postInvalidateOnAnimation()
                }
            }
            start()
        }
    }

    private fun stopAmplitudeAnimation() {
        amplitudeAnimator?.cancel()
        amplitudeAnimator = null
        amplitudeBreath = 1f
    }

    // region 对外 API
    /**
     * 设置水位变化的最小单位（毫升），默认 100ml。
     */
    fun setStepMl(minStepMl: Int) {
        stepMl = minStepMl.coerceAtLeast(1)
    }

    /**
     * 按目标毫升设置最大步数（量化为 stepMl 单位），如目标 2000ml、step 100ml => maxCups=20。
     */
    fun setTargetMl(targetMl: Int) {
        val maxSteps = ceil(targetMl / stepMl.toDouble()).toInt().coerceAtLeast(1)
        setMaxCups(maxSteps)
    }

    /**
     * 设置当前毫升数，水位以 stepMl 为最小单位量化到杯数并过渡动画。
     */
    fun setCurrentMl(currentMl: Int) {
        val newCups = (currentMl / stepMl).coerceIn(0, maxCups)
        // 无论增减，均直接设置水位并重绘，不执行动画
        levelAnimator?.cancel()
        currentCups = newCups
        animatedLevelRatio = if (maxCups > 0) newCups / maxCups.toFloat() else 0f
        updateWaveAnimationState()
        invalidate()
    }

    fun setMaxCups(max: Int) {
        // 保持当前动画水位比例，不因目标步数变化而重置，避免每次都从同一点开始
        val prevRatio = animatedLevelRatio
        maxCups = max.coerceAtLeast(1)
        animatedLevelRatio = prevRatio
        updateWaveAnimationState()
        invalidate()
    }

    fun setCurrentCups(current: Int) {
        currentCups = current.coerceIn(0, maxCups)
        animatedLevelRatio = currentCups / maxCups.toFloat()
        updateWaveAnimationState()
        invalidate()
    }

    fun getCurrentCups(): Int = currentCups
    fun getMaxCups(): Int = maxCups

    /**
     * 喝一杯水：水位上升一个单位，带过渡动画
     */
    fun drinkOneCup(step: Int = 1) {
        val targetCups = (currentCups + step).coerceAtMost(maxCups)
        animateWaterLevelTo(targetCups)
    }

    /**
     * 将水位平滑过渡到指定杯数
     */
    fun animateWaterLevelTo(targetCups: Int) {
        val newCups = targetCups.coerceIn(0, maxCups)
        val start = animatedLevelRatio
        val end = newCups / maxCups.toFloat()
        levelAnimator?.cancel()
        levelAnimator = ValueAnimator.ofFloat(start, end).apply {
            duration = 800L
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animator ->
                animatedLevelRatio = animator.animatedValue as Float
                updateWaveAnimationState()
                postInvalidateOnAnimation()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) { currentCups = newCups; updateWaveAnimationState() }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            start()
        }
    }
    // endregion

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun adjustAlpha(color: Int, factor: Float): Int {
        val a = (Color.alpha(color) * factor).roundToInt().coerceIn(0, 255)
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(a, r, g, b)
    }
}