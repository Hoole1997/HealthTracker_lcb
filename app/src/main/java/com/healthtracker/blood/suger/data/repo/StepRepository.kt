package com.healthtracker.blood.suger.data.repo

import android.content.Context
import com.healthtracker.blood.suger.data.database.HealthDatabase
import com.healthtracker.blood.suger.data.dao.StepDao
import com.healthtracker.blood.suger.data.entity.DailyStepStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import com.healthtracker.blood.suger.data.utils.DateTimeUtils

class StepRepository private constructor(private val appContext: Context, private val dao: StepDao) {
    companion object {
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
        return startOfDay.time / 86_400_000L
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
            val stepLength = 0.7
            val distanceKm = steps * stepLength / 1000.0
            val stepFreq = 110.0
            val durationSeconds = (steps / stepFreq * 60.0).toInt()
            val weightKg = 65.0
            val met = 3.5
            val kcal = met * weightKg * (durationSeconds / 3600.0)

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