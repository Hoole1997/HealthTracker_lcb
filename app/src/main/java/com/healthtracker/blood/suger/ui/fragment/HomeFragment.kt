package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.databinding.FragmentHomeBinding
import com.healthtracker.blood.suger.ui.viewmodel.HomeViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.launch

/**
 * 首页Fragment
 * 显示最近一次的血糖和血压记录
 */
class HomeFragment: BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {

   companion object{
       private const val TAG = "HomeFragment"
   }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentHomeBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = HomeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        observeData()
    }

    /**
     * 观察数据变化
     */
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察最新血糖记录
                launch {
                    mViewModel.latestBloodSugarRecord.collect { record ->
                        updateBloodSugarUI(record)
                    }
                }

                // 观察最新血压记录
                launch {
                    mViewModel.latestBloodPressureRecord.collect { record ->
                        updateBloodPressureUI(record)
                    }
                }
            }
        }
    }

    /**
     * 更新血糖记录UI
     */
    private fun updateBloodSugarUI(record: BloodSugarRecord?) {
        if (record == null) {
            // TODO: 显示无记录状态
            "没有血糖记录".logd(TAG)

            return
        }
        "latest blood suger = $record".logd(TAG)

        // TODO: 根据实际的ViewBinding属性更新UI
        // 显示血糖值: ${record.glucoseValue} mg/dL
        // 显示记录时间: ${dateFormat.format(record.recordTime)}
        // 显示测量标签: ${record.measurementTag}
    }

    /**
     * 更新血压记录UI
     */
    private fun updateBloodPressureUI(record: BloodPressureRecord?) {
        if (record == null) {
            // TODO: 显示无记录状态
            "没有血压记录".logd(TAG)
            return
        }
        "latest blood pressure = $record".logd(TAG)
        // TODO: 根据实际的ViewBinding属性更新UI
        // 显示血压值: ${record.systolicPressure}/${record.diastolicPressure} mmHg
        // 显示脉搏: ${record.pulseRate} bpm
        // 显示记录时间: ${dateFormat.format(record.recordTime)}
        // 显示测量标签: ${record.measurementTag}
    }
}