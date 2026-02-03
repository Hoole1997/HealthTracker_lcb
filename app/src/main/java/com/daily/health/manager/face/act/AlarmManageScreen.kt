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

    // 心率闹钟适配器
    private lateinit var heartRateAdapter: AlarmAdapter
    
    // BMI闹钟适配器
    private lateinit var bmiAdapter: AlarmAdapter
    
    // 胆固醇闹钟适配器
    private lateinit var cholesterolAdapter: AlarmAdapter


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
     * 显示编辑弹窗
     */
    private fun showEditDialog(record: AlarmRecord?, type: Int) {
        val dialog = com.daily.health.manager.face.dialog.AlarmEditDialogFragment.newInstance(
            alarmRecord = record,
            alarmType = type,
            onSave = { h, m, f ->
                if (record == null) {
                    when (type) {
                        AlarmRecord.TYPE_BLOOD_SUGAR -> mViewModel.addBloodSugarAlarm(h, m, f)
                        AlarmRecord.TYPE_BLOOD_PRESSURE -> mViewModel.addBloodPressureAlarm(h, m, f)
                        AlarmRecord.TYPE_HEART_RATE -> mViewModel.addHeartRateAlarm(h, m, f)
                        AlarmRecord.TYPE_BMI -> mViewModel.addBmiAlarm(h, m, f)
                        AlarmRecord.TYPE_CHOLESTEROL -> mViewModel.addCholesterolAlarm(h, m, f)
                    }
                } else {
                    mViewModel.updateAlarm(record.id, h, m, f)
                }
            },
            onDelete = if (record != null) {
                { mViewModel.deleteAlarm(record.id) }
            } else null
        )
        dialog.show(supportFragmentManager, "AlarmEditDialog")
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerViews() {
        // 初始化适配器辅助函数
        fun createAdapter(type: Int): AlarmAdapter {
            return AlarmAdapter(
                onSwitchChanged = { alarm, isEnabled ->
                    mViewModel.updateAlarmEnabled(alarm.id, isEnabled, type)
                },
                onItemClick = { alarm ->
                    showEditDialog(alarm, type)
                }
            )
        }

        bloodSugarAdapter = createAdapter(AlarmRecord.TYPE_BLOOD_SUGAR)
        bloodPressureAdapter = createAdapter(AlarmRecord.TYPE_BLOOD_PRESSURE)
        heartRateAdapter = createAdapter(AlarmRecord.TYPE_HEART_RATE)
        bmiAdapter = createAdapter(AlarmRecord.TYPE_BMI)
        cholesterolAdapter = createAdapter(AlarmRecord.TYPE_CHOLESTEROL)

        // 设置RecyclerViews
        mViewBind.rvBsAlarm.run {
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
            adapter = bloodSugarAdapter
        }
        mViewBind.rvBpAlarm.run {
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
            adapter = bloodPressureAdapter
        }
        mViewBind.rvHeartRateAlarm.run {
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
            adapter = heartRateAdapter
        }
        mViewBind.rvBmiAlarm.run {
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
            adapter = bmiAdapter
        }
        mViewBind.rvCholesterolAlarm.run {
            layoutManager = LinearLayoutManager(this@AlarmManageScreen)
            adapter = cholesterolAdapter
        }
    }

    /**
     * 设置点击监听器
     */
    private fun setupClickListeners() {
        mViewBind.ivAddBsAlarm.clickWithDuration {
            showEditDialog(null, AlarmRecord.TYPE_BLOOD_SUGAR)
        }
        mViewBind.ivAddBpAlarm.clickWithDuration {
            showEditDialog(null, AlarmRecord.TYPE_BLOOD_PRESSURE)
        }
        mViewBind.ivAddHeartRateAlarm.clickWithDuration {
            showEditDialog(null, AlarmRecord.TYPE_HEART_RATE)
        }
        mViewBind.ivAddBmiAlarm.clickWithDuration {
            showEditDialog(null, AlarmRecord.TYPE_BMI)
        }
        mViewBind.ivAddCholesterolAlarm.clickWithDuration {
            showEditDialog(null, AlarmRecord.TYPE_CHOLESTEROL)
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
        
        // 观察心率闹钟数据
        this.collectLatest(mViewModel.heartRateAlarms) { alarms ->
            heartRateAdapter.submitList(alarms)
        }
        
        // 观察BMI闹钟数据
        this.collectLatest(mViewModel.bmiAlarms) { alarms ->
            bmiAdapter.submitList(alarms)
        }
        
        // 观察胆固醇闹钟数据
        this.collectLatest(mViewModel.cholesterolAlarms) { alarms ->
            cholesterolAdapter.submitList(alarms)
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