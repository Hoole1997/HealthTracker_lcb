package com.daily.health.manager.analyzer

import com.daily.health.manager.analyzer.math.ButterworthBandpassFilter
import com.daily.health.manager.analyzer.math.FastFourierTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.logd

/**
 * PPG 心率分析器
 *
 * 通过摄像头采集的 PPG (光电容积脉搏波) 信号分析心率。
 *
 * 主要功能：
 * 1. 接收并缓存摄像头帧的红色通道平均亮度
 * 2. 检测手指是否正确覆盖摄像头
 * 3. 评估信号质量
 * 4. 使用峰值检测法进行实时心率估计
 * 5. 使用 FFT 进行最终精确心率计算
 */
class HeartRateAnalyzer {

    companion object {
        private const val TAG = "PPG_Analyzer"
        
        // ===== 业界标准参数 =====
        const val SAMPLE_RATE = 30.0           // 目标采样率 (Hz) - 业界标准 30Hz
        const val MIN_SAMPLES = 120            // 最少样本数 - 最终结果 (4秒)
        const val MIN_SAMPLES_INSTANT = 45     // 最少样本数 - 实时心率 (1.5秒) - 竞品标准
        const val MAX_SAMPLES = 600            // 最大样本数 (20秒)
        const val WINDOW_SIZE_SEC = 8          // 分析窗口 (秒) - 竞品标准
        const val MIN_BPM = 45                 // 最低有效心率 - 正常人低限
        const val MAX_BPM = 180                // 最高有效心率 - 静坐测量上限
        const val MIN_RR_INTERVAL = 0.33       // 最小 R-R 间隔 (秒) = 180 BPM
        const val MAX_RR_INTERVAL = 1.33       // 最大 R-R 间隔 (秒) = 45 BPM

        // ===== BPM 平滑参数 =====
        private const val BPM_SMOOTHING_ALPHA = 0.3         // EMA 平滑系数 (0.2-0.4 业界常用)
        private const val BPM_CHANGE_THRESHOLD = 15         // BPM 变化阈值，超过此值认为异常
        
        // ===== 手指检测防抖参数 =====
        private const val FINGER_DETECT_CONFIRM_COUNT = 2   // 连续确认次数 - 快速响应
        private const val FINGER_LOST_CONFIRM_COUNT = 3     // 连续丢失次数 (约100ms) - 快速响应
        private const val DETECTION_SAMPLE_SIZE = 30        // 检测用样本数 (1秒)

        // ===== 手指检测阈值 =====
        // 有手指时：亮度较低（被遮挡）约 30-80
        // 无手指时：亮度较高（无遮挡）约 120-140
        private const val BRIGHTNESS_MIN = 25.0             // 亮度下限 - 避免纯黑
        private const val BRIGHTNESS_MAX = 100.0            // 亮度上限 - 避免无手指误判
        private const val VARIANCE_MIN = 0.01               // 方差下限 - 需要有脉动信号
        private const val VARIANCE_MAX = 200.0              // 方差上限
    }

    /**
     * 采样数据点
     * @param timestamp 纳秒级时间戳
     * @param value 红色通道平均亮度
     */
    data class Sample(val timestamp: Long, val value: Double)

    /**
     * 测量结果
     */
    data class MeasurementResult(
        val bpm: Int?,
        val quality: SignalQuality,
        val confidence: Float
    )

    /**
     * 信号质量等级
     */
    enum class SignalQuality {
        EXCELLENT,  // 优秀
        GOOD,       // 良好
        FAIR,       // 一般
        POOR,       // 较差
        NO_SIGNAL   // 无信号
    }

    // 原始信号缓冲区
    private val rawSamples = mutableListOf<Sample>()

    // 滤波器实例
    private val bandpassFilter = ButterworthBandpassFilter()

    // 手指检测防抖状态
    private var fingerDetectedState = false                 // 当前稳定状态
    private var consecutiveDetectCount = 0                  // 连续检测到计数
    private var consecutiveLostCount = 0                    // 连续丢失计数
    
    // BPM 平滑状态 (业界标准: EMA 平滑)
    private var smoothedBpm: Double? = null                 // 平滑后的 BPM
    private var lastValidBpm: Int? = null                   // 上次有效 BPM

    /**
     * 添加新采样点
     *
     * @param value 红色通道平均亮度
     * @param timestamp 纳秒级时间戳 (来自 ImageProxy.imageInfo.timestamp)
     */
    fun addSample(value: Double, timestamp: Long) {
        rawSamples.add(Sample(timestamp, value))

        // 限制缓冲区大小
        while (rawSamples.size > MAX_SAMPLES) {
            rawSamples.removeAt(0)
        }

        // 每 100 个样本输出一次日志
        if (BuildState.debug && rawSamples.size % 100 == 0) {
            "采样数=${rawSamples.size}, 最新值=$value".logd(TAG)
        }
    }

