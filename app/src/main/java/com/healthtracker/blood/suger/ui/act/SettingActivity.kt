package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivitySettingBinding
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel

class SettingActivity: BaseMVVMActivity<BaseViewModel, ActivitySettingBinding>() {
    override fun createViewBinding() = ActivitySettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind){

        }
    }
}