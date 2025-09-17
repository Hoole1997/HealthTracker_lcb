package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.enum.BloodSugarLevel
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit

class BloodSugarRangeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentValue: Float = 0f
    private var currentUnit: BsUnit = BsUnit.MMOL_L
    private var currentStatus: BloodSugarStatus = BloodSugarStatus.DEFAULT

    // 可配置的属性
    private var itemHeight: Float = dpToPx(40f)
    private var itemSpacing: Float = dpToPx(6f)
    private var dotRadius: Float = dpToPx(12f)
    private var itemPaddingHorizontal: Float = dpToPx(16f)
    private var itemPaddingVertical: Float = dpToPx(8f)
    private var cornerRadius: Float = dpToPx(40f)
    private var rangeTextSize: Float = spToPx(16f)
    private var dotMarginEnd: Float = dpToPx(12f)
    private var rangeTextStyle: Int = 1 // 0: normal, 1: bold, 2: italic

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempRect = RectF()

    init {
        initAttrs(attrs)
        setupTextPaint()
    }

    private fun initAttrs(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.BloodSugarRangeView)
            try {
                itemHeight = typedArray.getDimension(R.styleable.BloodSugarRangeView_itemHeight, dpToPx(40f))
                itemSpacing = typedArray.getDimension(R.styleable.BloodSugarRangeView_itemSpacing, dpToPx(6f))
                dotRadius = typedArray.getDimension(R.styleable.BloodSugarRangeView_dotRadius, dpToPx(12f))
                itemPaddingHorizontal = typedArray.getDimension(R.styleable.BloodSugarRangeView_itemPaddingHorizontal, dpToPx(16f))
                itemPaddingVertical = typedArray.getDimension(R.styleable.BloodSugarRangeView_itemPaddingVertical, dpToPx(8f))
                cornerRadius = typedArray.getDimension(R.styleable.BloodSugarRangeView_cornerRadius, dpToPx(40f))
                rangeTextSize = typedArray.getDimension(R.styleable.BloodSugarRangeView_rangeTextSize, spToPx(16f))
                dotMarginEnd = typedArray.getDimension(R.styleable.BloodSugarRangeView_dotMarginEnd, dpToPx(12f))
                rangeTextStyle = typedArray.getInt(R.styleable.BloodSugarRangeView_rangeTextStyle, 1)
            } finally {
                typedArray.recycle()
            }
        }
    }

    private fun setupTextPaint() {
        textPaint.textSize = rangeTextSize
        when (rangeTextStyle) {
            0 -> textPaint.typeface = Typeface.DEFAULT
            1 -> textPaint.typeface = Typeface.DEFAULT_BOLD
            2 -> textPaint.typeface = Typeface.defaultFromStyle(Typeface.ITALIC)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val height = (itemHeight * 4 + itemSpacing * 3).toInt()
        setMeasuredDimension(
            MeasureSpec.getSize(widthMeasureSpec),
            height
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val ranges = currentStatus.getRangesForUnit(currentUnit)
        val currentLevel = currentStatus.getBloodSugarLevel(currentValue, currentUnit)

        val levels = listOf(
            BloodSugarLevel.LOW,
            BloodSugarLevel.NORMAL,
            BloodSugarLevel.PREDIABETES,
            BloodSugarLevel.DIABETES
        )

        var top = 0f

        levels.forEach { level ->
            val isSelected = level == currentLevel

            // 绘制背景
            tempRect.set(0f, top, width.toFloat(), top + itemHeight)
            backgroundPaint.color = if (isSelected) {
                ContextCompat.getColor(context, level.colorRes)
            } else {
                ContextCompat.getColor(context, com.healthtracker.framework.R.color.transparent)
            }
            canvas.drawRoundRect(tempRect, cornerRadius, cornerRadius, backgroundPaint)

            // 绘制圆点
            val dotCenterX = dotRadius + itemPaddingHorizontal
            val dotCenterY = top + itemHeight / 2
            dotPaint.color = if (isSelected) {
                ContextCompat.getColor(context, android.R.color.white)
            } else {
                ContextCompat.getColor(context, level.colorRes)
            }
            canvas.drawCircle(dotCenterX, dotCenterY, dotRadius, dotPaint)

            // 绘制等级名称（左侧）
            val levelTextX = dotCenterX + dotRadius + dotMarginEnd
            val levelTextY = top + itemHeight / 2 + getTextCenterOffsetY()
            textPaint.color = if (isSelected) {
                ContextCompat.getColor(context, android.R.color.white)
            } else {
                ContextCompat.getColor(context, R.color.t1)
            }

            val levelText = getLevelDisplayText(level)
            canvas.drawText(levelText, levelTextX, levelTextY, textPaint)

            // 绘制范围值（右侧）
            val rangeText = getRangeDisplayText(level, ranges)
            val rangeTextWidth = textPaint.measureText(rangeText)
            val rangeTextX = width - itemPaddingHorizontal - rangeTextWidth
            canvas.drawText(rangeText, rangeTextX, levelTextY, textPaint)

            top += itemHeight + itemSpacing
        }
    }

    private fun getLevelDisplayText(level: BloodSugarLevel): String {
        val stringRes = getLevelStringRes(level.level)
        return context.getString(stringRes)
    }

    private fun getLevelStringRes(level: Int): Int {
        return when (level) {
            0 -> R.string.blood_sugar_level_low
            1 -> R.string.blood_sugar_level_normal
            2 -> R.string.blood_sugar_level_prediabetes
            3 -> R.string.blood_sugar_level_diabetes
            else -> R.string.blood_sugar_level_normal
        }
    }

    private fun getRangeDisplayText(level: BloodSugarLevel, ranges: com.healthtracker.blood.suger.enum.BloodSugarRanges): String {
        // 只显示数值范围，不显示单位
        return when (level) {
            BloodSugarLevel.LOW -> "< ${formatValue(ranges.lowHigh)}"
            BloodSugarLevel.NORMAL -> "${formatValue(ranges.normalLow)}~${formatValue(ranges.normalHigh)}"
            BloodSugarLevel.PREDIABETES -> "${formatValue(ranges.prediabetesLow)}~${formatValue(ranges.prediabetesHigh)}"
            BloodSugarLevel.DIABETES -> "≥ ${formatValue(ranges.diabetesLow)}"
        }
    }

    private fun formatValue(value: Float): String {
        return BsUnit.formatValue(value, currentUnit)
    }

    private fun getTextCenterOffsetY(): Float {
        val fontMetrics = textPaint.fontMetrics
        return (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        )
    }

    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
    }

    fun updateValue(value: Float) {
        if (this.currentValue != value) {
            this.currentValue = value
            invalidate()
        }
    }

    fun updateUnit(unit: BsUnit) {
        if (this.currentUnit != unit) {
            this.currentUnit = unit
            invalidate()
        }
    }

    fun updateStatus(status: BloodSugarStatus) {
        if (this.currentStatus != status) {
            this.currentStatus = status
            invalidate()
        }
    }

    fun setCurrentState(value: Float, unit: BsUnit, status: BloodSugarStatus) {
        var needsRedraw = false

        if (this.currentValue != value) {
            this.currentValue = value
            needsRedraw = true
        }

        if (this.currentUnit != unit) {
            this.currentUnit = unit
            needsRedraw = true
        }

        if (this.currentStatus != status) {
            this.currentStatus = status
            needsRedraw = true
        }

        if (needsRedraw) {
            invalidate()
        }
    }
}