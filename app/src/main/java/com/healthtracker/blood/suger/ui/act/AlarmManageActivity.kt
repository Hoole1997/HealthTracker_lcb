package com.healthtracker.blood.suger.ui.act

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.alarm.AlarmPermissionResult
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.data.entity.AlarmRecord
import com.healthtracker.blood.suger.databinding.ActivityAlarmManagerBinding
import com.healthtracker.blood.suger.ui.adapter.AlarmAdapter
import com.healthtracker.blood.suger.ui.dialog.AlarmTimeSelectDialog
import com.healthtracker.blood.suger.ui.viewmodel.AlarmViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.TAG
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.logw
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AlarmManageActivity : BaseMVVMActivity<AlarmViewModel, ActivityAlarmManagerBinding>() {

    // 血糖闹钟适配器
    private lateinit var bloodSugarAdapter: AlarmAdapter

    // 血压闹钟适配器
    private lateinit var bloodPressureAdapter: AlarmAdapter
    
    @Inject
    lateinit var permissionManager: PermissionManager

    override fun createViewBinding() = ActivityAlarmManagerBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AlarmViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 首先检查权限
        checkAndRequestPermissions()
        
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
        mViewModel.bloodSugarAlarms.collectLatestLifecycle { alarms ->
            bloodSugarAdapter.submitList(alarms)
        }

        // 观察血压闹钟数据
        mViewModel.bloodPressureAlarms.collectLatestLifecycle { alarms ->
            bloodPressureAdapter.submitList(alarms)
        }
    }
    
    /**
     * 检查并申请必要权限
     */
    private fun checkAndRequestPermissions() {
        val permissionResult = permissionManager.checkAllPermissions()
        
        if (permissionResult.allGranted) {
            "All alarm permissions granted".logd(TAG)
        } else {
            "Some alarm permissions missing, requesting...".logw(TAG)
            handleMissingPermissions(permissionResult)
        }
    }
    
    /**
     * 处理缺失的权限
     */
    private fun handleMissingPermissions(permissionResult: AlarmPermissionResult) {
        val missingPermissions = permissionResult.getMissingPermissions()
        "Missing permissions: ${missingPermissions.joinToString(", ")}".logw(TAG)
        
        // 申请所有缺失的权限
        permissionManager.requestAllPermissions(this) { result ->
            if (result.allGranted) {
                "All permissions granted after request".logd(TAG)
            } else {
                "Some permissions still missing after request".logw(TAG)
                showPermissionDeniedMessage(result)
            }
        }
    }
    
    /**
     * 显示权限被拒绝的提示
     */
    private fun showPermissionDeniedMessage(permissionResult: AlarmPermissionResult) {
        val missingPermissions = permissionResult.getMissingPermissions()
        val message = "为了确保闹钟功能正常工作，请授予以下权限：\n${missingPermissions.joinToString("\n")}"
        
        // 这里可以显示一个对话框或Toast提示用户
        "Permission denied message: $message".logw(TAG)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        val handled = permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
        if (handled) {
            // 重新检查权限状态
            val permissionResult = permissionManager.checkAllPermissions()
            if (!permissionResult.allGranted) {
                showPermissionDeniedMessage(permissionResult)
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        val handled = permissionManager.handleActivityResult(requestCode, resultCode)
        if (handled) {
            // 重新检查权限状态
            val permissionResult = permissionManager.checkAllPermissions()
            if (!permissionResult.allGranted) {
                showPermissionDeniedMessage(permissionResult)
            }
        }
    }


}