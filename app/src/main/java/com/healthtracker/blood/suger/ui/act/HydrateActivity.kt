package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateBinding
import com.healthtracker.blood.suger.ui.dialog.ComingSoonDialog
import com.healthtracker.blood.suger.ui.adapter.HydrateAdapter
import com.healthtracker.blood.suger.ui.adapter.HydrateItem
import com.healthtracker.blood.suger.ui.adapter.HydrateRecordItem
import com.healthtracker.blood.suger.ui.viewmodel.HydrateViewModel
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine

@AndroidEntryPoint
class HydrateActivity : BaseInterActivity<HydrateViewModel, ActivityHydrateBinding>() {

    private var wasLoading: Boolean = false

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateActivity>()
        }
    }

    override fun createViewBinding(): ActivityHydrateBinding =
        ActivityHydrateBinding.inflate(layoutInflater)

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
                    // 返回到今天并触发回调刷新
                    mViewBind.weeklyDateSelector.resetToToday()
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
                // 快捷添加饮水记录
                mViewModel.addIntake(valueMl)
            },
            onRecordDeleteClick = { record ->
                // 删除选中记录
                mViewModel.deleteRecordById(record.id)
            },
            onDrinkClick = { valueMl ->
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
            val unitLabel = if (cupUnit == HydrateSettingManager.CupUnit.FL_OZ) getString(R.string.fl_oz) else getString(R.string.unit_ml)
            val totalSection = HydrateItem.TotalSection(
                totalIntake = totalMl,
                unit = unitLabel,
                description = String.format(this@HydrateActivity.getString(R.string.hydrate_cup_count_format), count),
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