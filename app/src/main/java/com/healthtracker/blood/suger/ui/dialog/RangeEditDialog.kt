package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BloodSugarRanges
import com.healthtracker.blood.suger.data.enums.BloodSugarStatus
import com.healthtracker.blood.suger.data.enums.BsUnit
import com.healthtracker.blood.suger.data.enums.getStatusStringRes
import com.healthtracker.blood.suger.databinding.DialogRangeEditBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.showToast

/**
 * 血糖目标范围编辑对话框
 */
class RangeEditDialog : BaseBottomSheetDialogFragment<DialogRangeEditBinding>() {

    private lateinit var bloodSugarStatus: BloodSugarStatus
    private var currentUnit: BsUnit = BsUnit.MG_DL
    private var onSaveCallback: ((BloodSugarStatus, BsUnit, BloodSugarRanges) -> Unit)? = null

    // 记录初始单位和初始范围值，用于检测是否有修改
    private var initialUnit: BsUnit = BsUnit.MG_DL
    private var initialRanges: BloodSugarRanges? = null

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogRangeEditBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        // 获取传入参数
        arguments?.let { args ->
            val statusType = args.getInt(ARG_STATUS_TYPE)
            bloodSugarStatus = BloodSugarStatus.entries.first { it.statusType == statusType }
            currentUnit = BsUnit.entries[args.getInt(ARG_UNIT)]
        } ?: run {
            dismissAllowingStateLoss()
            return
        }

