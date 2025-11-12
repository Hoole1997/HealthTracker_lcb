package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateSettingBinding
import com.healthtracker.blood.suger.ui.viewmodel.HydrateSettingViewModel
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
        // 仅骨架，不实现 UI 与业务
    }
}