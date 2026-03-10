package com.daily.health.manager.face.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.createSavedStateHandle
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.entity.BloodPressureRecord
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.HeartRateStatus
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.TrActivityBsDetailBinding
import com.daily.health.manager.databinding.TrActivityBpDetailBinding
import com.daily.health.manager.databinding.TrActivityBmiDetailBinding
import com.daily.health.manager.databinding.TrActivityCholesterolDetailBinding
import com.daily.health.manager.databinding.TrActivityHealthDetailBinding
import com.daily.health.manager.databinding.TrActivityHeartRateDetailBinding
import com.daily.health.manager.face.chart.HealthLineChartManager
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.HealthTypeProvider
import com.daily.health.manager.face.viewmodel.BmiDetailViewModel
import com.daily.health.manager.face.viewmodel.BpDetailViewModel
import com.daily.health.manager.face.viewmodel.BsDetailViewModel
import com.daily.health.manager.face.viewmodel.CholesterolDetailViewModel
import com.daily.health.manager.face.viewmodel.HeartRateDetailViewModel
import com.daily.health.manager.face.weight.LeveDataFactory
import com.daily.health.manager.face.widget.ExpertAdviceView
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Locale

class HealthDetailAct : BaseInterActivity<BaseViewModel, TrActivityHealthDetailBinding>(),
    HealthTypeProvider {

    companion object {
        private const val EXTRA_DETAIL_TYPE = "extra_detail_type"
        private const val EXTRA_RECORD_ID = "extra_record_id"
        private const val RECORD_ID = "record_id"
        private const val BMI_EXTRA_RECORD_ID = "extra_record_id"
        private const val EXTRA_IS_FROM_SAVE = "extra_is_from_save"

        fun start(context: Context, type: DetailType, recordId: Long, isFromSave: Boolean = false) {
            val intent = Intent(context, HealthDetailAct::class.java).apply {
                putExtra(EXTRA_DETAIL_TYPE, type.name)
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(RECORD_ID, recordId)
                putExtra(BMI_EXTRA_RECORD_ID, recordId)
                putExtra(EXTRA_IS_FROM_SAVE, isFromSave)
            }
            context.startActivity(intent)
        }
    }

    enum class DetailType {
        BLOOD_SUGAR,
        BLOOD_PRESSURE,
        BMI,
        HEART_RATE,
        CHOLESTEROL,
        ;

        fun toHealthType(): HealthType {
            return when (this) {
                BLOOD_SUGAR -> HealthType.BLOOD_SUGAR
                BLOOD_PRESSURE -> HealthType.BLOOD_PRESSURE
                BMI -> HealthType.BMI
                HEART_RATE -> HealthType.HEART_RATE
                CHOLESTEROL -> HealthType.CHOLESTEROL
            }
        }

        fun toLayoutRes(): Int {
            return when (this) {
                BLOOD_SUGAR -> R.layout.tr_activity_bs_detail
                BLOOD_PRESSURE -> R.layout.tr_activity_bp_detail
                BMI -> R.layout.tr_activity_bmi_detail
                HEART_RATE -> R.layout.tr_activity_heart_rate_detail
                CHOLESTEROL -> R.layout.tr_activity_cholesterol_detail
            }
        }

        companion object {
            fun from(intent: Intent): DetailType? {
                val name = intent.getStringExtra(EXTRA_DETAIL_TYPE) ?: return null
                return runCatching { valueOf(name) }.getOrNull()
            }
        }
    }

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()

    private var bsBinding: TrActivityBsDetailBinding? = null
    private var bpBinding: TrActivityBpDetailBinding? = null
    private var bmiBinding: TrActivityBmiDetailBinding? = null
    private var hrBinding: TrActivityHeartRateDetailBinding? = null
    private var choBinding: TrActivityCholesterolDetailBinding? = null

    private var chartManager: HealthLineChartManager? = null

    private val dateFormatBmi = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private val bsViewModel by lazy { obtainViewModel(BsDetailViewModel::class.java) }
    private val bpViewModel by lazy { obtainViewModel(BpDetailViewModel::class.java) }
    private val bmiViewModel by lazy { obtainViewModel(BmiDetailViewModel::class.java) }
    private val hrViewModel by lazy { obtainViewModel(HeartRateDetailViewModel::class.java) }
    private val choViewModel by lazy { obtainViewModel(CholesterolDetailViewModel::class.java) }

    private val detailType: DetailType? by lazy { DetailType.from(intent) }

    private val recordId: Long by lazy {
        intent.getLongExtra(EXTRA_RECORD_ID, -1L)
    }

    private val isFromSave: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_IS_FROM_SAVE, false)
    }

    private var pendingAlarmTypeForPermission: Int? = null

    override fun createViewBinding() = TrActivityHealthDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun onResume() {
        super.onResume()
        // [New Logic] 处理从设置页面返回后的权限授权成功自动弹窗
        val type = pendingAlarmTypeForPermission
        if (type != null && androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            pendingAlarmTypeForPermission = null
            showAlarmGuide(type)
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        val type = detailType
        if (type == null || recordId == -1L) {
            finish()
            return
        }

        mViewBind.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind.composeView.setContent {
            HealthDetailHost(type = type)
        }
    }

    @Composable
    private fun HealthDetailHost(type: DetailType) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                LayoutInflater.from(ctx)
                    .inflate(type.toLayoutRes(), null, false)
                    .also { root ->
                        bindAndSetup(type, root)
                    }
            }
        )
    }

    private fun bindAndSetup(type: DetailType, root: View) {
        when (type) {
            DetailType.BLOOD_SUGAR -> {
                val binding = TrActivityBsDetailBinding.bind(root)
                bsBinding = binding
                setupBs(binding)
            }

            DetailType.BLOOD_PRESSURE -> {
                val binding = TrActivityBpDetailBinding.bind(root)
                bpBinding = binding
                setupBp(binding)
            }

            DetailType.BMI -> {
                val binding = TrActivityBmiDetailBinding.bind(root)
                bmiBinding = binding
                setupBmi(binding)
            }

            DetailType.HEART_RATE -> {
                val binding = TrActivityHeartRateDetailBinding.bind(root)
                hrBinding = binding
                setupHeartRate(binding)
            }

            DetailType.CHOLESTEROL -> {
                val binding = TrActivityCholesterolDetailBinding.bind(root)
                choBinding = binding
                setupCholesterol(binding)
            }
        }
    }

    private fun setupCommonExpertAdvice(expertAdviceView: ExpertAdviceView) {
        expertAdviceView.setOnExpertAdviceListener(object : ExpertAdviceView.OnExpertAdviceListener {
            override fun onCountdownFinished() {
                showReword()
            }

            override fun onGetTipClicked() {
                showReword()
            }

            override fun onCancelClicked() {
            }
        })
    }

    private fun setupBs(binding: TrActivityBsDetailBinding) {
        bsViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnDelete.clickWithDuration { showDeleteConfirmBs() }
        binding.btnEdit.clickWithDuration {
            HealthRecordAct.start(this, HealthRecordAct.RecordType.BLOOD_SUGAR, recordId)
        }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, AdPosition.NA_DETAIL_BLOOD_SUGAR_BOTTOM, style = NativeAdStyle.CARD_5)

        collectLatest(bsViewModel.bloodSugarRecord) {
            if (it != null) {
                updateBsUI(binding)
            }
        }

        collectLatest(bsViewModel.error) { error ->
            error?.let {
                showToast(it)
                bsViewModel.clearError()
            }
        }

        collectLatest(bsViewModel.tags) {
            updateTags(binding.tvTags, it.take(2))
        }

        collectLatest(bsViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }

        lifecycleScope.launch {
            bsViewModel.isLoading.collect {
            }
        }
    }

    private fun updateBsUI(binding: TrActivityBsDetailBinding) {
        val status = bsViewModel.getBloodSugarStatus()
        val unit = bsViewModel.getDisplayUnit()
        val value = bsViewModel.getDisplayValue()

        binding.tvBsValue.text = value?.toString().orEmpty()
        binding.tvBsValueUnit.text = unit?.displayName ?: ""
        bsViewModel.getRecordTime()?.let {
            binding.tvTime.text = DateTimeUtils.formatDateTime(it)
        }

        if (status != null && unit != null && value != null) {
            val levels = LeveDataFactory.BloodSugar.buildItems(this, unit, status)
            binding.bsStatusView.setLevels(levels)
            val index = LeveDataFactory.BloodSugar.indexFor(value, unit, status)
            binding.bsStatusView.setCurrentLevel(index)

            val leveDescription = resources.getStringArray(R.array.tr_bs_level_expert_advice)[index]
            binding.expertAdviceView.setAdviceText(leveDescription)
        }
    }

    private fun showDeleteConfirmBs() {
        showDeleteConfirm {
            bsViewModel.deleteRecord()
        }
    }

    private fun setupBp(binding: TrActivityBpDetailBinding) {
        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { handleBackPress() }
        binding.btnDelete.clickWithDuration { showDeleteConfirmBp() }
        binding.btnEdit.clickWithDuration {
            bpViewModel.bloodPressureRecord.value?.let {
                HealthRecordAct.start(this, HealthRecordAct.RecordType.BLOOD_PRESSURE, it.id)
            }
        }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, AdPosition.NA_DETAIL_BLOOD_PRESSURE_BOTTOM, style = NativeAdStyle.CARD_5)

        collectLatest(bpViewModel.bloodPressureRecord) { record ->
            record?.let { updateBpUI(binding, it) }
        }

        collectLatest(bpViewModel.error) {
        }

        collectLatest(bpViewModel.isLoading) {
        }

        collectLatest(bpViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }
    }

    private fun updateBpUI(binding: TrActivityBpDetailBinding, record: BloodPressureRecord) {
        val levels = LeveDataFactory.BloodPressure.buildItems(this)
        binding.bpStatusView.setLevels(levels)
        val idx = LeveDataFactory.BloodPressure.indexFor(record.systolicPressure, record.diastolicPressure)
        binding.bpStatusView.setCurrentLevel(idx)

        binding.tvSystolicValue.text = record.systolicPressure.toString()
        binding.tvDiastolicValue.text = record.diastolicPressure.toString()
        binding.tvPulseValue.text = record.pulseRate.toString()
        binding.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)

        val rangeDes = resources.getStringArray(R.array.tr_bp_level_expert_advice)
        val adviceText = String.format(rangeDes[idx], record.systolicPressure, record.diastolicPressure)
        binding.expertAdviceView.setAdviceText(adviceText)
    }

    private fun showDeleteConfirmBp() {
        showDeleteConfirm {
            bpViewModel.deleteRecord()
        }
    }

    private fun setupBmi(binding: TrActivityBmiDetailBinding) {
        bmiViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnEdit.clickWithDuration {
            bmiViewModel.bmiRecord.value?.let {
                HealthRecordAct.start(this, HealthRecordAct.RecordType.BMI, it.id)
            }
        }
        binding.btnDelete.clickWithDuration { showDeleteConfirmBmi() }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, AdPosition.NA_DETAIL_BMI_BOTTOM, style = NativeAdStyle.CARD_5)

        collectLatest(bmiViewModel.bmiRecord) { record ->
            if (record != null) {
                "BMI record updated: $record".logd("HealthDetailActivity")
                updateBmiUI(binding)
            }
        }

        collectLatest(bmiViewModel.isLoading) {
        }

        collectLatest(bmiViewModel.error) { error ->
            if (error != null) {
                showToast(error)
                bmiViewModel.clearError()
            }
        }

        collectLatest(bmiViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }
    }

    private fun updateBmiUI(binding: TrActivityBmiDetailBinding) {
        val displayWeight = bmiViewModel.getDisplayWeight()
        binding.tvWeightValue.text = displayWeight

        val displayHeight = bmiViewModel.getDisplayHeight()
        binding.tvHeightValue.text = displayHeight

        val bmi = bmiViewModel.calculateBmi()
        binding.tvBmiValue.text = bmi?.let { NumberFormatter.formatNumber(it.toDouble(), LanguageUtils.getAppLocale(this), 1) } ?: "--"

        val recordTime = bmiViewModel.getRecordTime()
        binding.tvTime.text = recordTime?.let { dateFormatBmi.format(it) } ?: ""

        if (bmi != null) {
            val bmiItems = LeveDataFactory.BMI.buildItems(this)
            val currentIndex = LeveDataFactory.BMI.indexFor(bmi)
            binding.bpStatusView.setLevels(bmiItems)
            binding.bpStatusView.setCurrentLevel(currentIndex)

            val adviceArray = resources.getStringArray(R.array.tr_bmi_level_expert_advice)
            if (currentIndex in adviceArray.indices) {
                binding.expertAdviceView.setAdviceText(adviceArray[currentIndex])
            } else {
                binding.expertAdviceView.setAdviceText("")
            }
        }
    }

    private fun showDeleteConfirmBmi() {
        showDeleteConfirm {
            bmiViewModel.deleteRecord()
        }
    }

    private fun setupHeartRate(binding: TrActivityHeartRateDetailBinding) {
        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnDelete.click { showDeleteConfirmHr() }
        binding.btnEdit.click {
            hrViewModel.currentRecordId()?.let { id ->
                HealthRecordAct.start(this, HealthRecordAct.RecordType.HEART_RATE, id)
            } ?: showToast(getString(R.string.tr_record_not_ready))
        }

        val levels = LeveDataFactory.HeartRate.buildItems(this)
        binding.bpmStatusView.setLevels(levels)

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, AdPosition.NA_DETAIL_HEART_RATE_BOTTOM, style = NativeAdStyle.CARD_5)

        collectLatest(hrViewModel.record) { record ->
            if (record != null) {
                updateHeartRateRecord(binding, record)
            }
        }

        collectLatest(hrViewModel.status) { status ->
            status?.let { updateHeartRateStatus(binding, it) }
        }

        collectLatest(hrViewModel.tags) { tags ->
            updateTags(binding.tvTags, tags.take(2))
        }

        collectLatest(hrViewModel.error) { error ->
            error?.let {
                showToast(it)
                hrViewModel.clearError()
            }
        }

        collectLatest(hrViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }

        lifecycleScope.launch {
            hrViewModel.isLoading.collect {
            }
        }
    }

    private fun updateHeartRateRecord(binding: TrActivityHeartRateDetailBinding, record: HeartRateRecord) {
        binding.tvBpmValue.text = record.heartRateBpm.toString()
        binding.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)
        val index = LeveDataFactory.HeartRate.indexFor(record.heartRateBpm)
        binding.bpmStatusView.setCurrentLevel(index)
        val desArray = resources.getStringArray(R.array.tr_hr_level_expert_advice)
        binding.expertAdviceView.setAdviceText(desArray[index])
    }

    private fun updateHeartRateStatus(binding: TrActivityHeartRateDetailBinding, status: HeartRateStatus) {
        binding.bpmStatusView.setCurrentLevel(
            LeveDataFactory.HeartRate.indexFor(status)
        )
    }

    private fun showDeleteConfirmHr() {
        showDeleteConfirm {
            hrViewModel.deleteRecord()
        }
    }

    private fun setupCholesterol(binding: TrActivityCholesterolDetailBinding) {
        choViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnEdit.clickWithDuration {
            HealthRecordAct.start(this, HealthRecordAct.RecordType.CHOLESTEROL, recordId)
        }
        binding.btnDelete.clickWithDuration { showDeleteConfirmCho() }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, AdPosition.NA_DETAIL_CHOLESTEROL_BOTTOM, style = NativeAdStyle.CARD_5)

        collectLatest(choViewModel.cholesterolRecord) {
            if (it != null) {
                updateCholesterolUI(binding)
            }
        }

        collectLatest(choViewModel.errorMessage) { error ->
            error?.let {
                showToast(it)
                choViewModel.clearError()
            }
        }

        lifecycleScope.launch {
            choViewModel.isLoading.collect {
            }
        }

        collectLatest(choViewModel.chartUiState) { state ->
            chartManager?.render(state)
        }
    }

    private fun updateCholesterolUI(binding: TrActivityCholesterolDetailBinding) {
        binding.tvHdlValue.text = choViewModel.getHdlValue()
        binding.tvTcHdlValue.text = choViewModel.getTcHdlRatio()
        binding.tvLdlHdlValue.text = choViewModel.getLdlHdlRatio()

        choViewModel.getRecordTime()?.let {
            binding.tvTime.text = DateTimeUtils.formatDateTime(it)
        }

        val cholesterolLevel = choViewModel.getCholesterolLevel()
        val leveItems = LeveDataFactory.Cholesterol.buildItems(this)
        binding.cholesterolStatusView.setLevels(leveItems)
        val index = LeveDataFactory.Cholesterol.indexFor(cholesterolLevel)
        binding.cholesterolStatusView.setCurrentLevel(index)

        val adviceArray = resources.getStringArray(R.array.tr_cholesterol_level_expert_advice)
        val adviceIndex = when (cholesterolLevel) {
            com.daily.health.manager.data.enums.CholesterolLevel.UNKNOWN -> 0
            com.daily.health.manager.data.enums.CholesterolLevel.NORMAL -> 1
            com.daily.health.manager.data.enums.CholesterolLevel.NEAR_OPTIMAL -> 2
            com.daily.health.manager.data.enums.CholesterolLevel.BORDERLINE -> 3
            com.daily.health.manager.data.enums.CholesterolLevel.HIGH -> 4
            com.daily.health.manager.data.enums.CholesterolLevel.VERY_HIGH -> 5
        }
        binding.expertAdviceView.setAdviceText(adviceArray[adviceIndex])
    }

    private fun showDeleteConfirmCho() {
        showDeleteConfirm {
            choViewModel.deleteRecord()
        }
    }

    private fun updateTags(targetView: android.widget.TextView, tags: List<HealthTag>) {
        targetView.text = if (tags.isEmpty()) {
            getString(R.string.tr_heart_rate_no_tags)
        } else {
            tags.joinToString(" · ") { it.name }
        }
    }

    override fun getStatusBarColor() = R.color.c5

    override fun hideMask() {
        bsBinding?.expertAdviceView?.setMaskVisible(false)
        bpBinding?.expertAdviceView?.setMaskVisible(false)
        bmiBinding?.expertAdviceView?.setMaskVisible(false)
        hrBinding?.expertAdviceView?.setMaskVisible(false)
        choBinding?.expertAdviceView?.setMaskVisible(false)
    }

    override fun handleBackPress(): Boolean {
        if (detailType == DetailType.BLOOD_PRESSURE) {
            bpBinding?.expertAdviceView?.stopCountdown()
        }

        val type = detailType
        // [New Logic] 返回键拦截流程：仅限录入后进入 -> 频控通过
        if (isFromSave && type != null) {
            val alarmType = when(type) {
                DetailType.BLOOD_SUGAR -> com.daily.health.manager.data.entity.AlarmRecord.TYPE_BLOOD_SUGAR
                DetailType.BLOOD_PRESSURE -> com.daily.health.manager.data.entity.AlarmRecord.TYPE_BLOOD_PRESSURE
                DetailType.BMI -> com.daily.health.manager.data.entity.AlarmRecord.TYPE_BMI
                DetailType.HEART_RATE -> com.daily.health.manager.data.entity.AlarmRecord.TYPE_HEART_RATE
                DetailType.CHOLESTEROL -> com.daily.health.manager.data.entity.AlarmRecord.TYPE_CHOLESTEROL
            }

            if (com.daily.health.manager.face.compose.ReminderDialogHelper.canShowBackGuide(alarmType)) {
                lifecycleScope.launch {
                    val hasAlarm = alarmViewModel.hasAnyAlarm(alarmType)
                    if (!hasAlarm) {
                        // [Rule Update] 弹出任何引导环节(含权限)均视为已展示一次
                        com.daily.health.manager.face.compose.ReminderDialogHelper.markBackGuideShown(alarmType)
                        
                        if (androidx.core.app.NotificationManagerCompat.from(this@HealthDetailAct).areNotificationsEnabled()) {
                            showAlarmGuide(alarmType)
                        } else {
                            // 无权限：先弹出 V2 权限引导弹窗，点击按钮后再请求系统权限
                            val isDoNotAsk = com.hjq.permissions.XXPermissions.isDoNotAskAgainPermissions(
                                this@HealthDetailAct,
                                listOf(com.hjq.permissions.permission.PermissionLists.getPostNotificationsPermission())
                            )
                            com.daily.health.manager.face.dialog.NotificationPermissionV2DialogFragment.show(
                                supportFragmentManager,
                                alarmType = alarmType,
                                isDoNotAsk = isDoNotAsk,
                                onGoToSettings = {
                                    pendingAlarmTypeForPermission = alarmType
                                    App.INSTANCE.isGoSetting = true
                                    com.healthtracker.framework.util.PermissionUtils.openPermissionSettings(this@HealthDetailAct)
                                },
                                onRequestPermission = {
                                    com.healthtracker.framework.util.PermissionUtils.requestNotificationPermission(this@HealthDetailAct) { granted, _ ->
                                        if (granted) {
                                            showAlarmGuide(alarmType)
                                        } else {
                                            super.handleBackPress()
                                        }
                                    }
                                },
                                onCancelCallback = {
                                    // 拒绝授权或取消：继续执行原有返回流程(插屏 -> finish)
                                    super.handleBackPress()
                                }
                            )
                        }
                    } else {
                        super.handleBackPress()
                    }
                }
                return true
            }
        }

        return super.handleBackPress()
    }

    private fun showAlarmGuide(alarmType: Int) {
        val dialog = com.daily.health.manager.face.dialog.ReminderSettingsDialogFragment.newInstance(alarmType)
        dialog.setOnDismissListener {
            // 引导消失后，执行基类插屏广告流程
            super.handleBackPress()
        }
        dialog.show(supportFragmentManager, "ReminderBackGuide")
    }

    override fun getHealthType(): HealthType {
        return (detailType ?: return HealthType.OTHER).toHealthType()
    }

    override fun getCurrentHealthType(): HealthType = getHealthType()

    private fun <T : ViewModel> obtainViewModel(modelClass: Class<T>): T {
        val defaultFactory = defaultViewModelProviderFactory
        val koin = runCatching { GlobalContext.get() }.getOrNull()

        val factory = object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(clazz: Class<VM>): VM {
                val koinVm = koin?.let { runCatching { it.get<Any>(clazz = clazz.kotlin) }.getOrNull() }
                if (koinVm != null) {
                    @Suppress("UNCHECKED_CAST")
                    return koinVm as VM
                }
                return defaultFactory.create(clazz)
            }

            override fun <VM : ViewModel> create(clazz: Class<VM>, extras: CreationExtras): VM {
                val koinVm = koin?.let {
                    runCatching {
                        val savedStateHandle = extras.createSavedStateHandle()
                        it.get<Any>(
                            clazz = clazz.kotlin,
                            parameters = { org.koin.core.parameter.parametersOf(savedStateHandle) }
                        )
                    }.getOrNull()
                }
                if (koinVm != null) {
                    @Suppress("UNCHECKED_CAST")
                    return koinVm as VM
                }
                return defaultFactory.create(clazz, extras)
            }
        }

        return ViewModelProvider(this, factory)[modelClass]
    }
    private val alarmViewModel: com.daily.health.manager.face.viewmodel.AlarmViewModel by inject()
    
}
