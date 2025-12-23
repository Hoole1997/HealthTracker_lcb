package com.daily.health.manager.ui.weight

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.daily.health.manager.R
import androidx.core.content.withStyledAttributes

/**
 * 通用等级进度条自定义控件
 * 支持任意实现了LevelCategory接口的等级类型
 * @param T 等级类型，必须实现LevelCategory接口
 */
open class GenericLevelBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 自定义属性
    private var barHeight: Float = 0f
    private var barCornerRadius: Float = 0f
    private var indicatorWidth: Float = 0f
    private var indicatorHeight: Float = 0f
    private var indicatorFillColor: Int = 0
    private var indicatorStrokeColor: Int = 0
    private var indicatorStrokeWidth: Float = 0f
    private var indicatorCornerRadius: Float = 0f

    private val paddingHorizontal = 0f
    private val paddingVertical = dpToPx(4f)

    // 画笔
    private val gradientPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 通用模式：仅使用颜色数组与索引来展示
    private var gradientColors: IntArray? = null
    private var indicatorIndex: Int? = null

    init {
        initAttributes(attrs)
        initPaints()
    }

    /**
     * 初始化自定义属性
     */
    private fun initAttributes(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.LevelBar) {

            // 设置默认值（统一 LevelBar 命名）
            barHeight = getDimension(R.styleable.LevelBar_barHeight, dpToPx(10f))
            barCornerRadius =
                getDimension(R.styleable.LevelBar_barCornerRadius, dpToPx(12f))
            indicatorWidth =
                getDimension(R.styleable.LevelBar_indicatorWidth, dpToPx(5f))
            indicatorHeight =
                getDimension(R.styleable.LevelBar_indicatorHeight, dpToPx(18f))
            indicatorFillColor =
                getColor(R.styleable.LevelBar_indicatorFillColor, Color.WHITE)
            indicatorStrokeColor = getColor(
                R.styleable.LevelBar_indicatorStrokeColor,
                ContextCompat.getColor(context, R.color.color_666)
            )
            indicatorStrokeWidth =
                getDimension(R.styleable.LevelBar_indicatorStrokeWidth, dpToPx(1f))
            indicatorCornerRadius =
                getDimension(R.styleable.LevelBar_indicatorCornerRadius, dpToPx(2f))

        }
    }

    /**
     * dp转px工具方法
     */
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    private fun initPaints() {
        // 渐变条画笔
        gradientPaint.style = Paint.Style.FILL

        // 指示器填充画笔
        indicatorFillPaint.apply {
            style = Paint.Style.FILL
            color = indicatorFillColor
        }

        // 指示器描边画笔
        indicatorStrokePaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = indicatorStrokeWidth
            color = indicatorStrokeColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = MeasureSpec.getSize(widthMeasureSpec)
        // 控件高度为进度条和指示器高度的最大值加上内边距
        val desiredHeight = (maxOf(barHeight, indicatorHeight) + paddingVertical * 2).toInt()

        val width = resolveSize(desiredWidth, widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 若颜色数组为空，则不绘制
        val hasColors = gradientColors?.isNotEmpty() == true
        if (!hasColors) return

        val width = width.toFloat()
        val height = height.toFloat()

        // 绘制渐变条
        drawGradientBar(canvas, width, height)

        // 绘制当前位置指示器
        drawIndicator(canvas, width, height)
    }

    private fun drawGradientBar(canvas: Canvas, width: Float, height: Float) {
        val colorsArray = gradientColors
        if (colorsArray == null || colorsArray.isEmpty()) return

        // 进度条垂直居中
        val barTop = (height - barHeight) / 2
        val barBottom = barTop + barHeight
        val barLeft = paddingHorizontal
        val barRight = width - paddingHorizontal

        // 创建渐变色数组
        val colors: IntArray = colorsArray

        val gradient = LinearGradient(
            barLeft, 0f, barRight, 0f,
            colors, null, Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradient

        val rect = RectF(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, gradientPaint)
    }

    private fun drawIndicator(canvas: Canvas, width: Float, height: Float) {
        val colorsArray = gradientColors
        // 计算位置：基于索引在颜色数组上的线性位置
        if (colorsArray == null || colorsArray.isEmpty() || indicatorIndex == null) return
        val count = colorsArray.size
        // 将指示器绘制在当前等级区段的中心位置： (index + 0.5) / count
        val clampedIndex = indicatorIndex!!.coerceIn(0, count - 1)
        val fraction: Float = if (count <= 0) 0f else (clampedIndex + 0.5f) / count

        val barLeft = paddingHorizontal
        val barRight = width - paddingHorizontal
        val barWidth = barRight - barLeft

        // 指示器中心X坐标
        val indicatorCenterX = barLeft + barWidth * fraction

        // 指示器垂直居中
        val indicatorTop = (height - indicatorHeight) / 2
        val indicatorBottom = indicatorTop + indicatorHeight
        val indicatorLeft = indicatorCenterX - indicatorWidth / 2
        val indicatorRight = indicatorCenterX + indicatorWidth / 2

        val indicatorRect = RectF(indicatorLeft, indicatorTop, indicatorRight, indicatorBottom)

        // 绘制指示器填充
        canvas.drawRoundRect(indicatorRect, indicatorCornerRadius, indicatorCornerRadius, indicatorFillPaint)

        // 绘制指示器描边
        canvas.drawRoundRect(indicatorRect, indicatorCornerRadius, indicatorCornerRadius, indicatorStrokePaint)
    }

    /**
     * 动态设置指示器填充颜色
     */
    fun setIndicatorFillColor(color: Int) {
        this.indicatorFillColor = color
        indicatorFillPaint.color = color
        invalidate()
    }

    /**
     * 动态设置指示器描边颜色
     */
    fun setIndicatorStrokeColor(color: Int) {
        this.indicatorStrokeColor = color
        indicatorStrokePaint.color = color
        invalidate()
    }

    /**
     * 设置通用颜色数组（直接使用颜色值）
     */
    fun setColors(colors: IntArray) {
        this.gradientColors = colors
        invalidate()
    }

    /**
     * 设置颜色资源ID数组（会转换为颜色值）
     */
    fun setColorResArray(colorResIds: IntArray) {
        val colors = colorResIds.map { id -> ContextCompat.getColor(context, id) }.toIntArray()
        setColors(colors)
    }

    /**
     * 设置指示器索引（基于颜色数组的索引位置）
     */
    fun setIndicatorIndex(index: Int) {
        this.indicatorIndex = index
        invalidate()
    }
}