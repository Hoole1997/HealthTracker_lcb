package com.daily.health.manager.face.act

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.LinearLayoutManager
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.databinding.HtActivityHydrateBinding
import com.daily.health.manager.face.dialog.ComingSoonDialog
import com.daily.health.manager.face.adapter.HydrateAdapter
import com.daily.health.manager.face.adapter.HydrateItem
import com.daily.health.manager.face.adapter.HydrateRecordItem
import com.daily.health.manager.face.viewmodel.HydrateViewModel
import com.daily.health.manager.config.HydrateSettingManager
import com.daily.health.manager.data.utils.DateTimeUtils
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.startActivity
import kotlinx.coroutines.flow.combine
import net.corekit.core.report.ReportDataManager

class HydrateScreen : BaseInterActivity<HydrateViewModel, HtActivityHydrateBinding>() {

    private var actionBarBaseHeight: Int = -1
    private var currentDrinkAmountMl: Int = 100
    private val drinkTextFormat: String by lazy { getString(R.string.ht_drink_btn_format) }

    // 用于区分首次加载和饮水操作
    private var initializationComplete = false
    private var lastDisplayTotal: Int = -1  // 使用 -1 表示未初始化
    private var lastProgress: Int = -1
    private var intakeAnimator: ValueAnimator? = null

    companion object {
        private const val ANIMATION_DURATION = 800L

        fun start(context: Context) {
            context.startActivity<HydrateScreen>()
        }
    }

    override fun getStatusBarColor() = R.color.bg_window

    override fun createViewBinding(): HtActivityHydrateBinding =
        HtActivityHydrateBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HydrateViewModel::class.java


    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        mViewBind.ivSetting.setOnClickListener {
            HydrateSettingScreen.start(this)
        }

        // 周日期选择：拦截未来日期并弹窗
        mViewBind.weeklyDateSelector.setOnDateSelectedListener { selectedDate ->
            val now = DateTimeUtils.now()
            val endOfToday = DateTimeUtils.getTodayRange().second
            val isFutureDate = !DateTimeUtils.isSameDay(selectedDate, now) && selectedDate.after(endOfToday)

            if (isFutureDate) {
                ComingSoonDialog.show(supportFragmentManager) {
                    // 返回到今天并同步周视图到当前周（不改控件源码）
                    mViewBind.weeklyDateSelector.setDefaultSelectedDate(DateTimeUtils.now())
                }
                return@setOnDateSelectedListener
            }

            // 正常选择：重置动画状态并更新ViewModel
            initializationComplete = false
            lastDisplayTotal = -1
            lastProgress = -1
            mViewModel.onDateSelected(selectedDate)
        }

        // 配置 TotalSection 事件
        setupTotalSection()

        // 配置 RecyclerView（仅显示 QuickAdd + Records）
        mViewBind.rcyHydrate.layoutManager = LinearLayoutManager(this)
        val adapter = HydrateAdapter(
            onQuickAddClick = { valueMl ->
                ReportDataManager.reportData("water_quickAdd_click", mapOf("number" to valueMl))
                initializationComplete = true  // 用户操作，启用动画
                mViewModel.addIntake(valueMl)
            },
            onRecordDeleteClick = { record ->
                initializationComplete = true  // 用户操作，启用动画
                mViewModel.deleteRecordById(record.id)
            }
        )
        mViewBind.rcyHydrate.adapter = adapter

        // 合并 Flow 绑定 TotalSection 和 RecyclerView
        val uiFlow = combine(
            mViewModel.todayTotalIntakeMl,
            mViewModel.todayRecordItems,
            mViewModel.todayDrinkCount,
            HydrateSettingManager.cupUnitFlow(),
            HydrateSettingManager.cupVolumeFlow(),
            HydrateSettingManager.dailyTargetMlFlow()
        ) { values ->
            val totalMl = values[0] as Int
            @Suppress("UNCHECKED_CAST")
            val records = values[1] as List<HydrateRecordItem>
            val count = values[2] as Int
            val cupUnit = values[3] as HydrateSettingManager.CupUnit
            val cupVolumeMl = values[4] as Int
            val targetMl = values[5] as Int
            UiData(totalMl, targetMl, records, count, cupUnit, cupVolumeMl)
        }

        collect(uiFlow) { data ->
            // 绑定 TotalSection
            bindTotalSection(data)
            // 绑定 RecyclerView（QuickAdd + Records）
            val unitLabel = if (data.cupUnit == HydrateSettingManager.CupUnit.FL_OZ) getString(R.string.ht_fl_oz) else getString(R.string.ht_unit_ml)
            val quickAddSection = HydrateItem.QuickAddSection(values = listOf(100, 200, 250, 300, 500, 800), unit = unitLabel)
            val recordSection = HydrateItem.RecordSection(records = data.records, unit = unitLabel)
            adapter.submitList(listOf(quickAddSection, recordSection))
        }

        collect(mViewModel.isLoading) { isLoading ->

        }

