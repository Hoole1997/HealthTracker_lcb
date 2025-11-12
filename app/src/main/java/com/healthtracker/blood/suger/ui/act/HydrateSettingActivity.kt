package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.appcompat.widget.AppCompatTextView
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateSettingBinding
import com.healthtracker.blood.suger.ui.viewmodel.HydrateSettingViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.ui.adapter.HydrateSettingAdapter
import com.healthtracker.framework.ext.startActivity
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
        mViewBind.rcySetting.adapter = HydrateSettingAdapter()
    }

    // 旧版静态视图交互已移除，改为在适配器中处理单位切换与数值展示

    // RulerView 的 UI 更新逻辑已迁移至 RecyclerView 的杯子容量模块 item 中

    // 辅助方法已不再需要，旧视图引用移除
}