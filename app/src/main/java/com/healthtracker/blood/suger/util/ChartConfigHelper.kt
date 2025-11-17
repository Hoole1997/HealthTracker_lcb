package com.healthtracker.blood.suger.util

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.graphics.toColorInt
import com.healthtracker.blood.suger.App
import com.healthtracker.framework.util.getRobotoBold
import com.patrykandpatrick.vico.core.cartesian.CartesianChart
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.core.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.core.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.core.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.core.cartesian.marker.Interaction
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.Insets
import com.patrykandpatrick.vico.core.common.Position
import com.patrykandpatrick.vico.core.common.component.Component
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.component.Shadow
import com.patrykandpatrick.vico.core.common.component.ShapeComponent
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.CorneredShape.Corner
import com.patrykandpatrick.vico.core.common.shape.DashedShape
import com.patrykandpatrick.vico.core.common.shape.MarkerCorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow


// 默认颜色
private val DEFAULT_LINE1_COLOR = ChartPalette.lineBpSystolic
private val DEFAULT_LINE2_COLOR = ChartPalette.lineBpDiastolic
private val DEFAULT_GRID_COLOR = ChartPalette.grid
private val DEFAULT_LABEL_COLOR = ChartPalette.axisLabel
private val DEFAULT_BASELINE_COLOR = ChartPalette.grid
private val WHITE_COLOR = ChartPalette.pointStroke

// 默认尺寸
private const val LINE_THICKNESS = 1f
private const val POINT_SIZE = 8f
private const val POINT_SPACING = 24f
private const val POINT_STROKE = 1f
private const val COLUMN_WIDTH = 18f
private const val COLUMN_TOP_CORNER_RADIUS = 12f
private const val COLUMN_BOTTOM_CORNER_RADIUS = 0f
private const val COLUMN_MARKER_OFFSET_DP = 4f
private const val GRID_THICKNESS = 1f
private const val BASELINE_THICKNESS = 1f

// 默认虚线配置
private const val DASH_LENGTH = 4f
private const val GAP_LENGTH = 4f
private const val BASELINE_DASH_LENGTH = 4f
private const val BASELINE_GAP_LENGTH = 4f

// 默认文本配置
private const val LABEL_TEXT_SIZE = 12f
private const val LABEL_PADDING = 8f

// 图层配置
private const val LAYER_PADDING = 8f
private const val CURVATURE = 0.5f

// 坐标轴配置
private const val VERTICAL_AXIS_ITEM_COUNT = 6
private const val HORIZONTAL_LABEL_SPACING = 1


/**
 * 图表配置工具类
 *
 * 提供可复用的图表配置方法，支持自定义颜色和样式参数
 *
 * 使用示例：
 * ```kotlin
 * val chartConfig = ChartConfigHelper()
 * val chart = chartConfig.createDualLineChart(
 *     line1Color = "#FF6B4D",
 *     line2Color = "#4AD7FF"
 * )
 * chartView.chart = chart
 * ```
 */

class ChartConfigHelper {

