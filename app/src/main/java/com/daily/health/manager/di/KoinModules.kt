package com.daily.health.manager.di

import android.app.Application
import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.daily.health.manager.AppInitializer
import com.daily.health.manager.alarm.PermissionManager
import com.daily.health.manager.config.parsers.PushConfigParser
import com.daily.health.manager.config.parsers.PushMessageParser
import com.daily.health.manager.config.registry.AppConfigRegistry
import com.daily.health.manager.data.database.HealthDatabase
import com.daily.health.manager.data.repo.StepRepository
import com.daily.health.manager.data.repository.BloodPressureRepository
import com.daily.health.manager.data.repository.BloodSugarRepository
import com.daily.health.manager.data.repository.BmiRepository
import com.daily.health.manager.data.repository.CholesterolRepository
import com.daily.health.manager.data.repository.HeartRateRepository
import com.daily.health.manager.data.repository.HydrateRepository
import com.daily.health.manager.data.repository.AlarmRepository
import com.daily.health.manager.data.repository.HealthTagRepository
import com.daily.health.manager.data.repository.MedicineReminderRepository
import com.daily.health.manager.alarm.AlarmNotificationManager
import com.daily.health.manager.alarm.AlarmScheduler
import com.daily.health.manager.helper.CustomNotificationHelper
import com.daily.health.manager.helper.NotificationResourceMapper
import com.daily.health.manager.helper.ResidentNotificationHelper
import com.daily.health.manager.manager.HealthServiceManager
import com.daily.health.manager.observer.AppForegroundObserver
import com.daily.health.manager.observer.HealthServiceForegroundObserver
import com.daily.health.manager.strategy.LoopPushManager
import com.daily.health.manager.strategy.PushFrequencyController
import com.daily.health.manager.strategy.PushMessageRepository
import com.daily.health.manager.strategy.PushMessageSelector
import com.daily.health.manager.strategy.PushOrchestrator
import com.daily.health.manager.face.chart.HealthLineChartManager
import com.daily.health.manager.face.viewmodel.SplashViewModel
import com.daily.health.manager.face.viewmodel.MainViewModel
import com.daily.health.manager.face.viewmodel.HomeViewModel
import com.daily.health.manager.face.viewmodel.MedsViewModel
import com.daily.health.manager.face.viewmodel.TrackerViewModel
import com.daily.health.manager.face.viewmodel.AlarmViewModel
import com.daily.health.manager.face.viewmodel.BsRecordViewModel
import com.daily.health.manager.face.viewmodel.BpRecordViewModel
import com.daily.health.manager.face.viewmodel.BsDetailViewModel
import com.daily.health.manager.face.viewmodel.BpDetailViewModel
import com.daily.health.manager.face.viewmodel.BmiRecordViewModel
import com.daily.health.manager.face.viewmodel.BmiDetailViewModel
import com.daily.health.manager.face.viewmodel.HeartRateRecordViewModel
import com.daily.health.manager.face.viewmodel.HeartRateDetailViewModel
import com.daily.health.manager.face.viewmodel.CholesterolRecordViewModel
import com.daily.health.manager.face.viewmodel.CholesterolDetailViewModel
import com.daily.health.manager.face.viewmodel.HydrateViewModel
import com.daily.health.manager.face.viewmodel.HydrateSettingViewModel
import com.daily.health.manager.face.viewmodel.HistoryViewModel
import com.daily.health.manager.face.viewmodel.AddReminderViewModel
import com.daily.health.manager.face.viewmodel.TargetRangeViewModel
import com.daily.health.manager.face.viewmodel.StepSettingViewModel
import com.daily.health.manager.viewmodel.HealthStatisticsViewModel
import com.daily.health.manager.face.act.StepCountViewModel
import androidx.lifecycle.SavedStateHandle
import com.healthtracker.framework.config.core.ConfigCache
import com.healthtracker.framework.config.core.ConfigRegistry
import com.healthtracker.framework.config.core.RemoteConfigManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.core.module.dsl.viewModel

