package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BMIEnum
import com.healthtracker.blood.suger.ui.dialog.BmiLeveDialog
import com.healthtracker.framework.ext.clickWithDuration

/**
 * BMI 状态视图，复用通用状态视图框架
 */
class BMIStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericStatusView<BMIEnum>(context, attrs, defStyleAttr) {

    // 等级文本映射
    private val levelTexts = mapOf(
        BMIEnum.VERY_SEVERELY_UNDERWEIGHT to R.string.bmi_level_very_severely_underweight,
        BMIEnum.SEVERELY_UNDERWEIGHT to R.string.bmi_level_severely_underweight,
        BMIEnum.UNDERWEIGHT to R.string.bmi_level_underweight,
        BMIEnum.NORMAL to R.string.bmi_level_normal,
        BMIEnum.OVERWEIGHT to R.string.bmi_level_overweight,
        BMIEnum.OBESITY_CLASS_I to R.string.bmi_level_obesity_class_1,
        BMIEnum.OBESITY_CLASS_II to R.string.bmi_level_obesity_class_2,
        BMIEnum.OBESITY_CLASS_III to R.string.bmi_level_obesity_class_3
    )

    private var bmiLevelBar: BMILevelBar? = null

    init {
        // 点击范围说明，弹出 BMI 等级说明弹窗
        binding.rangeText.clickWithDuration {
            (context as? FragmentActivity)?.let { activity ->
                BmiLeveDialog.show(activity.supportFragmentManager)
            }
        }
    }

    override fun createLevelBar(): View {
        bmiLevelBar = context.createBmiLevelBar()
        return bmiLevelBar!!
    }

    override fun getLevelTexts(): Map<BMIEnum, Int> = levelTexts

    override fun getStatusTextRes(category: BMIEnum): Int {
        return levelTexts[category] ?: R.string.bmi_level_normal
    }

    override fun getRangeText(category: BMIEnum): String {
        val ranges = resources.getStringArray(R.array.bmi_level_ranges)
        return ranges[category.ordinal]
    }

    override fun getDefaultLevel(): BMIEnum = BMIEnum.NORMAL

    override fun updateLevelBar() {
        currentLevelValue?.let { level ->
            bmiLevelBar?.setIndicatorIndex(level.ordinal)
        }
    }

    /**
     * 根据 BMI 数值更新状态
     */
    fun updateWithBmiValue(bmi: Float) {
        setLevel(BMIEnum.fromBmi(bmi))
    }
}