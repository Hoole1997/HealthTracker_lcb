package com.daily.health.manager.ui.act

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
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.entity.BloodPressureRecord
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.entity.HeartRateRecord
import com.daily.health.manager.data.enums.HeartRateStatus
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtActivityBsDetailBinding
import com.daily.health.manager.databinding.HtActivityBpDetailBinding
import com.daily.health.manager.databinding.HtActivityBmiDetailBinding
import com.daily.health.manager.databinding.HtActivityCholesterolDetailBinding
import com.daily.health.manager.databinding.HtActivityHealthDetailBinding
import com.daily.health.manager.databinding.HtActivityHeartRateDetailBinding
import com.daily.health.manager.ui.chart.HealthLineChartManager
import com.daily.health.manager.ui.tracker.HealthType
import com.daily.health.manager.ui.tracker.HealthTypeProvider
import com.daily.health.manager.ui.viewmodel.BmiDetailViewModel
import com.daily.health.manager.ui.viewmodel.BpDetailViewModel
import com.daily.health.manager.ui.viewmodel.BsDetailViewModel
import com.daily.health.manager.ui.viewmodel.CholesterolDetailViewModel
import com.daily.health.manager.ui.viewmodel.HeartRateDetailViewModel
import com.daily.health.manager.ui.weight.LeveDataFactory
import com.daily.health.manager.ui.widget.ExpertAdviceView
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.showToast
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.Locale

