package com.daily.health.manager.analyzer.math

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 轻量级 FFT (快速傅里叶变换) 实现
 *
 * 基于 Cooley-Tukey Radix-2 算法，用于心率测量中的频谱分析。
 * 此实现避免了引入外部 FFT 库 (如 JTransforms)，减小包体积。
 *
 * 使用限制：
 * - 输入长度必须是 2 的幂次 (如 256, 512, 1024)
 * - 如果输入长度不是 2 的幂次，请使用 padToPowerOfTwo() 进行补零
 */
object FastFourierTransformer {

    /**
     * 复数表示
     */
    data class Complex(val re: Double, val im: Double) {
        operator fun plus(other: Complex) = Complex(re + other.re, im + other.im)
        operator fun minus(other: Complex) = Complex(re - other.re, im - other.im)
        operator fun times(other: Complex) = Complex(
            re * other.re - im * other.im,
            re * other.im + im * other.re
        )

        fun magnitude(): Double = sqrt(re * re + im * im)
        fun magnitudeSquared(): Double = re * re + im * im
    }

    /**
     * 对实数信号执行 FFT
     *
     * @param signal 输入信号 (实数数组，长度必须是 2 的幂次)
     * @return 复数频谱数组
     */
    fun fft(signal: DoubleArray): Array<Complex> {
        require(isPowerOfTwo(signal.size)) {
            "Signal length must be a power of 2, got ${signal.size}. Use padToPowerOfTwo() first."
        }

        // 转换为复数数组
        val complexSignal = Array(signal.size) { Complex(signal[it], 0.0) }
        return fftRecursive(complexSignal)
    }

    /**
     * 递归 Cooley-Tukey FFT 实现
     */
    private fun fftRecursive(x: Array<Complex>): Array<Complex> {
        val n = x.size
        if (n == 1) return x

        // 分离偶数和奇数索引
        val even = Array(n / 2) { x[it * 2] }
        val odd = Array(n / 2) { x[it * 2 + 1] }

        // 递归计算
        val evenFFT = fftRecursive(even)
        val oddFFT = fftRecursive(odd)

        // 合并
        val result = Array(n) { Complex(0.0, 0.0) }
        for (k in 0 until n / 2) {
            val angle = -2.0 * PI * k / n
            val twiddle = Complex(cos(angle), sin(angle))
            val t = twiddle * oddFFT[k]
            result[k] = evenFFT[k] + t
            result[k + n / 2] = evenFFT[k] - t
        }
        return result
    }

    /**
     * 计算功率谱密度 (PSD)
     *
     * @param signal 输入信号
     * @return 功率谱数组 (仅正频率部分，长度为 signal.size / 2)
     */
    fun powerSpectrum(signal: DoubleArray): DoubleArray {
        val spectrum = fft(signal)
        val n = spectrum.size
        return DoubleArray(n / 2) { i ->
            spectrum[i].magnitudeSquared() / n
        }
    }

    /**
     * 计算频率轴
     *
     * @param signalLength FFT 输入信号长度
     * @param sampleRate 采样率 (Hz)
     * @return 频率数组 (仅正频率部分)
     */
    fun frequencyAxis(signalLength: Int, sampleRate: Double): DoubleArray {
        val freqResolution = sampleRate / signalLength
        return DoubleArray(signalLength / 2) { it * freqResolution }
    }

    /**
     * 在指定频率范围内找到主频
     *
     * @param signal 输入信号
     * @param sampleRate 采样率
     * @param minFreq 最小频率 (Hz)
     * @param maxFreq 最大频率 (Hz)
     * @return 主频 (Hz)，如果未找到返回 null
     */
    fun findDominantFrequency(
        signal: DoubleArray,
        sampleRate: Double,
        minFreq: Double = 0.7,
        maxFreq: Double = 4.0
    ): Double? {
        if (signal.size < 4) return null

        val paddedSignal = padToPowerOfTwo(signal)
        val power = powerSpectrum(paddedSignal)
        val freqs = frequencyAxis(paddedSignal.size, sampleRate)

        var maxPower = 0.0
        var dominantFreq: Double? = null

        for (i in power.indices) {
            val freq = freqs[i]
            if (freq in minFreq..maxFreq && power[i] > maxPower) {
                maxPower = power[i]
                dominantFreq = freq
            }
        }

        // 使用抛物线插值提高频率精度
        if (dominantFreq != null) {
            val peakIdx = freqs.indexOfFirst { it == dominantFreq }
            if (peakIdx > 0 && peakIdx < power.size - 1) {
                val alpha = power[peakIdx - 1]
                val beta = power[peakIdx]
                val gamma = power[peakIdx + 1]
                if (2 * beta - alpha - gamma != 0.0) {
                    val delta = 0.5 * (alpha - gamma) / (alpha - 2 * beta + gamma)
                    val freqResolution = sampleRate / paddedSignal.size
                    dominantFreq = (peakIdx + delta) * freqResolution
                }
            }
        }

        return dominantFreq
    }

