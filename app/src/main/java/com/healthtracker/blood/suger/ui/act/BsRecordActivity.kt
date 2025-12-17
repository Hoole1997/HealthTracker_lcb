package com.healthtracker.blood.suger.ui.act

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.app.raise.AppraiseManager
import com.app.raise.listeners.EvaluateListener
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.constants.APPRAISE_SHOW_TIME
import com.healthtracker.blood.suger.constants.IN_APP_REVIEW_STATE
import com.healthtracker.blood.suger.constants.IN_APP_REVIEW_TIMES
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.enums.getStatusStringRes
import com.healthtracker.blood.suger.databinding.ActivityBsRecordBinding
import com.healthtracker.blood.suger.ui.dialog.HealthTagDialog
import com.healthtracker.blood.suger.ui.dialog.SaveCompleteDialog
import com.healthtracker.blood.suger.ui.dialog.StatusSelectDialog
import com.healthtracker.blood.suger.ui.tracker.HealthType
import com.healthtracker.blood.suger.ui.tracker.trackAddNewRecord
import com.healthtracker.blood.suger.ui.viewmodel.BsRecordViewModel
import com.healthtracker.blood.suger.ui.weight.RulerView
import com.healthtracker.blood.suger.util.BloodSugarScaleHelper
import com.healthtracker.blood.suger.utils.getTodayStart
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.blood.suger.utils.showInter
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.corekit.monetize.ui.NativeAdStyle
import java.util.Calendar

@OptIn(FlowPreview::class)
@AndroidEntryPoint
class BsRecordActivity: BaseInterActivity<BsRecordViewModel, ActivityBsRecordBinding>() {