        mViewBind?.apply {
            // 设置标题
            tvStatusTitle.text = getString(getStatusStringRes(bloodSugarStatus.statusType))

            // 初始化单位切换器
            setupUnitSwitcher()

            // 加载初始范围值
            loadRangeValues(currentUnit)

            // 设置输入联动
            setupInputLinkage()

            // 初始化时检查是否需要显示Reset按钮
            updateResetButtonVisibility()

            // 重置按钮
            tvReset.click {
                resetToDefaults()
            }

            // 取消按钮
            btnCancel.click {
                dismissAllowingStateLoss()
            }

            // 保存按钮
            btnSave.clickWithDuration {
                saveRanges()
            }
        }
    }

    /**
     * 设置单位切换器
     */
    private fun setupUnitSwitcher() {
        mViewBind?.apply {
            // 设置初始选中状态
            if (currentUnit == BsUnit.MG_DL) {
                rbMgdl.isChecked = true
            } else {
                rbMmol.isChecked = true
            }

            // 单位切换监听
            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                val newUnit = when (checkedId) {
                    rbMgdl.id -> BsUnit.MG_DL
                    rbMmol.id -> BsUnit.MMOL_L
                    else -> return@setOnCheckedChangeListener
                }

                if (newUnit != currentUnit) {
                    // 获取当前输入的值
                    val currentRanges = getCurrentInputRanges() ?: return@setOnCheckedChangeListener

                    // 转换单位
                    val convertedRanges = BsUnit.convertRanges(currentRanges, currentUnit, newUnit)
                    currentUnit = newUnit

                    // 显示转换后的值
                    displayRanges(convertedRanges)
                }
            }
        }
    }

    /**
     * 加载范围值
     */
    private fun loadRangeValues(unit: BsUnit) {
        val ranges = bloodSugarStatus.getRangesForUnit(unit)
        // 记录初始值，用于检测是否有修改
        initialUnit = unit
        initialRanges = ranges
        displayRanges(ranges)
    }

    /**
     * 显示范围值
     */
    private fun displayRanges(ranges: BloodSugarRanges) {
        mViewBind?.apply {
            etLowValueMax.setText(BsUnit.formatValue(ranges.lowHigh, currentUnit))
            etNormalValueMax.setText(BsUnit.formatValue(ranges.normalHigh, currentUnit))
            etDisValueMin.setText(BsUnit.formatValue(ranges.diabetesLow, currentUnit))

            // TextView会通过输入联动自动更新:
            // tvNormalValueMin = etLowValueMax (lowHigh)
            // tvPreValueMin = etNormalValueMax (normalHigh)
            // tvPreValueMax = etDisValueMin (diabetesLow)
        }
    }

    /**
     * 设置输入联动
     * 3个EditText的变化会自动更新对应的3个TextView
     */
    private fun setupInputLinkage() {
        mViewBind?.apply {
            // Low的max → Normal的min
            etLowValueMax.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    tvNormalValueMin.text = s.toString()
                    updateResetButtonVisibility()
                }
            })

            // Normal的max → Prediabetes的min
            etNormalValueMax.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    tvPreValueMin.text = s.toString()
                    updateResetButtonVisibility()
                }
            })

            // Diabetes的min → Prediabetes的max
            etDisValueMin.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    tvPreValueMax.text = s.toString()
                    updateResetButtonVisibility()
                }
            })
        }
    }

    /**
     * 更新重置按钮可见性
     */
    private fun updateResetButtonVisibility() {
        mViewBind?.apply {
            val currentRanges = getCurrentInputRanges() ?: return
            val defaultRanges = getDefaultRanges()

            val isDifferent = !rangesAreEqual(currentRanges, defaultRanges)
            tvReset.isVisible = isDifferent
        }
    }

    /**
     * 获取当前输入的范围值
     * 返回完整的7字段BloodSugarRanges
     */
    private fun getCurrentInputRanges(): BloodSugarRanges? {
        mViewBind?.apply {
            val lowHigh = etLowValueMax.text.toString().toFloatOrNull() ?: return null
            val normalHigh = etNormalValueMax.text.toString().toFloatOrNull() ?: return null
            val diabetesLow = etDisValueMin.text.toString().toFloatOrNull() ?: return null

            // 构建完整的BloodSugarRanges对象
            // low = 0 (固定值)
            // normalLow = lowHigh (Normal的下限等于Low的上限)
            // prediabetesLow = normalHigh (Prediabetes的下限等于Normal的上限)
            // prediabetesHigh = diabetesLow (Prediabetes的上限等于Diabetes的下限)
            return BloodSugarRanges(
                low = 0f,
                lowHigh = lowHigh,
                normalLow = lowHigh,
                normalHigh = normalHigh,
                prediabetesLow = normalHigh,
                prediabetesHigh = diabetesLow,
                diabetesLow = diabetesLow
            )
        }
        return null
    }

    /**
     * 获取默认范围值
     */
    private fun getDefaultRanges(): BloodSugarRanges {
        return bloodSugarStatus.getDefaultRanges(currentUnit)
    }

    /**
     * 重置到默认值
     */
    private fun resetToDefaults() {
        val defaultRanges = getDefaultRanges()
        displayRanges(defaultRanges)
        updateResetButtonVisibility()
    }

    /**
     * 检查是否有编辑过
     * 包括单位变化和数值变化
     */
    private fun hasChanges(): Boolean {
        val currentRanges = getCurrentInputRanges() ?: return false
        val initial = initialRanges ?: return false

        // 检查单位是否变化
        if (currentUnit != initialUnit) return true

        // 检查值是否变化（需要考虑浮点数精度）
        return !rangesAreEqual(currentRanges, initial)
    }

    /**
     * 比较两个范围是否相等（考虑浮点数精度）
     */
    private fun rangesAreEqual(r1: BloodSugarRanges, r2: BloodSugarRanges): Boolean {
        val epsilon = 0.01f  // 精度阈值
        return kotlin.math.abs(r1.low - r2.low) < epsilon &&
                kotlin.math.abs(r1.lowHigh - r2.lowHigh) < epsilon &&
                kotlin.math.abs(r1.normalLow - r2.normalLow) < epsilon &&
                kotlin.math.abs(r1.normalHigh - r2.normalHigh) < epsilon &&
                kotlin.math.abs(r1.prediabetesLow - r2.prediabetesLow) < epsilon &&
                kotlin.math.abs(r1.prediabetesHigh - r2.prediabetesHigh) < epsilon &&
                kotlin.math.abs(r1.diabetesLow - r2.diabetesLow) < epsilon
    }

    /**
     * 保存范围值
     */
    private fun saveRanges() {
        // 0. 检查是否有修改
        if (!hasChanges()) {
            // 没有修改，直接关闭对话框
            dismissAllowingStateLoss()
            return
        }

        // 1. 获取当前输入
        val ranges = getCurrentInputRanges()
        if (ranges == null) {
            showToast(getString(R.string.please_input_complete_data))
            return
        }

        // 2. 验证范围顺序
        if (!validateRanges(ranges)) {
            showToast(getString(R.string.range_order_error))
            return
        }

        // 3. 验证范围边界
        if (!validateBounds(ranges)) {
            showToast(getString(R.string.range_bounds_error))
            return
        }

        // 4. 回调保存
        onSaveCallback?.invoke(bloodSugarStatus, currentUnit, ranges)
        dismissAllowingStateLoss()
    }

    /**
     * 验证范围顺序
     */
    private fun validateRanges(ranges: BloodSugarRanges): Boolean {
        return ranges.lowHigh < ranges.normalHigh && ranges.normalHigh < ranges.diabetesLow
    }

    /**
     * 验证范围边界
     */
    private fun validateBounds(ranges: BloodSugarRanges): Boolean {
        return if (currentUnit == BsUnit.MG_DL) {
            ranges.lowHigh >= 10 && ranges.diabetesLow <= 600
        } else {
            ranges.lowHigh >= 0.6f && ranges.diabetesLow <= 33.3f
        }
    }

    companion object {
        private const val ARG_STATUS_TYPE = "status_type"
        private const val ARG_UNIT = "unit"

        /**
         * 创建对话框实例
         */
        fun newInstance(
            status: BloodSugarStatus,
            unit: BsUnit,
            onSave: (BloodSugarStatus, BsUnit, BloodSugarRanges) -> Unit
        ): RangeEditDialog {
            return RangeEditDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_STATUS_TYPE, status.statusType)
                    putInt(ARG_UNIT, unit.ordinal)
                }
                onSaveCallback = onSave
            }
        }
    }
}