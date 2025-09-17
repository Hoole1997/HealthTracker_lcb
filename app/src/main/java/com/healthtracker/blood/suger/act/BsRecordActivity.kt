package com.healthtracker.blood.suger.act

import android.os.Bundle
import android.widget.RadioGroup
import com.healthtracker.blood.suger.databinding.ActivityBsRecordBinding
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

    override fun createViewBinding() = ActivityBsRecordBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind) {
            btnBack.click {
                finish()
            }

            setupRulerView()
            setupUnitSwitcher()
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
                        tvSelectValue.text = BloodSugarUnit.formatValue(currentValue, currentUnit)
                    } catch (e: NumberFormatException) {
                        // 处理转换异常
                    }
                }

                override fun onScrollResult(result: String) {
                    try {
                        val value = result.toFloat()
                        tvSelectValue.text = BloodSugarUnit.formatValue(value, currentUnit)
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
        mViewBind.tvSelectValue.text = BloodSugarUnit.formatValue(convertedValue, newUnit)
    }

    private fun configureRulerForUnit(unit: BloodSugarUnit) {
        BloodSugarScaleHelper.configureRulerForUnit(mViewBind.rulerView, unit)
    }
}