    // ActivityResult launcher for target range settings
    private val targetRangeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            updateBloodSugarRangeView()
        }
    }
    private var manager: ReviewManager? = null
    private val healthTags = mutableListOf<HealthTag>()
    private val addTagIds = mutableListOf<Long>()

    companion object {
        private const val TAG = "BsRecordActivity"
        private const val EXTRA_RECORD_ID = "extra_record_id"
        // 启动编辑模式
        fun start(context: Context, recordId: Long? = null) {
            context.startActivity<BsRecordActivity>(EXTRA_RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() = ActivityBsRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BsRecordViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {

        // 获取传入的记录ID（如果有）
        val recordId = intent.getLongExtra(EXTRA_RECORD_ID, -1L)
        val editRecordId = if (recordId == -1L) null else recordId

        // 在观察者设置完成后再初始化ViewModel数据
        mViewModel.initializeWithRecord(editRecordId)

        with(mViewBind) {
            btnBack.clickWithDuration {
                onBackPress()
            }

            showAppraiseDialog()

            editRecordId?.let {
                tvTitle.text = getString(R.string.edit_record)
            }

            clRangeTarget.clickWithDuration {
                targetRangeLauncher.launch(Intent(this@BsRecordActivity, TargetRangeActivity::class.java))
            }

            // 设置DateTimeSelectionView的标签点击监听
            dateTimeSelectionView.setOnLabelClickListener {
                val addTags = if(addTagIds.isEmpty()) null else {
                    val tempTags = mutableListOf<HealthTag>()
                    for(id in addTagIds){
                        healthTags.find { it.id == id }?.let {
                            tempTags.add(it)
                        }
                    }
                    tempTags
                }
                HealthTagDialog.showBloodSugarDialog(
                    supportFragmentManager,
                    mViewModel.getAvailableHealthTags(),
                    addTags,
                    onSave = { selectedTags ->
                        mViewModel.updateTags(selectedTags)
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

            setupRulerView()
            setupUnitSwitcher()
            setupRangeView()
            setupStatusSelector()
            setupSaveButton()
            // 先设置观察者，确保能接收到数据变化
            observeViewModel()
            loadNative(adContainer, style = NativeAdStyle.STANDARD)
        }
    }

    private fun setupRulerView() {
        with(mViewBind) {
            rulerView.setOnChooseResultListener(object : RulerView.OnChooseResultListener {
                override fun onEndResult(result: Float) {
                    try {
                        "onEndResult result = $result".logd(TAG)
                        mViewModel.updateValue(result.toFloat())
                        rangeView.updateValue(result.toFloat())
                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }

                override fun onScrollResult(result: Float) {
                    try {
                        "onScrollResult result = $result".logd(TAG)
                        val currentUnit = mViewModel.currentUnit.value
                        tvSelectValue.text = BsUnit.formatValue(result, currentUnit)

                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }
            })
        }
    }

    private fun setupUnitSwitcher() {
        with(mViewBind) {
            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                val newUnit = when (checkedId) {
                    rbMgdl.id -> BsUnit.MG_DL
                    rbMmol.id -> BsUnit.MMOL_L
                    else -> return@setOnCheckedChangeListener
                }

                if (newUnit != mViewModel.currentUnit.value) {
                    mViewModel.switchUnit(newUnit)
                }
            }
        }
    }

    private fun setupSaveButton() {
        mViewBind.btnSave.clickWithDuration {
            trackAddNewRecord(HealthType.BLOOD_SUGAR)
            lifecycleScope.launch {
                mViewModel.updateRecordTime(mViewBind.dateTimeSelectionView.getSelectDate())
                val result = mViewModel.saveRecord()

                when (result) {
                    is BsRecordViewModel.SaveRecordResult.Created -> {
                        // 新建记录成功，跳转到详情页
                        goDetail(result.recordId)
                    }
                    is BsRecordViewModel.SaveRecordResult.Updated -> {
                        // 更新记录成功，跳转到详情页
                        goDetail(result.recordId)
                    }
                    is BsRecordViewModel.SaveRecordResult.Failed -> {
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
               BsDetailActivity.start(this@BsRecordActivity, recordId)
               finish()
           }
        }
    }

    private fun observeViewModel() {
        this.collectLatest(mViewModel.currentValue) { value ->
            try {
                updateDisplayValues()
                updateRangeView()
                mViewBind.rulerView.setScaleImmediately(value)
            } catch (e: Exception) {
                // 记录错误或显示用户友好的错误信息
            }
        }

        this.collectLatest(mViewModel.currentUnit) { unit ->
            updateUnitRadioButtons(unit)
            updateDisplayValues()
            configureRulerForUnit(unit)
            // 单位切换后，立即设置当前值位置（无动画，抑制回调避免闪烁）
            mViewBind.rulerView.setScaleImmediately(mViewModel.currentValue.value, suppressCallback = true)
        }

        this.collectLatest(mViewModel.currentStatus) { status ->
            mViewBind.tvStatus.text = getStatusDisplayText(status.statusType)
            updateRangeView()
        }

        this.collect(mViewModel.isLoading) { isLoading ->
            mViewBind.btnSave.isEnabled = !isLoading
            mViewBind.btnSave.text = if (isLoading) {
                getString(R.string.saving)
            } else {
                getString(R.string.save)
            }
        }

        this.collectLatest(mViewModel.recordTime) { recordTime ->
            // 将Date转换为DateTimePicker需要的参数
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

        lifecycleScope.launch {
            mViewModel.getAvailableHealthTags().collectLatest { tags ->
                "Blood sugar tags loaded: ${tags.size} tags".logd(TAG)
                tags.forEach { tag ->
                    "Tag: ${tag.name}, isPredefined: ${tag.isPredefined}, type: ${tag.tagType}".logd(TAG)
                }
                healthTags.clear()
                healthTags.addAll(tags)
            }
        }

        this.collectLatest(mViewModel.healthTags) { tagIds ->
            addTagIds.clear()
            addTagIds.addAll(tagIds)
        }
    }

    private fun updateUnitRadioButtons(unit: BsUnit) {
        mViewBind.rgUnit.check(
            when (unit) {
                BsUnit.MG_DL -> mViewBind.rbMgdl.id
                BsUnit.MMOL_L -> mViewBind.rbMmol.id
            }
        )
    }

    private fun configureRulerForUnit(unit: BsUnit) {
        BloodSugarScaleHelper.configureRulerForUnit(mViewBind.rulerView, unit)
    }

    private fun setupRangeView() {
        // 范围视图将通过observeViewModel自动更新
    }

    private fun setupStatusSelector() {
        with(mViewBind) {
            // 设置状态选择点击事件
            clStatu.click {
                StatusSelectDialog.show(supportFragmentManager,mViewModel.currentStatus.value){
                    it?.run {
                        mViewModel.updateStatus(this)
                    }

                }

            }
        }
    }


    private fun updateDisplayValues() {
        val currentValue = mViewModel.currentValue.value
        val currentUnit = mViewModel.currentUnit.value
        mViewBind.tvSelectValue.text = BsUnit.formatValue(currentValue, currentUnit)
    }

    private fun updateRangeView() {
        mViewBind.rangeView.setCurrentState(
            mViewModel.currentValue.value,
            mViewModel.currentUnit.value,
            mViewModel.currentStatus.value
        )
    }

    private fun getStatusDisplayText(statusType: Int): String {
        val stringRes = getStatusStringRes(statusType)
        return getString(stringRes)
    }

    /**
     * 更新血糖范围视图
     * 当从目标范围设置页面返回时调用
     */
    private fun updateBloodSugarRangeView() {
        mViewBind.rangeView.updateStatus(mViewModel.currentStatus.value)
    }


    private fun showAppraiseDialog() {
        val appraiseShowtime = SpUtils.getLong(APPRAISE_SHOW_TIME, 0)
        val reviewShowTimes = SpUtils.getInt(IN_APP_REVIEW_TIMES, 0)
        val reviewShowState = SpUtils.getBoolean(IN_APP_REVIEW_STATE, false)
//        if (reviewShowTimes == 3 || reviewShowState || getTodayStart().time <= appraiseShowtime) {
//            "showAppraiseDialog.reviewShowTimes:$reviewShowTimes".logd(TAG)
//            "showAppraiseDialog.reviewShowState:$reviewShowState".logd(TAG)
//            "showAppraiseDialog.appraiseShowtime:$appraiseShowtime,getTodayStart${getTodayStart().time}".logd(
//                TAG
//            )
//            return
//        }

        if (reviewShowTimes > 0) {
            return
        }
        AppraiseManager(this@BsRecordActivity).showAppraiseDialog(object : EvaluateListener {
            override fun evaluateUs(evaluateScore: Int) {
                SpUtils.putBoolean(IN_APP_REVIEW_STATE, true)
                initInnerReview()
                "showAppraiseDialog.evaluateUs".logd(TAG)
            }

            override fun feedback(evaluateScore: Int) {
                startActivity<FeedbackActivity>()
                SpUtils.putBoolean(IN_APP_REVIEW_STATE, true)
                "showAppraiseDialog.feedback".logd(TAG)
            }

            override fun cancelDialog(dialog: Dialog) {
                "showAppraiseDialog.cancelDialog".logd(TAG)
            }

            override fun dismissDialog(dialog: Dialog) {
                "showAppraiseDialog.dismissDialog".logd(TAG)
            }

            override fun sendEvent(var1: String?, var2: String?, var3: String?) {
            }

            override fun sendException(throwable: Throwable?) {
            }
        })
        SpUtils.putLong(APPRAISE_SHOW_TIME, getTodayStart().time)
        SpUtils.putInt(IN_APP_REVIEW_TIMES, reviewShowTimes + 1)
    }


   private fun initInnerReview() {
        //1、经过 ReviewManagerFactory 创立 ReviewManager 目标，用于发动运用内点评的流程
        manager = ReviewManagerFactory.create(App.INSTANCE)
        //2、获取ReviewInfo目标。当咱们判别能够让用户进行点评时，运用ReviewManager创立一个使命，
        //用于真实发动运用内点评流程。这儿谷歌文档中建议提前一点缓存好 ReviewInfo 目标。
        val request = manager?.requestReviewFlow()
        request?.addOnCompleteListener {
            "init: get reviewInfo sucess".logd(TAG)
            if (it.isSuccessful) {
                innerReview(it.result)
            } else {
                "init: get reviewInfo failed:${it.exception}".logd(TAG)
            }
        }
    }

    /**
     * 应用内点评
     */
    private fun innerReview(reviewInfo: ReviewInfo) {
        //调用 launchReviewFlow 来发动点评流程，剩余的工作就交给 Google 了
        reviewInfo.apply {
            val flow = manager?.launchReviewFlow(this@BsRecordActivity, this)
            flow?.addOnCompleteListener {
                if (it.isSuccessful) {
                    "innerReview: launchReviewFlow success".logd(TAG)
                    SpUtils.putBoolean(IN_APP_REVIEW_STATE, true)
                } else {
                    "innerReview: launchReviewFlow failed".logd(TAG)
                }
            }
        }
    }


}