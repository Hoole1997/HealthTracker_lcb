package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateBinding
import com.healthtracker.blood.suger.ui.adapter.HydrateAdapter
import com.healthtracker.blood.suger.ui.adapter.HydrateItem
import com.healthtracker.blood.suger.ui.adapter.HydrateRecordItem
import com.healthtracker.blood.suger.ui.viewmodel.HydrateViewModel
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HydrateActivity : BaseInterActivity<HydrateViewModel, ActivityHydrateBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateActivity>()
        }
    }

    override fun createViewBinding(): ActivityHydrateBinding =
        ActivityHydrateBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HydrateViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
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
            }
        )
        mViewBind.rcyHydrate.adapter = adapter

        // 观察 ViewModel 流，实时更新 UI
        // 今日总量与次数
        collectLatest(mViewModel.todayTotalIntakeMl) { totalMl ->
            val count = mViewModel.todayDrinkCount.value
            val cups = totalMl / 250
            val totalSection = HydrateItem.TotalSection(
                totalIntake = totalMl,
                unit = "ML",
                description = "今天已饮水 ${count} 次",
                currentCups = cups,
                maxCups = 8
            )

            val quickAddSection = HydrateItem.QuickAddSection(values = listOf(100, 200, 250, 300, 500, 800))
            val recordSection = HydrateItem.RecordSection(records = mViewModel.todayRecordItems.value)

            adapter.submitList(listOf(totalSection, quickAddSection, recordSection))
        }

        // 今日记录列表（确保记录变化也能刷新）
        collectLatest(mViewModel.todayRecordItems) { records ->
            val totalMl = mViewModel.todayTotalIntakeMl.value
            val count = records.size
            val cups = totalMl / 250
            val totalSection = HydrateItem.TotalSection(
                totalIntake = totalMl,
                unit = "ML",
                description = "今天已饮水 ${count} 次",
                currentCups = cups,
                maxCups = 8
            )
            val quickAddSection = HydrateItem.QuickAddSection(values = listOf(100, 200, 250, 300, 500, 800))
            val recordSection = HydrateItem.RecordSection(records = records)
            adapter.submitList(listOf(totalSection, quickAddSection, recordSection))
        }
    }
}