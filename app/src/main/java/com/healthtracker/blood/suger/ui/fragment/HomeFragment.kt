package com.healthtracker.blood.suger.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.entity.CholesterolRecord
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.BmiUnit
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.databinding.FragmentHomeBinding
import com.healthtracker.blood.suger.hasAddProfile
import com.healthtracker.blood.suger.hasShowAllGuide
import com.healthtracker.blood.suger.hasShowGuideBp
import com.healthtracker.blood.suger.hasShowGuideBs
import com.healthtracker.blood.suger.hasShowGuideHr
import com.healthtracker.blood.suger.saveShowGuideBp
import com.healthtracker.blood.suger.saveShowGuideBs
import com.healthtracker.blood.suger.saveShowGuideHr
import com.healthtracker.blood.suger.ui.act.BmiRecordActivity
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.act.BsRecordActivity
import com.healthtracker.blood.suger.ui.act.CholesterolRecordActivity
import com.healthtracker.blood.suger.ui.act.HeartRateRecordActivity
import com.healthtracker.blood.suger.ui.act.HistoryRecordActivity
import com.healthtracker.blood.suger.ui.act.HydrateActivity
import com.healthtracker.blood.suger.ui.act.MainActivity
import com.healthtracker.blood.suger.ui.act.ProfileActivity
import com.healthtracker.blood.suger.ui.act.reportGuide
import com.healthtracker.blood.suger.ui.dialog.NativeCardDialog
import com.healthtracker.blood.suger.ui.viewmodel.HomeViewModel
import com.healthtracker.blood.suger.util.CholesterolCalculator
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
import dagger.hilt.android.AndroidEntryPoint
import net.corekit.core.report.ReportDataManager
import java.util.Date
import java.util.Locale

/**
 * 首页Fragment
 * 显示最近一次的血糖和血压记录
 */
