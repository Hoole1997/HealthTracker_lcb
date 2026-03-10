package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.data.enums.BmiUnit
import com.daily.health.manager.databinding.TrDialogBmiPickerBinding
import com.daily.health.manager.face.weight.RulerView
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.daily.health.manager.util.BmiScaleHelper
import com.daily.health.manager.util.BmiScaleHelper.ScaleType
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * BMI 数值选择弹窗，复用通用标尺控件
 * 通过参数区分身高/体重，并支持单位切换与当前值回显
 */
class BmiPickerDialog : BaseBottomSheetDialogFragment<TrDialogBmiPickerBinding>() {

    private var style: Int = STYLE_WEIGHT
    private var initialValue: Float = 0f
    private var initialUnit: BmiUnit = BmiUnit.METRIC
    private var currentUnit: BmiUnit = BmiUnit.METRIC
    private var currentValue: Float = 0f
    private var currentScaleConfig: BmiUnit.ScaleConfig? = null
    private var isUpdatingUnitSelection = false

    private var onSave: ((Float, BmiUnit) -> Unit)? = null

    companion object {
        private const val ARG_STYLE = "arg_style"
        private const val ARG_INITIAL_VALUE = "arg_initial_value"
        private const val ARG_INITIAL_UNIT = "arg_initial_unit"

        private const val STYLE_WEIGHT = 0
        private const val STYLE_HEIGHT = 1

        fun showWeightPicker(
            fragmentManager: FragmentManager,
            initialDisplayValue: Float,
            unit: BmiUnit,
            onSave: (Float, BmiUnit) -> Unit
        ) {
            newInstance(STYLE_WEIGHT, initialDisplayValue, unit).apply {
                this.onSave = onSave
            }.show(fragmentManager)
        }

        fun showHeightPicker(
            fragmentManager: FragmentManager,
            initialDisplayValue: Float,
            unit: BmiUnit,
            onSave: (Float, BmiUnit) -> Unit
        ) {
            newInstance(STYLE_HEIGHT, initialDisplayValue, unit).apply {
                this.onSave = onSave
            }.show(fragmentManager)
        }

        private fun newInstance(
            style: Int,
            initialValue: Float,
            unit: BmiUnit
        ): BmiPickerDialog {
            return BmiPickerDialog().apply {
                arguments = Bundle().apply {
                    putInt(ARG_STYLE, style)
                    putFloat(ARG_INITIAL_VALUE, initialValue)
                    putInt(ARG_INITIAL_UNIT, unit.value)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            style = bundle.getInt(ARG_STYLE, STYLE_WEIGHT)
            initialValue = bundle.getFloat(ARG_INITIAL_VALUE, 0f)
            val unitValue = bundle.getInt(ARG_INITIAL_UNIT, BmiUnit.METRIC.value)
            initialUnit = BmiUnit.fromValue(unitValue)
        }
        currentUnit = initialUnit
        currentValue = initialValue
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): TrDialogBmiPickerBinding = TrDialogBmiPickerBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        setupTitle()
        setupUnitTabs()
        setupRuler()
        setupButtons()
    }

    private fun setupTitle() {
        mViewBind?.tvTitle?.text = getString(
            if (isWeightMode()) {
                R.string.tr_weight
            } else {
                R.string.tr_height
            }
        )
    }

    private fun setupUnitTabs() {
        mViewBind?.apply {
            //在健康/健身类 app 的单位切换控件里，行业主流都是用大写缩写（KG、LBS、CM、FT），
            // 一方面与医学和度量衡的常见写法一致，另一方面在按钮或 Pill 形式的切换控件里，
            // 大写字母识别度更高、视觉上也更均衡。因此保留大写通常更符合用户预期
            rb1.text = getUnitLabel(isWeightMode(), BmiUnit.IMPERIAL).uppercase()
            rb2.text = getUnitLabel(isWeightMode(), BmiUnit.METRIC).uppercase()
            setUnitSelection(currentUnit)

            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                if (isUpdatingUnitSelection) return@setOnCheckedChangeListener
                val newUnit = if (checkedId == R.id.rb_1) BmiUnit.IMPERIAL else BmiUnit.METRIC
                onUnitChanged(newUnit)
            }
        }
    }

