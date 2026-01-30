package com.daily.health.manager.face.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.daily.health.manager.R
import com.daily.health.manager.config.HydrateSettingManager
import com.daily.health.manager.data.entity.BloodPressureRecord
import com.daily.health.manager.data.entity.BloodSugarRecord
import com.daily.health.manager.data.entity.BmiRecord
import com.daily.health.manager.data.entity.CholesterolRecord
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.data.enums.BsUnit
import com.daily.health.manager.databinding.HtFragmentHomeBinding
import com.daily.health.manager.hasAddProfile
import com.daily.health.manager.hasShowAllGuide
import com.daily.health.manager.hasShowGuideBs
import com.daily.health.manager.saveShowGuideBs
import com.daily.health.manager.face.act.HealthRecordScreen
import com.daily.health.manager.face.act.HistoryRecordScreen
import com.daily.health.manager.face.act.HydrateScreen
import com.daily.health.manager.face.act.MainScreen
import com.daily.health.manager.face.act.ProfileActivity
import com.daily.health.manager.face.act.reportGuide
import com.daily.health.manager.face.viewmodel.HomeViewModel
import com.daily.health.manager.util.CholesterolCalculator
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.startActivity
import com.hyy.highlightpro.HighlightPro
import com.hyy.highlightpro.parameter.Constraints
import com.hyy.highlightpro.parameter.HighlightParameter
import com.hyy.highlightpro.parameter.MarginOffset
import com.hyy.highlightpro.shape.RectShape
import com.hyy.highlightpro.util.dp
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.trackEnterPageClick
import com.daily.health.manager.helper.HealthTrackerEvaluateListener
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.CompletableDeferred
import java.util.Date
import java.util.Locale

/**
 * 首页Fragment
 * 显示最近一次的血糖和血压记录
 */
class HomeFrg: BaseMVVMFragment<HomeViewModel, HtFragmentHomeBinding>() {

   companion object{
       private const val TAG = "HomeFragment"
   }

    private var latestSugerID :Long? = null

