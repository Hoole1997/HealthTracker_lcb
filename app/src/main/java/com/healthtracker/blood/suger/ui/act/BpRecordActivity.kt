package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.ad.reportAddRecord
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.databinding.ActivityBpRecordBinding
import com.healthtracker.blood.suger.ui.dialog.HealthTagDialog
import com.healthtracker.blood.suger.ui.dialog.LevelExplainDialog
import com.healthtracker.blood.suger.ui.dialog.SaveCompleteDialog
import com.healthtracker.blood.suger.ui.viewmodel.BpRecordViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.blood.suger.utils.showInter
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoRegular
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import java.util.Calendar

@AndroidEntryPoint
class BpRecordActivity: BaseInterActivity<BpRecordViewModel, ActivityBpRecordBinding>() {
    
    private val healthTags = mutableListOf<HealthTag>()
    private val addTagIds = mutableListOf<Long>()
    private var latestSystolic: Int = 120
    private var latestDiastolic: Int = 80
    
    companion object{
        private const val TAG = "BpRecordActivity"
        private const val EXTRA_RECORD_ID = "extra_record_id"
        
        // 启动编辑模式
        fun start(context: android.content.Context, recordId: Long? = null) {
            val intent = android.content.Intent(context, BpRecordActivity::class.java)
            recordId?.let { intent.putExtra(EXTRA_RECORD_ID, it) }
            context.startActivity(intent)
        }
    }
    override fun createViewBinding() = ActivityBpRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BpRecordViewModel::class.java


    override fun initView(savedInstanceState: Bundle?) {
        // 获取传入的记录ID（如果有）
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        val editRecordId = if (recordId == -1L) null else recordId

        // 初始化ViewModel
        if (editRecordId != null) {
            mViewModel.loadEditRecord(editRecordId)
        }
        
        // 初始化标签数据
        mViewModel.initializeTags()
        
        with(mViewBind){
            btnBack.clickWithDuration {
                handleBackPress()
            }

            editRecordId?.let {
                mViewModel.loadEditRecord(it)
                tvTitle.text = getString(R.string.edit_record)
            }
            
            val tfRegular = getRobotoRegular(this@BpRecordActivity)
            val tfBold = getRobotoBold(this@BpRecordActivity)
            npvDiastolic.setContentSelectedTextTypeface(tfBold)
            npvSystolic.setContentSelectedTextTypeface(tfBold)
            npvPulse.setContentSelectedTextTypeface(tfBold)
            npvDiastolic.setContentNormalTextTypeface(tfRegular)
            npvSystolic.setContentNormalTextTypeface(tfRegular)
            npvPulse.setContentNormalTextTypeface(tfRegular)
            npvDiastolic.minValue = 20
            npvSystolic.minValue = 20
            npvSystolic.maxValue = 300
            npvDiastolic.maxValue = 300
            npvPulse.minValue = 40
            npvPulse.maxValue = 220
            npvSystolic.setOnValueChangedListener { _, _, _ ->
                val systolic = npvSystolic.contentByCurrValue.toInt()
                mViewModel.updateSystolicPressure(systolic)
            }
            npvDiastolic.setOnValueChangedListener { _, _, _ ->
                val diastolic = npvDiastolic.contentByCurrValue.toInt()
                mViewModel.updateDiastolicPressure(diastolic)
            }
            npvPulse.setOnValueChangedListener { _, _, _ ->
                mViewModel.updatePulseRate(npvPulse.contentByCurrValue.toInt())
            }
            
            // 设置DateTimeSelectionView的标签点击监听
            dateTimeSelectionView.setOnLabelClickListener {
                val selectedTags = if(addTagIds.isEmpty()) null else {
                    val tempTags = mutableListOf<HealthTag>()
                    for(id in addTagIds){
                        healthTags.find { it.id == id }?.let {
                            tempTags.add(it)
                        }
                    }
                    tempTags
                }
                HealthTagDialog.showBloodPressureDialog(
                    supportFragmentManager,
                    mViewModel.getBloodPressureTagsFlow(),
                    selectedTags,
                    onSave = { selectedTagList ->
                        val tagIds = selectedTagList.map { it.id }
                        addTagIds.clear()
                        addTagIds.addAll(tagIds)
                        mViewModel.clearSelectedTags()
                        tagIds.forEach { tagId ->
                            mViewModel.toggleTagSelection(tagId)
                        }
                    },
                    onDelete = { tag ->
                        mViewModel.deleteTag(tag)
                    },
                    onAdd = { tagName ->
                        lifecycleScope.launch {
                            val id = mViewModel.createCustomTag(tagName)
                            if (id <= 0L) {
                                showToast(getString(R.string.create_label_failed))
                            }
                        }
                    }
                )
            }
            
            // 设置保存按钮点击事件
            setupSaveButton()
            // 初始化 LeveStatusView 的等级列表并设置初始索引
            setupLeveStatusView()
            loadNative(adContainer, style = NativeAdStyle.STANDARD)
        }

        observeViewModel()
    }
    
