package com.healthtracker.blood.suger.ui.weight

import android.widget.Scroller
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.enum.BloodSugarUnit
import com.healthtracker.framework.ext.logd
import kotlin.math.abs
import kotlin.math.roundToInt

class BloodSugarRulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "BloodSugarRulerView"
        private const val DEFAULT_SCALE_STEP = 0.1f
        private const val DEFAULT_SCALE_COUNT = 10
        private const val DEFAULT_SCALE_GAP = 20f
        private const val ANIMATION_DURATION = 300L
        private const val MIN_VELOCITY_THRESHOLD = 50
    }

    // 基础配置
    private var scaleStep = DEFAULT_SCALE_STEP
    private var rulerHeight = 50f
    private var scaleCount = DEFAULT_SCALE_COUNT
    private var scaleGap = DEFAULT_SCALE_GAP
    private var minScale = 0f
    private var maxScale = 100f
    private var scrollableMinScale = 0f
    private var scrollableMaxScale = 100f
    private var firstScale = 50f

    // 颜色配置
    private var bgColor = 0xfffcfffc.toInt()
    private var smallScaleColor = 0xff999999.toInt()
    private var midScaleColor = 0xff666666.toInt()
    private var largeScaleColor = 0xff50b586.toInt()
    private var scaleNumColor = 0xff333333.toInt()
    private var indicatorColor = 0xff50b586.toInt()

    // 尺寸配置
    private var smallScaleStroke = 1f
    private var midScaleStroke = 2f
    private var largeScaleStroke = 3f
    private var indicatorStroke = 3f
    private var scaleNumTextSize = 16f
    private var scaleTextMargin = 8f
    private var rulerPaddingHorizontal = 0f
    private var rulerPaddingVertical = 0f

    // 高度配置
    private var smallScaleHeight = rulerHeight / 4
    private var midScaleHeight = rulerHeight / 2
    private var largeScaleHeight = rulerHeight / 2 + 5
    private var indicatorHeight = rulerHeight

    // 其他配置
    private var isBgRoundRect = true
    private var decimalPlaces = 1
    private var currentUnit = BloodSugarUnit.MMOL_L

    // 回调接口
    private var onChooseResultListener: OnChooseResultListener? = null

    // 滑动控制
    private var computeScale = -1f
    private var currentScale = firstScale
    private lateinit var scroller: Scroller
    private var velocityTracker: VelocityTracker? = null

    // 绘制对象
    private lateinit var bgPaint: Paint
    private lateinit var smallScalePaint: Paint
    private lateinit var midScalePaint: Paint
    private lateinit var largeScalePaint: Paint
    private lateinit var scaleNumPaint: Paint
    private lateinit var indicatorPaint: Paint
    private lateinit var scaleNumRect: Rect
    private lateinit var bgRect: RectF

    // 尺寸
    private var viewWidth = 0
    private var viewHeight = 0
    private var centerX = 0f

    // 触摸控制
    private var downX = 0f
    private var moveX = 0f
    private var currentX = 0f
    private var lastMoveX = 0f
    private var isUp = false
    private var isFirstScale = true

    init {
        setAttr(attrs, defStyleAttr)
        initPaints()
        scroller = Scroller(context, DecelerateInterpolator())
    }

    private fun setAttr(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context.theme.obtainStyledAttributes(
                attrs, R.styleable.RulerView, defStyleAttr, 0
            )

            scaleStep = typedArray.getFloat(R.styleable.RulerView_scaleStep, DEFAULT_SCALE_STEP)

            rulerHeight = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_rulerHeight,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rulerHeight, resources.displayMetrics).toInt()
            ).toFloat()


            scaleCount = typedArray.getInt(R.styleable.RulerView_scaleCount, DEFAULT_SCALE_COUNT)

            scaleGap = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_scaleGap,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_SCALE_GAP, resources.displayMetrics).toInt()
            ).toFloat()

            minScale = typedArray.getFloat(R.styleable.RulerView_minScale, 0f)
            maxScale = typedArray.getFloat(R.styleable.RulerView_maxScale, 100f)
            scrollableMinScale = typedArray.getFloat(R.styleable.RulerView_scrollableMinScale, minScale)
            scrollableMaxScale = typedArray.getFloat(R.styleable.RulerView_scrollableMaxScale, maxScale)
            firstScale = typedArray.getFloat(R.styleable.RulerView_firstScale, 50f)

            bgColor = typedArray.getColor(R.styleable.RulerView_bgColor, bgColor)
            smallScaleColor = typedArray.getColor(R.styleable.RulerView_smallScaleColor, smallScaleColor)
            midScaleColor = typedArray.getColor(R.styleable.RulerView_midScaleColor, midScaleColor)
            largeScaleColor = typedArray.getColor(R.styleable.RulerView_largeScaleColor, largeScaleColor)
            scaleNumColor = typedArray.getColor(R.styleable.RulerView_scaleNumColor, scaleNumColor)
            indicatorColor = typedArray.getColor(R.styleable.RulerView_indicatorColor, indicatorColor)

            smallScaleStroke = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_smallScaleStroke,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, smallScaleStroke, resources.displayMetrics).toInt()
            ).toFloat()

            midScaleStroke = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_midScaleStroke,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, midScaleStroke, resources.displayMetrics).toInt()
            ).toFloat()

            largeScaleStroke = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_largeScaleStroke,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, largeScaleStroke, resources.displayMetrics).toInt()
            ).toFloat()

            indicatorStroke = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_indicatorStroke,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorStroke, resources.displayMetrics).toInt()
            ).toFloat()

            scaleNumTextSize = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_scaleNumTextSize,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, scaleNumTextSize, resources.displayMetrics).toInt()
            ).toFloat()

            scaleTextMargin = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_scaleTextMargin,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scaleTextMargin, resources.displayMetrics).toInt()
            ).toFloat()

            rulerPaddingHorizontal = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_rulerPaddingHorizontal,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rulerPaddingHorizontal, resources.displayMetrics).toInt()
            ).toFloat()

            rulerPaddingVertical = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_rulerPaddingVertical,
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, rulerPaddingVertical, resources.displayMetrics).toInt()
            ).toFloat()

            smallScaleHeight = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_smallScaleHeight,
                (rulerHeight / 4).toInt()
            ).toFloat()

            midScaleHeight = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_midScaleHeight,
                (rulerHeight / 2).toInt()
            ).toFloat()

            largeScaleHeight = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_largeScaleHeight,
                (rulerHeight / 2 + 5).toInt()
            ).toFloat()

            indicatorHeight = typedArray.getDimensionPixelSize(
                R.styleable.RulerView_indicatorHeight,
                rulerHeight.toInt()
            ).toFloat()

            isBgRoundRect = typedArray.getBoolean(R.styleable.RulerView_isBgRoundRect, true)
            decimalPlaces = typedArray.getInt(R.styleable.RulerView_decimalPlaces, 1)

            typedArray.recycle()
        }

        currentScale = firstScale.coerceIn(scrollableMinScale, scrollableMaxScale)
    }

    private fun initPaints() {
        bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }

        smallScalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = smallScaleColor
            style = Paint.Style.FILL
            strokeWidth = smallScaleStroke
            strokeCap = Paint.Cap.ROUND
        }

        midScalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = midScaleColor
            style = Paint.Style.FILL
            strokeWidth = midScaleStroke
            strokeCap = Paint.Cap.ROUND
        }

        largeScalePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = largeScaleColor
            style = Paint.Style.FILL
            strokeWidth = largeScaleStroke
            strokeCap = Paint.Cap.ROUND
        }

        scaleNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = scaleNumColor
            textSize = scaleNumTextSize
        }

        indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = indicatorColor
            style = Paint.Style.FILL
            strokeWidth = indicatorStroke
            strokeCap = Paint.Cap.ROUND
        }

        scaleNumRect = Rect()
        bgRect = RectF()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        viewHeight = when (heightMode) {
            MeasureSpec.AT_MOST, MeasureSpec.UNSPECIFIED -> {
                (rulerHeight + rulerPaddingVertical * 2 + paddingTop + paddingBottom).toInt()
            }
            else -> heightSize
        }

        viewWidth = widthSize
        centerX = viewWidth / 2f

        setMeasuredDimension(viewWidth, viewHeight)
    }

    override fun onDraw(canvas: Canvas) {
        drawBg(canvas)
        drawScalesAndNumbers(canvas)
        drawIndicator(canvas)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            moveX = scroller.currX.toFloat()
            lastMoveX = moveX
            invalidate()

            // 检查是否滑动完成
            if (!scroller.isFinished) {
                postInvalidateOnAnimation()
            } else {
                // 滑动完成，进行对齐处理
                isUp = true
                invalidate()
            }
        }
    }

    private fun drawBg(canvas: Canvas) {
        bgRect.set(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        if (isBgRoundRect) {
            canvas.drawRoundRect(bgRect, 20f, 20f, bgPaint)
        } else {
            canvas.drawRect(bgRect, bgPaint)
        }
    }

    private fun drawScalesAndNumbers(canvas: Canvas) {
        canvas.save()
        canvas.translate(rulerPaddingHorizontal, rulerPaddingVertical)

        if (isFirstScale) {
            moveX = getScalePosition(firstScale)
            lastMoveX = moveX
            isFirstScale = false
        }

        if (computeScale != -1f) {
            animateToScale(computeScale)
            computeScale = -1f
        }

        val scaleIndex = -(moveX / scaleGap).toInt()
        val offset = moveX % scaleGap

        canvas.save()
        canvas.translate(offset, 0f)

        var drawPosition = 0f
        var currentIndex = scaleIndex
        val availableWidth = viewWidth - rulerPaddingHorizontal * 2

        while (drawPosition < availableWidth) {
            val scaleValue = getScaleValue(currentIndex)

            if (scaleValue >= minScale && scaleValue <= maxScale) {
                when {
                    shouldDrawLargeScale(currentIndex, scaleValue) -> {
                        // 绘制大刻度线
                        canvas.drawLine(0f, 0f, 0f, largeScaleHeight, largeScalePaint)

                        // 根据单位决定是否显示刻度值文字
                        if (shouldShowScaleText(scaleValue)) {
                            val scaleText = formatScaleValueForDisplay(scaleValue)
                            scaleNumPaint.getTextBounds(scaleText, 0, scaleText.length, scaleNumRect)
                            canvas.drawText(
                                scaleText,
                                -scaleNumRect.width() / 2f,
                                largeScaleHeight + scaleTextMargin + scaleNumRect.height(),
                                scaleNumPaint
                            )
                        }
                    }
                    shouldDrawSmallScale(currentIndex, scaleValue) -> {
                        // 绘制小刻度线（根据单位决定绘制规则）
                        canvas.drawLine(0f, 0f, 0f, smallScaleHeight, smallScalePaint)
                    }
                }
            }

            currentIndex++
            drawPosition += scaleGap
            canvas.translate(scaleGap, 0f)
        }

        canvas.restore()

        updateCurrentScale()

        if (isUp) {
            snapToNearestScale()
            isUp = false
        }

        canvas.restore()
    }

    private fun drawIndicator(canvas: Canvas) {
        canvas.drawLine(
            centerX, rulerPaddingVertical,
            centerX, rulerPaddingVertical + indicatorHeight,
            indicatorPaint
        )
    }

    private fun getScaleValue(index: Int): Float {
        return minScale + index * scaleStep
    }

    private fun getScalePosition(scale: Float): Float {
        val index = ((scale - minScale) / scaleStep).roundToInt()
        return centerX - scaleGap * index - rulerPaddingHorizontal
    }

    private fun formatScaleValue(value: Float): String {
        return String.format("%.${decimalPlaces}f", value)
    }

    /**
     * 判断是否应该绘制大刻度线
     */
    private fun shouldDrawLargeScale(index: Int, value: Float): Boolean {
        return when (currentUnit) {
            BloodSugarUnit.MMOL_L -> {
                // mmol/L: 每5个小刻度绘制大刻度 (0.5间隔)
                index % scaleCount == 0
            }
            BloodSugarUnit.MG_DL -> {
                // mg/dL: 每50个小刻度绘制大刻度 (5单位间隔)
                val intValue = value.roundToInt()
                intValue % 5 == 0 && abs(value - intValue) < 0.01f
            }
        }
    }

    /**
     * 判断是否应该绘制小刻度线
     */
    private fun shouldDrawSmallScale(index: Int, value: Float): Boolean {
        return when (currentUnit) {
            BloodSugarUnit.MMOL_L -> {
                // mmol/L: 每个0.1都绘制小刻度线
                !shouldDrawLargeScale(index, value)
            }
            BloodSugarUnit.MG_DL -> {
                // mg/dL: 只在0.5的倍数位置绘制小刻度线（不包括大刻度）
                val remainder = ((value * 10).roundToInt() % 5)
                remainder == 0 && !shouldDrawLargeScale(index, value)
            }
        }
    }

    /**
     * 判断是否应该显示刻度值文字
     */
    private fun shouldShowScaleText(value: Float): Boolean {
        return when (currentUnit) {
            BloodSugarUnit.MMOL_L -> {
                // mmol/L: 所有大刻度都显示文字
                true
            }
            BloodSugarUnit.MG_DL -> {
                // mg/dL: 只在5的倍数整数位置显示文字
                val intValue = value.roundToInt()
                intValue % 5 == 0 && abs(value - intValue) < 0.01f
            }
        }
    }

    /**
     * 格式化用于显示的刻度值
     */
    private fun formatScaleValueForDisplay(value: Float): String {
        return when (currentUnit) {
            BloodSugarUnit.MMOL_L -> {
                // mmol/L: 显示一位小数
                String.format("%.1f", value)
            }
            BloodSugarUnit.MG_DL -> {
                // mg/dL: 只显示整数
                value.roundToInt().toString()
            }
        }
    }

    private fun updateCurrentScale() {
        // 根据滑动位置计算精确的刻度值
        val exactValue = minScale - ((moveX - centerX) / scaleGap) * scaleStep
        val clampedValue = exactValue.coerceIn(scrollableMinScale, scrollableMaxScale)

        currentScale = when (currentUnit) {
            BloodSugarUnit.MMOL_L -> {
                // mmol/L: 直接按0.1精度四舍五入
                (clampedValue * 10).roundToInt() / 10f
            }
            BloodSugarUnit.MG_DL -> {
                // mg/dL: 支持0.1精度，但需要智能处理
                calculateMgDlValue(clampedValue)
            }
        }

        onChooseResultListener?.onScrollResult(formatScaleValue(currentScale))
    }

    /**
     * 计算mg/dL模式下的值
     * 支持0.1精度，但会根据距离刻度线的位置进行智能估算
     */
    private fun calculateMgDlValue(exactValue: Float): Float {
        // 四舍五入到0.1精度
        val roundedValue = (exactValue * 10).roundToInt() / 10f

        // 检查是否正好在0.5倍数的刻度线上
        val remainder = ((roundedValue * 10).roundToInt() % 5)

        return if (remainder == 0) {
            // 在刻度线上或很接近刻度线，返回精确值
            roundedValue
        } else {
            // 在两个刻度线之间，保持0.1精度
            roundedValue
        }
    }

    private fun snapToNearestScale() {
        when (currentUnit) {
            BloodSugarUnit.MMOL_L -> {
                // mmol/L: 对齐到最近的0.1刻度
                snapToNearestStep()
            }
            BloodSugarUnit.MG_DL -> {
                // mg/dL: 智能对齐逻辑
                snapToNearestMgDlPosition()
            }
        }
    }

    private fun snapToNearestStep() {
        val targetIndex = -((moveX - centerX) / scaleGap).roundToInt()
        val targetScale = (minScale + targetIndex * scaleStep).coerceIn(scrollableMinScale, scrollableMaxScale)
        val targetPosition = getScalePosition(targetScale)

        if (abs(moveX - targetPosition) > 0.1f) {
            animateToPosition(targetPosition)
        } else {
            onChooseResultListener?.onEndResult(formatScaleValue(currentScale))
        }
    }

    private fun snapToNearestMgDlPosition() {
        // 计算当前精确位置对应的值
        val exactValue = minScale - ((moveX - centerX) / scaleGap) * scaleStep
        val clampedValue = exactValue.coerceIn(scrollableMinScale, scrollableMaxScale)

        // 四舍五入到0.1精度
        val roundedValue = (clampedValue * 10).roundToInt() / 10f

        // 计算对应的目标位置
        val targetPosition = getScalePosition(roundedValue)

        if (abs(moveX - targetPosition) > 0.1f) {
            animateToPosition(targetPosition)
        } else {
            updateCurrentScale()
            onChooseResultListener?.onEndResult(formatScaleValue(currentScale))
        }
    }

    private fun animateToPosition(targetPosition: Float) {
        val startX = moveX.toInt()
        val targetX = targetPosition.toInt()
        val deltaX = targetX - startX

        if (abs(deltaX) < 1) {
            updateCurrentScale()
            onChooseResultListener?.onEndResult(formatScaleValue(currentScale))
            return
        }

        scroller.startScroll(startX, 0, deltaX, 0, ANIMATION_DURATION.toInt())
        postInvalidateOnAnimation()
    }

    private fun animateToScale(targetScale: Float) {
        val clampedScale = targetScale.coerceIn(scrollableMinScale, scrollableMaxScale)
        val targetPosition = getScalePosition(clampedScale)

        val startX = moveX.toInt()
        val targetX = targetPosition.toInt()
        val deltaX = targetX - startX

        if (abs(deltaX) < 1) {
            updateCurrentScale()
            onChooseResultListener?.onEndResult(formatScaleValue(currentScale))
            return
        }

        val duration = (abs(deltaX) / 100 * ANIMATION_DURATION).toLong().coerceIn(100, 1000)
        scroller.startScroll(startX, 0, deltaX, 0, duration.toInt())
        postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        "onTouchEvent action=${event.action}".logd(TAG)

        // 确保 VelocityTracker 正确初始化
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }

        currentX = event.x
        isUp = false
        velocityTracker?.addMovement(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                "ACTION_DOWN".logd(TAG)
                scroller.forceFinished(true)
                downX = event.x
                // 请求父View不要拦截后续事件
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = currentX - downX + lastMoveX
                val minPosition = getScalePosition(scrollableMaxScale)
                val maxPosition = getScalePosition(scrollableMinScale)

                moveX = deltaX.coerceIn(minPosition, maxPosition)
                "ACTION_MOVE moveX = $moveX".logd(TAG)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                "ACTION_UP/CANCEL".logd(TAG)
                lastMoveX = moveX

                // 安全处理 VelocityTracker
                try {
                    velocityTracker?.computeCurrentVelocity(500)
                    val xVelocity = velocityTracker?.xVelocity?.toInt() ?: 0
                    autoVelocityScroll(xVelocity)
                } finally {
                    // 正确释放 VelocityTracker 资源
                    velocityTracker?.recycle()
                    velocityTracker = null
                }

                // 允许父View重新拦截事件
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        invalidate()
        return true
    }

    private fun autoVelocityScroll(xVelocity: Int) {
        "autoVelocityScroll start".logd(TAG)
        if (abs(xVelocity) < MIN_VELOCITY_THRESHOLD) {
            "autoVelocityScroll".logd(TAG)
            isUp = true
            return
        }

        val startX = moveX.toInt()
        val minPosition = getScalePosition(scrollableMaxScale).toInt()
        val maxPosition = getScalePosition(scrollableMinScale).toInt()
        "startX = $startX,xVelocity = $xVelocity,minPosition = $minPosition,maxPosition = $maxPosition".logd(TAG)

        scroller.fling(
            startX, 0,  // 起始位置
            xVelocity, 0,  // 初始速度
            minPosition, maxPosition,  // X范围
            0, 0  // Y范围（不使用）
        )

        postInvalidateOnAnimation()
    }

    fun setOnChooseResultListener(listener: OnChooseResultListener) {
        this.onChooseResultListener = listener
    }

    fun scrollToScale(scale: Float) {
        computeScale = scale.coerceIn(scrollableMinScale, scrollableMaxScale)
        invalidate()
    }

    /**
     * 立即设置刻度位置，无动画效果
     * 用于初始化和单位切换时的直接定位
     * @param scale 目标刻度值
     */
    fun setScaleImmediately(scale: Float) {
        val clampedScale = scale.coerceIn(scrollableMinScale, scrollableMaxScale)
        val targetPosition = getScalePosition(clampedScale)
        
        // 直接设置位置，跳过动画
        moveX = targetPosition
        lastMoveX = moveX
        currentScale = clampedScale
        
        // 停止任何正在进行的滚动动画
        if (scroller.isFinished.not()) {
            scroller.abortAnimation()
        }
        
        // 重置动画相关标志
        computeScale = -1f
        isFirstScale = false
        
        // 立即更新显示
        invalidate()
        
        // 通知监听器最终结果
        onChooseResultListener?.onEndResult(formatScaleValue(currentScale))
    }

    fun getCurrentScale(): Float = currentScale

    fun setScaleRange(min: Float, max: Float) {
        this.minScale = min
        this.maxScale = max
        invalidate()
    }

    fun setScrollableRange(min: Float, max: Float) {
        this.scrollableMinScale = min.coerceAtLeast(minScale)
        this.scrollableMaxScale = max.coerceAtMost(maxScale)

        currentScale = currentScale.coerceIn(scrollableMinScale, scrollableMaxScale)
        invalidate()
    }

    fun setScaleStep(step: Float) {
        this.scaleStep = step
        invalidate()
    }

    fun setDecimalPlaces(places: Int) {
        this.decimalPlaces = places
        invalidate()
    }

    fun setScaleCount(count: Int) {
        this.scaleCount = count
        invalidate()
    }

    fun setScaleGap(gap: Float) {
        this.scaleGap = gap
        invalidate()
    }

    fun setCurrentUnit(unit: BloodSugarUnit) {
        this.currentUnit = unit
        invalidate()
    }

    interface OnChooseResultListener {
        fun onEndResult(result: String)
        fun onScrollResult(result: String)
    }
}