    private fun setupRuler() {
        val ruler = mViewBind?.rulerView ?: return
        val config = BmiScaleHelper.configureRuler(ruler, currentUnit, currentScaleType())
        currentScaleConfig = config
        val clampedValue = BmiScaleHelper.clampValue(currentValue, config)
        currentValue = clampedValue
        applyScaleToRuler(ruler, clampedValue)
        ruler.setOnChooseResultListener(object : RulerView.OnChooseResultListener {
            override fun onEndResult(result: Float) {
                val valueStr = NumberFormatter.formatNumber(
                    result.toDouble(),
                    LanguageUtils.getAppLocale(App.INSTANCE), 1
                )
                updateCurrentValue(valueStr)
            }

            override fun onScrollResult(result: Float) {
                val valueStr = NumberFormatter.formatNumber(
                    result.toDouble(),
                    LanguageUtils.getAppLocale(App.INSTANCE), 1
                )
                updateCurrentValue(valueStr)
            }
        })
    }

    private fun setupButtons() {
        mViewBind?.apply {
            btnCancel.click { dismiss() }
            btnOk.click {
                onSave?.invoke(currentValue, currentUnit)
                dismiss()
            }
        }
    }

    private fun onUnitChanged(newUnit: BmiUnit) {
        if (currentUnit == newUnit) return
        val baseValue = if (isWeightMode()) {
            BmiUnit.toBaseWeightKg(currentValue, currentUnit)
        } else {
            BmiUnit.toBaseHeightCm(currentValue, currentUnit)
        }
        currentUnit = newUnit
        val convertedValue = if (isWeightMode()) {
            BmiUnit.toDisplayWeight(baseValue, currentUnit)
        } else {
            BmiUnit.toDisplayHeight(baseValue, currentUnit)
        }
        currentValue = convertedValue
        setUnitSelection(newUnit)
        mViewBind?.rulerView?.let { ruler ->
            val config = BmiScaleHelper.configureRuler(ruler, newUnit, currentScaleType())
            currentScaleConfig = config
            val clampedValue = BmiScaleHelper.clampValue(convertedValue, config)
            currentValue = clampedValue
            applyScaleToRuler(ruler, clampedValue)
        }
    }

    private fun updateCurrentValue(result: String) {
        val value = result.toFloatOrNull() ?: return
        currentValue = value
        updateSelectedValueDisplay(value)
    }

    private fun updateSelectedValueDisplay(value: Float) {
        val formatted = formatValue(value)
        mViewBind?.tvSelectValue?.text = formatted
    }

    private fun formatValue(value: Float): String {
        val config = currentScaleConfig
        val decimalPlaces = config?.decimalPlaces ?: 1
        return if (decimalPlaces <= 0) {
            value.roundToInt().toString()
        } else {
            String.format(Locale.ROOT, "%.${decimalPlaces}f", value)
        }
    }

    private fun setUnitSelection(unit: BmiUnit) {
        isUpdatingUnitSelection = true
        mViewBind?.rgUnit?.check(
            if (unit == BmiUnit.IMPERIAL) {
                R.id.rb_1
            } else {
                R.id.rb_2
            }
        )
        isUpdatingUnitSelection = false
    }

    private fun getUnitLabel(isWeight: Boolean, unit: BmiUnit): String {
        val resId = when {
            isWeight && unit == BmiUnit.METRIC -> R.string.tr_unit_kg
            isWeight && unit == BmiUnit.IMPERIAL -> R.string.tr_unit_lb
            !isWeight && unit == BmiUnit.METRIC -> R.string.tr_unit_cm
            else -> R.string.tr_unit_in
        }
        return getString(resId)
    }

    private fun isWeightMode(): Boolean = style == STYLE_WEIGHT

    private fun currentScaleType(): ScaleType {
        return if (isWeightMode()) {
            ScaleType.WEIGHT
        } else {
            ScaleType.HEIGHT
        }
    }

    private fun applyScaleToRuler(rulerView: RulerView, value: Float) {
        val action = {
            rulerView.setScaleImmediately(value, suppressCallback = true)
            updateSelectedValueDisplay(value)
        }
        if (rulerView.width == 0 || rulerView.height == 0) {
            rulerView.post(action)
        } else {
            action()
        }
    }

}
