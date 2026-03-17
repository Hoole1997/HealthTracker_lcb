package com.daily.health.manager.face.act

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.config.HydrateSettingManager
import com.daily.health.manager.databinding.FcActivityHydrateSettingBinding
import com.daily.health.manager.face.adapter.HydrateSettingAdapter
import com.daily.health.manager.face.viewmodel.HydrateSettingViewModel
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.startActivity

class HydrateSettingAct : BaseInterActivity<HydrateSettingViewModel, FcActivityHydrateSettingBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateSettingAct>()
        }
    }

    override fun createViewBinding(): FcActivityHydrateSettingBinding =
        FcActivityHydrateSettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HydrateSettingViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 仅 UI 初始化，不涉及任何业务逻辑或数据持久化
        mViewBind.btnBack.setOnClickListener { finish() }

        // 使用 RecyclerView 渲染设置页面的模块化列表
        mViewBind.rcySetting.layoutManager = LinearLayoutManager(this)
        val adapter = HydrateSettingAdapter(
            onDailyCupsChanged = { cups ->
                HydrateSettingManager.setDailyCups(cups)
                // 同步更新目标饮水量 = 杯数 × 杯子容量
                val newTargetMl = cups * HydrateSettingManager.getCupVolume()
                HydrateSettingManager.setDailyTargetMl(newTargetMl)
                // DailyCups 变更后，同步当天插入的记录字段
                mViewModel.syncTodayHydrateRecordSettings()
            },
            onCupSettingChanged = { value, isMl ->
                val unit = if (isMl) HydrateSettingManager.CupUnit.ML else HydrateSettingManager.CupUnit.FL_OZ
                val ml = HydrateSettingManager.toMl(value, unit)
                HydrateSettingManager.setCupVolume(ml)
                HydrateSettingManager.setCupUnit(unit)
                // 同步更新目标饮水量 = 杯数 × 杯子容量
                val newTargetMl = HydrateSettingManager.getDailyCups() * ml
                HydrateSettingManager.setDailyTargetMl(newTargetMl)
                // CupVolume 变更后，同步当天插入的记录字段
                mViewModel.syncTodayHydrateRecordSettings()
            },
            onCupUnitChanged = { isMl ->
                HydrateSettingManager.setCupUnit(
                    if (isMl) HydrateSettingManager.CupUnit.ML else HydrateSettingManager.CupUnit.FL_OZ
                )
            },
            onAddReminderTime = { h, m ->
                mViewModel.addReminder(h, m)
            },
            onDeleteReminderTime = { h, m ->
                mViewModel.deleteReminder(h, m)
            },
            onToggleReminderEnabled = { h, m, enabled ->
                mViewModel.updateReminderEnabled(h, m, enabled)
            }
        )
        mViewBind.rcySetting.adapter = adapter

        // 观察提醒时间并同步到适配器显示
        collect(mViewModel.reminderTimes) { times ->
            adapter.setReminderTimes(times)
        }

        // 观察提醒实体列表（包含 enabled）并同步到适配器开关显示
        collect(mViewModel.reminders) { reminders ->
            adapter.setReminderStates(reminders)
        }
    }

}