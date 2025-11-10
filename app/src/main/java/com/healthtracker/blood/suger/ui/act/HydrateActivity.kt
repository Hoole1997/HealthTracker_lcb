package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateBinding
import com.healthtracker.blood.suger.ui.viewmodel.HydrateViewModel
import com.healthtracker.framework.ext.startActivity
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
        // 仅初始化最基本的页面结构，无业务逻辑
    }
}