    companion object {


        private val tapOnlyMarkerController = object : CartesianMarkerController {
            override fun shouldAcceptInteraction(
                interaction: Interaction,
                targets: List<CartesianMarker.Target>,
            ): Boolean = interaction is Interaction.Tap || interaction is Interaction.LongPress

            override fun shouldShowMarker(
                interaction: Interaction,
                targets: List<CartesianMarker.Target>,
            ): Boolean = true      // 长按/点击后显示
        }

        val interactiveMarker = ChartConfigHelper.getDefaultMark()
        val defaultMarker = DefaultMarker(interactiveMarker)
        private val columnInteractiveMarker = ColumnMarker()
        private val defaultColumnMarker = DefaultMarker(ColumnMarker())

        /**
         * 创建任意条折线的通用图表
         */
        fun createLineChart(
            lineStyles: List<LineStyle>,
            axisStyle: AxisStyle = AxisStyle(),
            baselineStyle: BaselineStyle? = null,
            layerPaddingDp: Float = LAYER_PADDING,
            pointSpacingDp: Float = POINT_SPACING,
            marker: CartesianMarker? = interactiveMarker,
            lastX : Double? = null,
            isShowLabel: Boolean = true,
            onInvalidate : (() -> Unit)? = null
        ): CartesianChart {
            val styles = if (lineStyles.isEmpty()) listOf(LineStyle(color = DEFAULT_LINE1_COLOR)) else lineStyles

            val lineLayer = createLineLayer(
                lineStyles = styles,
                spacingDp = pointSpacingDp,
                axisStyle = axisStyle
            )

            val startAxis = createStartAxis(axisStyle,isShowLabel)
            val bottomAxis = createBottomAxis(axisStyle,isShowLabel)

            val decorations = mutableListOf<HorizontalLine>()
            baselineStyle?.let { decorations.add(createBaseline(it)) }
            axisStyle.zeroBaselineStyle()?.let { decorations.add(createBaseline(it)) }

            val shouldShowPersistentMarker = isShowLabel && lastX != null
            // 统一处理默认高亮逻辑：满足条件时直接展示持久化 Marker

            return CartesianChart(
                lineLayer,
                startAxis = startAxis,
                bottomAxis = bottomAxis,
                layerPadding = {
                    CartesianLayerPadding(
                        scalableStartDp = layerPaddingDp,
                        scalableEndDp = layerPaddingDp
                    )
                },
                decorations = decorations,
                marker = if(isShowLabel) marker else null,
                markerController = tapOnlyMarkerController,
            ).copy(
                persistentMarkers = {
                    if (isShowLabel) {
                        lastX?.let {
                            defaultMarker at it
                        }
                    }
                    defaultMarker.visible = shouldShowPersistentMarker
                },
                markerVisibilityListener = object : CartesianMarkerVisibilityListener{
                    override fun onShown(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        update(targets)
                    }

                    override fun onHidden(marker: CartesianMarker) {
                        defaultMarker.visible = shouldShowPersistentMarker
                    }
                    override fun onUpdated(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        update(targets)
                    }
                    fun update(targets: List<CartesianMarker.Target>) {
                        if (!shouldShowPersistentMarker) return
                        defaultMarker.visible = targets.any { it.x == lastX }
                        onInvalidate?.invoke()    // 直接重绘即可，persistentMarkers 不需要再执行
                    }


                }
            )
        }

        /**
         * 创建柱状图
         */
        fun createColumnChart(
            columnStyle: ColumnStyle = ColumnStyle(color = ChartPalette.columnSteps),
            axisStyle: AxisStyle = AxisStyle(),
            baselineStyle: BaselineStyle? = null,
            layerPaddingDp: Float = LAYER_PADDING,
            isShowLabel: Boolean = true,
            marker: CartesianMarker? = columnInteractiveMarker,
            lastX : Double? = null,
            persistentMarker: DefaultMarker = defaultColumnMarker,
            onInvalidate : (() -> Unit)? = null
        ): CartesianChart {
            val columnLayer = createColumnLayer(columnStyle, axisStyle)
            val startAxis = createStartAxis(axisStyle, isShowLabel)
            val bottomAxis = createBottomAxis(axisStyle, isShowLabel)
            val decorations = mutableListOf<HorizontalLine>()
            baselineStyle?.let { decorations.add(createBaseline(it)) }
            axisStyle.zeroBaselineStyle()?.let { decorations.add(createBaseline(it)) }
            val shouldShowPersistentMarker = isShowLabel && lastX != null
            // 柱状图同样依赖该标记来控制默认高亮

            return CartesianChart(
                columnLayer,
                startAxis = startAxis,
                bottomAxis = bottomAxis,
                layerPadding = {
                    CartesianLayerPadding(
                        scalableStartDp = layerPaddingDp,
                        scalableEndDp = layerPaddingDp
                    )
                },
                marker = if(isShowLabel) marker else null,
                markerController = tapOnlyMarkerController,
                decorations = decorations,
            ).copy(
                persistentMarkers = {
                    if (isShowLabel) {
                        lastX?.let {
                            persistentMarker at it
                        }
                    }
                    persistentMarker.visible = shouldShowPersistentMarker
                },
                markerVisibilityListener = object : CartesianMarkerVisibilityListener{
                    override fun onShown(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        update(targets)
                    }

                    override fun onHidden(marker: CartesianMarker) {
                        persistentMarker.visible = shouldShowPersistentMarker
                    }
                    override fun onUpdated(
                        marker: CartesianMarker,
                        targets: List<CartesianMarker.Target>
                    ) {
                        update(targets)
                    }
                    fun update(targets: List<CartesianMarker.Target>) {
                        if (!shouldShowPersistentMarker) return
                        persistentMarker.visible = targets.any { it.x == lastX }
                        onInvalidate?.invoke()
                    }


                }
            )
        }

        // ==================== 内部实现方法 ====================

        private fun createLineLayer(
            lineStyles: List<LineStyle>,
            spacingDp: Float,
            axisStyle: AxisStyle
        ): LineCartesianLayer {
            val lines = lineStyles.map { createLine(it) }
            return LineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(lines),
                pointSpacingDp = spacingDp,
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minY = axisStyle.minY,
                    maxY = axisStyle.maxY
                )
            )
        }

        /**
         * 创建单条折线
         */
        private fun createLine(style: LineStyle): LineCartesianLayer.Line {
            return LineCartesianLayer.Line(
                fill = LineCartesianLayer.LineFill.single(Fill(style.color)),
                stroke = LineCartesianLayer.LineStroke.Continuous(
                    thicknessDp = style.thickness,
                    cap = Paint.Cap.ROUND
                ),
                areaFill = null,
                pointProvider = LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        component = createPointComponent(style),
                        sizeDp = style.pointSize
                    )
                ),
                pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = style.curvature),
                dataLabel = null,
                dataLabelPosition = Position.Vertical.Top,
                dataLabelRotationDegrees = 0f
            )
        }

        /**
         * 创建数据点组件
         */
        private fun createPointComponent(style: LineStyle): Component {
            return ShapeComponent(
                fill = Fill(style.color),
                shape = CorneredShape.Pill,
                strokeFill = Fill(style.pointStrokeColor),
                strokeThicknessDp = style.pointStrokeThickness
            )
        }

        /**
         * 创建左侧坐标轴
         */
        private fun createStartAxis(style: AxisStyle, isShowLabel: Boolean = true): VerticalAxis<Axis.Position.Vertical.Start> {
            return VerticalAxis.start(
                line = null,
                label = TextComponent(
                    color = style.labelColor,
                    textSizeSp = if(isShowLabel) style.labelTextSize else 0f,
                    padding = Insets(endDp = LABEL_PADDING)
                ),
                tick = null,
                guideline = if (style.showGridLines) {
                    LineComponent(
                        fill = Fill(style.gridLineColor),
                        thicknessDp = GRID_THICKNESS,
                        shape = DashedShape(
                            shape = Shape.Rectangle,
                            dashLengthDp = style.gridLineDashLength,
                            gapLengthDp = style.gridLineGapLength
                        )
                    )
                } else {
                    null
                },
                valueFormatter = style.startAxisValueFormatter,
                itemPlacer = VerticalAxis.ItemPlacer.count(count = { style.verticalItemCount }),
                horizontalLabelPosition = VerticalAxis.HorizontalLabelPosition.Outside,
                verticalLabelPosition = Position.Vertical.Center
            )
        }

        /**
         * 创建底部坐标轴
         */
        private fun createBottomAxis(style: AxisStyle, isShowLabel: Boolean = true): HorizontalAxis<Axis.Position.Horizontal.Bottom> {

            // 获取基础 formatter
            val baseFormatter = style.bottomAxisValueFormatter
                ?: CartesianValueFormatter.decimal()

            // 如果需要去重，包装 formatter
            val finalFormatter = if (style.deduplicateBottomLabels) {
                DeduplicatingValueFormatter(baseFormatter)
            } else {
                baseFormatter
            }

            return HorizontalAxis.bottom(
                line = null,
                label = TextComponent(
                    color = style.labelColor,
                    textSizeSp = if(isShowLabel) style.labelTextSize else 0f,
                    padding = Insets(topDp = LABEL_PADDING)
                ),
                tick = null,
                guideline = null,
                valueFormatter = finalFormatter,
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(
                    spacing = { HORIZONTAL_LABEL_SPACING },
                    offset = { 0 }
                )
            )
        }

        /**
         * 创建基准线
         */
        private fun createBaseline(style: BaselineStyle): HorizontalLine {
            val labelComponent = style.labelText?.let {
                TextComponent(
                    color = style.labelColor,
                    textSizeSp = style.labelTextSize,
                    padding = Insets(endDp = LABEL_PADDING)
                )
            }
            return HorizontalLine(
                y = { style.yValue },
                line = LineComponent(
                    fill = Fill(style.color),
                    thicknessDp = style.thickness,
                    shape = DashedShape(
                        shape = Shape.Rectangle,
                        dashLengthDp = style.dashLength,
                        gapLengthDp = style.gapLength
                    )
                ),
                labelComponent = labelComponent,
                label = { style.labelText ?: HorizontalLine.getLabel(style.yValue) },
                horizontalLabelPosition = style.labelHorizontalPosition,
                verticalLabelPosition = style.labelVerticalPosition,
                verticalAxisPosition = Axis.Position.Vertical.Start
            )
        }

        /**
         * 创建柱状图层
         */
        private fun createColumnLayer(
            style: ColumnStyle,
            axisStyle: AxisStyle,
        ): ColumnCartesianLayer {
            val columnComponent = LineComponent(
                fill = Fill(style.color),
                thicknessDp = style.widthDp,
                shape = CorneredShape.rounded(
                    topLeftDp = style.topCornerRadiusDp,
                    topRightDp = style.topCornerRadiusDp,
                    bottomLeftDp = style.bottomCornerRadiusDp,
                    bottomRightDp = style.bottomCornerRadiusDp
                )
            )
            return ColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(columnComponent),
                rangeProvider = CartesianLayerRangeProvider.fixed(
                    minY = axisStyle.minY,
                    maxY = axisStyle.maxY
                )
            )
        }

        fun getDefaultMark() =
            DefaultCartesianMarker(
                label = createMarkerLabelComponent(),
                valueFormatter = DefaultCartesianMarker.ValueFormatter
                    .default(colorCode = true),   // 关键：按线条颜色渲染文字
                indicator = { color ->
                    ShapeComponent(
                        fill = Fill(Color.WHITE),
                        strokeFill = Fill(color),
                        strokeThicknessDp = 4f,
                        shape = CorneredShape.Pill
                    )
                },
                guideline = createMarkerGuideline()
            )

        fun computeNiceRange(
            series: List<List<Double>>,
            axisSteps: Int = VERTICAL_AXIS_ITEM_COUNT,
            paddingRatio: Double = 0.1, // 上下各保留 10% 留白
            minLimit: Double? = 0.0
        ): Pair<Double, Double> {
            require(axisSteps >= 2) { "轴刻度数量至少为 2" }

            val rawMin = series.minOfOrNull { it.minOrNull() ?: Double.POSITIVE_INFINITY }
                ?: Double.POSITIVE_INFINITY
            val rawMax = series.maxOfOrNull { it.maxOrNull() ?: Double.NEGATIVE_INFINITY }
                ?: Double.NEGATIVE_INFINITY
            if (rawMin == Double.POSITIVE_INFINITY || rawMax == Double.NEGATIVE_INFINITY) {
                return 0.0 to 1.0 // 没有数据时的兜底
            }

            val span = max(rawMax - rawMin, 1.0)
            val padding = max(span * paddingRatio, 1.0)
            val targetMin = rawMin - padding
            val targetMax = rawMax + padding

            val step = niceStep((targetMax - targetMin) / (axisSteps - 1))
            var minAligned = floor(targetMin / step) * step
            val maxAligned = ceil(targetMax / step) * step

            if (minLimit != null && minAligned < minLimit) {
                minAligned = 0.0
            }

            return minAligned to maxAligned
        }

        private fun niceStep(rawStep: Double): Double {
            val base = 10.0.pow(floor(log10(rawStep)))
            val scaled = rawStep / base
            val nice = when {
                scaled < 1.5 -> 1.0
                scaled < 3.0 -> 2.0
                scaled < 7.0 -> 5.0
                else -> 10.0
            }
            return nice * base
        }
    }




}


