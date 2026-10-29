package com.daily.health.manager.data.repo

import android.content.Context
import com.daily.health.manager.data.database.HealthDatabase
import com.daily.health.manager.data.dao.StepDao
import com.daily.health.manager.data.entity.DailyStepStat
import com.daily.health.manager.data.preferences.BodyMetricsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.data.utils.toLocalEpochDay
import com.daily.health.manager.steps.StepCounterAccumulator
import com.daily.health.manager.steps.StepCounterReading
import com.daily.health.manager.steps.StepCounterState
import java.util.Date

class StepRepository internal constructor(private val dao: StepDao) {
    companion object {
        private const val STEP_LENGTH_RATIO = 0.414  // 文档公式：身高(米) * 0.414 = 步幅(米)
        private const val KCAL_COEFFICIENT = 0.7
        private const val STEP_FREQUENCY_PER_MINUTE = 110.0
        @Volatile private var INSTANCE: StepRepository? = null
        fun get(appContext: Context): StepRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StepRepository(HealthDatabase.getDatabase(appContext.applicationContext).stepDao())
                    .also { INSTANCE = it }
            }
        }
    }

    private fun currentEpochDay(): Long {
        val startOfDay = DateTimeUtils.getTodayRange().first
        return startOfDay.toLocalEpochDay()
    }
    private val writeMutex = Mutex()

    fun observeToday(): Flow<DailyStepStat?> = dao.observeByDate(currentEpochDay())

    fun range(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyStepStat>> = dao.getRange(startEpochDay, endEpochDay)

    suspend fun handleReading(reading: StepCounterReading) {
        if (reading.rawSteps < 0) return
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val epochDay = Date(reading.recordedAtMillis).toLocalEpochDay()
                val existing = dao.getByDate(epochDay)
                val previous = existing?.let { StepCounterState(it.steps, it.baselineRaw, it.lastRaw, it.bootCount) }
                val next = StepCounterAccumulator.update(previous, reading.rawSteps, reading.bootCount)
                val heightCm = BodyMetricsPreferences.getHeightCm()
                val stepLengthMeters = (heightCm / 100.0) * STEP_LENGTH_RATIO
                val distanceKm = next.steps * stepLengthMeters / 1000.0
                val weightKg = BodyMetricsPreferences.getWeightKg()
                val durationSeconds = (next.steps / STEP_FREQUENCY_PER_MINUTE * 60.0).toInt()
                val kcal = distanceKm * weightKg * KCAL_COEFFICIENT

                // 步数与原始值、启动次数一次写入；不节流丢弃末尾 1～4 步。
                dao.upsert(
                    DailyStepStat(
                        dateEpochDay = epochDay,
                        steps = next.steps,
                        distanceKm = distanceKm,
                        kcal = kcal,
                        durationSeconds = durationSeconds,
                        baselineRaw = next.baselineRaw,
                        lastRaw = next.lastRaw,
                        updatedAt = reading.recordedAtMillis,
                        bootCount = next.bootCount,
                    )
                )
            }
        }
    }

    fun observeTodayDynamic(): Flow<DailyStepStat?> = flow {
        var lastEpochDay: Long? = null
        while (true) {
            val current = currentEpochDay()
            if (lastEpochDay != current) {
                emit(current)
                lastEpochDay = current
            }
            kotlinx.coroutines.delay(60_000L)
        }
    }.flatMapLatest { day -> dao.observeByDate(day) }
}
