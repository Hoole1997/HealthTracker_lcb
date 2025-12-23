package com.daily.health.manager.ui.act

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.constants.BodyMetricsDefaults
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.data.enums.TagType
import com.daily.health.manager.databinding.HtActivityBmiRecordBinding
import com.daily.health.manager.ui.dialog.BmiPickerDialog
import com.daily.health.manager.ui.dialog.HealthTagDialog
import com.daily.health.manager.ui.dialog.LevelExplainDialog
import com.daily.health.manager.ui.dialog.SaveCompleteDialog
import com.daily.health.manager.ui.viewmodel.BmiRecordViewModel
import com.daily.health.manager.ui.weight.LeveDataFactory
import com.daily.health.manager.utils.loadNative
import com.daily.health.manager.utils.showInter
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.daily.health.manager.ui.tracker.HealthType
import com.daily.health.manager.ui.tracker.trackAddNewRecord
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import java.util.Calendar

class BmiRecordActivity : BaseInterActivity<BmiRecordViewModel, HtActivityBmiRecordBinding>() {

    private val healthTags = mutableListOf<HealthTag>()
    private val addTagIds = mutableListOf<Long>()

    // 基础存储：公制(cm/kg)
    private var latestHeightCm: Float = BodyMetricsDefaults.DEFAULT_HEIGHT_CM.toFloat()
    private var latestWeightKg: Float = BodyMetricsDefaults.DEFAULT_WEIGHT_KG.toFloat()
    private var currentWeightUnit: BmiUnit = BmiUnit.getPreferredWeightUnit()
    private var currentHeightUnit: BmiUnit = BmiUnit.getPreferredHeightUnit()

    companion object {
        private const val EXTRA_RECORD_ID = "extra_record_id"

        fun start(context: android.content.Context, recordId: Long? = null) {
            val intent = android.content.Intent(context, BmiRecordActivity::class.java)
            recordId?.let { intent.putExtra(EXTRA_RECORD_ID, it) }
            context.startActivity(intent)
        }
    }

    override fun createViewBinding() = HtActivityBmiRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BmiRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 读取编辑记录ID（如有）
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        val editRecordId = if (recordId == -1L) null else recordId

        // 初始化 ViewModel（支持编辑模式）
        mViewModel.initializeWithRecord(editRecordId)

        with(mViewBind) {
            btnBack.clickWithDuration { onBackPress() }
            editRecordId?.let {
                tvTitle.text = getString(R.string.ht_edit_record)
            }
            // 体重/身高编辑：复用通用输入 BottomSheet
            clWeight.clickWithDuration {
                val curDisplay = BmiUnit.toDisplayWeight(latestWeightKg, currentWeightUnit)
                BmiPickerDialog.showWeightPicker(
                    supportFragmentManager,
                    initialDisplayValue = curDisplay,
                    unit = currentWeightUnit
                ) { value, unit ->
                    if (currentWeightUnit != unit) {
                        currentWeightUnit = unit
                        mViewModel.switchWeightUnit(unit)
                    }
                    mViewModel.updateWeightFromDisplay(value)
                }
            }
            clHeight.clickWithDuration {
                val curDisplay = BmiUnit.toDisplayHeight(latestHeightCm, currentHeightUnit)
                BmiPickerDialog.showHeightPicker(
                    supportFragmentManager,
                    initialDisplayValue = curDisplay,
                    unit = currentHeightUnit
                ) { value, unit ->
                    if (currentHeightUnit != unit) {
                        currentHeightUnit = unit
                        mViewModel.switchHeightUnit(unit)
                    }
                    mViewModel.updateHeightFromDisplay(value)
                }
            }

            // 单位切换（点击单位文案分别在公制/英制之间切换）
            tvWeightUnit.setOnClickListener { toggleWeightUnit() }
            tvHeightUnit.setOnClickListener { toggleHeightUnit() }

            // 标签按钮：打开 BMI 标签选择
            dateTimeSelectionView.setOnLabelClickListener {
                val selectedTags = if (addTagIds.isEmpty()) null else {
                    val temp = mutableListOf<HealthTag>()
                    for (id in addTagIds) {
                        healthTags.find { it.id == id }?.let { temp.add(it) }
                    }
                    temp
                }
                HealthTagDialog(
                    tagType = TagType.BMI,
                    tagsFlow = mViewModel.loadAvailableHealthTagsFlow(),
                    selectedTags = selectedTags,
                    onSave = { selectedTagList ->
                        val tagIds = selectedTagList.map { it.id }
                        addTagIds.clear()
                        addTagIds.addAll(tagIds)
                        mViewModel.clearSelectedTags()
                        tagIds.forEach { tagId -> mViewModel.addTag(tagId) }
                    },
                    onDelete = { tag ->
                        mViewModel.deleteTag(tag)
                    },
                    onAdd = { tagName ->
                        lifecycleScope.launch {
                            val id = mViewModel.createCustomTag(tagName)
                            if (id <= 0L) {
                                showToast(getString(R.string.ht_create_label_failed))
                            }
                        }
                    }
                ).show(supportFragmentManager)
            }

            // 保存按钮
            setupSaveButton()

            // 初始化 BMI 等级视图
            setupLeveStatusView()
            loadNative(adContainer, style = NativeAdStyle.STANDARD)
        }

