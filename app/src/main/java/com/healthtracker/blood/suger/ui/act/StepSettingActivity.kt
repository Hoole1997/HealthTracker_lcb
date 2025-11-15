package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.constants.KEY_STEP_COUNT_GOLE
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.enums.BmiUnit
import com.healthtracker.blood.suger.databinding.ActivityStepSettingBinding
import com.healthtracker.blood.suger.ui.viewmodel.StepSettingViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StepSettingActivity: BaseInterActivity<StepSettingViewModel, ActivityStepSettingBinding>() {
    override fun createViewBinding() = ActivityStepSettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = StepSettingViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewModel.loadRecord()
        with(mViewBind){
            clBmi.clickWithDuration {
                startActivity<BmiRecordActivity>()
            }

            llGoal.clickWithDuration {

            }

            btnSave.clickWithDuration {

            }

            btnBack.clickWithDuration {
                handleBackPress()
            }


        }

        updateGoal()
        collectLatest(mViewModel.bmiRecord){
            updateBmiUI()
        }
    }

    private fun updateGoal() {
        mViewBind.tvGoalValue.text = SpUtils.getInt(KEY_STEP_COUNT_GOLE,6000).toString()
    }

    private fun updateBmiUI() {
       with(mViewBind){
           tvWeightValue.text = mViewModel.getDisplayWeight()
           tvHeightValue.text = mViewModel.getDisplayHeight()
           "(${BmiUnit.getWeightUnitLabel()})".also { tvWeightUnit.text = it }
           "(${BmiUnit.getHeightUnitLabel()})".also { tvHeightUnit.text = it }
       }
    }
}