    /**
     * 检测手指是否正确覆盖摄像头
     *
     * 使用滞后机制防止状态频繁跳变：
     * - 从“未检测到”到“检测到”需连续 FINGER_DETECT_CONFIRM_COUNT 次确认
     * - 从“检测到”到“未检测到”需连续 FINGER_LOST_CONFIRM_COUNT 次确认
     */
    fun isFingerDetected(): Boolean {
        if (rawSamples.size < DETECTION_SAMPLE_SIZE) return fingerDetectedState

        val instantDetected = checkFingerInstant()

        // 滞后逻辑：防止状态频繁跳变
        if (instantDetected) {
            consecutiveLostCount = 0
            consecutiveDetectCount++

            // 当前未检测到 -> 需要连续确认多次才切换
            if (!fingerDetectedState && consecutiveDetectCount >= FINGER_DETECT_CONFIRM_COUNT) {
                fingerDetectedState = true
                if (BuildState.debug) "手指检测: 状态切换为已检测到 (连续${consecutiveDetectCount}次)".logd(TAG)
            }
        } else {
            consecutiveDetectCount = 0
            consecutiveLostCount++

            // 当前已检测到 -> 需要连续丢失多次才切换
            if (fingerDetectedState && consecutiveLostCount >= FINGER_LOST_CONFIRM_COUNT) {
                fingerDetectedState = false
                if (BuildState.debug) "手指检测: 状态切换为未检测到 (连续丢失${consecutiveLostCount}次)".logd(TAG)
            }
        }

        return fingerDetectedState
    }

    /**
     * 即时检测手指 (无防抖)
     */
    private fun checkFingerInstant(): Boolean {
        val recentValues = rawSamples.takeLast(DETECTION_SAMPLE_SIZE).map { it.value }
        val mean = recentValues.average()
        val variance = recentValues.map { (it - mean).pow(2) }.average()

        // 手指覆盖时：亮度适中（被手指遮挡）且有脉动
        val brightnessOk = mean in BRIGHTNESS_MIN..BRIGHTNESS_MAX
        val hasSignal = variance in VARIANCE_MIN..VARIANCE_MAX

        val detected = brightnessOk && hasSignal
        if (BuildState.debug && rawSamples.size % 30 == 0) {
            "手指检测(instant): detected=$detected, mean=${String.format("%.1f", mean)}, var=${String.format("%.2f", variance)}, state=$fingerDetectedState".logd(TAG)
        }
        return detected
    }

    /**
     * 获取信号质量
     */
    fun getSignalQuality(): SignalQuality {
        if (rawSamples.size < MIN_SAMPLES) return SignalQuality.NO_SIGNAL
        if (!isFingerDetected()) return SignalQuality.NO_SIGNAL

        val recentValues = rawSamples.takeLast(WINDOW_SIZE_SEC.toInt() * SAMPLE_RATE.toInt())
            .map { it.value }

        if (recentValues.size < MIN_SAMPLES) return SignalQuality.POOR

        val mean = recentValues.average()
        val std = sqrt(recentValues.map { (it - mean).pow(2) }.average())
        val cv = std / mean  // 变异系数

        return when {
            cv > 0.05 && cv < 0.3 -> SignalQuality.EXCELLENT.also {
                if (BuildState.debug) "信号质量: 优秀 (cv=$cv)".logd(TAG)
            }
            cv > 0.03 && cv < 0.5 -> SignalQuality.GOOD.also {
                if (BuildState.debug) "信号质量: 良好 (cv=$cv)".logd(TAG)
            }
            cv > 0.01 && cv < 0.8 -> SignalQuality.FAIR.also {
                if (BuildState.debug) "信号质量: 一般 (cv=$cv)".logd(TAG)
            }
            else -> SignalQuality.POOR.also {
                if (BuildState.debug) "信号质量: 较差 (cv=$cv)".logd(TAG)
            }
        }
    }

