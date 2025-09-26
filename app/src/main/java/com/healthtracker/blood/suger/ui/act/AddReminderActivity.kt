package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityAddReminderBinding
import com.healthtracker.blood.suger.ui.adapter.ReminderTimeAdapter
import com.healthtracker.blood.suger.ui.dialog.AlarmTimeSelectDialog
import com.healthtracker.blood.suger.ui.dialog.DosesTimesDialog
import com.healthtracker.blood.suger.ui.viewmodel.AddReminderUiState
import com.healthtracker.blood.suger.ui.viewmodel.AddReminderViewModel
import com.healthtracker.blood.suger.ui.viewmodel.SaveState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class AddReminderActivity: BaseMVVMActivity<AddReminderViewModel, ActivityAddReminderBinding>() {

    private lateinit var timeAdapter: ReminderTimeAdapter

    override fun createViewBinding() = ActivityAddReminderBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AddReminderViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 获取传入的参数
        val remindId = intent.getLongExtra("remindId", -1L).takeIf { it != -1L }
        val startDate = intent.getStringExtra("startDate")

        // 初始化ViewModel
        mViewModel.initPage(remindId, startDate)

        setupViews()
        setupRecyclerView()
        observeViewModel()
    }

    companion object {
        /**
         * 启动Activity的便利方法
         * @param context 上下文
         * @param remindId 提醒ID，null表示新建模式
         * @param startDate 新建模式的起始日期
         */
        @JvmStatic
        fun start(context: android.content.Context, remindId: Long? = null, startDate: String? = null) {
            val intent = android.content.Intent(context, AddReminderActivity::class.java).apply {
                remindId?.let { putExtra("remindId", it) }
                startDate?.let { putExtra("startDate", it) }
            }
            context.startActivity(intent)
        }
    }

    private fun setupViews() {
        with(mViewBind) {
            // 返回按钮
            btnBack.click {
                finish()
            }

            // 药物名称输入监听
            etMedicationName.addTextChangedListener { text ->
                mViewModel.setMedicineName(text?.toString() ?: "")
            }

            // 备注输入监听
            etNotes.addTextChangedListener { text ->
                mViewModel.setNotes(text?.toString() ?: "")
            }

            // 日历同步选择
            cbSyncCalendar.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.setSyncCalendar(isChecked)
            }

            // 每日服药次数点击
            tvDoseCount.click {
                showDoseCountDialog()
            }

            // 保存按钮
            btnSave.click {
                mViewModel.saveReminder()
            }
        }
    }

    private fun setupRecyclerView() {
        timeAdapter = ReminderTimeAdapter { position ->
            showTimePickerDialog(position)
        }

        with(mViewBind.rvDailyRemind) {
            layoutManager = GridLayoutManager(this@AddReminderActivity, 3)
            adapter = timeAdapter
        }
    }

    private fun observeViewModel() {
        mViewModel.uiState.collectLatestLifecycle { state ->
            updateUI(state)
        }

        mViewModel.saveState.collectLatestLifecycle { saveState ->
            handleSaveState(saveState)
        }
    }

    private fun updateUI(state: AddReminderUiState) {
        with(mViewBind) {
            // 更新表单内容
            if (etMedicationName.text.toString() != state.medicineName) {
                etMedicationName.setText(state.medicineName)
                etMedicationName.setSelection(state.medicineName.length)
            }

            if (etNotes.text.toString() != state.notes) {
                etNotes.setText(state.notes)
            }

            cbSyncCalendar.isChecked = state.syncCalendar

            // 更新每日服药次数显示
            tvDoseCount.text = state.dailyDoses.toString()

            // 更新时间列表
            timeAdapter.updateTimes(state.reminderTimes)

            // 更新保存按钮状态和文字
            btnSave.isEnabled = state.isFormValid
            btnSave.text = if (state.isEditMode) getString(R.string.save_changes) else getString(R.string.save)
        }
    }

    private fun handleSaveState(saveState: SaveState) {
        when (saveState) {
            is SaveState.Idle -> {
                mViewBind.btnSave.isEnabled = saveState.isAlbe
                mViewBind.btnSave.alpha = if(saveState.isAlbe) 1.0f else 0.3f
            }
            is SaveState.Loading -> {
                mViewBind.btnSave.isEnabled = false
                mViewBind.btnSave.text = getString(R.string.saving)
            }
            is SaveState.Success -> {
                finish()
            }
            is SaveState.Error -> {
                ToastUtils.showLong("Save failed")
            }
        }
    }

    private fun showDoseCountDialog() {
        DosesTimesDialog(mViewModel.uiState.value.dailyDoses){
            mViewModel.setDailyDoses(it)
        }.show(supportFragmentManager)
    }

    private fun showTimePickerDialog(position: Int) {
        val currentTime = mViewModel.uiState.value.reminderTimes[position]
        val timeParts = currentTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        AlarmTimeSelectDialog.show(supportFragmentManager,hour to minute){
            val timeString = String.format(Locale.ENGLISH,"%02d:%02d", it.first, it.second)
            mViewModel.updateReminderTime(position,timeString)

        }
    }
}