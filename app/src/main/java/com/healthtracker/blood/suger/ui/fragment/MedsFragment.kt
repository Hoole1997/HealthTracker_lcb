package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.FragmentMedsBinding
import com.healthtracker.blood.suger.ui.act.AddReminderActivity
import com.healthtracker.blood.suger.ui.adapter.MedsReminderAdapter
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.blood.suger.ui.model.MedsReminderItem
import com.healthtracker.blood.suger.ui.viewmodel.MedsViewModel
import com.healthtracker.blood.suger.ui.widget.MedsRemindDropdownMenu
import com.healthtracker.blood.suger.ui.widget.MenuAction
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import net.corekit.core.report.ReportDataManager
import java.util.Date

@AndroidEntryPoint
class MedsFragment: BaseMVVMFragment<MedsViewModel, FragmentMedsBinding>() {


    private lateinit var reminderAdapter: MedsReminderAdapter

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentMedsBinding.inflate(layoutInflater,parent,attachToParent)

    companion object{
        private const val TAG = "MedsFragment"
    }

    override fun getVMModelClass() = MedsViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupRecyclerView()
        setupClickListeners()
        setupWeeklyDateSelector()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        if (::reminderAdapter.isInitialized) {
            reminderAdapter.setFragmentVisible(true)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::reminderAdapter.isInitialized) {
            reminderAdapter.setFragmentVisible(false)
        }
    }


    private fun setupRecyclerView() {
        reminderAdapter = MedsReminderAdapter(requireActivity(),
            onItemClick = { view,item ->
                handleReminderItemClick(view,item)
            },
        )

        mViewBind?.rvRemind?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = reminderAdapter
        }
    }

    private fun setupClickListeners() {
        mViewBind?.btnAdd?.clickWithDuration {
            ReportDataManager.reportData("med_click_add",mapOf())
            requireActivity().startActivity<AddReminderActivity>()
        }
    }

    /**
     * 设置周视图日期选择器
     */
    private fun setupWeeklyDateSelector() {
        mViewBind?.run {
            // WeeklyDateSelector 在初始化时已经默认选中当前日期，无需重复设置
            // weeklyDateSelector.setDefaultSelectedDate() // 已优化：跳过重复的默认日期设置
            
            // 设置日期选择监听器
             weeklyDateSelector.setOnDateSelectedListener { selectedDate ->
                 "Date selection listener triggered: ${DateTimeUtils.formatDate(selectedDate)}".logd(TAG)
                 // 将选中的日期传递给ViewModel处理
                 mViewModel.onDateSelected(selectedDate)
             }
             
             // 设置周切换监听器
             weeklyDateSelector.setOnWeekChangedListener { isCurrentWeek ->
                 "Week change listener triggered: isCurrentWeek=$isCurrentWeek".logd(TAG)
                 // 将周切换状态传递给ViewModel处理
                 mViewModel.onWeekChanged(isCurrentWeek)
             }
        }
    }

    /**
     * 观察ViewModel的数据变化
     * 使用新的Flow扩展函数，代码更简洁且自动处理生命周期
     */
    private fun observeViewModel() {
        // 观察选中日期的变化
        collectLatest(mViewModel.selectedDate) { date ->
            onDateChanged(date)
        }

        // 观察周状态的变化
        collectLatest(mViewModel.isCurrentWeek) { isCurrentWeek ->
            onWeekStatusChanged(isCurrentWeek)
        }

        // 观察格式化月份的变化
        collectLatest(mViewModel.formattedMonth) { date ->
            onFormattedMonthChanged(DateTimeUtils.formatMonthYear(date))
        }

        // 观察药物提醒列表数据变化 - 使用collectLatest确保只处理最新的列表状态
        collectLatest(mViewModel.reminderItems) { reminderItems ->
            updateReminderList(reminderItems)
        }

        // 观察是否可以添加提醒的状态变化
        collectLatest(mViewModel.canAddReminder) { canAdd ->
            updateAddButtonState(canAdd)
        }

    }

    /**
     * 处理日期变化
     * @param selectedDate 选中的日期
     */
    private fun onDateChanged(selectedDate: java.util.Date) {
         // TODO: 根据选中日期更新UI，比如加载该日期的药物数据
         "Handle date change: ${DateTimeUtils.formatDate(selectedDate)}".logd(TAG)
     }

     /**
      * 处理周状态变化
      * @param isCurrentWeek 是否为当前周
      */
     private fun onWeekStatusChanged(isCurrentWeek: Boolean) {
         // TODO: 根据周状态更新UI，比如显示不同的提示信息
         "Handle week status change: ${if (isCurrentWeek) "current week" else "other week"}".logd(TAG)
     }

     /**
      * 处理格式化月份变化
      * @param formattedMonth 格式化的月份字符串
      */
     private fun onFormattedMonthChanged(formattedMonth: String) {
         // 处理格式化月份变化的逻辑
         "Formatted month updated: $formattedMonth".logd(TAG)
         // 可以在这里更新UI显示月份信息
     }

     /**
      * 更新提醒列表显示
      * @param reminderItems 提醒项列表
      */
     private fun updateReminderList(reminderItems: List<MedsReminderItem>) {
         reminderAdapter.submitList(reminderItems)

         // 根据数据是否为空显示/隐藏空状态视图
         mViewBind?.run {
             if (reminderItems.isEmpty()) {
                 tvEmpty.visibility = View.VISIBLE
                 rvRemind.visibility = View.GONE
             } else {
                 tvEmpty.visibility = View.GONE
                 rvRemind.visibility = View.VISIBLE
             }
         }
     }

     /**
      * 处理提醒项点击事件
      * @param item 被点击的提醒项
      */
     private fun handleReminderItemClick(view:View,item: MedsReminderItem) {
//         "点击提醒项: ${item.medicineName} ${item.time}".logd(TAG)
//
//         // 如果尚未服药，可以标记为已服药
//         if (item.status == com.healthtracker.blood.suger.ui.model.ReminderStatus.PENDING) {
//             mViewModel.markMedicationTaken(item.reminderId, item.reminderDateTime)
//             "标记服药: ${item.medicineName} ${item.time}".logd(TAG)
//         }
         MedsRemindDropdownMenu(view.context){
             when(it){
                 MenuAction.TAKE_NOW -> {
                     if(item.isTaken()){
                         return@MedsRemindDropdownMenu
                     }
                     mViewModel.markMedicationTaken(item.reminderId,item.reminderDateTime)
                 }
                 MenuAction.EDIT -> {
                     // 跳转到编辑页面，传递提醒ID
                     "Navigate to edit medication reminder: ID=${item.reminderId}".logd(TAG)
                     AddReminderActivity.start(requireContext(), item.reminderId)
                 }
                 MenuAction.DELETE -> {
                     // 删除服药提醒，任意选都是删除当前整个服药提醒，而不是针对某次
                     "Prepare to delete medication reminder: ID=${item.reminderId}, Medicine=${item.medicineName}".logd(TAG)
                     showDeleteConfirmDialog(item)
                 }
             }
         }.apply {
             isFocusable = true
             isOutsideTouchable = true

             setBackgroundDrawable(android.graphics.Color.TRANSPARENT.toDrawable())
//             showAsDropDown(view)
             showAsDropDown(view,-180,0)
         }
     }


     /**
      * 更新添加按钮的状态
      * @param canAdd 是否可以添加提醒
      */
     private fun updateAddButtonState(canAdd: Boolean) {
         mViewBind?.btnAdd?.apply {
             // 使用动画平滑过渡按钮状态
             if(canAdd) visible() else gone()

         }
     }

     /**
      * 显示删除确认对话框
      * @param item 要删除的提醒项
      */
     private fun showDeleteConfirmDialog(item: MedsReminderItem) {
         ConfirmDialog(
             title = getString(R.string.tips),
             message = getString(R.string.delete_tips_content),
             leftText = getString(R.string.cancel),
             rightText = getString(R.string.confirm),
             onDialogListener = object : DialogListener {
                 override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                     super.onItemClick(dialogFragment, which)
                     if (which == R.id.btn_ok) {
                         mViewModel.deleteMedicineReminder(item.reminderId)
                     }
                 }
             }
         ).show(childFragmentManager)
     }

    
    /**
     * 获取格式化的月份字符串
     * 供MainActivity调用以显示月份信息
     * 统一使用ViewModel中的方法进行格式化
     * @return 格式化的月份字符串，如"Sep.2025"
     */
    /**
     * 获取格式化的月份Flow
     * @return StateFlow<String> 格式化的月份字符串Flow
     */
    fun getDateFlow(): StateFlow<Date> {
        return mViewModel.formattedMonth
    }

    fun needLoadAd(){
        if(::reminderAdapter.isInitialized && reminderAdapter.itemCount > 1 && !reminderAdapter.needLoadAd){
            reminderAdapter.needLoadAd = true
            reminderAdapter.notifyItemChanged(1)
        }
    }



}
