package com.healthtracker.blood.suger.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.enums.BMICategory
import com.healthtracker.blood.suger.ui.dialog.BmiLeveDialog
import com.healthtracker.framework.ext.clickWithDuration

/**
 * BMI 状态视图，复用通用状态视图框架
 */
class BMIStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GenericStatusView<BMICategory>(context, attrs, defStyleAttr) {

    // 等级文本映射
    private val levelTexts = mapOf(
        BMICategory.VERY_SEVERELY_UNDERWEIGHT to R.string.bmi_level_very_severely_underweight,
        BMICategory.SEVERELY_UNDERWEIGHT to R.string.bmi_level_severely_underweight,
        BMICategory.UNDERWEIGHT to R.string.bmi_level_underweight,
        BMICategory.NORMAL to R.string.bmi_level_normal,
        BMICategory.OVERWEIGHT to R.string.bmi_level_overweight,
        BMICategory.OBESITY_CLASS_I to R.string.bmi_level_obesity_class_1,
        BMICategory.OBESITY_CLASS_II to R.string.bmi_level_obesity_class_2,
        BMICategory.OBESITY_CLASS_III to R.string.bmi_level_obesity_class_3
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

    override fun getLevelTexts(): Map<BMICategory, Int> = levelTexts

    override fun getStatusTextRes(category: BMICategory): Int {
        return levelTexts[category] ?: R.string.bmi_level_normal
    }

    override fun getRangeText(category: BMICategory): String {
        val ranges = resources.getStringArray(R.array.bmi_level_ranges)
        return ranges[category.ordinal]
    }

    override fun getDefaultLevel(): BMICategory = BMICategory.NORMAL

    override fun updateLevelBar() {
        currentLevelValue?.let { level ->
            bmiLevelBar?.setCategory(level)
        }
    }

    /**
     * 根据 BMI 数值更新状态
     */
    fun updateWithBmiValue(bmi: Float) {
        setLevel(BMICategory.fromBmi(bmi))
    }
}