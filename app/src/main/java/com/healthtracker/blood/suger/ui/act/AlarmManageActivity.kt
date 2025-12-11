package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.databinding.ActivityAlarmManagerBinding
import com.healthtracker.blood.suger.ui.adapter.AlarmAdapter
import com.healthtracker.blood.suger.ui.dialog.AlarmTimeSelectDialog
import com.healthtracker.blood.suger.ui.viewmodel.AlarmViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmManageActivity : BaseMVVMActivity<AlarmViewModel, ActivityAlarmManagerBinding>() {

    @Inject
    lateinit var permissionManager: PermissionManager

    // 血糖闹钟适配器
    private lateinit var bloodSugarAdapter: AlarmAdapter

    // 血压闹钟适配器
    private lateinit var bloodPressureAdapter: AlarmAdapter


    override fun createViewBinding() = ActivityAlarmManagerBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AlarmViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 检查通知权限
        checkNotificationPermission()
        setupActionBar()
        setupRecyclerViews()
        setupClickListeners()
        observeData()
    }

    /**
     * 设置ActionBar
     */
    private fun setupActionBar() {
        mViewBind.btnBack.setOnClickListener {
            finish()
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerViews() {
        // 初始化血糖闹钟适配器
        bloodSugarAdapter = AlarmAdapter { alarm, isEnabled ->
            mViewModel.updateAlarmEnabled(alarm.id, isEnabled, AlarmRecord.TYPE_BLOOD_SUGAR)
        }

        // 初始化血压闹钟适配器
        bloodPressureAdapter = AlarmAdapter { alarm, isEnabled ->
            mViewModel.updateAlarmEnabled(alarm.id, isEnabled, AlarmRecord.TYPE_BLOOD_PRESSURE)
        }

        // 设置血糖闹钟RecyclerView
        mViewBind.rvBsAlarm.apply {
            layoutManager = LinearLayoutManager(this@AlarmManageActivity)
            adapter = bloodSugarAdapter
        }

        // 设置血压闹钟RecyclerView
        mViewBind.rvBpAlarm.apply {
            layoutManager = LinearLayoutManager(this@AlarmManageActivity)
            adapter = bloodPressureAdapter
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        // 血糖闹钟添加按钮
        mViewBind.ivAddBsAlarm.clickWithDuration {

            AlarmTimeSelectDialog.show(supportFragmentManager) {
                mViewModel.addBloodSugarAlarm(it.first, it.second)

            }
        }

        // 血压闹钟添加按钮
        mViewBind.ivAddBpAlarm.clickWithDuration {
            AlarmTimeSelectDialog.show(supportFragmentManager) {
                mViewModel.addBloodPressureAlarm(it.first, it.second)
            }
        }
    }

    /**
     * 观察数据变化
     */
    private fun observeData() {
        // 观察血糖闹钟数据
        this.collectLatest(mViewModel.bloodSugarAlarms) { alarms ->
            bloodSugarAdapter.submitList(alarms)
        }

        // 观察血压闹钟数据
        this.collectLatest(mViewModel.bloodPressureAlarms) { alarms ->
            bloodPressureAdapter.submitList(alarms)
        }
    }
    
    /**
     * 检查通知权限
     */
    private fun checkNotificationPermission() {
        var isTurnOn = false
        permissionManager.checkNotificationPermission(this, onGoSetting = { isTurnOn = true}){
            if(!it){
                finish()
            }
        }



    }



}