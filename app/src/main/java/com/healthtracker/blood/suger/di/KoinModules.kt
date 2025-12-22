package com.healthtracker.blood.suger.di

import android.app.Application
import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.healthtracker.blood.suger.AppInitializer
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.config.parsers.PushConfigParser
import com.healthtracker.blood.suger.config.parsers.PushMessageParser
import com.healthtracker.blood.suger.config.registry.AppConfigRegistry
import com.healthtracker.blood.suger.data.database.HealthDatabase
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.blood.suger.data.repository.BloodPressureRepository
import com.healthtracker.blood.suger.data.repository.BloodSugarRepository
import com.healthtracker.blood.suger.data.repository.BmiRepository
import com.healthtracker.blood.suger.data.repository.CholesterolRepository
import com.healthtracker.blood.suger.data.repository.HeartRateRepository
import com.healthtracker.blood.suger.data.repository.HydrateRepository
import com.healthtracker.blood.suger.data.repository.AlarmRepository
import com.healthtracker.blood.suger.data.repository.HealthTagRepository
import com.healthtracker.blood.suger.data.repository.MedicineReminderRepository
import com.healthtracker.blood.suger.alarm.AlarmNotificationManager
import com.healthtracker.blood.suger.alarm.AlarmScheduler
import com.healthtracker.blood.suger.helper.CustomNotificationHelper
import com.healthtracker.blood.suger.helper.NotificationResourceMapper
import com.healthtracker.blood.suger.helper.ResidentNotificationHelper
import com.healthtracker.blood.suger.manager.HealthServiceManager
import com.healthtracker.blood.suger.observer.AppForegroundObserver
import com.healthtracker.blood.suger.observer.HealthServiceForegroundObserver
import com.healthtracker.blood.suger.strategy.LoopPushManager
import com.healthtracker.blood.suger.strategy.PushFrequencyController
import com.healthtracker.blood.suger.strategy.PushMessageRepository
import com.healthtracker.blood.suger.strategy.PushMessageSelector
import com.healthtracker.blood.suger.strategy.PushOrchestrator
import com.healthtracker.blood.suger.ui.chart.HealthLineChartManager
import com.healthtracker.blood.suger.ui.viewmodel.SplashViewModel
import com.healthtracker.blood.suger.ui.viewmodel.MainViewModel
import com.healthtracker.blood.suger.ui.viewmodel.HomeViewModel
import com.healthtracker.blood.suger.ui.viewmodel.MedsViewModel
import com.healthtracker.blood.suger.ui.viewmodel.TrackerViewModel
import com.healthtracker.blood.suger.ui.viewmodel.AlarmViewModel
import com.healthtracker.blood.suger.ui.viewmodel.BsRecordViewModel
import com.healthtracker.blood.suger.ui.viewmodel.BpRecordViewModel
import com.healthtracker.blood.suger.ui.viewmodel.BsDetailViewModel
import com.healthtracker.blood.suger.ui.viewmodel.BpDetailViewModel
import com.healthtracker.blood.suger.ui.viewmodel.BmiRecordViewModel
import com.healthtracker.blood.suger.ui.viewmodel.BmiDetailViewModel
import com.healthtracker.blood.suger.ui.viewmodel.HeartRateRecordViewModel
import com.healthtracker.blood.suger.ui.viewmodel.HeartRateDetailViewModel
import com.healthtracker.blood.suger.ui.viewmodel.CholesterolRecordViewModel
import com.healthtracker.blood.suger.ui.viewmodel.CholesterolDetailViewModel
import com.healthtracker.blood.suger.ui.viewmodel.HydrateViewModel
import com.healthtracker.blood.suger.ui.viewmodel.HydrateSettingViewModel
import com.healthtracker.blood.suger.ui.viewmodel.HistoryViewModel
import com.healthtracker.blood.suger.ui.viewmodel.AddReminderViewModel
import com.healthtracker.blood.suger.ui.viewmodel.TargetRangeViewModel
import com.healthtracker.blood.suger.ui.viewmodel.StepSettingViewModel
import com.healthtracker.blood.suger.viewmodel.HealthStatisticsViewModel
import com.healthtracker.blood.suger.ui.act.StepCountViewModel
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
