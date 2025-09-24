package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivityAlarmManagerBinding
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AlarmManageActivity: BaseMVVMActivity<BaseViewModel, ActivityAlarmManagerBinding>() {
    override fun createViewBinding()  = ActivityAlarmManagerBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {

    }
}