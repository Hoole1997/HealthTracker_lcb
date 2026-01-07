package com.daily.health.manager.face.act

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.daily.health.manager.alarm.PermissionManager
import com.daily.health.manager.data.entity.AlarmRecord
import com.daily.health.manager.databinding.HtActivityAlarmManagerBinding
import com.daily.health.manager.face.adapter.AlarmAdapter
import com.daily.health.manager.face.dialog.AlarmTimeSelectDialog
import com.daily.health.manager.face.viewmodel.AlarmViewModel
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject

class AlarmManageScreen : BaseMVVMActivity<AlarmViewModel, HtActivityAlarmManagerBinding>() {

    private val permissionManager: PermissionManager by inject()

    // 血糖闹钟适配器
    private lateinit var bloodSugarAdapter: AlarmAdapter

    // 血压闹钟适配器
    private lateinit var bloodPressureAdapter: AlarmAdapter


    override fun createViewBinding() = HtActivityAlarmManagerBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AlarmViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 检查通知权限
        checkNotificationPermission()
        setupActionBar()
        setupRecyclerViews()
        setupClickListeners()
        observeData()
        loadNative(mViewBind.adContainer, AdPosition.NA_ALARM_MANAGER_BOTTOM, style = NativeAdStyle.CARD_7)
    }

    /**
     * 设置ActionBar
     */
    private fun setupActionBar() {
        mViewBind.btnBack.click {
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
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
            adapter = bloodSugarAdapter
        }

        // 设置血压闹钟RecyclerView
        mViewBind.rvBpAlarm.apply {
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
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