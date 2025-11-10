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
        // 配置 RecyclerView：将 "总饮水量" 区块作为第一个部分放入列表
        mViewBind.rcyHydrate.layoutManager = LinearLayoutManager(this)
        val adapter = HydrateAdapter(
            onQuickAddClick = { valueMl ->
                // TODO: 接入ViewModel逻辑，如增加饮水量、更新水杯动画
                // 这里先简单打印或占位
                println("QuickAdd clicked: ${valueMl}ml")
            },
            onRecordDeleteClick = { record ->
                // TODO: 接入删除逻辑
                println("Delete record: ${record.intakeMl}ml at ${DateTimeUtils.formatDateTimeWithSeconds(record.date)}")
            }
        )
        mViewBind.rcyHydrate.adapter = adapter

        // 第一部分：总饮水量 + 水杯
        val totalSection = HydrateItem.TotalSection(
            totalIntake = 200,
            unit = "ML",
            description = "You drink 2 cups of water today",
            currentCups = 3,
            maxCups = 8
        )

        // 第二部分：Quick Add 快捷添加
        val quickAddSection = HydrateItem.QuickAddSection(values = listOf(100, 200, 250, 300, 500, 800))

        // 第三部分：Record 记录列表（示例数据）
        val now = DateTimeUtils.now()
        val oneHourAgo = DateTimeUtils.addHours(now, -1)
        val recordSection = HydrateItem.RecordSection(
            records = listOf(
                HydrateRecordItem(intakeMl = 200, date = now),
                HydrateRecordItem(intakeMl = 200, date = oneHourAgo)
            )
        )

        adapter.submitList(listOf(totalSection, quickAddSection, recordSection))
    }
}