    /**
     * 获取实时心率估计 (峰值检测法 + EMA 平滑)
     *
     * 业界标准实现：
     * 1. 使用较低的样本门槛 (1.5秒) 快速响应
     * 2. 使用 EMA 平滑处理，减少抖动
     * 3. 过滤异常跳变
     */
    fun getInstantBpm(): Int? {
        // 业界标准：1.5秒即可开始计算
        if (rawSamples.size < MIN_SAMPLES_INSTANT) {
            if (BuildState.debug && rawSamples.size % 15 == 0) {
                "实时心率: 样本不足 ${rawSamples.size}/$MIN_SAMPLES_INSTANT".logd(TAG)
            }
            return lastValidBpm
        }

        val signal = prepareSignal()
        if (signal.isEmpty()) {
            if (BuildState.debug) "实时心率: 信号为空".logd(TAG)
            return lastValidBpm
        }

        val filtered = bandpassFilter.filtfilt(signal)
        val rawBpm = detectBpmFromPeaks(filtered)
        
        if (rawBpm == null) {
            if (BuildState.debug && rawSamples.size % 30 == 0) {
                "实时心率: 无法计算 (样本=${rawSamples.size})".logd(TAG)
            }
            return lastValidBpm
        }
        
        // EMA 平滑处理 (业界标准)
        val smoothed = if (smoothedBpm == null) {
            rawBpm.toDouble()
        } else {
            // 如果变化过大，可能是异常值，降低权重
            val alpha = if (abs(rawBpm - smoothedBpm!!) > BPM_CHANGE_THRESHOLD) {
                BPM_SMOOTHING_ALPHA * 0.5  // 异常值使用更小的权重
            } else {
                BPM_SMOOTHING_ALPHA
            }
            alpha * rawBpm + (1 - alpha) * smoothedBpm!!
        }
        
        smoothedBpm = smoothed
        val resultBpm = smoothed.toInt().coerceIn(MIN_BPM, MAX_BPM)
        lastValidBpm = resultBpm
        
        if (BuildState.debug) {
            "实时心率: raw=$rawBpm, smoothed=${String.format("%.1f", smoothed)}, result=$resultBpm BPM".logd(TAG)
        }
        
        return resultBpm
    }

    /**
     * 获取最终精确心率 (FFT 法)
     *
     * 使用 FFT 频谱分析，精度更高，在测量结束时调用。
     * 此方法在后台线程执行，避免阻塞主线程。
     */
    suspend fun getFinalBpm(): Int? = withContext(Dispatchers.Default) {
        if (rawSamples.size < MIN_SAMPLES) return@withContext null
        if (!isFingerDetected()) return@withContext null

        val signal = prepareSignal()
        if (signal.isEmpty()) return@withContext null

        // 应用滤波
        val filtered = bandpassFilter.filtfilt(signal)

        // 应用汉宁窗
        val windowed = FastFourierTransformer.applyHanningWindow(filtered)

        // 查找主频
        val dominantFreq = FastFourierTransformer.findDominantFrequency(
            windowed,
            SAMPLE_RATE,
            MIN_BPM / 60.0,
            MAX_BPM / 60.0
        ) ?: return@withContext null.also {
            if (BuildState.debug) "FFT 分析: 未找到主频率".logd(TAG)
        }

        val bpm = FastFourierTransformer.frequencyToBpm(dominantFreq)
        if (BuildState.debug) "FFT 分析: 频率=$dominantFreq Hz, 心率=$bpm BPM".logd(TAG)

        // 有效性检查
        if (bpm in MIN_BPM..MAX_BPM) bpm else null
    }

    /**
     * 获取完整测量结果
     */
    suspend fun getMeasurementResult(): MeasurementResult {
        val quality = getSignalQuality()
        val bpm = if (quality != SignalQuality.NO_SIGNAL && quality != SignalQuality.POOR) {
            getFinalBpm()
        } else {
            null
        }

        val confidence = when (quality) {
            SignalQuality.EXCELLENT -> 0.95f
            SignalQuality.GOOD -> 0.85f
            SignalQuality.FAIR -> 0.70f
            SignalQuality.POOR -> 0.40f
            SignalQuality.NO_SIGNAL -> 0f
        }

        if (BuildState.debug) "测量结果: 心率=$bpm, 质量=$quality, 置信度=$confidence".logd(TAG)
        return MeasurementResult(bpm, quality, confidence)
    }

    /**
     * 重置分析器
     */
    fun reset() {
        if (BuildState.debug) "重置分析器: 清除 ${rawSamples.size} 个样本".logd(TAG)
        rawSamples.clear()
        bandpassFilter.reset()
        // 重置防抖状态
        fingerDetectedState = false
        consecutiveDetectCount = 0
        consecutiveLostCount = 0
        // 重置 BPM 平滑状态
        smoothedBpm = null
        lastValidBpm = null
    }

    /**
     * 获取当前采样数
     */
    fun getSampleCount(): Int = rawSamples.size

    /**
     * 准备信号：重采样到固定采样率
     *
     * 由于摄像头帧率不稳定 (VFR 问题)，需要将不均匀采样的信号
     * 线性插值到均匀的固定采样率。
     */
    private fun prepareSignal(): DoubleArray {
        if (rawSamples.size < 2) return doubleArrayOf()

        val samples = rawSamples.toList()
        val startTime = samples.first().timestamp
        val endTime = samples.last().timestamp
        val durationNanos = endTime - startTime

        if (durationNanos <= 0) return doubleArrayOf()

        // 计算目标采样点数
        val durationSec = durationNanos / 1_000_000_000.0
        val targetSamples = (durationSec * SAMPLE_RATE).toInt()

        if (targetSamples < 2) return doubleArrayOf()

        // 线性插值重采样
        return DoubleArray(targetSamples) { i ->
            val targetTime = startTime + (i * durationNanos / (targetSamples - 1))
            interpolateValue(samples, targetTime)
        }
    }

