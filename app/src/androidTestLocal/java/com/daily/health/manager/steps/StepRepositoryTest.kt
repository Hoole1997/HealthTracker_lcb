package com.daily.health.manager.steps

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daily.health.manager.data.database.HealthDatabase
import com.daily.health.manager.data.repo.StepRepository
import com.daily.health.manager.data.utils.toLocalEpochDay
import java.util.Calendar
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StepRepositoryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private lateinit var database: HealthDatabase
    private lateinit var repository: StepRepository
    private val now = System.currentTimeMillis()
    private val today = Date(now).toLocalEpochDay()

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, HealthDatabase::class.java).build()
        repository = StepRepository(database.stepDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun rebootAndRepositoryRecreationPreserveTheDailyTotal() = runBlocking {
        repository.handleReading(StepCounterReading(1000, 4, now))
        repository.handleReading(StepCounterReading(1125, 4, now + 1))
        val recreated = StepRepository(database.stepDao())
        recreated.handleReading(StepCounterReading(12, 5, now + 2))
        recreated.handleReading(StepCounterReading(12, 5, now + 3))
        recreated.handleReading(StepCounterReading(15, 5, now + 4))
        val record = database.stepDao().getByDate(today)!!
        assertEquals(140, record.steps)
        assertEquals(15, record.lastRaw)
        assertEquals(5, record.bootCount)
    }

    @Test
    fun rebootIsDetectedWhenTheNewCounterAlreadyExceedsTheOldOne() = runBlocking {
        repository.handleReading(StepCounterReading(100, 4, now))
        repository.handleReading(StepCounterReading(120, 4, now + 1))
        repository.handleReading(StepCounterReading(150, 5, now + 2))
        assertEquals(170, database.stepDao().getByDate(today)!!.steps)
    }

    @Test
    fun finalFewStepsArePersistedWithoutWaitingForAnotherSensorEvent() = runBlocking {
        repository.handleReading(StepCounterReading(100, 4, now))
        repository.handleReading(StepCounterReading(101, 4, now + 1))
        repository.handleReading(StepCounterReading(102, 4, now + 2))
        assertEquals(2, database.stepDao().getByDate(today)!!.steps)
    }

    @Test
    fun aNewDayHasItsOwnBaselineAndPreservesYesterday() = runBlocking {
        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        repository.handleReading(StepCounterReading(100, 4, midnight - 2000))
        repository.handleReading(StepCounterReading(105, 4, midnight - 1000))
        repository.handleReading(StepCounterReading(110, 4, midnight + 1000))
        repository.handleReading(StepCounterReading(112, 4, midnight + 2000))
        assertEquals(5, database.stepDao().getByDate(Date(midnight - 1000).toLocalEpochDay())!!.steps)
        assertEquals(2, database.stepDao().getByDate(Date(midnight).toLocalEpochDay())!!.steps)
    }

    @Test
    fun version2MigrationKeepsBothStepsAndOtherHealthRecords() = runBlocking {
        val name = "step-migration-${UUID.randomUUID()}.db"
        var migrated: HealthDatabase? = null
        try {
            context.openOrCreateDatabase(name, 0, null).use { old ->
                val schema = instrumentation.context.assets.open("step_migration_v2.sql")
                    .bufferedReader().use { it.readText() }
                schema.lineSequence().filterNot { it.startsWith("--") }.joinToString("\n")
                    .split(';').filter { it.isNotBlank() }.forEach { old.execSQL(it) }
                old.execSQL("INSERT INTO t11 VALUES (?, 125, 0.1, 1.0, 60, 1000, 1125, ?)", arrayOf(today, now))
                old.execSQL("INSERT INTO t01 (c01, c02, c03, c04, c05, c06) VALUES (7, ?, 1, 5.6, 0, 0)", arrayOf(now))
                old.version = 2
            }
            val upgraded = Room.databaseBuilder(context, HealthDatabase::class.java, name)
                .addMigrations(HealthDatabase.MIGRATION_2_3).build()
            migrated = upgraded
            val before = upgraded.stepDao().getByDate(today)!!
            assertEquals(125, before.steps)
            assertEquals(-1, before.bootCount)
            upgraded.openHelper.readableDatabase.query("SELECT c04 FROM t01 WHERE c01 = 7").use { cursor ->
                assertEquals(true, cursor.moveToFirst())
                assertEquals(5.6, cursor.getDouble(0), 0.0001)
            }
            StepRepository(upgraded.stepDao()).handleReading(StepCounterReading(12, 5, now + 1))
            val after = upgraded.stepDao().getByDate(today)
            assertNotNull(after)
            assertEquals(137, after!!.steps)
        } finally {
            migrated?.close()
            context.deleteDatabase(name)
        }
    }
}