    /**
     * 将信号补零到下一个 2 的幂次长度
     */
    fun padToPowerOfTwo(signal: DoubleArray): DoubleArray {
        if (isPowerOfTwo(signal.size)) return signal

        var newSize = 1
        while (newSize < signal.size) {
            newSize *= 2
        }

        return signal.copyOf(newSize)
    }

    /**
     * 应用汉宁窗 (Hanning Window)
     *
     * 减少 FFT 频谱泄漏，提高频率分辨率
     *
     * @param signal 输入信号
     * @return 应用窗函数后的信号
     */
    fun applyHanningWindow(signal: DoubleArray): DoubleArray {
        val n = signal.size
        return DoubleArray(n) { i ->
            val windowCoeff = 0.5 * (1 - cos(2 * PI * i / (n - 1)))
            signal[i] * windowCoeff
        }
    }

    /**
     * 检查是否是 2 的幂次
     */
    private fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0

    /**
     * FFT 分析结果，包含主频和信噪比
     */
    data class FrequencyResult(
        val frequency: Double,
        val snr: Double,       // 峰值功率 / 平均功率
        val topFrequencies: List<Pair<Double, Double>>  // 前3个峰值 (频率, 功率)
    )

    /**
     * 增强版：查找主频并返回信噪比
     */
    fun findDominantFrequencyWithSNR(
        signal: DoubleArray,
        sampleRate: Double,
        minFreq: Double = 0.7,
        maxFreq: Double = 4.0
    ): FrequencyResult? {
        if (signal.size < 4) return null

        val paddedSignal = padToPowerOfTwo(signal)
        val power = powerSpectrum(paddedSignal)
        val freqs = frequencyAxis(paddedSignal.size, sampleRate)

        // 收集带内频率及其功率
        val bandPowers = mutableListOf<Pair<Int, Double>>()  // (index, power)
        for (i in power.indices) {
            if (freqs[i] in minFreq..maxFreq) {
                bandPowers.add(i to power[i])
            }
        }
        if (bandPowers.isEmpty()) return null

        // 按功率排序，取前3个
        val sorted = bandPowers.sortedByDescending { it.second }
        val peakIdx = sorted[0].first
        val peakPower = sorted[0].second

        // 计算带内平均功率（排除峰值附近±3 bin）
        val noisePowers = bandPowers.filter {
            kotlin.math.abs(it.first - peakIdx) > 3
        }.map { it.second }
        val meanNoisePower = if (noisePowers.isNotEmpty()) noisePowers.average() else 1.0
        val snr = if (meanNoisePower > 0) peakPower / meanNoisePower else 0.0

        // 抛物线插值提高频率精度
        var dominantFreq = freqs[peakIdx]
        if (peakIdx > 0 && peakIdx < power.size - 1) {
            val alpha = power[peakIdx - 1]
            val beta = power[peakIdx]
            val gamma = power[peakIdx + 1]
            if (2 * beta - alpha - gamma != 0.0) {
                val delta = 0.5 * (alpha - gamma) / (alpha - 2 * beta + gamma)
                val freqResolution = sampleRate / paddedSignal.size
                dominantFreq = (peakIdx + delta) * freqResolution
            }
        }

        val topFreqs = sorted.take(3).map { freqs[it.first] to it.second }
        return FrequencyResult(dominantFreq, snr, topFreqs)
    }

    /**
     * 从频率计算 BPM
     *
     * @param frequency 频率 (Hz)
     * @return 心率 (BPM)
     */
    fun frequencyToBpm(frequency: Double): Int = (frequency * 60).roundToInt()

    /**
     * 验证 FFT 实现正确性
     *
     * 通过对已知频率的正弦波进行 FFT，验证能正确检测到该频率
     */
    fun validateImplementation(): Boolean {
        val sampleRate = 30.0
        val testFreq = 1.5  // 1.5 Hz = 90 BPM
        val duration = 10.0
        val numSamples = (sampleRate * duration).toInt()

        // 生成测试正弦波
        val testSignal = DoubleArray(numSamples) { i ->
            sin(2 * PI * testFreq * i / sampleRate)
        }

        // 应用窗函数
        val windowed = applyHanningWindow(testSignal)

        // 查找主频
        val detectedFreq = findDominantFrequency(windowed, sampleRate) ?: return false

        // 验证误差在 0.1 Hz 以内
        return kotlin.math.abs(detectedFreq - testFreq) < 0.1
    }
}
