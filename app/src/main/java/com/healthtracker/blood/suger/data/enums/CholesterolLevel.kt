package com.healthtracker.blood.suger.data.enums
import androidx.annotation.StringRes
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.weight.LevelCategory

/**
 * 胆固醇风险等级
 * 综合总胆固醇、非 HDL、LDL 与 HDL 多指标进行判定
 */
enum class CholesterolLevel(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    override val colorRes: Int
) : LevelCategory {

    /**
     * 数据不足或无法判定
     */
    UNKNOWN(
        labelRes = R.string.cholesterol_level_unknown,
        descriptionRes = R.string.cholesterol_level_unknown_desc,
        colorRes = R.color.color_CCCCCC
    ),

    /**
     * 正常水平：
     * TC < 200、Non-HDL < 130、LDL < 100、HDL > 40
     */
    NORMAL(
        labelRes = R.string.cholesterol_level_normal,
        descriptionRes = R.string.cholesterol_level_normal_desc,
        colorRes = R.color.color_05BA7B
    ),

    /**
     * 接近理想值：
     * TC < 200、Non-HDL < 130、LDL ∈ [100,129]、HDL ≤ 45
     */
    NEAR_OPTIMAL(
        labelRes = R.string.cholesterol_level_near_optimal,
        descriptionRes = R.string.cholesterol_level_near_optimal_desc,
        colorRes = R.color.color_FFE902
    ),

    /**
     * 临界偏高：
     * TC ∈ [200,239]、Non-HDL < 130、LDL ∈ [130,159]、HDL ≤ 45
     */
    BORDERLINE(
        labelRes = R.string.cholesterol_level_borderline,
        descriptionRes = R.string.cholesterol_level_borderline_desc,
        colorRes = R.color.color_FFB909
    ),

    /**
     * 高风险：
     * TC ≥ 240、Non-HDL ≥ 130、LDL ∈ [160,189]、HDL ≤ 45
     */
    HIGH(
        labelRes = R.string.cholesterol_level_high,
        descriptionRes = R.string.cholesterol_level_high_desc,
        colorRes = R.color.color_FF8000
    ),

    /**
     * 极高风险：
     * TC ≥ 240、Non-HDL ≥ 130、LDL ≥ 190、HDL ≤ 45
     */
    VERY_HIGH(
        labelRes = R.string.cholesterol_level_very_high,
        descriptionRes = R.string.cholesterol_level_very_high_desc,
        colorRes = R.color.color_FB0301
    );

    companion object {
        private const val LDL_NEAR_OPTIMAL_MIN = 100f
        private const val LDL_BORDERLINE_MIN = 130f
        private const val LDL_HIGH_MIN = 160f
        private const val LDL_VERY_HIGH_MIN = 190f

        private const val NON_HDL_BORDERLINE_MIN = 130f
        private const val NON_HDL_HIGH_MIN = 160f
        private const val NON_HDL_VERY_HIGH_MIN = 190f

        private const val TC_BORDERLINE_MIN = 200f
        private const val TC_HIGH_MIN = 240f

        private const val HDL_LOW_THRESHOLD = 45f
        private const val HDL_VERY_LOW_THRESHOLD = 35f

        /**
         * 根据多项指标综合得出胆固醇等级
         */
        fun fromMetrics(
            totalCholesterol: Float?,
            nonHdl: Float?,
            ldl: Float?,
            hdl: Float?
        ): CholesterolLevel {
            if (totalCholesterol == null || nonHdl == null || ldl == null || hdl == null) {
                return UNKNOWN
            }

            // 评估各指标对应的风险等级分值，取最高项作为最终风险
            val worstScore = listOf(
                scoreFromTotalCholesterol(totalCholesterol),
                scoreFromNonHdl(nonHdl),
                scoreFromLdl(ldl),
                scoreFromHdl(hdl)
            ).maxOrNull() ?: 0

            return when (worstScore) {
                4 -> VERY_HIGH
                3 -> HIGH
                2 -> BORDERLINE
                1 -> NEAR_OPTIMAL
                else -> NORMAL
            }
        }

        private fun scoreFromTotalCholesterol(value: Float): Int {
            return when {
                value >= TC_HIGH_MIN -> 3
                value >= TC_BORDERLINE_MIN -> 2
                else -> 0
            }
        }

        private fun scoreFromNonHdl(value: Float): Int {
            return when {
                value >= NON_HDL_VERY_HIGH_MIN -> 4
                value >= NON_HDL_HIGH_MIN -> 3
                value >= NON_HDL_BORDERLINE_MIN -> 2
                else -> 0
            }
        }

        private fun scoreFromLdl(value: Float): Int {
            return when {
                value >= LDL_VERY_HIGH_MIN -> 4
                value >= LDL_HIGH_MIN -> 3
                value >= LDL_BORDERLINE_MIN -> 2
                value >= LDL_NEAR_OPTIMAL_MIN -> 1
                else -> 0
            }
        }

        private fun scoreFromHdl(value: Float): Int {
            return when {
                value <= HDL_VERY_LOW_THRESHOLD -> 2
                value <= HDL_LOW_THRESHOLD -> 1
                else -> 0
            }
        }
    }
}
