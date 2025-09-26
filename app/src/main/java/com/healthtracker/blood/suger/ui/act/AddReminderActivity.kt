package com.healthtracker.blood.suger.ui.act

import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class AddReminderActivity: BaseMVVMActivity<AddReminderViewModel, ActivityAddReminderBinding>() {

    private lateinit var timeAdapter: ReminderTimeAdapter

    override fun createViewBinding() = ActivityAddReminderBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AddReminderViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupViews()
        setupRecyclerView()
        observeViewModel()
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
            // 更新每日服药次数显示
            tvDoseCount.text = state.dailyDoses.toString()

            // 更新时间列表
            timeAdapter.updateTimes(state.reminderTimes)

            // 更新保存按钮状态
            btnSave.isEnabled = state.isFormValid
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
        DosesTimesDialog{
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