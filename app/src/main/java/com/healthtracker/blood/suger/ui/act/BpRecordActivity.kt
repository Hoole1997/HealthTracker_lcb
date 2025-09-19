package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivityBpRecordBinding
import com.healthtracker.blood.suger.ui.viewmodel.BpRecordViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.util.FontUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BpRecordActivity: BaseMVVMActivity<BpRecordViewModel, ActivityBpRecordBinding>() {
    companion object{
        private const val TAG = "BpRecordActivity"
    }
    override fun createViewBinding() = ActivityBpRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BpRecordViewModel::class.java

//    private val sysArrays = resources.getStringArray(R.array.systolic).toList()
//    private val pulseArrays = resources.getStringArray(R.array.pulse).toList()

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind){
            btnBack.click {
                finish()
            }
            val tfRegular = FontUtils.getInstance().robotoRegular
            val tfBold = FontUtils.getInstance().robotoBold
            npvDiastolic.setContentSelectedTextTypeface(tfBold)
            npvSystolic.setContentSelectedTextTypeface(tfBold)
            npvPulse.setContentSelectedTextTypeface(tfBold)
            npvDiastolic.setContentNormalTextTypeface(tfRegular)
            npvSystolic.setContentNormalTextTypeface(tfRegular)
            npvPulse.setContentNormalTextTypeface(tfRegular)
            npvDiastolic.minValue = 20
            npvSystolic.minValue = 20
            npvSystolic.maxValue = 300
            npvDiastolic.maxValue = 300
            npvPulse.minValue = 40
            npvPulse.maxValue = 220
            npvSystolic.setOnValueChangedListener { _, _, _ ->
                val systolic = npvSystolic.contentByCurrValue.toInt()
                mViewModel.updateSystolicPressure(systolic)
            }
            npvDiastolic.setOnValueChangedListener { _, _, _ ->
                val diastolic = npvDiastolic.contentByCurrValue.toInt()
                mViewModel.updateDiastolicPressure(diastolic)
            }
            npvPulse.setOnValueChangedListener { _, _, _ ->
                mViewModel.updatePulseRate(npvPulse.contentByCurrValue.toInt())
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        mViewModel.systolicPressure.collectLifecycle {
            with(mViewBind){
                bpStatusView.updateSystolic(it)
                if(it == npvSystolic.contentByCurrValue.toInt()){
                    return@collectLifecycle
                }
                //将当前值的位置设置给滚动控件
                npvSystolic.value = it

            }
        }

        mViewModel.diastolicPressure.collectLifecycle {
            with(mViewBind){
                bpStatusView.updateDiastolic(it)
                if(it == npvDiastolic.contentByCurrValue.toInt()){
                    return@collectLifecycle
                }
                //将当前值的位置设置给滚动控件
                npvDiastolic.value = it

            }
        }

        mViewModel.pulseRate.collectLifecycle {
            with(mViewBind){
                if(it == npvPulse.contentByCurrValue.toInt()){
                    return@collectLifecycle
                }
                //将当前值的位置设置给滚动控件
                npvPulse.value = it
            }
        }
    }


}