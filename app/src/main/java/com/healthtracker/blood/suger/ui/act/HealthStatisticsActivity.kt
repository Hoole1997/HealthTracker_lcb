package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHealthStatisticsBinding
import com.healthtracker.blood.suger.viewmodel.HealthStatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Health Statistics Activity
 */
@AndroidEntryPoint
class HealthStatisticsActivity : BaseInterActivity<HealthStatisticsViewModel, ActivityHealthStatisticsBinding>() {

    override fun createViewBinding(): ActivityHealthStatisticsBinding {
        return ActivityHealthStatisticsBinding.inflate(layoutInflater)
    }

    override fun getVMModelClass(): Class<HealthStatisticsViewModel> {
        return HealthStatisticsViewModel::class.java
    }

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun createObserver() {
        // TODO: Observe LiveData from ViewModel
    }
}