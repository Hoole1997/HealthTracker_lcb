package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.FragmentMedsBinding
import com.healthtracker.blood.suger.ui.viewmodel.MedsViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MedsFragment: BaseMVVMFragment<MedsViewModel, FragmentMedsBinding>() {
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentMedsBinding.inflate(layoutInflater,parent,attachToParent)

    override fun getVMModelClass() = MedsViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // Fragment初始化逻辑
        setupWeeklyDateSelector()
        observeViewModel()
    }

    /**
     * 设置周视图日期选择器
     */
    private fun setupWeeklyDateSelector() {
        mViewBind?.run {
            // 设置默认选中日期
            weeklyDateSelector.setDefaultSelectedDate(DateTimeUtils.now())
            
            // 设置日期选择监听器
             weeklyDateSelector.setOnDateSelectedListener { selectedDate ->
                 "日期选择监听器触发: ${DateTimeUtils.formatDate(selectedDate)}".logd(TAG)
                 // 将选中的日期传递给ViewModel处理
                 mViewModel?.onDateSelected(selectedDate)
             }
             
             // 设置周切换监听器
             weeklyDateSelector.setOnWeekChangedListener { isCurrentWeek ->
                 "周切换监听器触发: 是否当前周=$isCurrentWeek".logd(TAG)
                 // 将周切换状态传递给ViewModel处理
                 mViewModel?.onWeekChanged(isCurrentWeek)
             }
        }
    }

    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        mViewModel?.let { viewModel ->
            // 观察选中日期的变化
             lifecycleScope.launch {
                 viewModel.selectedDate.collect { selectedDate ->
                     "ViewModel中选中日期更新: ${DateTimeUtils.formatDate(selectedDate)}".logd(TAG)
                     // 这里可以根据选中日期更新UI或执行其他操作
                     onDateChanged(selectedDate)
                 }
             }
             
             // 观察当前周状态的变化
             lifecycleScope.launch {
                 viewModel.isCurrentWeek.collect { isCurrentWeek ->
                     "ViewModel中当前周状态更新: $isCurrentWeek".logd(TAG)
                     // 这里可以根据周状态更新UI或执行其他操作
                     onWeekStatusChanged(isCurrentWeek)
                 }
             }
             
             // 观察格式化月份的变化
             lifecycleScope.launch {
                 viewModel.formattedMonth.collect { formattedMonth ->
                     "ViewModel中格式化月份更新: $formattedMonth".logd(TAG)
                     // 这里可以将格式化月份显示在UI上
                     onFormattedMonthChanged(formattedMonth)
                 }
             }
        }
    }

    /**
     * 处理日期变化
     * @param selectedDate 选中的日期
     */
    private fun onDateChanged(selectedDate: java.util.Date) {
         // TODO: 根据选中日期更新UI，比如加载该日期的药物数据
         "处理日期变化: ${DateTimeUtils.formatDate(selectedDate)}".logd(TAG)
     }

     /**
      * 处理周状态变化
      * @param isCurrentWeek 是否为当前周
      */
     private fun onWeekStatusChanged(isCurrentWeek: Boolean) {
         // TODO: 根据周状态更新UI，比如显示不同的提示信息
         "处理周状态变化: ${if (isCurrentWeek) "当前周" else "其他周"}".logd(TAG)
     }

     /**
      * 处理格式化月份变化
      * @param formattedMonth 格式化的月份字符串
      */
     private fun onFormattedMonthChanged(formattedMonth: String) {
         // 处理格式化月份变化的逻辑
         "格式化月份已更新: $formattedMonth".logd(TAG)
         // 可以在这里更新UI显示月份信息
     }

    
    /**
     * 获取格式化的月份字符串
     * 供MainActivity调用以显示月份信息
     * 统一使用ViewModel中的方法进行格式化
     * @return 格式化的月份字符串，如"Sep.2025"
     */
    /**
     * 获取格式化的月份Flow
     * @return 格式化月份的StateFlow，如"Sep.2025"
     */
    fun getFormattedMonthFlow(): StateFlow<String> {
        return mViewModel?.formattedMonth ?: flowOf(DateTimeUtils.getCurrentMonthYear()).stateIn(
            scope = lifecycleScope,
            started = SharingStarted.Lazily,
            initialValue = DateTimeUtils.getCurrentMonthYear()
        )
    }
}