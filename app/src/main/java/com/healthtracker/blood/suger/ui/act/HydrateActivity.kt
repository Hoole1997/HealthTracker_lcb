package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.HtActivityHydrateBinding
import com.healthtracker.blood.suger.ui.dialog.ComingSoonDialog
import com.healthtracker.blood.suger.ui.adapter.HydrateAdapter
import com.healthtracker.blood.suger.ui.adapter.HydrateItem
import com.healthtracker.blood.suger.ui.adapter.HydrateRecordItem
import com.healthtracker.blood.suger.ui.viewmodel.HydrateViewModel
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.startActivity
import kotlinx.coroutines.flow.combine
import net.corekit.core.report.ReportDataManager

class HydrateActivity : BaseInterActivity<HydrateViewModel, HtActivityHydrateBinding>() {

    private var wasLoading: Boolean = false
    private var actionBarBaseHeight: Int = -1

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateActivity>()
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
            HydrateSettingActivity.start(this)
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

            // 正常选择：更新ViewModel
            mViewModel.onDateSelected(selectedDate)
        }

        // 配置 RecyclerView
        mViewBind.rcyHydrate.layoutManager = LinearLayoutManager(this)
        val adapter = HydrateAdapter(
            onQuickAddClick = { valueMl ->
                ReportDataManager.reportData("water_quickAdd_click",mapOf("number" to valueMl))
                // 快捷添加饮水记录
                mViewModel.addIntake(valueMl)
            },
            onRecordDeleteClick = { record ->
                // 删除选中记录
                mViewModel.deleteRecordById(record.id)
            },
            onDrinkClick = { valueMl ->
                ReportDataManager.reportData("water_drink_click",mapOf())
                // 中间饮水按钮点击：插入对应饮水记录
                mViewModel.addIntake(valueMl)
            }
        )
        mViewBind.rcyHydrate.adapter = adapter

        // 合并三个 Flow，确保一次性、原子性地刷新 UI
        val uiFlow = combine(
            mViewModel.todayTotalIntakeMl,
            mViewModel.todayRecordItems,
            mViewModel.todayDrinkCount,
            HydrateSettingManager.cupUnitFlow(),
            HydrateSettingManager.cupVolumeFlow()
        ) { totalMl: Int, records: List<HydrateRecordItem>, count: Int, cupUnit, cupVolumeMl ->
            val cups = totalMl / 250
            val unitLabel = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) getString(R.string.ht_fl_oz) else getString(R.string.ht_unit_ml)
            val totalSection = HydrateItem.TotalSection(
                totalIntake = totalMl,
                unit = unitLabel,
                description = String.format(this@HydrateActivity.getString(R.string.ht_hydrate_cup_count_format), count),
                currentCups = cups,
                maxCups = 8,
                cupVolumeMl = cupVolumeMl
            )
            val quickAddSection = HydrateItem.QuickAddSection(values = listOf(100, 200, 250, 300, 500, 800), unit = unitLabel)
            val recordSection = HydrateItem.RecordSection(records = records, unit = unitLabel)
            listOf(totalSection, quickAddSection, recordSection)
        }

        collect(uiFlow) { uiList: List<HydrateItem> ->
            adapter.submitList(uiList)
        }

        collect(mViewModel.isLoading) { isLoading ->
            mViewBind.loadingOverlay.isVisible = isLoading
            if (isLoading) {
                mViewBind.lottieHydrateLoading.playAnimation()
            } else {
                mViewBind.lottieHydrateLoading.cancelAnimation()
                if (wasLoading) {
                    HydrateCompleteActivity.start(this)
                }
            }
            wasLoading = isLoading
        }
    }
}