package com.daily.health.manager.ui.act

import android.os.Bundle
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.constants.KEY_STEP_COUNT_GOLE
import com.daily.health.manager.data.entity.BmiRecord
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.databinding.HtActivityStepSettingBinding
import com.daily.health.manager.ui.dialog.StepGoalSelectDialog
import com.daily.health.manager.ui.viewmodel.StepSettingViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager

class StepSettingActivity: BaseInterActivity<StepSettingViewModel, HtActivityStepSettingBinding>() {
    override fun createViewBinding() = HtActivityStepSettingBinding.inflate(layoutInflater)

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
                        ReportDataManager.reportData("step_target_set",mapOf("Number" to it))
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