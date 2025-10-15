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

        private const val TC_NORMAL_MAX = 200f
        private const val TC_BORDERLINE_MAX = 239f

        private const val NON_HDL_THRESHOLD = 130f
        private const val HDL_NORMAL_THRESHOLD = 40f
        private const val HDL_UPPER_LIMIT = 45f

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

            if (totalCholesterol < TC_NORMAL_MAX &&
                nonHdl < NON_HDL_THRESHOLD &&
                ldl < LDL_NEAR_OPTIMAL_MIN &&
                hdl > HDL_NORMAL_THRESHOLD
            ) {
                return NORMAL
            }

            if (totalCholesterol >= TC_NORMAL_MAX &&
                nonHdl >= NON_HDL_THRESHOLD &&
                ldl >= LDL_VERY_HIGH_MIN &&
                hdl <= HDL_UPPER_LIMIT
            ) {
                return VERY_HIGH
            }

            if (totalCholesterol >= TC_NORMAL_MAX &&
                nonHdl >= NON_HDL_THRESHOLD &&
                ldl in LDL_HIGH_MIN until LDL_VERY_HIGH_MIN &&
                hdl <= HDL_UPPER_LIMIT
            ) {
                return HIGH
            }

            if (totalCholesterol in TC_NORMAL_MAX..TC_BORDERLINE_MAX &&
                nonHdl < NON_HDL_THRESHOLD &&
                ldl in LDL_BORDERLINE_MIN until LDL_HIGH_MIN &&
                hdl <= HDL_UPPER_LIMIT
            ) {
                return BORDERLINE
            }

            if (totalCholesterol < TC_NORMAL_MAX &&
                nonHdl < NON_HDL_THRESHOLD &&
                ldl in LDL_NEAR_OPTIMAL_MIN until LDL_BORDERLINE_MIN &&
                hdl <= HDL_UPPER_LIMIT
            ) {
                return NEAR_OPTIMAL
            }

            return UNKNOWN
        }
    }
}
