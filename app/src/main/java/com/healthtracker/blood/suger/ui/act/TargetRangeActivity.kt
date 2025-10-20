package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivityTargetRangeBinding
import com.healthtracker.blood.suger.ui.viewmodel.TargetRangeViewModel
import com.healthtracker.framework.base.BaseMVVMActivity

class TargetRangeActivity : BaseMVVMActivity<TargetRangeViewModel, ActivityTargetRangeBinding>() {
    override fun createViewBinding() = ActivityTargetRangeBinding.inflate(layoutInflater)

    override fun getVMModelClass() = TargetRangeViewModel::class.java
    override fun initView(savedInstanceState: Bundle?) {

    }
}