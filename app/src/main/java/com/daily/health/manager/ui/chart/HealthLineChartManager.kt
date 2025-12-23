package com.daily.health.manager.ui.chart

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.daily.health.manager.util.AxisStyle
import com.daily.health.manager.util.BaselineStyle
import com.daily.health.manager.util.ChartConfigHelper
import com.daily.health.manager.util.ColumnStyle
import com.daily.health.manager.util.ChartPalette
import com.daily.health.manager.util.LineStyle
import com.patrykandpatrick.vico.core.cartesian.AutoScrollCondition
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.Scroll
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.views.cartesian.CartesianChartView
import com.patrykandpatrick.vico.views.cartesian.ScrollHandler
import com.patrykandpatrick.vico.views.cartesian.ZoomHandler
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.max

/**
 * 将 [ChartUiState] 渲染到 Vico 的 [CartesianChartView] 上。
 *
 * 该 Manager 对业务单位无感：ViewModel 需要在生成 [ChartUiState] 之前完成单位换算与数据筛选。
 *
 * **生命周期管理**：
 * - Manager 会自动绑定到提供的 [LifecycleOwner]
 * - 在 LifecycleOwner.onDestroy() 时自动释放资源
 * - 在 Activity 中使用：create(chartView, this)
 * - 在 Fragment 中使用：create(chartView, viewLifecycleOwner)
 *
 * @param chartView 要渲染的图表视图
 * @param lifecycleOwner 生命周期所有者，用于自动资源管理
 */
