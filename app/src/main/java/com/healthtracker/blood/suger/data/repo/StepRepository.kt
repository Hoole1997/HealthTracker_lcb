package com.healthtracker.blood.suger.data.repo

import android.content.Context
import com.healthtracker.blood.suger.data.database.HealthDatabase
import com.healthtracker.blood.suger.data.dao.StepDao
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import com.healthtracker.blood.suger.data.preferences.BodyMetricsPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.data.utils.toLocalEpochDay

class StepRepository private constructor(private val appContext: Context, private val dao: StepDao) {
    companion object {
        private const val STEP_LENGTH_RATIO = 0.414  // 文档公式：身高(米) * 0.414 = 步幅(米)
        private const val KCAL_COEFFICIENT = 0.7
        private const val STEP_FREQUENCY_PER_MINUTE = 110.0
        @Volatile private var INSTANCE: StepRepository? = null
        fun get(appContext: Context): StepRepository {
            return INSTANCE ?: synchronized(this) {
                val db = HealthDatabase.getDatabase(appContext)
                val instance = StepRepository(appContext.applicationContext, db.stepDao())
                INSTANCE = instance
                instance
            }
        }
    }

    private fun currentEpochDay(): Long {
        val startOfDay = DateTimeUtils.getTodayRange().first
        return startOfDay.toLocalEpochDay()
    }
    private var lastPersistTs = 0L
    private var lastPersistSteps = 0

    fun observeToday(): Flow<DailyStepStat?> = dao.observeByDate(currentEpochDay())

    fun range(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyStepStat>> = dao.getRange(startEpochDay, endEpochDay)

    suspend fun handleRawStep(raw: Int) {
        withContext(Dispatchers.IO) {
            val epochDay = currentEpochDay()
            val now = System.currentTimeMillis()
            val existing = dao.getByDate(epochDay)
            val baseline = when {
                existing == null -> raw
                raw < existing.baselineRaw || raw < existing.lastRaw -> raw
                else -> existing.baselineRaw
            }
            val steps = (raw - baseline).coerceAtLeast(0)
            val heightCm = BodyMetricsPreferences.getHeightCm()
            val stepLengthMeters = (heightCm / 100.0) * STEP_LENGTH_RATIO
            val distanceKm = steps * stepLengthMeters / 1000.0
            val weightKg = BodyMetricsPreferences.getWeightKg()
            val durationSeconds = (steps / STEP_FREQUENCY_PER_MINUTE * 60.0).toInt()
            val kcal = distanceKm * weightKg * KCAL_COEFFICIENT

            val shouldPersist = (now - lastPersistTs) >= 2000L || (steps - lastPersistSteps) >= 5
            if (!shouldPersist && existing != null) return@withContext

            if (existing == null) {
                dao.upsert(
                    DailyStepStat(
                        dateEpochDay = epochDay,
                        steps = steps,
                        distanceKm = distanceKm,
                        kcal = kcal,
                        durationSeconds = durationSeconds,
                        baselineRaw = baseline,
                        lastRaw = raw,
                        updatedAt = now
                    )
                )
            } else {
                dao.updateMetrics(epochDay, steps, distanceKm, kcal, durationSeconds, raw, now)
                if (baseline != existing.baselineRaw) {
                    dao.upsert(
                        existing.copy(
                            baselineRaw = baseline,
                            lastRaw = raw,
                            steps = steps,
                            distanceKm = distanceKm,
                            kcal = kcal,
                            durationSeconds = durationSeconds,
                            updatedAt = now
                        )
                    )
                }
            }

            lastPersistTs = now
            lastPersistSteps = steps
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
