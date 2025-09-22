package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BloodPressureCategory
import androidx.core.content.withStyledAttributes

/**
 * 血压等级进度条自定义控件
 * 显示血压等级的渐变条和当前位置指示器
 */
class BloodPressureLevelBar @JvmOverloads constructor(
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

    // 当前血压分类
    private var currentCategory: BloodPressureCategory = BloodPressureCategory.NORMAL

    init {
        initAttributes(attrs)
        initPaints()
    }
    
    /**
     * 初始化自定义属性
     */
    private fun initAttributes(attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.BloodPressureLevelBar) {

            // 设置默认值
            barHeight = getDimension(R.styleable.BloodPressureLevelBar_barHeight, dpToPx(10f))
            barCornerRadius =
                getDimension(R.styleable.BloodPressureLevelBar_barCornerRadius, dpToPx(12f))
            indicatorWidth =
                getDimension(R.styleable.BloodPressureLevelBar_bpIndicatorWidth, dpToPx(5f))
            indicatorHeight =
                getDimension(R.styleable.BloodPressureLevelBar_bpIndicatorHeight, dpToPx(18f))
            indicatorFillColor =
                getColor(R.styleable.BloodPressureLevelBar_bpIndicatorFillColor, Color.WHITE)
            indicatorStrokeColor = getColor(
                R.styleable.BloodPressureLevelBar_bpIndicatorStrokeColor,
                ContextCompat.getColor(context, R.color.color_666)
            )
            indicatorStrokeWidth =
                getDimension(R.styleable.BloodPressureLevelBar_bpIndicatorStrokeWidth, dpToPx(1f))
            indicatorCornerRadius =
                getDimension(R.styleable.BloodPressureLevelBar_bpIndicatorCornerRadius, dpToPx(2f))

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

        val width = width.toFloat()
        val height = height.toFloat()

        // 绘制渐变条
        drawGradientBar(canvas, width, height)

        // 绘制当前位置指示器
        drawIndicator(canvas, width, height)
    }

    private fun drawGradientBar(canvas: Canvas, width: Float, height: Float) {
        // 进度条垂直居中
        val barTop = (height - barHeight) / 2
        val barBottom = barTop + barHeight
        val barLeft = paddingHorizontal
        val barRight = width - paddingHorizontal

        // 创建渐变色
        val colors = intArrayOf(
            ContextCompat.getColor(context, R.color.color_3487FC), // 低血压
            ContextCompat.getColor(context, R.color.color_05BA7B), // 正常
            ContextCompat.getColor(context, R.color.color_FFE902), // 偏高
            ContextCompat.getColor(context, R.color.color_FFB909), // 高血压1期
            ContextCompat.getColor(context, R.color.color_FF8000), // 高血压2期
            ContextCompat.getColor(context, R.color.color_FB0301)  // 高血压危象
        )

        val gradient = LinearGradient(
            barLeft, 0f, barRight, 0f,
            colors, null, Shader.TileMode.CLAMP
        )
        gradientPaint.shader = gradient

        val rect = RectF(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, gradientPaint)
    }

    private fun drawIndicator(canvas: Canvas, width: Float, height: Float) {
        val barLeft = paddingHorizontal
        val barRight = width - paddingHorizontal
        val barWidth = barRight - barLeft

        // 根据当前血压分类计算指示器位置
        val position = when (currentCategory) {
            BloodPressureCategory.LOW -> 0.1f
            BloodPressureCategory.NORMAL -> 0.25f
            BloodPressureCategory.ELEVATED -> 0.45f
            BloodPressureCategory.HIGH_STAGE_1 -> 0.65f
            BloodPressureCategory.HIGH_STAGE_2 -> 0.8f
            BloodPressureCategory.HYPERTENSIVE_CRISIS -> 0.95f
            else -> 0.25f
        }

        // 指示器中心X坐标
        val indicatorCenterX = barLeft + barWidth * position
        
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
     * 设置血压分类
     * @param category 血压分类
     */
    fun setCategory(category: BloodPressureCategory) {
        this.currentCategory = category
        invalidate()
    }

    /**
     * 获取当前血压分类
     */
    fun getCurrentCategory(): BloodPressureCategory = currentCategory
    
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
}