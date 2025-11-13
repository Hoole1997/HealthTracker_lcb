package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateSettingBinding
import com.healthtracker.blood.suger.ui.viewmodel.HydrateSettingViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.ui.adapter.HydrateSettingAdapter
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.framework.util.LogUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HydrateSettingActivity : BaseInterActivity<HydrateSettingViewModel, ActivityHydrateSettingBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateSettingActivity>()
        }
    }

    override fun createViewBinding(): ActivityHydrateSettingBinding =
        ActivityHydrateSettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HydrateSettingViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 仅 UI 初始化，不涉及任何业务逻辑或数据持久化
        mViewBind.btnBack.setOnClickListener { finish() }

        // 使用 RecyclerView 渲染设置页面的模块化列表
        mViewBind.rcySetting.layoutManager = LinearLayoutManager(this)
        val adapter = HydrateSettingAdapter(
            onDailyCupsChanged = { cups ->
                HydrateSettingManager.setDailyCups(cups)
            },
            onCupSettingChanged = { value, isMl ->
                val unit = if (isMl) HydrateSettingManager.CupUnit.ML else HydrateSettingManager.CupUnit.FL_OZ
                val ml = HydrateSettingManager.toMl(value, unit)
                Log.d("aaaaa", "aaaaa = $ml and unit = $unit")
                HydrateSettingManager.setCupVolume(ml)
                HydrateSettingManager.setCupUnit(unit)
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
            }
        )
        mViewBind.rcySetting.adapter = adapter

        // 观察提醒时间并同步到适配器显示
        collect(mViewModel.reminderTimes) { times ->
            adapter.setReminderTimes(times)
        }
    }

}