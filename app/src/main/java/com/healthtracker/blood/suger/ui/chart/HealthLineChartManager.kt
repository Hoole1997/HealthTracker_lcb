package com.healthtracker.blood.suger.ui.chart

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.healthtracker.blood.suger.util.AxisStyle
import com.healthtracker.blood.suger.util.ChartConfigHelper
import com.healthtracker.blood.suger.util.LineStyle
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.views.cartesian.CartesianChartView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.text.DecimalFormat

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
class HealthLineChartManager @AssistedInject constructor(
    @Assisted private val chartView: CartesianChartView,
    @Assisted private val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

    private val modelProducer = CartesianChartModelProducer()
    private var labels: List<String> = emptyList()

    @Volatile
    private var isReleased = false

    init {
        chartView.modelProducer = modelProducer

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
   suspend fun render(state: ChartUiState): Boolean {
        if (isReleased) {
            Log.w(TAG, "Attempted to render on released manager, ignoring")
            return false
        }

        labels = state.labels
        val (minY, maxY) = state.computeRange()

        val axisStyle = AxisStyle(
            bottomAxisValueFormatter = createBottomAxisFormatter(),
            deduplicateBottomLabels = true,
            minY = minY,
            maxY = maxY,
            startAxisValueFormatter = CartesianValueFormatter.decimal(DecimalFormat("#"))
        )

        val lineStyles = state.dataSets.mapIndexed { index, dataSet ->
            dataSet.style ?: DEFAULT_LINE_STYLES[index % DEFAULT_LINE_STYLES.size]
        }
        val lineCount = if (lineStyles.isEmpty()) 1 else lineStyles.size

        chartView.chart = ChartConfigHelper.createLineChart(
            lineStyles = lineStyles,
            axisStyle = axisStyle
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

    private fun createBottomAxisFormatter(): CartesianValueFormatter {
        return object : CartesianValueFormatter {
            override fun format(
                context: CartesianMeasuringContext,
                value: Double,
                verticalAxisPosition: Axis.Position.Vertical?
            ): CharSequence {
                return labels.getOrNull(value.toInt()) ?: ""
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
        chartView.modelProducer = null
        labels = emptyList()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            chartView: CartesianChartView,
            lifecycleOwner: LifecycleOwner
        ): HealthLineChartManager
    }

    companion object {
        private const val TAG = "HealthLineChartManager"

        private val DEFAULT_LINE_STYLES = listOf(
            LineStyle(color = "#FF6B4D"),
            LineStyle(color = "#4AD7FF"),
            LineStyle(color = "#88C057"),
            LineStyle(color = "#F7B801")
        )
    }
}
