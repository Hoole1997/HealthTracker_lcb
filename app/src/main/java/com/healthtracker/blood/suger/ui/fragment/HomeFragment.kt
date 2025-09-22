package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.act.BsRecordActivity
import com.healthtracker.blood.suger.data.entity.BloodPressureRecord
import com.healthtracker.blood.suger.data.entity.BloodSugarRecord
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.databinding.FragmentHomeBinding
import com.healthtracker.blood.suger.ui.act.BpRecordActivity
import com.healthtracker.blood.suger.ui.act.HistoryRecordActivity
import com.healthtracker.blood.suger.ui.viewmodel.HomeViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*

/**
 * 首页Fragment
 * 显示最近一次的血糖和血压记录
 */
@AndroidEntryPoint
class HomeFragment: BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {

   companion object{
       private const val TAG = "HomeFragment"
   }

    private var latestSugerID :Long? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentHomeBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = HomeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.run {
            clHistory.clickWithDuration {
//                BsRecordActivity.start(requireActivity(), latestSugerID)
                requireActivity().startActivity<HistoryRecordActivity>()
            }

            btnRecordNow.clickWithDuration {
                BsRecordActivity.start(requireActivity())

            }

            clBloodPressure.clickWithDuration {
                requireActivity().startActivity<BpRecordActivity>()
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
        latestSugerID = record.id
        // 根据用户选择的单位显示血糖值（保留一位小数）
        mViewBind?.tvLatestBsValue?.text = record.getFormattedDisplayValue()
        mViewBind?.tvLatestBsUnit?.text = if(record.selectedUnit == BsUnit.MG_DL.value) BsUnit.MG_DL.displayName else BsUnit.MMOL_L.displayName
        // 显示相对时间
        mViewBind?.tvLatestRecordDate?.text = formatRelativeTime(record.recordTime)
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

    /**
     * 格式化相对时间显示
     * @param recordTime 记录时间
     * @return 格式化后的时间字符串
     */
    private fun formatRelativeTime(recordTime: Date): String {
        val currentTime = System.currentTimeMillis()
        val recordTimeMs = recordTime.time
        val timeDiff = currentTime - recordTimeMs

        // 如果记录时间在当前时间之后，显示Latest
        if (timeDiff < 0) {
            return getString(R.string.latest)
        }

        // 转换为秒
        val seconds = timeDiff / 1000
        return when {
            seconds < 60 -> {
                // 不满1分钟：x秒前
                getString(R.string.seconds_ago, seconds.toInt())
            }
            seconds < 3600 -> {
                // 不满1小时：x分钟前
                val minutes = seconds / 60
                getString(R.string.minutes_ago, minutes.toInt())
            }
            seconds < 86400 -> {
                // 不满1天：x小时前
                val hours = seconds / 3600
                getString(R.string.hours_ago, hours.toInt())
            }
            else -> {
                // 超过1天：x天前
                val days = seconds / 86400
                getString(R.string.days_ago, days.toInt())
            }
        }
    }
}