        // 消费达标事件（不再单独处理跳转，统一在动画结束后跳转）
        collect(mViewModel.justReachedTarget) { reached ->
            if (reached) {
                mViewModel.consumeJustReachedTarget()
            }
        }
    }

    private fun setupTotalSection() {
        mViewBind.totalSection.drinkMore.setOnClickListener {
            val cupUnit = HydrateSettingManager.getCupUnit()
            val stepMl = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) {
                HydrateSettingManager.toMl(1, HydrateSettingManager.CupUnit.FL_OZ)
            } else {
                10
            }
            currentDrinkAmountMl += stepMl
            updateDrinkButtonText()
        }
        mViewBind.totalSection.drinkLess.setOnClickListener {
            val cupUnit = HydrateSettingManager.getCupUnit()
            val stepMl = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) {
                HydrateSettingManager.toMl(1, HydrateSettingManager.CupUnit.FL_OZ)
            } else {
                10
            }
            currentDrinkAmountMl = (currentDrinkAmountMl - stepMl).coerceAtLeast(stepMl)
            updateDrinkButtonText()
        }
        mViewBind.totalSection.drinkBtn.setOnClickListener {
            ReportDataManager.reportData("water_drink_click", mapOf())
            initializationComplete = true  // 用户操作，启用动画
            mViewModel.addIntake(currentDrinkAmountMl)
        }
    }

    private fun updateDrinkButtonText() {
        val cupUnit = HydrateSettingManager.getCupUnit()
        val unitText = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) getString(R.string.ht_fl_oz) else getString(R.string.ht_unit_ml)
        val displayAmount = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) {
            HydrateSettingManager.fromMl(currentDrinkAmountMl, HydrateSettingManager.CupUnit.FL_OZ)
        } else {
            currentDrinkAmountMl
        }
        mViewBind.totalSection.drinkBtn.text = String.format(drinkTextFormat, displayAmount, unitText)
    }

    private fun bindTotalSection(data: UiData) {
        val cupUnit = data.cupUnit
        val minMl = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) {
            HydrateSettingManager.toMl(1, HydrateSettingManager.CupUnit.FL_OZ)
        } else {
            10
        }
        currentDrinkAmountMl = data.cupVolumeMl.coerceAtLeast(minMl)

        val displayTotal = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) {
            HydrateSettingManager.fromMl(data.totalMl, HydrateSettingManager.CupUnit.FL_OZ)
        } else {
            data.totalMl
        }
        val displayTarget = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) {
            HydrateSettingManager.fromMl(data.targetMl, HydrateSettingManager.CupUnit.FL_OZ)
        } else {
            data.targetMl
        }
        val unitLabel = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) getString(R.string.ht_fl_oz) else getString(R.string.ht_unit_ml)

        // 计算进度
        val targetMl = data.targetMl.coerceAtLeast(1)
        val progress = ((data.totalMl.toFloat() / targetMl) * 100).toInt().coerceIn(0, 100)

        // 根据初始化状态决定是否播放动画
        if (!initializationComplete) {
            // 初始化阶段：无动画直接设置
            mViewBind.totalSection.totalWaterIntake.text = displayTotal.toString()
            mViewBind.totalSection.waterCupView.setProgressValueNoAnimation(progress)
        } else if (displayTotal != lastDisplayTotal && lastDisplayTotal >= 0) {
            // 饮水量变化，播放动画
            animateIntakeChange(lastDisplayTotal, displayTotal)
            if (progress != lastProgress) {
                mViewBind.totalSection.waterCupView.progressValue = progress
            }
        } else if (progress != lastProgress && lastProgress >= 0) {
            // 只有进度变化（如目标改变），无动画直接设置
            mViewBind.totalSection.waterCupView.setProgressValueNoAnimation(progress)
        } else {
            // 无变化，直接设置（不播放动画）
            mViewBind.totalSection.totalWaterIntake.text = displayTotal.toString()
            mViewBind.totalSection.waterCupView.setProgressValueNoAnimation(progress)
        }
        lastDisplayTotal = displayTotal
        lastProgress = progress

        mViewBind.totalSection.totalWaterUnit.text = "/$displayTarget$unitLabel"
        mViewBind.totalSection.totalWaterDesc.text = String.format(getString(R.string.ht_hydrate_cup_count_format), data.count)

        updateDrinkButtonText()
    }

    /**
     * 饮水量数字动画：从 fromValue 渐变到 toValue
     */
    private fun animateIntakeChange(fromValue: Int, toValue: Int) {
        intakeAnimator?.cancel()
        // 判断是否为添加饮水（增加量），删除记录时不跳转
        val isAddingIntake = toValue > fromValue
        intakeAnimator = ValueAnimator.ofInt(fromValue, toValue).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val currentValue = animator.animatedValue as Int
                mViewBind.totalSection.totalWaterIntake.text = currentValue.toString()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 仅添加饮水时跳转到完成页面，删除记录时不跳转
                    if (isAddingIntake) {
                        HydrateCompleteScreen.start(this@HydrateScreen)
                    }
                }
            })
            start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        intakeAnimator?.cancel()
        intakeAnimator = null
    }

    private data class UiData(
        val totalMl: Int,
        val targetMl: Int,
        val records: List<HydrateRecordItem>,
        val count: Int,
        val cupUnit: HydrateSettingManager.CupUnit,
        val cupVolumeMl: Int
    )
}