    /**
     * 线性插值
     */
    private fun interpolateValue(samples: List<Sample>, targetTime: Long): Double {
        // 二分查找目标时间点的位置
        var left = 0
        var right = samples.size - 1

        while (left < right - 1) {
            val mid = (left + right) / 2
            if (samples[mid].timestamp <= targetTime) {
                left = mid
            } else {
                right = mid
            }
        }

        val s1 = samples[left]
        val s2 = samples[right]

        if (s1.timestamp == s2.timestamp) return s1.value

        // 线性插值
        val t = (targetTime - s1.timestamp).toDouble() / (s2.timestamp - s1.timestamp)
        return s1.value + t * (s2.value - s1.value)
    }

    /**
     * 从滤波后信号中通过峰值检测计算心率
     *
     * 策略：
     * 1. 使用较低的自适应阈值 (40%) 确保检测到足够的峰值
     * 2. 不应期防止重复检测
     * 3. 如果峰值不足，尝试降低阈值重新检测
     */
    private fun detectBpmFromPeaks(signal: DoubleArray): Int? {
        if (signal.size < MIN_SAMPLES_INSTANT) {
            if (BuildState.debug) "峰值检测: 信号长度不足 ${signal.size}".logd(TAG)
            return null
        }

        // 计算信号统计量
        val signalMax = signal.max()
        val signalMin = signal.min()
        val amplitude = signalMax - signalMin
        
        if (BuildState.debug && rawSamples.size % 60 == 0) {
            "峰值检测: 信号长度=${signal.size}, 幅度=$amplitude, min=$signalMin, max=$signalMax".logd(TAG)
        }
        
        // 如果幅度太小，信号可能无效
        if (amplitude < 0.5) {
            if (BuildState.debug) "峰值检测: 信号幅度过小 $amplitude".logd(TAG)
            return null
        }

        // 尝试不同的阈值，从低到高
        val thresholdLevels = listOf(0.35, 0.45, 0.55)
        
        for (thresholdRatio in thresholdLevels) {
            val threshold = signalMin + amplitude * thresholdRatio
            val peaks = detectPeaksWithThreshold(signal, threshold)
            
            if (BuildState.debug && rawSamples.size % 60 == 0) {
                "峰值检测: 阈值比例=$thresholdRatio, 检测到 ${peaks.size} 个峰值".logd(TAG)
            }
            
            if (peaks.size >= 2) {
                val bpm = calculateBpmFromPeaks(peaks)
                if (bpm != null) {
                    if (BuildState.debug) "峰值检测成功: 阈值=$thresholdRatio, 峰值数=${peaks.size}, BPM=$bpm".logd(TAG)
                    return bpm
                }
            }
        }
        
        if (BuildState.debug) "峰值检测失败: 所有阈值都无法检测到有效峰值".logd(TAG)
        return null
    }
    
    /**
     * 使用指定阈值检测峰值
     */
    private fun detectPeaksWithThreshold(signal: DoubleArray, threshold: Double): List<Int> {
        val peaks = mutableListOf<Int>()
        val refractoryPeriod = (SAMPLE_RATE * MIN_RR_INTERVAL).toInt()
        var lastPeakIdx = -refractoryPeriod
        
        for (i in 1 until signal.size - 1) {
            // 局部最大值：比前后都大
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1]) {
                // 超过阈值且在不应期之外
                if (signal[i] > threshold && (i - lastPeakIdx) >= refractoryPeriod) {
                    peaks.add(i)
                    lastPeakIdx = i
                }
            }
        }
        
        return peaks
    }
    
    /**
     * 从峰值位置计算 BPM
     */
    private fun calculateBpmFromPeaks(peaks: List<Int>): Int? {
        if (peaks.size < 2) return null
        
        // 计算 R-R 间隔
        val rrIntervals = peaks.zipWithNext { a, b ->
            (b - a) / SAMPLE_RATE
        }

        // 过滤生理范围外的异常值
        val validIntervals = rrIntervals.filter { it in MIN_RR_INTERVAL..MAX_RR_INTERVAL }

        if (validIntervals.isEmpty()) {
            if (BuildState.debug) "BPM计算: 无有效的 R-R 间隔".logd(TAG)
            return null
        }

        // 使用中位数而非平均值，更抗噪声
        val sortedIntervals = validIntervals.sorted()
        val medianInterval = sortedIntervals[sortedIntervals.size / 2]
        
        val bpm = (60.0 / medianInterval).toInt()

        return if (bpm in MIN_BPM..MAX_BPM) bpm else null
    }
}
