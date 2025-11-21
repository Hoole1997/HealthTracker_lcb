package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.constants.KEY_STEP_COUNT_GOLE
import com.healthtracker.blood.suger.data.entity.BmiRecord
import com.healthtracker.blood.suger.data.enums.BmiUnit
import com.healthtracker.blood.suger.databinding.ActivityStepSettingBinding
import com.healthtracker.blood.suger.ui.dialog.StepGoalSelectDialog
import com.healthtracker.blood.suger.ui.viewmodel.StepSettingViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.SpUtils
import dagger.hilt.android.AndroidEntryPoint
import net.corekit.core.report.ReportDataManager

@AndroidEntryPoint
class StepSettingActivity: BaseInterActivity<StepSettingViewModel, ActivityStepSettingBinding>() {
    override fun createViewBinding() = ActivityStepSettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = StepSettingViewModel::class.java

    private val goal = SpUtils.getInt(KEY_STEP_COUNT_GOLE,6000)

    private var newGoal = 0

    override fun initView(savedInstanceState: Bundle?) {
        mViewModel.loadRecord()
        with(mViewBind){
            clWeight.clickWithDuration {
                startActivity<BmiRecordActivity>()
            }
            clHeight.clickWithDuration {
                startActivity<BmiRecordActivity>()
            }

            llGoal.clickWithDuration {
                StepGoalSelectDialog{
                    if((newGoal > 0 && it != newGoal) || (newGoal == 0 && it != goal)){
                        newGoal = it
                        tvGoalValue.text = it.toString()
                        ReportDataManager.reportData("step_targe_set",mapOf("Number" to it))
                    }

                }.show(supportFragmentManager)

            }

            btnSave.clickWithDuration {
                if(newGoal != goal){
                    SpUtils.putInt(KEY_STEP_COUNT_GOLE,newGoal)
                    handleBackPress()
                }

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
        mViewBind.tvGoalValue.text = goal.toString()
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