/**
 * 线条样式配置
 *
 * @param color 线条颜色（十六进制字符串，如 "#FF6B4D"）
 * @param thickness 线条粗细（dp）
 * @param pointSize 数据点大小（dp）
 * @param pointStrokeColor 数据点边框颜色
 * @param pointStrokeThickness 数据点边框粗细（dp）
 * @param curvature 曲线平滑度（0.0-1.0）
 */
data class LineStyle(
    @ColorInt val color: Int,
    val thickness: Float = LINE_THICKNESS,
    val pointSize: Float = POINT_SIZE,
    @ColorInt val pointStrokeColor: Int = WHITE_COLOR,
    val pointStrokeThickness: Float = POINT_STROKE,
    val curvature: Float = CURVATURE
)

/**
 * 坐标轴样式配置
 *
 * @param showGridLines 是否显示网格线
 * @param gridLineColor 网格线颜色
 * @param gridLineDashLength 网格线虚线段长度
 * @param gridLineGapLength 网格线虚线间隙
 * @param labelColor 标签颜色
 * @param labelTextSize 标签文字大小（sp）
 * @param verticalItemCount 垂直轴刻度数量
 * @param bottomAxisValueFormatter 底部轴值格式化器，为null时使用默认数字格式
 * @param deduplicateBottomLabels 是否去重底部标签（相同的值只在第一次出现时显示）
 */