    /**
     * 设置保存按钮
     */
    private fun setupSaveButton() {
        // 设置保存按钮点击事件
        mViewBind.btnSave.clickWithDuration {
            reportAddRecord(getString(R.string.blood_pressure))
            lifecycleScope.launch {
                mViewModel.updateRecordTime(mViewBind.dateTimeSelectionView.getSelectDate())
                val result = mViewModel.saveBloodPressureRecord()

                when (result) {
                    is BpRecordViewModel.SaveRecordResult.Created -> {
                        goDetail(result.recordId)
                    }
                    is BpRecordViewModel.SaveRecordResult.Updated -> {
                       goDetail(result.recordId)
                    }
                    is BpRecordViewModel.SaveRecordResult.Failed -> {
                        // 显示保存失败提示
                        // TODO: 添加Toast显示错误信息
                    }
                }
            }
        }
    }


    private fun goDetail(recordId:Long){
        SaveCompleteDialog.show(supportFragmentManager){
            showInter {
                BpDetailActivity.start(this@BpRecordActivity, recordId)
                finish()
            }

        }
    }
    private fun observeViewModel() {
        this.collect(mViewModel.systolicPressure) {
            with(mViewBind){
                latestSystolic = it
                updateLsvCurrentIndex()
                if(it == npvSystolic.contentByCurrValue.toInt()){
                    return@collect
                }
                //将当前值的位置设置给滚动控件
                npvSystolic.value = it

            }
        }

        this.collect(mViewModel.diastolicPressure) {
            with(mViewBind){
                latestDiastolic = it
                updateLsvCurrentIndex()
                if(it == npvDiastolic.contentByCurrValue.toInt()){
                    return@collect
                }
                //将当前值的位置设置给滚动控件
                npvDiastolic.value = it

            }
        }

        this.collect(mViewModel.pulseRate) {
            with(mViewBind){
                if(it == npvPulse.contentByCurrValue.toInt()){
                    return@collect
                }
                //将当前值的位置设置给滚动控件
                npvPulse.value = it
            }
        }

        // 观察记录时间变化
        this.collectLatest(mViewModel.recordTime) { recordTime ->
            val calendar = Calendar.getInstance()
            calendar.time = recordTime
            if(!isDestroyed && !isFinishing){
                mViewBind.dateTimeSelectionView.getDateTimePicker().initView(
                    year = calendar.get(Calendar.YEAR),
                    month = calendar.get(Calendar.MONTH) + 1,
                    day = calendar.get(Calendar.DAY_OF_MONTH),
                    hour = calendar.get(Calendar.HOUR_OF_DAY),
                    minute = calendar.get(Calendar.MINUTE)
                )
            }
        }
        
        // 观察加载状态
        this.collectLatest(mViewModel.isLoading) { isLoading ->
            // 可以在这里显示加载状态
            mViewBind.btnSave.isEnabled = !isLoading
            mViewBind.btnSave.text = if (isLoading) {
                getString(com.healthtracker.blood.suger.R.string.saving)
            } else {
                getString(com.healthtracker.blood.suger.R.string.save)
            }
        }
        
        // 观察可用标签
        this.collectLatest(mViewModel.availableTags) { tags ->
                "Blood pressure tags loaded: ${tags.size} tags".logd(TAG)
                tags.forEach { tag ->
                    "Tag: ${tag.name}, isPredefined: ${tag.isPredefined}, type: ${tag.tagType}".logd(TAG)
                }
                healthTags.clear()
                healthTags.addAll(tags)
        }
        
        // 观察选中的标签ID
        this.collectLatest(mViewModel.selectedTagIds) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }
    }

    /**
     * 构建并设置 LeveStatusView 的等级列表与初始索引
     */
    private fun setupLeveStatusView() {
        val levels = LeveDataFactory.BloodPressure.buildItems(this)
        mViewBind.lsvStatusBp.setLevels(levels)
        // 初始索引根据当前选择器的值计算
        latestSystolic = mViewBind.npvSystolic.contentByCurrValue.toInt()
        latestDiastolic = mViewBind.npvDiastolic.contentByCurrValue.toInt()
        updateLsvCurrentIndex()

        // 在记录页开启范围说明点击，弹出通用等级说明对话框
        mViewBind.lsvStatusBp.setExplainClickable(true)
        mViewBind.lsvStatusBp.setOnExplainClick {
            val items = ArrayList(LeveDataFactory.BloodPressure.buildExplainItems(this))
            LevelExplainDialog.show(
                supportFragmentManager,
                des = getString(R.string.bp_range_des),
                items = items
            )
        }
    }

    /**
     * 根据当前收缩压/舒张压计算分类并更新 LeveStatusView 的 currentIndex
     */
    private fun updateLsvCurrentIndex() {
        val idx = LeveDataFactory.BloodPressure.indexFor(latestSystolic, latestDiastolic)
        mViewBind.lsvStatusBp.setCurrentLevel(idx)
    }


}