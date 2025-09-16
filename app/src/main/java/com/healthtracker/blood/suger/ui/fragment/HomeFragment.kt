package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.databinding.FragmentHomeBinding
import com.healthtracker.blood.suger.ui.viewmodel.HomeViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 首页Fragment
 * 显示最近一次的血糖和血压记录
 */
@AndroidEntryPoint
class HomeFragment: BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {

   companion object{
       private const val TAG = "HomeFragment"
   }

    private var hasLatestRecord = false

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentHomeBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = HomeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.run {
            clHistory.clickWithDuration {

            }

            btnRecordNow.clickWithDuration {

            }

            clBloodPressure.clickWithDuration {

            }


        }
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
            mViewBind?.tvLatestBsValue?.text = "--"
            mViewBind?.tvLatestRecordDate?.text = getString(R.string.click_to_record)
            return
        }
        hasLatestRecord = true
        // 根据用户选择的单位显示血糖值（保留一位小数）
        mViewBind?.tvLatestBsValue?.text = record.getFormattedDisplayValue()
    }

    /**
     * 更新血压记录UI
     */
    private fun updateBloodPressureUI(record: BloodPressureRecord?) {
        if (record == null) {
            mViewBind?.tvLatestBpValue?.text = "-/-"
            return
        }
        "${record.systolicPressure}/${record.diastolicPressure}".also {
            mViewBind?.tvLatestBpValue?.text = it
        }

    }
}