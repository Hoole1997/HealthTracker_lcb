package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivityFsiNotificationBinding
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel

class FsiNotificationActivity: BaseMVVMActivity<BaseViewModel, ActivityFsiNotificationBinding>() {
    override fun createViewBinding() = ActivityFsiNotificationBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {

    }
}