data class AxisStyle(
    val showGridLines: Boolean = true,
    @ColorInt val gridLineColor: Int = DEFAULT_GRID_COLOR,
    val gridLineDashLength: Float = DASH_LENGTH,
    val gridLineGapLength: Float = GAP_LENGTH,
    @ColorInt val labelColor: Int = DEFAULT_LABEL_COLOR,
    val labelTextSize: Float = LABEL_TEXT_SIZE,
    val verticalItemCount: Int = VERTICAL_AXIS_ITEM_COUNT,
    val startAxisValueFormatter: CartesianValueFormatter = CartesianValueFormatter.decimal(),
    val bottomAxisValueFormatter: CartesianValueFormatter? = null,
    val deduplicateBottomLabels: Boolean = false,  // 新增：是否去重底部标签
    val minY: Double? = null,
    val maxY: Double? = null,

)

/**
 * 基准线样式配置
 *
 * @param yValue Y 轴位置
 * @param color 基准线颜色
 * @param thickness 基准线粗细
 * @param dashLength 虚线段长度
 * @param gapLength 虚线间隙
 */
data class BaselineStyle(
    val yValue: Double = 0.0,
    @ColorInt val color: Int = DEFAULT_BASELINE_COLOR,
    val thickness: Float = BASELINE_THICKNESS,
    val dashLength: Float = BASELINE_DASH_LENGTH,
    val gapLength: Float = BASELINE_GAP_LENGTH,
    val labelText: CharSequence? = null,
    @ColorInt val labelColor: Int = DEFAULT_LABEL_COLOR,
    val labelTextSize: Float = LABEL_TEXT_SIZE,
    val labelHorizontalPosition: Position.Horizontal = Position.Horizontal.End,
    val labelVerticalPosition: Position.Vertical = Position.Vertical.Top
)

