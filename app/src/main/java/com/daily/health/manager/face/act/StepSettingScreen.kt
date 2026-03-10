package com.daily.health.manager.face.act

import android.os.Bundle
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.constants.KEY_STEP_COUNT_GOLE
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.databinding.TrActivityStepSettingBinding
import com.daily.health.manager.face.dialog.StepGoalSelectDialog
import com.daily.health.manager.face.viewmodel.StepSettingViewModel
import com.daily.health.manager.utils.loadInterstitial
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.AdPosition

class StepSettingScreen: BaseInterActivity<StepSettingViewModel, TrActivityStepSettingBinding>() {
    override fun createViewBinding() = TrActivityStepSettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = StepSettingViewModel::class.java

    private val goal = SpUtils.getInt(KEY_STEP_COUNT_GOLE,6000)

    private var newGoal = 0

    override fun initView(savedInstanceState: Bundle?) {
        mViewModel.loadRecord()
        with(mViewBind){
            clWeight.clickWithDuration {
                HealthRecordScreen.start(
                    this@StepSettingScreen,
                    HealthRecordScreen.RecordType.BMI
                )
            }
            clHeight.clickWithDuration {
                HealthRecordScreen.start(
                    this@StepSettingScreen,
                    HealthRecordScreen.RecordType.BMI
                )
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
                if(newGoal > 0 && newGoal != goal){
                    SpUtils.putInt(KEY_STEP_COUNT_GOLE,newGoal)
                }
                loadInterstitial(AdPosition.IV_STEP_GOAL_SAVE){
                    finish()
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

    override fun getBackAdPosition() = AdPosition.IV_STEP_SETTING_BACK
}