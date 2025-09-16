package com.healthtracker.blood.suger.act

import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivityBsRecordBinding
import com.healthtracker.blood.suger.ui.weight.BloodSugarRulerView
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BsRecordActivity: BaseMVVMActivity<BaseViewModel, ActivityBsRecordBinding>() {
    override fun createViewBinding() = ActivityBsRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
      with(mViewBind){
          btnBack.click {
              finish()
          }

          rulerView.setOnChooseResultListener(object : BloodSugarRulerView.OnChooseResultListener{
              override fun onEndResult(result: String) {
                  tvSelectValue.text = result
              }

              override fun onScrollResult(result: String) {
                  tvSelectValue.text = result
              }

          })

      }
    }
}