private fun AxisStyle.zeroBaselineStyle(): BaselineStyle? {
    val min = minY ?: return null
    val max = maxY ?: return null
    if (min > 0.0 || max < 0.0) return null
    return BaselineStyle(
        yValue = 0.0,
        color = gridLineColor,
        thickness = GRID_THICKNESS,
        dashLength = DASH_LENGTH,
        gapLength = GAP_LENGTH
    )
}

/**
 * 柱状图样式配置
 *
 * @param color 柱体颜色
 * @param widthDp 柱体宽度
 * @param cornerRadiusDp 圆角半径
 */
data class ColumnStyle(
    @ColorInt val color: Int,
    val widthDp: Float = COLUMN_WIDTH,
    val topCornerRadiusDp: Float = COLUMN_TOP_CORNER_RADIUS,
    val bottomCornerRadiusDp: Float = COLUMN_BOTTOM_CORNER_RADIUS
)

/**
 * 去重 ValueFormatter - 包装现有的 formatter，相同的值只在第一次出现时显示
 */
private class DeduplicatingValueFormatter(
    private val baseFormatter: CartesianValueFormatter
) : CartesianValueFormatter {

    private val cache = mutableMapOf<Int, String>()

    override fun format(
        context: CartesianMeasuringContext,
        value: Double,
        verticalAxisPosition: Axis.Position.Vertical?
    ): CharSequence {
        val index = value.toInt()

        // 获取当前标签
        val currentLabel = baseFormatter.format(context, value, verticalAxisPosition).toString()

        // 缓存当前标签
        cache[index] = currentLabel

        // 检查前一个标签是否相同
        val previousLabel = if (index > 0) {
            cache[index - 1]
        } else {
            null
        }

        // 如果和前一个标签相同，返回空格（隐藏）
        return if (currentLabel == previousLabel && currentLabel.isNotBlank()) {
            " "  // 空格占位符
        } else {
            currentLabel
        }
    }
}