class HealthLineChartManager(
    private val chartView: CartesianChartView,
    private val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

    // 优先使用已存在的 modelProducer，避免在配置变更时创建新实例导致崩溃
    private val modelProducer: CartesianChartModelProducer = 
        chartView.modelProducer ?: CartesianChartModelProducer().also {
            chartView.modelProducer = it
        }
    
    private var labels: List<String> = emptyList()

    @Volatile
    private var isReleased = false

    init {
        // modelProducer 已在属性初始化时设置，这里不需要再赋值
        
        // 自动绑定生命周期，避免在已销毁的LifecycleOwner上注册
        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            lifecycleOwner.lifecycle.addObserver(this)
        } else {
            Log.w(TAG, "LifecycleOwner is already destroyed, releasing immediately")
            release()
        }
    }



    /**
     * 渲染最新的图表数据
     * @return 是否存在可绘制的数据
     */
   suspend fun render(state: ChartUiState,isShowLabel : Boolean = true): Boolean {
        if (isReleased) {
            Log.w(TAG, "Attempted to render on released manager, ignoring")
            return false
        }

        labels = state.labels
        val (minY, maxY) = state.computeRange()
        val yFormatter = state.startAxisFormatter
            ?: createStartAxisFormatter(minY, maxY, state.forceIntegerYAxis)

        val axisStyle = AxisStyle(
            bottomAxisValueFormatter = createBottomAxisFormatter(),
            deduplicateBottomLabels = true,
            minY = minY,
            maxY = maxY,
            startAxisValueFormatter = yFormatter,
            labelTextSize = AXIS_LABEL_TEXT_SIZE,
            verticalItemCount = state.axisSteps
        )

        val lineStyles = state.dataSets.mapIndexed { index, dataSet ->
            dataSet.style ?: DEFAULT_LINE_STYLES[index % DEFAULT_LINE_STYLES.size]
        }
        val lineCount = if (lineStyles.isEmpty()) 1 else lineStyles.size
        val highlightX = state.highlightX ?: state.dataSets.lastOrNull()?.xValues?.lastOrNull()?.toDouble()

        chartView.chart = ChartConfigHelper.createLineChart(
            lineStyles = lineStyles,
            axisStyle = axisStyle,
            lastX = highlightX,
            isShowLabel = isShowLabel,
            onInvalidate = {
                chartView.invalidate()
            }
        )

        chartView.scrollHandler = ScrollHandler(
            initialScroll = Scroll.Absolute.End,
            autoScroll = Scroll.Absolute.End,
            autoScrollCondition = AutoScrollCondition.OnModelGrowth
        )

        chartView.zoomHandler = ZoomHandler(
            zoomEnabled = true,                 // 允许用户再缩放就保留 true
            initialZoom = Zoom.x(7.0),          // 初次显示 7 个点
            minZoom = Zoom.x(14.0),              // 最多放大到只看 5 个点
            maxZoom = Zoom.x(5.0),             // 最多缩小到 14 个点
        )

        if (!state.hasData) {
            val placeholder = listOf(0f)
            modelProducer.runTransaction {
                lineSeries {
                    repeat(lineCount) {
                        series(x = placeholder, y = placeholder)
                    }
                }
            }
            return false
        }
        modelProducer.runTransaction {
            lineSeries {
                state.dataSets.forEach { dataSet ->
                    series(x = dataSet.xValues, y = dataSet.yValues)
                }
            }
        }

        return true
    }


    /**
     * 渲染柱状图数据
     * @return 是否存在可绘制的数据
     */
    suspend fun renderColumn(
        state: ChartUiState,
        isShowLabel: Boolean = true,
        enableScroll: Boolean = false,
        showBaseline: Boolean = true,
        columnWidthScale: Float = 1f
    ): Boolean {
        if (isReleased) {
            Log.w(TAG, "Attempted to render on released manager, ignoring")
            return false
        }

        labels = state.labels
        val (minY, maxY) = state.computeRange()
        val yFormatter = state.startAxisFormatter
            ?: createStartAxisFormatter(minY, maxY, state.forceIntegerYAxis)

        val axisStyle = AxisStyle(
            bottomAxisValueFormatter = createBottomAxisFormatter(),
            deduplicateBottomLabels = true,
            minY = minY,
            maxY = maxY,
            startAxisValueFormatter = yFormatter,
            labelTextSize = AXIS_LABEL_TEXT_SIZE,
            verticalItemCount = state.axisSteps
        )
        val baselineStyle = if (showBaseline) {
            state.goalValue
                ?.takeIf { it < maxY }
                ?.let {
                    BaselineStyle(
                        yValue = it,
                        color = ChartPalette.columnSteps,
                        labelText = state.baselineLabel,
                        labelColor = ChartPalette.columnSteps,
                        labelHorizontalPosition = Position.Horizontal.End,
                        labelVerticalPosition = Position.Vertical.Bottom
                    )
                }
        } else {
            null
        }

        val highlightX = state.highlightX ?: state.dataSets.lastOrNull()?.xValues?.lastOrNull()?.toDouble()
        val columnCount = state.dataSets.firstOrNull()?.xValues?.size ?: 0
        val columnStyle = computeColumnStyle(columnCount, enableScroll, columnWidthScale)

        chartView.chart = ChartConfigHelper.createColumnChart(
            columnStyle = columnStyle,
            axisStyle = axisStyle,
            baselineStyle = baselineStyle,
            isShowLabel = isShowLabel,
            lastX = highlightX,
            onInvalidate = {
                chartView.invalidate()
            }
        )
        if (enableScroll) {
            // 获取数据点数量
            val dataPointCount = state.dataSets.firstOrNull()?.xValues?.size ?: 0

            if (dataPointCount <= 7) {
                // 数据点少于等于7个时，禁用滚动和缩放，让数据居中显示并平均分配空间
                chartView.scrollHandler = ScrollHandler(scrollEnabled = false)
                chartView.zoomHandler = ZoomHandler(zoomEnabled = false)
            } else {
                chartView.animateIn = false
                chartView.setAnimationDuration(0L)
                // 数据点多于7个时，启用滚动和缩放
                chartView.scrollHandler = ScrollHandler(
                    initialScroll = Scroll.Absolute.End,
                    autoScroll = Scroll.Absolute.End,
                    autoScrollCondition = AutoScrollCondition.OnModelGrowth
                )
                chartView.zoomHandler = ZoomHandler(
                    zoomEnabled = true,
                    initialZoom = Zoom.x(7.0),
                    minZoom = Zoom.x(14.0),
                    maxZoom = Zoom.x(5.0)
                )
            }
        } else {
            chartView.scrollHandler = ScrollHandler(scrollEnabled = false)
            chartView.zoomHandler = ZoomHandler(zoomEnabled = false)
        }

        if (!state.hasData) {
            val placeholder = listOf(0f)
            modelProducer.runTransaction {
                columnSeries {
                    series(x = placeholder, y = placeholder)
                }
            }
            return false
        }

        modelProducer.runTransaction {
            columnSeries {
                state.dataSets.forEach { dataSet ->
                    series(x = dataSet.xValues, y = dataSet.yValues)
                }
            }
        }

        return true
    }



    /**
     * 根据当前 y 轴范围推导刻度步长，并动态构造匹配的小数格式。
     *
     * Vico 默认 formatter 会把值取整，导致 mmol/L 这类小数单位的刻度被折叠成同一个数字。
     * 这里通过 (max-min)/(steps-1) 估算真实步长，再用阈值选择 0~3 位小数，既保证血压等整数型
     * 数据仍显示为整数，又能在血糖等精度更高的图表上展示细粒度刻度。
     */
    private fun createStartAxisFormatter(
        minY: Double,
        maxY: Double,
        forceInteger: Boolean
    ): CartesianValueFormatter {
        if (forceInteger) {
            return CartesianValueFormatter.decimal(DecimalFormat("#"))
        }

        val steps = (DEFAULT_AXIS_STEPS - 1).coerceAtLeast(1)
        val span = (maxY - minY).takeIf { it > 0 } ?: max(abs(maxY), 1.0)
        val step = span / steps

        val decimalPlaces = when {
            step >= 1 -> 0
            step >= 0.1 -> 1
            step >= 0.01 -> 2
            else -> 3
        }

        val pattern = buildString {
            append("#")
            if (decimalPlaces > 0) {
                append('.')
                repeat(decimalPlaces) { append('#') }
            }
        }

        return CartesianValueFormatter.decimal(DecimalFormat(pattern))
    }

    private fun createBottomAxisFormatter(): CartesianValueFormatter =
        object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?
            ): CharSequence {
                val label = labels.getOrNull(value.toInt())
                return if (label.isNullOrEmpty()) {
                    // Vico 不允许返回空字符串，否则会抛 IllegalStateException
                    // 当没有可显示的数据点时，用短横线占位，避免崩溃。
                    ZERO_DATA_PLACEHOLDER
                } else {
                    label
                }
            }
        }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        release()
        owner.lifecycle.removeObserver(this)
    }

    /**
     * 释放资源（通常由lifecycle自动调用）
     * 此方法是幂等的，可以安全地多次调用
     */
    fun release() {
        if (isReleased) return
        isReleased = true
        labels = emptyList()
    }

    interface Factory {
        fun create(
            chartView: CartesianChartView,
            lifecycleOwner: LifecycleOwner
        ): HealthLineChartManager
    }

    companion object {
        private const val TAG = "HealthLineChartManager"
        private const val DEFAULT_AXIS_STEPS = 6
        private const val ZERO_DATA_PLACEHOLDER = "-"
        private const val AXIS_LABEL_TEXT_SIZE = 12f
        private const val DEFAULT_COLUMN_WIDTH_DP = 18f
        private const val MIN_COLUMN_WIDTH_DP = 8f
        private const val MAX_COLUMN_WIDTH_DP = 28f
        private const val COLUMN_GAP_FACTOR = 1.6f

        private val DEFAULT_LINE_STYLES = listOf(
            LineStyle(color = ChartPalette.lineBpSystolic),
            LineStyle(color = ChartPalette.lineBpDiastolic),
            LineStyle(color = ChartPalette.lineBloodSugar),
            LineStyle(color = ChartPalette.lineHeartRate)
        )
    }

    private fun computeColumnStyle(pointCount: Int, enableScroll: Boolean, scale: Float): ColumnStyle {
        val normalizedScale = if (scale > 0f) scale else 1f
        if (enableScroll || pointCount <= 0) {
            return ColumnStyle(color = ChartPalette.columnSteps, widthDp = DEFAULT_COLUMN_WIDTH_DP * normalizedScale)
        }
        val availableWidthPx = (chartView.width - chartView.paddingStart - chartView.paddingEnd).coerceAtLeast(0)
        if (availableWidthPx <= 0) {
            return ColumnStyle(color = ChartPalette.columnSteps, widthDp = DEFAULT_COLUMN_WIDTH_DP * normalizedScale)
        }
        val density = chartView.resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val rawWidthPx = availableWidthPx / (pointCount * COLUMN_GAP_FACTOR)
        val boundedWidthDp = (rawWidthPx / density).coerceIn(MIN_COLUMN_WIDTH_DP, MAX_COLUMN_WIDTH_DP) * normalizedScale
        return ColumnStyle(color = ChartPalette.columnSteps, widthDp = boundedWidthDp)
    }
}