@AndroidEntryPoint
class HomeFragment: BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {

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

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentHomeBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = HomeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.run {
            clHistory.clickWithDuration {
                latestSugerID?.let {
                    requireActivity().startActivity<HistoryRecordActivity>()
                } ?: requireActivity().startActivity<BsRecordActivity>()
            }

            clPsRecord.clickWithDuration {
                reportEnterPage("Blood Sugar")
                BsRecordActivity.start(requireActivity())

            }
            btnRecordNow.clickWithDuration {
                reportEnterPage("Blood Sugar")
                BsRecordActivity.start(requireActivity())
            }

            clBloodPressure.clickWithDuration {
                reportEnterPage("Blood Pressure")
                requireActivity().startActivity<BpRecordActivity>()
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
                reportEnterPage("Hydrate")
                requireActivity().startActivity<HydrateActivity>()
            }
            clStepCount.clickWithDuration {
                if(requireActivity() is MainActivity){
                    (requireActivity() as MainActivity).checkStepPermissionAndNavigate()
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

        collectLatest(mViewModel.todyCupCount) { count ->
            mViewBind?.run {
                tvHasDrinkCup.text = count ?: ""
            }
        }

        collectLatest(mViewModel.todayStepStat) { stat ->
            mViewBind?.tvStepCountValue?.text = stat?.steps?.toString() ?: "0"
        }
    }

    override fun onResume() {
        super.onResume()
        mViewBind?.tvTargetCupCount?.text = "/${HydrateSettingManager.getDailyCups()}"
        if (!guidFeature() && !isHeighLightLeave) {
            NativeCardDialog.showOncePerMinute(requireActivity())
        }
        isHeighLightLeave = false
    }

    /**
     * 更新血糖记录UI
     */
    private fun updateBloodSugarUI(record: BloodSugarRecord?) {
        if (record == null) {
            latestSugerID = null
            mViewBind?.tvLatestBsValue?.text = "--"
            mViewBind?.tvLatestRecordDate?.text = getString(R.string.click_to_record)
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
        val unitText = getString(R.string.bpm)
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
        val displayText = totalValue?.let { String.format(Locale.getDefault(), "%.0f", it) } ?: "--"
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
            return getString(R.string.latest)
        }

        // 转换为秒
        val seconds = timeDiff / 1000
        return when {
            seconds < 60 -> {
                // Less than 1 minute: show "just now" if 0 seconds, otherwise show "x seconds ago"
                if (seconds <= 0) {
                    getString(R.string.just_now)
                } else {
                    getString(R.string.seconds_ago, seconds.toInt())
                }
            }

            seconds < 3600 -> {
                // 不满1小时：x分钟前
                val minutes = seconds / 60
                getString(R.string.minutes_ago, minutes.toInt())
            }

            seconds < 86400 -> {
                // 不满1天：x小时前
                val hours = seconds / 3600
                getString(R.string.hours_ago, hours.toInt())
            }

            else -> {
                // 超过1天：x天前
                val days = seconds / 86400
                getString(R.string.days_ago, days.toInt())
            }
        }
    }

    private fun getWeightUnitLabel(unit: BmiUnit) = if (unit == BmiUnit.METRIC) {
        getString(R.string.unit_kg)
    } else {
        getString(R.string.unit_lb)
    }

    private var isShowHighligh = false
    private var isHeighLightLeave = false
    private fun guidFeature(): Boolean {
        if (hasShowAllGuide() || isShowHighligh) {
            return false
        }

        if (guideBp()) {
            return true
        }

        if (guideBs()) {
            return true
        }

        if (guideHr()) {
            return true
        }
        return false

    }

    private fun guideBp(): Boolean {
        if (hasShowGuideBp()) {
            return false
        }
        HighlightPro.with(this)
            .setHighlightParameter {
                HighlightParameter.Builder()
                    .setHighlightViewId(R.id.cl_blood_pressure)
                    .setTipsViewId(R.layout.layout_guide_bp)
                    .setHighlightShape(RectShape(4f.dp, 4f.dp, 12f))
                    .setHighlightHorizontalPadding(0f.dp)
                    .setConstraints(Constraints.StartToStartOfHighlight + Constraints.TopToBottomOfHighlight + Constraints.EndToEndOfHighlight)
                    .setMarginOffset(MarginOffset(start = 7.dp, top = 16.dp, end = 16.dp))
                    .build()
            }
            .setOnMaskViewClickCallback { index ->
                //do something
                BpRecordActivity.start(requireActivity())
                reportGuide(5)
            }
            .setOnShowCallback {
                isShowHighligh = true
                saveShowGuideBp()
            }
            .setOnDismissCallback {
                isShowHighligh = false
            }
            .show()

        return true

    }

    private fun guideBs(): Boolean {
        if (hasShowGuideBs()) {
            return false
        }
        HighlightPro.with(this)
            .setHighlightParameter {
                HighlightParameter.Builder()
                    .setHighlightViewId(R.id.cl_ps_record)
                    .setTipsViewId(R.layout.layout_guide_bs)
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
                BsRecordActivity.start(requireActivity())
                reportGuide(6)
            }
            .setOnShowCallback {
                saveShowGuideBs()
            }
            .setOnDismissCallback {
                isShowHighligh = false
            }
            .show()

        return true

    }

    private fun guideHr(): Boolean {
        if (hasShowGuideHr()) {
            return false
        }
        HighlightPro.with(this)
            .setHighlightParameter {
                HighlightParameter.Builder()
                    .setHighlightViewId(R.id.cl_heart_rate)
                    .setTipsViewId(R.layout.layout_guide_hr)
                    .setHighlightShape(RectShape(4f.dp, 4f.dp, 12f))
                    .setHighlightHorizontalPadding(0f.dp)
                    .setConstraints(Constraints.StartToStartOfHighlight + Constraints.TopToBottomOfHighlight + Constraints.EndToEndOfHighlight)
                    .setMarginOffset(MarginOffset(start = 7.dp, top = 16.dp, end = 16.dp))
                    .build()
            }
            .setOnMaskViewClickCallback { index ->
                //do something
                isHeighLightLeave = true
                navigateToActivityWithProfileCheck(PendingActivityType.HEART_RATE)
                reportGuide(7)
            }
            .setOnShowCallback {
                isShowHighligh = true
                saveShowGuideHr()
            }
            .setOnDismissCallback {
                isShowHighligh = false
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
                reportEnterPage("BMI")
                requireActivity().startActivity<BmiRecordActivity>()
            }
            PendingActivityType.CHOLESTEROL -> {
                reportEnterPage("Cholesterol")
                CholesterolRecordActivity.start(requireActivity())
            }
            PendingActivityType.HEART_RATE -> {
                reportEnterPage("Heart Rate")
                requireActivity().startActivity<HeartRateRecordActivity>()
            }
        }
    }


}
fun reportEnterPage(pageName:String){
    ReportDataManager.reportData("enter_page_click",mapOf("page_name" to pageName))
}