val appModule = module {
    single<CoroutineDispatcher>(named("IoDispatcher")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("MainDispatcher")) { Dispatchers.Main }
    single<CoroutineDispatcher>(named("DefaultDispatcher")) { Dispatchers.Default }

    single { StepRepository.get(get<Context>()) }

    single { PermissionManager() }
    single { NotificationResourceMapper() }
    single { RemoteConfigManager(get(), get(), get()) }
    single { CustomNotificationHelper(get<Context>(), get(), get()) }
    single { ResidentNotificationHelper(get<Context>(), get()) }
    single { HealthServiceManager(get<Context>(), get(), get()) }

    single { PushFrequencyController(get<Context>(), get()) }
    single { PushMessageRepository(get()) }
    single { PushMessageSelector(get(), get<Context>()) }
    single { LoopPushManager(get(), get(), get<Context>()) }
    single { PushOrchestrator(get(), get(), get(), get(), get()) }

    factory { MainViewModel(get(), get()) }

    single { AppForegroundObserver() }
    single { HealthServiceForegroundObserver(get(), get()) }
    single {
        AppInitializer(
            application = get<Context>() as Application,
            ioDispatcher = get(named("IoDispatcher")),
            remoteConfigManager = get(),
            appConfigRegistry = get(),
            healthServiceForegroundObserver = get(),
        )
    }

    single<HealthLineChartManager.Factory> {
        object : HealthLineChartManager.Factory {
            override fun create(
                chartView: com.patrykandpatrick.vico.views.cartesian.CartesianChartView,
                lifecycleOwner: androidx.lifecycle.LifecycleOwner
            ): HealthLineChartManager {
                return HealthLineChartManager(chartView, lifecycleOwner)
            }
        }
    }
}

val databaseModule = module {
    single { HealthDatabase.getDatabase(get()) }
    factory { get<HealthDatabase>().bloodSugarDao() }
    factory { get<HealthDatabase>().bloodPressureDao() }
    factory { get<HealthDatabase>().alarmDao() }
    factory { get<HealthDatabase>().healthTagDao() }
    factory { get<HealthDatabase>().medicineReminderDao() }
    factory { get<HealthDatabase>().bmiDao() }
    factory { get<HealthDatabase>().heartRateDao() }
    factory { get<HealthDatabase>().cholesterolDao() }
    factory { get<HealthDatabase>().hydrateDao() }
    factory { get<HealthDatabase>().hydrateReminderDao() }

    single { BloodPressureRepository(get()) }
    single { BloodSugarRepository(get()) }
    single { BmiRepository(get()) }
    single { CholesterolRepository(get()) }
    single { HeartRateRepository(get()) }
    single { HydrateRepository(get()) }
    single { AlarmRepository(get()) }
    single { HealthTagRepository(get(), get<Context>()) }

    single { AlarmScheduler(get<Context>()) }
    single { AlarmNotificationManager(get<Context>()) }
    single { MedicineReminderRepository(get(), get(), get()) }

    factory { SplashViewModel(get(), get(), get(), get(), get()) }

    factory { HomeViewModel(get(), get(), get(), get(), get(), get()) }
    factory { MedsViewModel(get()) }
    factory { TrackerViewModel(get(), get(), get(), get(), get(), get()) }

    factory { AlarmViewModel(get(), get()) }
    factory { BsRecordViewModel(get(), get()) }
    factory { BpRecordViewModel(get(), get()) }
    factory { BsDetailViewModel(get(), get()) }
    viewModel { (handle: SavedStateHandle) -> BpDetailViewModel(get(), handle) }
    factory { BmiRecordViewModel(get(), get()) }
    viewModel { (handle: SavedStateHandle) -> BmiDetailViewModel(get(), handle) }
    factory { HeartRateRecordViewModel(get(), get()) }
    viewModel { (handle: SavedStateHandle) -> HeartRateDetailViewModel(get(), get(), handle) }
    factory { CholesterolRecordViewModel(get()) }
    factory { CholesterolDetailViewModel(get()) }
    factory { HydrateViewModel(get()) }
    factory { HydrateSettingViewModel(get(), get(), get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> HistoryViewModel(get(), get(), get(), get(), get(), handle) }
    factory { AddReminderViewModel(get()) }
    factory { TargetRangeViewModel() }
    factory { StepSettingViewModel(get()) }
    viewModel { (handle: SavedStateHandle) -> HealthStatisticsViewModel(get(), get(), get(), get(), get(), get(), get(), handle) }
    factory { StepCountViewModel() }
}

val frameworkConfigModule = module {
    single { FirebaseRemoteConfig.getInstance() }
    single { Gson() }
    single { ConfigRegistry() }
    single { ConfigCache() }
}

val appConfigModule = module {
    single { PushMessageParser(get(), get<Context>()) }
    single { PushConfigParser(get(), get(), get()) }
    single { AppConfigRegistry(get(), get(), get()) }
}