class DefaultMarker(
    private val base: CartesianMarker,
) : CartesianMarker by base {
    var visible: Boolean = true

    override fun drawUnderLayers(context: CartesianDrawingContext, targets: List<CartesianMarker.Target>) {
        if (visible) base.drawUnderLayers(context, targets)
    }

    override fun drawOverLayers(context: CartesianDrawingContext, targets: List<CartesianMarker.Target>) {
        if (visible) base.drawOverLayers(context, targets)
    }
}

private class ColumnMarker : DefaultCartesianMarker(
    label = createMarkerLabelComponent(),
    valueFormatter = DefaultCartesianMarker.ValueFormatter.default(colorCode = true),
    indicator = null,
    guideline = createMarkerGuideline()
) {
    override fun drawOverLayers(
        context: CartesianDrawingContext,
        targets: List<CartesianMarker.Target>
    ) {
        context.drawColumnGuideline(targets)
        drawLabel(context, targets)
    }

    private fun CartesianDrawingContext.drawColumnGuideline(
        targets: List<CartesianMarker.Target>
    ) {
        val component = guideline ?: return
        val offsetPx = COLUMN_MARKER_OFFSET_DP * density
        targets.forEach { target ->
            if (target is ColumnCartesianLayerMarkerTarget) {
                val columnTop = target.columns.minOf { it.canvasY }
                val bottom = (columnTop - offsetPx).coerceAtLeast(layerBounds.top)
                component.drawVertical(this, target.canvasX, layerBounds.top, bottom)
            } else {
                component.drawVertical(this, target.canvasX, layerBounds.top, layerBounds.bottom)
            }
        }
    }
}

private fun createMarkerLabelComponent() =
    TextComponent(
        color = Color.WHITE,
        typeface = getRobotoBold(App.INSTANCE) ?: Typeface.DEFAULT,
        textSizeSp = 12f,
        padding = Insets(8f, 4f),
        background = ShapeComponent(
            fill = Fill("#E6FFFFFF".toColorInt()),
            shape = MarkerCorneredShape(Corner.Rounded),
            shadow = Shadow(radiusDp = 4f, color = 0x33000000, xDp = 0f, yDp = 2f)
        )
    )

private fun createMarkerGuideline() =
    LineComponent(
        fill = Fill("#886BD3C9".toColorInt()),
        thicknessDp = 1f
    )
