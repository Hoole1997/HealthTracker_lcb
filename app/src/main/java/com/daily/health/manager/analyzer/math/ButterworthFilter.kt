package com.daily.health.manager.analyzer.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Butterworth 带通滤波器
 *
 * 该滤波器用于在心率测量中过滤 PPG 信号，保留心率频率范围 (0.75Hz - 2.5Hz，对应 45-150 BPM)。
 * 这个范围更适合静坐测量场景，减少高频噪声干扰。
 *
 * 系数通过以下 Python 代码生成：
 * ```python
 * from scipy.signal import butter
 * import numpy as np
 *
 * fs = 30.0  # 采样率
 * lowcut = 0.75   # 45 BPM
 * highcut = 2.5   # 150 BPM
 * nyq = fs / 2
 * low = lowcut / nyq
 * high = highcut / nyq
 *
 * b, a = butter(2, [low, high], btype='band')
 * print("b =", b.tolist())
 * print("a =", a.tolist())
 * ```
 *
 * 输出 (2阶带通, 0.75-2.5Hz @ 30Hz):
 * b = [0.04658291, 0.0, -0.09316582, 0.0, 0.04658291]
 * a = [1.0, -3.16778851, 3.85957102, -2.14702379, 0.45946049]
 */
class ButterworthBandpassFilter(
    private val sampleRate: Double = 30.0,
    private val lowCutoff: Double = 0.75,
    private val highCutoff: Double = 2.5
) {
    // 滤波器系数 (2阶 Butterworth 带通, 0.75-2.5Hz @ 30Hz)
    // 适用于静坐测量 (45-150 BPM)
    private val b = doubleArrayOf(
        0.04658291,
        0.0,
        -0.09316582,
        0.0,
        0.04658291
    )

    private val a = doubleArrayOf(
        1.0,
        -3.16778851,
        3.85957102,
        -2.14702379,
        0.45946049
    )

    // 滤波器状态缓冲区
    private val xBuffer = DoubleArray(5) { 0.0 }
    private val yBuffer = DoubleArray(5) { 0.0 }

    /**
     * 对单个样本进行滤波
     * @param input 输入样本值
     * @return 滤波后的输出值
     */
    fun filterSample(input: Double): Double {
        // 移位输入缓冲区
        for (i in 4 downTo 1) {
            xBuffer[i] = xBuffer[i - 1]
        }
        xBuffer[0] = input

        // 移位输出缓冲区
        for (i in 4 downTo 1) {
            yBuffer[i] = yBuffer[i - 1]
        }

        // 计算 IIR 滤波输出
        // y[n] = b[0]*x[n] + b[1]*x[n-1] + ... - a[1]*y[n-1] - a[2]*y[n-2] - ...
        var output = 0.0
        for (i in b.indices) {
            output += b[i] * xBuffer[i]
        }
        for (i in 1 until a.size) {
            output -= a[i] * yBuffer[i]
        }

        yBuffer[0] = output
        return output
    }

    /**
     * 对整个信号数组进行滤波
     * @param signal 输入信号数组
     * @return 滤波后的信号数组
     */
    fun filter(signal: DoubleArray): DoubleArray {
        reset()
        return signal.map { filterSample(it) }.toDoubleArray()
    }

    /**
     * 零相位滤波 (filtfilt)
     *
     * 对信号进行前向和后向两次滤波，消除相位失真。
     * 这对于精确的峰值检测非常重要。
     *
     * @param signal 输入信号数组
     * @return 零相位滤波后的信号数组
     */
    fun filtfilt(signal: DoubleArray): DoubleArray {
        if (signal.isEmpty()) return signal

        // 前向滤波
        reset()
        val forward = signal.map { filterSample(it) }.toDoubleArray()

        // 反转信号
        forward.reverse()

        // 后向滤波
        reset()
        val backward = forward.map { filterSample(it) }.toDoubleArray()

        // 再次反转得到最终结果
        backward.reverse()
        return backward
    }

    /**
     * 重置滤波器状态
     */
    fun reset() {
        xBuffer.fill(0.0)
        yBuffer.fill(0.0)
    }

    companion object {
        /**
         * 验证滤波器系数是否正确
         *
         * 通过检查在截止频率处的增益来验证滤波器行为。
         * @return 如果滤波器响应在预期范围内返回 true
         */
        fun validateCoefficients(): Boolean {
            val filter = ButterworthBandpassFilter()

            // 生成测试信号：1Hz 正弦波 (应通过) 和 10Hz 正弦波 (应衰减)
            val sampleRate = 30.0
            val duration = 5.0
            val numSamples = (sampleRate * duration).toInt()

            // 1Hz 测试信号 (在通带内)
            val signal1Hz = DoubleArray(numSamples) { i ->
                sin(2 * PI * 1.0 * i / sampleRate)
            }

            // 10Hz 测试信号 (在阻带内)
            val signal10Hz = DoubleArray(numSamples) { i ->
                sin(2 * PI * 10.0 * i / sampleRate)
            }

            val filtered1Hz = filter.filtfilt(signal1Hz)
            filter.reset()
            val filtered10Hz = filter.filtfilt(signal10Hz)

            // 计算 RMS
            fun rms(arr: DoubleArray): Double {
                val sum = arr.drop(numSamples / 2).sumOf { it * it }
                return sqrt(sum / (numSamples / 2))
            }

            val gain1Hz = rms(filtered1Hz) / rms(signal1Hz)
            val gain10Hz = rms(filtered10Hz) / rms(signal10Hz)

            // 1Hz 应保留大部分能量 (>0.7)，10Hz 应被显著衰减 (<0.1)
            return gain1Hz > 0.7 && gain10Hz < 0.1
        }
    }
}
