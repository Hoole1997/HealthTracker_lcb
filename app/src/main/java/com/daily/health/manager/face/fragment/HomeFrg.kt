package com.daily.health.manager.face.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.daily.health.manager.R
import com.daily.health.manager.config.HydrateSettingManager
import com.daily.health.manager.data.entity.BloodSugarRecord
import com.daily.health.manager.data.entity.BmiRecord
import com.daily.health.manager.data.entity.CholesterolRecord
import com.daily.health.manager.data.entity.DailyStepStat
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.data.enums.BsUnit
import com.daily.health.manager.databinding.HtFragmentHomeBinding
import com.daily.health.manager.face.act.HealthRecordScreen
import com.daily.health.manager.face.act.HistoryRecordScreen
import com.daily.health.manager.face.act.HydrateScreen
import com.daily.health.manager.face.act.MainScreen
import com.daily.health.manager.face.act.ProfileActivity
import com.daily.health.manager.face.compose.HomeDashboardScreen
import com.daily.health.manager.face.compose.HomeFeatureCardUi
import com.daily.health.manager.face.compose.HomeHeroUi
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.trackEnterPageClick
import com.daily.health.manager.face.viewmodel.HomeViewModel
import com.daily.health.manager.hasAddProfile
import com.daily.health.manager.util.CholesterolCalculator
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import kotlinx.coroutines.CompletableDeferred
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFrg : BaseMVVMFragment<HomeViewModel, HtFragmentHomeBinding>() {

    private val profileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            navigateToPendingActivity()
        }
        pendingActivityType = null
    }

    private var pendingActivityType: PendingActivityType? = null

    var highLightComplete = CompletableDeferred<Boolean>()

    private enum class PendingActivityType {
        BMI,
        CHOLESTEROL,
        HEART_RATE,
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean,
    ) = HtFragmentHomeBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = HomeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.composeView?.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind?.composeView?.setContent {
            val bloodSugarRecord by mViewModel.latestBloodSugarRecord.collectAsStateWithLifecycle()
            val bloodPressureRecord by mViewModel.latestBloodPressureRecord.collectAsStateWithLifecycle()
            val bmiRecord by mViewModel.latestBmiRecord.collectAsStateWithLifecycle()
            val heartRateRecord by mViewModel.latestHeartRateRecord.collectAsStateWithLifecycle()
            val cholesterolRecord by mViewModel.latestCholesterolRecord.collectAsStateWithLifecycle()
            val todayTotalIntakeMl by mViewModel.todayTotalIntakeMl.collectAsStateWithLifecycle()
            val todayStepStat by mViewModel.todayStepStat.collectAsStateWithLifecycle()

            HealthTrackerTheme {
                HomeDashboardScreen(
                    hero = buildHeroUi(heartRateRecord),
                    cards = buildCardUiList(
                        bloodSugarRecord = bloodSugarRecord,
                        bloodPressureRecord = bloodPressureRecord,
                        cholesterolRecord = cholesterolRecord,
                        bmiRecord = bmiRecord,
                        todayTotalIntakeMl = todayTotalIntakeMl,
                        todayStepStat = todayStepStat,
                    ),
                    onHeartRateClick = {
                        navigateToActivityWithProfileCheck(PendingActivityType.HEART_RATE)
                    },
                    onBloodSugarCardClick = {
                        openBloodSugarHistoryOrRecord(bloodSugarRecord?.id)
                    },
                    onBloodSugarRecordClick = ::openBloodSugarRecord,
                    onBloodPressureClick = ::openBloodPressureRecord,
                    onCholesterolClick = {
                        navigateToActivityWithProfileCheck(PendingActivityType.CHOLESTEROL)
                    },
                    onBmiClick = {
                        navigateToActivityWithProfileCheck(PendingActivityType.BMI)
                    },
                    onHydrateClick = ::openHydrate,
                    onStepCountClick = ::openStepCount,
                    onBloodSugarBoundsChanged = {},
                )
            }
        }
    }

    fun onNotificationPermissionFlowFinished() {
        if (!highLightComplete.isCompleted) {
            highLightComplete.complete(true)
        }
    }

    private fun buildHeroUi(record: HeartRateRecord?): HomeHeroUi {
        val bpmText = record?.heartRateBpm?.toString() ?: "--"
        val footerText = record?.recordTime?.let(::formatAbsoluteTime) ?: getString(R.string.ht_click_to_record)
        return HomeHeroUi(
            title = getString(R.string.ht_heart_rate),
            subtitle = getString(R.string.ht_home_heart_subtitle),
            cta = getString(R.string.ht_measure_now),
            value = bpmText,
            valueUnit = getString(R.string.ht_bpm),
            footerText = footerText,
        )
    }

    private fun buildCardUiList(
        bloodSugarRecord: BloodSugarRecord?,
        bloodPressureRecord: com.daily.health.manager.data.entity.BloodPressureRecord?,
        cholesterolRecord: CholesterolRecord?,
        bmiRecord: BmiRecord?,
        todayTotalIntakeMl: Int,
        todayStepStat: DailyStepStat?,
    ): List<HomeFeatureCardUi> {
        val targetMl = HydrateSettingManager.getDailyTargetMl()
        return listOf(
            HomeFeatureCardUi.BloodPressure(
                title = getString(R.string.ht_blood_pressure),
                value = bloodPressureRecord?.let { "${it.systolicPressure}/${it.diastolicPressure}" } ?: "-/-",
                unit = getString(R.string.ht_mmHg),
            ),
            HomeFeatureCardUi.BloodSugar(
                title = getString(R.string.ht_blood_suger),
                value = bloodSugarRecord?.getFormattedDisplayValue() ?: "--",
                unit = bloodSugarRecord?.let {
                    if (it.selectedUnit == BsUnit.MG_DL.value) {
                        BsUnit.MG_DL.displayName
                    } else {
                        BsUnit.MMOL_L.displayName
                    }
                } ?: BsUnit.MG_DL.displayName,
            ),
            HomeFeatureCardUi.Cholesterol(
                title = getString(R.string.ht_cholesterol),
                value = formatCholesterolValue(cholesterolRecord),
                unit = BsUnit.MG_DL.displayName,
            ),
            HomeFeatureCardUi.Bmi(
                title = getString(R.string.ht_weight_and_bmi),
                value = bmiRecord?.getDisplayWeightValue() ?: "--",
                unit = BmiUnit.getWeightUnitLabel(),
            ),
            HomeFeatureCardUi.Hydrate(
                title = getString(R.string.ht_hydrate),
                currentValue = todayTotalIntakeMl.toString(),
                targetValue = targetMl.toString(),
                unit = getString(R.string.ht_ml).lowercase(Locale.ROOT),
            ),
            HomeFeatureCardUi.StepCount(
                title = getString(R.string.ht_step_count),
                stepsValue = todayStepStat?.steps?.toString() ?: "0",
                stepsUnit = getString(R.string.ht_text_steps).lowercase(Locale.ROOT),
                kcalValue = formatKcal(todayStepStat?.kcal),
                kcalUnit = getString(R.string.ht_kcal).lowercase(Locale.ROOT),
            ),
        )
    }

    private fun openBloodSugarHistoryOrRecord(recordId: Long?) {
        recordId?.let {
            requireActivity().startActivity<HistoryRecordScreen>()
        } ?: openBloodSugarRecord()
    }

    private fun openBloodSugarRecord() {
        requireContext().trackEnterPageClick(HealthType.BLOOD_SUGAR)
        HealthRecordScreen.start(
            requireActivity(),
            HealthRecordScreen.RecordType.BLOOD_SUGAR,
        )
    }

    private fun openBloodPressureRecord() {
        requireContext().trackEnterPageClick(HealthType.BLOOD_PRESSURE)
        HealthRecordScreen.start(
            requireActivity(),
            HealthRecordScreen.RecordType.BLOOD_PRESSURE,
        )
    }

    private fun openHydrate() {
        requireContext().trackEnterPageClick(HealthType.HYDRATE)
        requireActivity().startActivity<HydrateScreen>()
    }

    private fun openStepCount() {
        (requireActivity() as? MainScreen)?.checkStepPermissionAndNavigate()
    }

    private fun formatCholesterolValue(record: CholesterolRecord?): String {
        if (record == null) {
            return "--"
        }
        val totalValue = record.tc ?: run {
            val hdl = record.hdl?.toFloat()
            val ldl = record.ldl?.toFloat()
            val triglyceride = record.triglyceride?.toFloat()
            if (hdl != null && ldl != null && triglyceride != null) {
                CholesterolCalculator.calculateTotalCholesterol(hdl, ldl, triglyceride)
            } else {
                null
            }
        }
        return totalValue?.let {
            NumberFormatter.formatNumber(
                it.toDouble(),
                LanguageUtils.mapAppLocale() ?: Locale.getDefault(),
                0,
            )
        } ?: "--"
    }

    private fun formatKcal(kcal: Double?): String {
        if (kcal == null) {
            return "0"
        }
        val decimalCount = if (kcal % 1.0 == 0.0) 0 else 1
        return NumberFormatter.formatNumber(
            kcal,
            LanguageUtils.getAppLocale(requireContext()),
            decimalCount,
        )
    }

    private fun formatAbsoluteTime(recordTime: Date): String {
        val locale = LanguageUtils.getAppLocale(requireContext())
        return SimpleDateFormat("HH:mm EEE,MMM d", locale).format(recordTime)
    }

    @SuppressLint("StringFormatInvalid")
    private fun formatRelativeTime(recordTime: Date): String {
        val currentTime = System.currentTimeMillis()
        val recordTimeMs = recordTime.time
        val timeDiff = currentTime - recordTimeMs

        if (timeDiff < 0) {
            return getString(R.string.ht_latest)
        }

        val seconds = timeDiff / 1000
        return when {
            seconds < 60 -> {
                if (seconds <= 0) {
                    getString(R.string.ht_just_now)
                } else {
                    getString(R.string.ht_seconds_ago, seconds.toInt())
                }
            }

            seconds < 3600 -> getString(R.string.ht_minutes_ago, (seconds / 60).toInt())
            seconds < 86400 -> getString(R.string.ht_hours_ago, (seconds / 3600).toInt())
            else -> getString(R.string.ht_days_ago, (seconds / 86400).toInt())
        }
    }

    private fun navigateToActivityWithProfileCheck(activityType: PendingActivityType) {
        if (hasAddProfile()) {
            navigateToActivity(activityType)
        } else {
            pendingActivityType = activityType
            profileLauncher.launch(ProfileActivity.createGuideIntent(requireContext()))
        }
    }

    private fun navigateToPendingActivity() {
        pendingActivityType?.let(::navigateToActivity)
    }

    private fun navigateToActivity(activityType: PendingActivityType) {
        when (activityType) {
            PendingActivityType.BMI -> {
                requireContext().trackEnterPageClick(HealthType.BMI)
                HealthRecordScreen.start(requireActivity(), HealthRecordScreen.RecordType.BMI)
            }

            PendingActivityType.CHOLESTEROL -> {
                requireContext().trackEnterPageClick(HealthType.CHOLESTEROL)
                HealthRecordScreen.start(requireActivity(), HealthRecordScreen.RecordType.CHOLESTEROL)
            }

            PendingActivityType.HEART_RATE -> {
                requireContext().trackEnterPageClick(HealthType.HEART_RATE)
                HealthRecordScreen.start(requireActivity(), HealthRecordScreen.RecordType.HEART_RATE)
            }
        }
    }
}
