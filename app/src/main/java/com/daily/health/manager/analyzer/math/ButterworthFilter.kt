package com.daily.health.manager.analyzer.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Butterworth 带通滤波器
 *
 * 该滤波器用于在心率测量中过滤 PPG 信号，保留心率频率范围 (0.75Hz - 3.0Hz，对应 45-180 BPM)。
 * 这个范围覆盖静坐测量场景的完整心率区间，包括运动后或年轻用户的较高心率。
 *
 * 系数通过以下 Python 代码生成：
 * ```python
 * from scipy.signal import butter
 * import numpy as np
 *
 * fs = 30.0  # 采样率
 * lowcut = 0.75   # 45 BPM
 * highcut = 3.0   # 180 BPM
 * nyq = fs / 2
 * low = lowcut / nyq
 * high = highcut / nyq
 *
 * b, a = butter(2, [low, high], btype='band')
 * print("b =", b.tolist())
 * print("a =", a.tolist())
 * ```
 *
 * 输出 (2阶带通, 0.75-3.0Hz @ 30Hz):
 * b = [0.0413908148, 0.0, -0.0827816296, 0.0, 0.0413908148]
 * a = [1.0, -3.1819605790, 3.9334965035, -2.2583981138, 0.5139818942]
 */
class ButterworthBandpassFilter(
    private val sampleRate: Double = 30.0,
    private val lowCutoff: Double = 0.75,
    private val highCutoff: Double = 3.0
) {
    // 滤波器系数 (2阶 Butterworth 带通, 0.75-3.0Hz @ 30Hz)
    // 适用于静坐测量 (45-180 BPM)
    private val b = doubleArrayOf(
        0.0413908148,
        0.0,
        -0.0827816296,
        0.0,
        0.0413908148
    )

    private val a = doubleArrayOf(
        1.0,
        -3.1819605790,
        3.9334965035,
        -2.2583981138,
        0.5139818942
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
        if (signal.size < 2) return signal

        // 镜像填充长度 = 3 * 滤波器阶数（4阶 IIR → 12 个样本）
        // 这是 scipy filtfilt 的标准做法，用于消除边缘瞬态效应
        val padLen = minOf(3 * (a.size - 1), signal.size - 1)

        // 构建填充后的信号: [镜像前端] + [原始信号] + [镜像后端]
        val padded = DoubleArray(signal.size + 2 * padLen)

        // 前端镜像填充: 以 signal[0] 为对称轴反射
        for (i in 0 until padLen) {
            padded[i] = 2.0 * signal[0] - signal[padLen - i]
        }
        // 原始信号
        signal.copyInto(padded, padLen)
        // 后端镜像填充: 以 signal[last] 为对称轴反射
        val last = signal.size - 1
        for (i in 0 until padLen) {
            padded[padLen + signal.size + i] = 2.0 * signal[last] - signal[last - 1 - i]
        }

        // 前向滤波
        reset()
        val forward = padded.map { filterSample(it) }.toDoubleArray()

        // 反转信号
        forward.reverse()

        // 后向滤波
        reset()
        val backward = forward.map { filterSample(it) }.toDoubleArray()

        // 再次反转得到最终结果
        backward.reverse()

        // 截取中间部分（去除填充）
        return backward.copyOfRange(padLen, padLen + signal.size)
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
