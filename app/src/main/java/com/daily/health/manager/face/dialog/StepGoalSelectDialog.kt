package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.daily.health.manager.constants.KEY_STEP_COUNT_GOLE
import com.daily.health.manager.databinding.FcDialogSelectStepGoalBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.getRobotoBold

class StepGoalSelectDialog(private val callBack: ((Int) -> Unit)? = null) :
    BaseBottomSheetDialogFragment<FcDialogSelectStepGoalBinding>() {

    constructor() : this(null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FcDialogSelectStepGoalBinding.inflate(layoutInflater)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            btnSave.click {
                callBack?.invoke(numberPicker.contentByCurrValue.toInt())
                dismissAllowingStateLoss()
            }
            val font = getRobotoBold(view.context)
            numberPicker.setContentSelectedTextTypeface(font)
            numberPicker.displayedValues = Array(80) { i -> ((i + 1) * 500).toString() }
            numberPicker.minValue = 0
            numberPicker.maxValue = 79

            // ✅ 修复：增加安全检查，防止崩溃
            val currentGoal = SpUtils.getInt(KEY_STEP_COUNT_GOLE, 6000).toString()
            var goalIndex = numberPicker.displayedValues.indexOf(currentGoal)

            // 如果找不到已保存的值，则回退到默认值6000的索引
            if (goalIndex == -1) {
                goalIndex = numberPicker.displayedValues.indexOf("6000")
                // 如果连默认值都找不到（几乎不可能），则使用第一个索引作为最终回退
                if (goalIndex == -1) {
                    goalIndex = 0
                }
            }
            numberPicker.value = goalIndex
        }
    }
}