class HealthDetailActivity : BaseInterActivity<BaseViewModel, HtActivityHealthDetailBinding>(),
    HealthTypeProvider {

    companion object {
        private const val EXTRA_DETAIL_TYPE = "extra_detail_type"
        private const val EXTRA_RECORD_ID = "extra_record_id"

        private const val RECORD_ID = "record_id"
        private const val BMI_EXTRA_RECORD_ID = "extra_record_id"

        fun start(context: Context, type: DetailType, recordId: Long) {
            val intent = Intent(context, HealthDetailActivity::class.java).apply {
                putExtra(EXTRA_DETAIL_TYPE, type.name)
                putExtra(EXTRA_RECORD_ID, recordId)
                putExtra(RECORD_ID, recordId)
                putExtra(BMI_EXTRA_RECORD_ID, recordId)
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
                BLOOD_SUGAR -> R.layout.ht_activity_bs_detail
                BLOOD_PRESSURE -> R.layout.ht_activity_bp_detail
                BMI -> R.layout.ht_activity_bmi_detail
                HEART_RATE -> R.layout.ht_activity_heart_rate_detail
                CHOLESTEROL -> R.layout.ht_activity_cholesterol_detail
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

    private var bsBinding: HtActivityBsDetailBinding? = null
    private var bpBinding: HtActivityBpDetailBinding? = null
    private var bmiBinding: HtActivityBmiDetailBinding? = null
    private var hrBinding: HtActivityHeartRateDetailBinding? = null
    private var choBinding: HtActivityCholesterolDetailBinding? = null

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

    override fun createViewBinding() = HtActivityHealthDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

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
                val binding = HtActivityBsDetailBinding.bind(root)
                bsBinding = binding
                setupBs(binding)
            }

            DetailType.BLOOD_PRESSURE -> {
                val binding = HtActivityBpDetailBinding.bind(root)
                bpBinding = binding
                setupBp(binding)
            }

            DetailType.BMI -> {
                val binding = HtActivityBmiDetailBinding.bind(root)
                bmiBinding = binding
                setupBmi(binding)
            }

            DetailType.HEART_RATE -> {
                val binding = HtActivityHeartRateDetailBinding.bind(root)
                hrBinding = binding
                setupHeartRate(binding)
            }

            DetailType.CHOLESTEROL -> {
                val binding = HtActivityCholesterolDetailBinding.bind(root)
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

    private fun setupBs(binding: HtActivityBsDetailBinding) {
        bsViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnDelete.clickWithDuration { showDeleteConfirmBs() }
        binding.btnEdit.clickWithDuration { BsRecordActivity.start(this, recordId) }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, style = NativeAdStyle.CARD_5)

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

    private fun updateBsUI(binding: HtActivityBsDetailBinding) {
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

            val leveDescription = resources.getStringArray(R.array.ht_bs_level_expert_advice)[index]
            binding.expertAdviceView.setAdviceText(leveDescription)
        }
    }

    private fun showDeleteConfirmBs() {
        showDeleteConfirm {
            bsViewModel.deleteRecord()
        }
    }

    private fun setupBp(binding: HtActivityBpDetailBinding) {
        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { handleBackPress() }
        binding.btnDelete.clickWithDuration { showDeleteConfirmBp() }
        binding.btnEdit.clickWithDuration {
            bpViewModel.bloodPressureRecord.value?.let {
                BpRecordActivity.start(this, it.id)
            }
        }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, style = NativeAdStyle.CARD_5)

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

    private fun updateBpUI(binding: HtActivityBpDetailBinding, record: BloodPressureRecord) {
        val levels = LeveDataFactory.BloodPressure.buildItems(this)
        binding.bpStatusView.setLevels(levels)
        val idx = LeveDataFactory.BloodPressure.indexFor(record.systolicPressure, record.diastolicPressure)
        binding.bpStatusView.setCurrentLevel(idx)

        binding.tvSystolicValue.text = record.systolicPressure.toString()
        binding.tvDiastolicValue.text = record.diastolicPressure.toString()
        binding.tvPulseValue.text = record.pulseRate.toString()
        binding.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)

        val rangeDes = resources.getStringArray(R.array.ht_bp_level_expert_advice)
        val adviceText = String.format(rangeDes[idx], record.systolicPressure, record.diastolicPressure)
        binding.expertAdviceView.setAdviceText(adviceText)
    }

    private fun showDeleteConfirmBp() {
        showDeleteConfirm {
            bpViewModel.deleteRecord()
        }
    }

    private fun setupBmi(binding: HtActivityBmiDetailBinding) {
        bmiViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnEdit.clickWithDuration {
            bmiViewModel.bmiRecord.value?.let {
                BmiRecordActivity.start(this, it.id)
            }
        }
        binding.btnDelete.clickWithDuration { showDeleteConfirmBmi() }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, style = NativeAdStyle.CARD_5)

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

    private fun updateBmiUI(binding: HtActivityBmiDetailBinding) {
        val displayWeight = bmiViewModel.getDisplayWeight()
        binding.tvWeightValue.text = displayWeight

        val displayHeight = bmiViewModel.getDisplayHeight()
        binding.tvHeightValue.text = displayHeight

        val bmi = bmiViewModel.calculateBmi()
        binding.tvBmiValue.text = bmi?.let { String.format("%.1f", it) } ?: "--"

        val recordTime = bmiViewModel.getRecordTime()
        binding.tvTime.text = recordTime?.let { dateFormatBmi.format(it) } ?: ""

        if (bmi != null) {
            val bmiItems = LeveDataFactory.BMI.buildItems(this)
            val currentIndex = LeveDataFactory.BMI.indexFor(bmi)
            binding.bpStatusView.setLevels(bmiItems)
            binding.bpStatusView.setCurrentLevel(currentIndex)

            val adviceArray = resources.getStringArray(R.array.ht_bmi_level_expert_advice)
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

    private fun setupHeartRate(binding: HtActivityHeartRateDetailBinding) {
        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnDelete.click { showDeleteConfirmHr() }
        binding.btnEdit.click {
            hrViewModel.currentRecordId()?.let { id ->
                HeartRateRecordActivity.start(this, id)
            } ?: showToast(getString(R.string.ht_record_not_ready))
        }

        val levels = LeveDataFactory.HeartRate.buildItems(this)
        binding.bpmStatusView.setLevels(levels)

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, style = NativeAdStyle.CARD_5)

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

    private fun updateHeartRateRecord(binding: HtActivityHeartRateDetailBinding, record: HeartRateRecord) {
        binding.tvBpmValue.text = record.heartRateBpm.toString()
        binding.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)
        val index = LeveDataFactory.HeartRate.indexFor(record.heartRateBpm)
        binding.bpmStatusView.setCurrentLevel(index)
        val desArray = resources.getStringArray(R.array.ht_hr_level_expert_advice)
        binding.expertAdviceView.setAdviceText(desArray[index])
    }

    private fun updateHeartRateStatus(binding: HtActivityHeartRateDetailBinding, status: HeartRateStatus) {
        binding.bpmStatusView.setCurrentLevel(
            LeveDataFactory.HeartRate.indexFor(status)
        )
    }

    private fun showDeleteConfirmHr() {
        showDeleteConfirm {
            hrViewModel.deleteRecord()
        }
    }

    private fun setupCholesterol(binding: HtActivityCholesterolDetailBinding) {
        choViewModel.initializeWithRecord(recordId)

        chartManager = chartManagerFactory.create(binding.chartView, this)

        binding.btnBack.clickWithDuration { onBackPress() }
        binding.btnEdit.clickWithDuration {
            CholesterolRecordActivity.start(this, recordId)
        }
        binding.btnDelete.clickWithDuration { showDeleteConfirmCho() }

        setupCommonExpertAdvice(binding.expertAdviceView)
        loadNative(binding.adContainer, style = NativeAdStyle.CARD_5)

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

    private fun updateCholesterolUI(binding: HtActivityCholesterolDetailBinding) {
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

        val adviceArray = resources.getStringArray(R.array.ht_cholesterol_level_expert_advice)
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
            getString(R.string.ht_heart_rate_no_tags)
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
        return super.handleBackPress()
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
}
