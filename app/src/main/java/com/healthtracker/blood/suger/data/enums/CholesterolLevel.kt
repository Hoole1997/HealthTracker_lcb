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
        labelRes = R.string.ht_cholesterol_level_unknown,
        descriptionRes = R.string.ht_cholesterol_level_unknown_desc,
        colorRes = R.color.color_CCCCCC
    ),

    /**
     * 正常水平：
     * TC < 200、Non-HDL < 130、LDL < 100、HDL > 40
     */
    NORMAL(
        labelRes = R.string.ht_cholesterol_level_normal,
        descriptionRes = R.string.ht_cholesterol_level_normal_desc,
        colorRes = R.color.color_05BA7B
    ),

    /**
     * 接近理想值：
     * TC < 200、Non-HDL < 130、LDL ∈ [100,129]、HDL ≤ 45
     */
    NEAR_OPTIMAL(
        labelRes = R.string.ht_cholesterol_level_near_optimal,
        descriptionRes = R.string.ht_cholesterol_level_near_optimal_desc,
        colorRes = R.color.color_FFE902
    ),

    /**
     * 临界偏高：
     * TC ∈ [200,239]、Non-HDL < 130、LDL ∈ [130,159]、HDL ≤ 45
     */
    BORDERLINE(
        labelRes = R.string.ht_cholesterol_level_borderline,
        descriptionRes = R.string.ht_cholesterol_level_borderline_desc,
        colorRes = R.color.color_FFB909
    ),

    /**
     * 高风险：
     * TC ≥ 240、Non-HDL ≥ 130、LDL ∈ [160,189]、HDL ≤ 45
     */
    HIGH(
        labelRes = R.string.ht_cholesterol_level_high,
        descriptionRes = R.string.ht_cholesterol_level_high_desc,
        colorRes = R.color.color_FF8000
    ),

    /**
     * 极高风险：
     * TC ≥ 240、Non-HDL ≥ 130、LDL ≥ 190、HDL ≤ 45
     */
    VERY_HIGH(
        labelRes = R.string.ht_cholesterol_level_very_high,
        descriptionRes = R.string.ht_cholesterol_level_very_high_desc,
        colorRes = R.color.color_FB0301
    );

    companion object {

        /**
         * 统计各等级满足条件的数量，取命中最多的等级
         * 若数量相同，倾向于等级较低者以避免过度判定
         */
        fun fromMetrics(
            totalCholesterol: Float?,
            nonHdl: Float?,
            ldl: Float?,
            hdl: Float?
        ): CholesterolLevel {
            if (totalCholesterol == null && nonHdl == null && ldl == null && hdl == null) {
                return UNKNOWN
            }

            val levels = listOf(NORMAL, NEAR_OPTIMAL, BORDERLINE, HIGH, VERY_HIGH)
            val best = levels
                .map { level ->
                    val matches = when (level) {
                        NORMAL -> listOfNotNull(
                            totalCholesterol?.let { it < 200f },
                            nonHdl?.let { it < 130f },
                            ldl?.let { it < 100f },
                            hdl?.let { it > 40f }
                        )
                        NEAR_OPTIMAL -> listOfNotNull(
                            totalCholesterol?.let { it < 200f },
                            nonHdl?.let { it < 130f },
                            ldl?.let { it in 100f..129f },
                            hdl?.let { it <= 45f }
                        )
                        BORDERLINE -> listOfNotNull(
                            totalCholesterol?.let { it in 200f..239f },
                            nonHdl?.let { it in 130f..159f },
                            ldl?.let { it in 130f..159f },
                            hdl?.let { it <= 45f }
                        )
                        HIGH -> listOfNotNull(
                            totalCholesterol?.let { it >= 240f },
                            nonHdl?.let { it >= 130f },
                            ldl?.let { it in 160f..189f },
                            hdl?.let { it <= 45f }
                        )
                        VERY_HIGH -> listOfNotNull(
                            totalCholesterol?.let { it >= 240f },
                            nonHdl?.let { it >= 130f },
                            ldl?.let { it >= 190f },
                            hdl?.let { it <= 45f }
                        )
                        else -> emptyList()
                    }.count { it }
                    level to matches
                }
                .maxWithOrNull { a, b ->
                    when {
                        a.second != b.second -> a.second.compareTo(b.second)
                        else -> a.first.ordinal.compareTo(b.first.ordinal)
                    }
                }

            return best?.takeIf { it.second > 0 }?.first ?: UNKNOWN
        }
    }
}