    private val profileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            navigateToPendingActivity()
        }
        pendingActivityType = null
    }

    private var pendingActivityType: PendingActivityType? = null

    private enum class PendingActivityType {
        BMI,
        CHOLESTEROL,
        HEART_RATE
    }

    var highLightComplete = CompletableDeferred<Boolean>()

    private var notificationPermissionFlowFinished = false

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtFragmentHomeBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = HomeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.run {
            clHistory.clickWithDuration {
                latestSugerID?.let {
                    requireActivity().startActivity<HistoryRecordScreen>()
                } ?: HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.BLOOD_SUGAR
                )
            }

            clPsRecord.clickWithDuration {
                requireContext().trackEnterPageClick(HealthType.BLOOD_SUGAR)
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.BLOOD_SUGAR
                )

            }
            btnRecordNow.clickWithDuration {
                requireContext().trackEnterPageClick(HealthType.BLOOD_SUGAR)
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.BLOOD_SUGAR
                )
            }

            clBloodPressure.clickWithDuration {
                requireContext().trackEnterPageClick(HealthType.BLOOD_PRESSURE)
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.BLOOD_PRESSURE
                )
            }

            clHeartRate.clickWithDuration {
                navigateToActivityWithProfileCheck(PendingActivityType.HEART_RATE)
            }

            clCholesterol.clickWithDuration {

                navigateToActivityWithProfileCheck(PendingActivityType.CHOLESTEROL)
            }

            clBmi.clickWithDuration {

                navigateToActivityWithProfileCheck(PendingActivityType.BMI)
            }

            clHydrate.clickWithDuration {
                requireContext().trackEnterPageClick(HealthType.HYDRATE)
                requireActivity().startActivity<HydrateScreen>()
            }
            clStepCount.clickWithDuration {
                if(requireActivity() is MainScreen){
                    (requireActivity() as MainScreen).checkStepPermissionAndNavigate()
                }
            }


        }
        observeData()
    }

    /**
     * 观察数据变化
     */
    private fun observeData() {
        collectLatest(mViewModel.latestBloodSugarRecord) {
            updateBloodSugarUI(it)
        }

        collectLatest(mViewModel.latestBloodPressureRecord) {
            updateBloodPressureUI(it)
        }

        collectLatest(mViewModel.latestBmiRecord) {
            updateBmiUI(it)
        }

        collectLatest(mViewModel.latestHeartRateRecord) { record ->
            updateHeartRateUI(record)
        }

        collectLatest(mViewModel.latestCholesterolRecord) { record ->
            updateCholesterolUI(record)
        }

        collectLatest(mViewModel.todayTotalIntakeMl) { totalMl ->
            mViewBind?.run {
                tvHasDrinkCup.text = totalMl.toString()
                updateWaterStatus(totalMl)
            }
        }

        collectLatest(mViewModel.todayStepStat) { stat ->
            mViewBind?.run {
                tvStepCountValue.text = stat?.steps?.toString() ?: "0"
                tvStepKcal.text = stat?.let { 
                    NumberFormatter.formatNumber(it.kcal, LanguageUtils.getAppLocale(requireContext()), 1)
                } ?: "0"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateWaterStatus(mViewModel.todayTotalIntakeMl.value)
        isHeighLightLeave = false
        if (notificationPermissionFlowFinished) {
            guidFeature()
        }
    }

    fun onNotificationPermissionFlowFinished() {
        if (notificationPermissionFlowFinished) {
            return
        }
        notificationPermissionFlowFinished = true
        if (isResumed) {
            guidFeature()
        }
        if (hasShowAllGuide()) {
            if (!highLightComplete.isCompleted) {
                highLightComplete.complete(true)
            }
        }
    }

    private fun updateWaterStatus(currentIntakeMl: Int) {
        // 显示目标饮水量，格式: /2000ml
        val targetMl = HydrateSettingManager.getDailyTargetMl()
        mViewBind?.tvTargetCupCount?.text = "/${targetMl}ml"

        // 根据是否达标显示不同图标
        val statusRes = if (currentIntakeMl >= targetMl) R.drawable.ht_ic_checked else R.drawable.ht_ic_hydrate_not_reach_goal
        mViewBind?.ivTargetStatus?.setImageResource(statusRes)
    }

    /**
     * 更新血糖记录UI
     */
    private fun updateBloodSugarUI(record: BloodSugarRecord?) {
        if (record == null) {
            latestSugerID = null
            mViewBind?.tvLatestBsValue?.text = "--"
            mViewBind?.tvLatestRecordDate?.text = getString(R.string.ht_click_to_record)
            return
        }
        latestSugerID = record.id
        // 根据用户选择的单位显示血糖值（保留一位小数）
        mViewBind?.tvLatestBsValue?.text = record.getFormattedDisplayValue()
        mViewBind?.tvLatestBsUnit?.text =
            if (record.selectedUnit == BsUnit.MG_DL.value) BsUnit.MG_DL.displayName else BsUnit.MMOL_L.displayName
        // 显示相对时间
        mViewBind?.tvLatestRecordDate?.text = formatRelativeTime(record.recordTime)
    }

    /**
     * 更新血压记录UI
     */
    private fun updateBloodPressureUI(record: BloodPressureRecord?) {
        if (record == null) {
            mViewBind?.tvLatestBpValue?.text = "-/-"
            return
        }
        "${record.systolicPressure}/${record.diastolicPressure}".also {
            mViewBind?.tvLatestBpValue?.text = it
        }
    }

    /**
     * 更新 BMI（体重）记录UI
     */
    private fun updateBmiUI(record: BmiRecord?) {
        mViewBind?.tvLatestWeightUnit?.text = BmiUnit.getWeightUnitLabel()
        if (record == null) {
            mViewBind?.tvLatestWeightValue?.text = "--"
            return
        }
        mViewBind?.tvLatestWeightValue?.text = record.getDisplayWeightValue()
    }

    /**
     * 更新心率记录UI
     */
    private fun updateHeartRateUI(record: HeartRateRecord?) {
        val unitText = getString(R.string.ht_bpm)
        if (record == null) {
            mViewBind?.tvLatestHeartRateValue?.text = "--"
            mViewBind?.tvLatestHeartRateUnit?.text = unitText
        } else {
            mViewBind?.tvLatestHeartRateValue?.text = record.heartRateBpm.toString()
            mViewBind?.tvLatestHeartRateUnit?.text = unitText
        }
    }

    /**
     * 更新胆固醇记录UI
     */
    private fun updateCholesterolUI(record: CholesterolRecord?) {
        if (record == null) {
            mViewBind?.tvLatestCholesterolValue?.text = "--"
            return
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
        val displayText = totalValue?.let { NumberFormatter.formatNumber(it.toDouble(), LanguageUtils.mapAppLocale() ?: Locale.getDefault(), 0) } ?: "--"
        mViewBind?.tvLatestCholesterolValue?.text = displayText
    }

    /**
     * 格式化相对时间显示
     * @param recordTime 记录时间
     * @return 格式化后的时间字符串
     */
    @SuppressLint("StringFormatInvalid")
    private fun formatRelativeTime(recordTime: Date): String {
        val currentTime = System.currentTimeMillis()
        val recordTimeMs = recordTime.time
        val timeDiff = currentTime - recordTimeMs

        // 如果记录时间在当前时间之后，显示Latest
        if (timeDiff < 0) {
            return getString(R.string.ht_latest)
        }

        // 转换为秒
        val seconds = timeDiff / 1000
        return when {
            seconds < 60 -> {
                // Less than 1 minute: show "just now" if 0 seconds, otherwise show "x seconds ago"
                if (seconds <= 0) {
                    getString(R.string.ht_just_now)
                } else {
                    getString(R.string.ht_seconds_ago, seconds.toInt())
                }
            }

            seconds < 3600 -> {
                // 不满1小时：x分钟前
                val minutes = seconds / 60
                getString(R.string.ht_minutes_ago, minutes.toInt())
            }

            seconds < 86400 -> {
                // 不满1天：x小时前
                val hours = seconds / 3600
                getString(R.string.ht_hours_ago, hours.toInt())
            }

            else -> {
                // 超过1天：x天前
                val days = seconds / 86400
                getString(R.string.ht_days_ago, days.toInt())
            }
        }
    }

    private var isShowHighligh = false
    private var isHeighLightLeave = false
    private fun guidFeature(): Boolean {
        if (!notificationPermissionFlowFinished) {
            return false
        }
        if (hasShowAllGuide()) {
            if (!highLightComplete.isCompleted) {
                highLightComplete.complete(true)
            }
            return false
        }

        if(isShowHighligh){
            return false
        }

        return guideBs()
    }

    private fun onGuideDismissed() {
        if (hasShowAllGuide()) {
            // 所有引导已完成，设置待评分标记
            SpUtils.putBoolean(
                HealthTrackerEvaluateListener.KEY_PENDING_RATE_AFTER_ONBOARDING,
                true
            )
            if (!highLightComplete.isCompleted) {
                highLightComplete.complete(true)
            }
            return
        }
        if (!isHeighLightLeave && isResumed) {
            guidFeature()
        }
    }

    private fun guideBs(): Boolean {
        if (hasShowGuideBs()) {
            return false
        }
        HighlightPro.with(this)
            .setHighlightParameter {
                HighlightParameter.Builder()
                    .setHighlightViewId(R.id.cl_ps_record)
                    .setTipsViewId(R.layout.ht_layout_guide_bs)
                    .setHighlightShape(RectShape(4f.dp, 4f.dp, 12f))
                    .setHighlightHorizontalPadding(0f.dp)
                    .setConstraints(Constraints.StartToStartOfHighlight + Constraints.TopToBottomOfHighlight + Constraints.EndToEndOfHighlight)
                    .setMarginOffset(MarginOffset(start = 7.dp, top = 16.dp, end = 16.dp))
                    .build()
            }
            .interceptBackPressed(true)
            .setOnMaskViewClickCallback { index ->
                //do something
                isShowHighligh = true
                isHeighLightLeave = true
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.BLOOD_SUGAR
                )
                reportGuide(7)
            }
            .setOnShowCallback {
                isShowHighligh = true
                saveShowGuideBs()
            }
            .setOnDismissCallback {
                isShowHighligh = false
                onGuideDismissed()
            }
            .show()

        return true

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
        pendingActivityType?.let { activityType ->
            navigateToActivity(activityType)
        }
    }

    private fun navigateToActivity(activityType: PendingActivityType) {
        when (activityType) {
            PendingActivityType.BMI -> {
                requireContext().trackEnterPageClick(HealthType.BMI)
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.BMI
                )
            }
            PendingActivityType.CHOLESTEROL -> {
                requireContext().trackEnterPageClick(HealthType.CHOLESTEROL)
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.CHOLESTEROL
                )
            }
            PendingActivityType.HEART_RATE -> {
                requireContext().trackEnterPageClick(HealthType.HEART_RATE)
                HealthRecordScreen.start(
                    requireActivity(),
                    HealthRecordScreen.RecordType.HEART_RATE
                )
            }
        }
    }


}
