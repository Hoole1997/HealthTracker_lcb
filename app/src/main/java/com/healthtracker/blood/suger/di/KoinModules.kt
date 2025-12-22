package com.healthtracker.blood.suger.di

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.healthtracker.blood.suger.config.parsers.PushConfigParser
import com.healthtracker.blood.suger.config.parsers.PushMessageParser
import com.healthtracker.blood.suger.config.registry.AppConfigRegistry
import com.healthtracker.blood.suger.data.database.HealthDatabase
import com.healthtracker.blood.suger.data.repo.StepRepository
import com.healthtracker.framework.config.core.ConfigCache
import com.healthtracker.framework.config.core.ConfigRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single<CoroutineDispatcher>(named("IoDispatcher")) { Dispatchers.IO }
    single<CoroutineDispatcher>(named("MainDispatcher")) { Dispatchers.Main }
    single<CoroutineDispatcher>(named("DefaultDispatcher")) { Dispatchers.Default }

    single { StepRepository.get(get<Context>()) }
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
