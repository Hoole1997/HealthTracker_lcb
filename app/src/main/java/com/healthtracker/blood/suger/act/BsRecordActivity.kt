package com.healthtracker.blood.suger.act

import android.os.Bundle
import android.widget.RadioGroup
import com.healthtracker.blood.suger.databinding.ActivityBsRecordBinding
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.BloodSugarUnit
import com.healthtracker.blood.suger.ui.weight.BloodSugarRulerView
import com.healthtracker.blood.suger.util.BloodSugarScaleHelper
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BsRecordActivity: BaseMVVMActivity<BaseViewModel, ActivityBsRecordBinding>() {

    private var currentUnit = BloodSugarUnit.MMOL_L
    private var currentValue = 4.2f
    private var currentStatus = BloodSugarStatus.DEFAULT

    override fun createViewBinding() = ActivityBsRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind) {
            btnBack.click {
                finish()
            }

            setupRulerView()
            setupUnitSwitcher()
            setupRangeView()
            setupStatusSelector()
        }
    }

    private fun setupRulerView() {
        with(mViewBind) {
            // 初始化为 mmol/L 模式
            configureRulerForUnit(BloodSugarUnit.MMOL_L)

            rulerView.setOnChooseResultListener(object : BloodSugarRulerView.OnChooseResultListener {
                override fun onEndResult(result: String) {
                    try {
                        currentValue = result.toFloat()
                        updateDisplayValues()
                        updateRangeView()
                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }

                override fun onScrollResult(result: String) {
                    try {
                        val value = result.toFloat()
                        tvSelectValue.text = BloodSugarUnit.formatValue(value, currentUnit)
                        // 实时更新范围显示
                        rangeView.updateValue(value)
                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }
            })
        }
    }

    private fun setupUnitSwitcher() {
        with(mViewBind) {
            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                val newUnit = when (checkedId) {
                    rbMgdl.id -> BloodSugarUnit.MG_DL
                    rbMmol.id -> BloodSugarUnit.MMOL_L
                    else -> return@setOnCheckedChangeListener
                }

                if (newUnit != currentUnit) {
                    switchToUnit(newUnit)
                }
            }
        }
    }

    private fun switchToUnit(newUnit: BloodSugarUnit) {
        // 转换当前值到新单位
        val convertedValue = BloodSugarUnit.convertValue(currentValue, currentUnit, newUnit)

        // 更新当前单位和值
        currentUnit = newUnit
        currentValue = convertedValue

        // 重新配置刻度尺
        configureRulerForUnit(newUnit)

        // 设置转换后的值到刻度尺
        mViewBind.rulerView.scrollToScale(convertedValue)

        // 更新显示的值
        updateDisplayValues()
        updateRangeView()
    }

    private fun configureRulerForUnit(unit: BloodSugarUnit) {
        BloodSugarScaleHelper.configureRulerForUnit(mViewBind.rulerView, unit)
    }

    private fun setupRangeView() {
        with(mViewBind) {
            // 初始化范围视图
            rangeView.setCurrentState(currentValue, currentUnit, currentStatus)
        }
    }

    private fun setupStatusSelector() {
        with(mViewBind) {
            // 设置状态选择点击事件
            clStatu.click {
                // TODO: 显示状态选择弹窗
                // 这里暂时模拟切换到不同状态进行测试
                val statuses = BloodSugarStatus.values()
                val currentIndex = statuses.indexOf(currentStatus)
                val nextIndex = (currentIndex + 1) % statuses.size
                val newStatus = statuses[nextIndex]

                switchToStatus(newStatus)
            }
        }
    }

    private fun switchToStatus(newStatus: BloodSugarStatus) {
        if (newStatus != currentStatus) {
            currentStatus = newStatus

            // 更新状态显示
            // TODO: 这里应该通过newStatus.statusType的Int值从string资源获取多语言文本
            // 暂时使用硬编码文本，后续需要改为 getString(getStatusStringRes(newStatus.statusType))
            mViewBind.tvStatus.text = getStatusDisplayText(newStatus.statusType)

            // 更新范围视图
            updateRangeView()
        }
    }

    private fun updateDisplayValues() {
        mViewBind.tvSelectValue.text = BloodSugarUnit.formatValue(currentValue, currentUnit)
    }

    private fun updateRangeView() {
        mViewBind.rangeView.setCurrentState(currentValue, currentUnit, currentStatus)
    }

    private fun getStatusDisplayText(statusType: Int): String {
        // TODO: 这里应该通过statusType的Int值从string资源获取多语言文本
        // 暂时使用硬编码文本，后续需要改为 getString(getStatusStringRes(statusType))
        return when (statusType) {
            0 -> "默认"
            1 -> "禁食"
            2 -> "吃饭前"
            3 -> "睡前"
            4 -> "运动后"
            5 -> "饭后1小时"
            6 -> "运动前"
            7 -> "饭后2小时"
            else -> "默认"
        }
    }
}