        observeViewModel()
    }

    private fun setupSaveButton() {
        mViewBind.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.BMI)
            lifecycleScope.launch {
                mViewBind.dateTimeSelectionView.getSelectDate().let { date ->
                    mViewModel.updateRecordTime(date)
                }
                mViewModel.saveBmiRecord { result ->
                    when (result) {
                        is BmiRecordViewModel.SaveRecordResult.Created -> {
                            goDetail(result.recordId)
                        }
                        is BmiRecordViewModel.SaveRecordResult.Updated -> {
                           goDetail(result.recordId)
                        }
                        is BmiRecordViewModel.SaveRecordResult.Failed -> {
                            showToast(result.error)
                        }
                    }
                }
            }
        }
    }

    private fun goDetail(recordId:Long){
        SaveCompleteDialog.show(supportFragmentManager){
           showInter {
               BmiDetailActivity.start(this@BmiRecordActivity,recordId)
               finish()
           }
        }
    }

    private fun observeViewModel() {
        // 身高/体重变化时更新 UI 与等级索引
        this.collect(mViewModel.heightCm) { height ->
            latestHeightCm = height
            updateDisplayValues()
            updateLsvCurrentIndex()
        }

        this.collect(mViewModel.weightKg) { weight ->
            latestWeightKg = weight
            updateDisplayValues()
            updateLsvCurrentIndex()
        }

        // 体重显示单位变化
        this.collect(mViewModel.weightUnit) { unit ->
            currentWeightUnit = unit
            updateDisplayValues()
        }

        // 身高显示单位变化
        this.collect(mViewModel.heightUnit) { unit ->
            currentHeightUnit = unit
            updateDisplayValues()
        }

        // 记录时间变化
        this.collectLatest(mViewModel.recordTime) { recordTime ->
            val calendar = Calendar.getInstance()
            calendar.time = recordTime
            if (!isDestroyed && !isFinishing) {
                mViewBind.dateTimeSelectionView.getDateTimePicker().initView(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                    day = calendar.get(Calendar.DAY_OF_MONTH),
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE)
                )
            }
        }

        // 加载状态
        this.collectLatest(mViewModel.isLoading) { isLoading ->
            mViewBind.btnSave.isEnabled = !isLoading
            mViewBind.btnSave.text = if (isLoading) {
                getString(R.string.ht_saving)
            } else {
                getString(R.string.ht_save)
            }
        }

        // 可用标签
        this.collectLatest(mViewModel.availableTags) { tags ->
            healthTags.clear()
            healthTags.addAll(tags)
        }

        // 选中标签ID，同步标签文案
        this.collectLatest(mViewModel.selectedTagIds) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }
    }

    private fun setupLeveStatusView() {
        val levels = LeveDataFactory.BMI.buildItems(this)
        mViewBind.bpStatusView.setLevels(levels)
        updateLsvCurrentIndex()

        // 在记录页开启范围说明点击，弹出通用等级说明对话框
        mViewBind.bpStatusView.setExplainClickable(true)
        mViewBind.bpStatusView.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.BMI.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                items = items,
                des = null // BMI 无额外范围说明文案
            )
        }
    }

    private fun updateLsvCurrentIndex() {
        val heightM = latestHeightCm / 100f
        if (heightM > 0f) {
            val bmi = (latestWeightKg / (heightM * heightM))
            val idx = LeveDataFactory.BMI.indexFor(bmi)
            mViewBind.bpStatusView.setCurrentLevel(idx)
        }
    }

    private fun updateDisplayValues() {
        // 根据当前单位格式化显示文案
        val displayWeight = BmiUnit.formatDisplayWeight(latestWeightKg, currentWeightUnit)
        val displayHeight = BmiUnit.formatDisplayHeight(latestHeightCm, currentHeightUnit)
        mViewBind.tvWeightValue.text = displayWeight
        mViewBind.tvHeightValue.text = displayHeight
        mViewBind.tvWeightUnit.text = getUnitLabelText(isWeight = true, unit = currentWeightUnit)
        mViewBind.tvHeightUnit.text = getUnitLabelText(isWeight = false, unit = currentHeightUnit)
    }

    private fun toggleWeightUnit() {
        val newUnit = if (currentWeightUnit == BmiUnit.METRIC) BmiUnit.IMPERIAL else BmiUnit.METRIC
        mViewModel.switchWeightUnit(newUnit)
    }

    private fun toggleHeightUnit() {
        val newUnit = if (currentHeightUnit == BmiUnit.METRIC) BmiUnit.IMPERIAL else BmiUnit.METRIC
        mViewModel.switchHeightUnit(newUnit)
    }

    private fun getUnitLabelText(isWeight: Boolean, unit: BmiUnit): String {
        val unitName = when {
            isWeight && unit == BmiUnit.METRIC -> getString(R.string.ht_unit_kg)
            isWeight && unit == BmiUnit.IMPERIAL -> getString(R.string.ht_unit_lb)
            !isWeight && unit == BmiUnit.METRIC -> getString(R.string.ht_unit_cm)
            else -> getString(R.string.ht_unit_ft_in)
        }
        return getString(R.string.ht_unit_in_brackets, unitName.lowercase())
    }
}
