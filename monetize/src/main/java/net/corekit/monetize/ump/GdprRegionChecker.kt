package net.corekit.monetize.ump

import net.corekit.monetize.ads.log.AdLogger

/**
 * GDPR 区域检测器
 * 
 * 判断给定国家代码是否属于 GDPR 覆盖区域
 * GDPR 区域用户需要展示 UMP 同意弹窗
 */
object GdprRegionChecker {
    
    private const val TAG = "GdprChecker"
    
    /**
     * GDPR 覆盖的国家代码列表
     * 
     * 包括：
     * - 欧盟 27 国
     * - 英国（脱欧后仍适用 UK GDPR）
     * - 欧洲经济区（冰岛、列支敦士登、挪威）
     * - 瑞士（适用类似 GDPR 的数据保护法）
     */
    private val GDPR_COUNTRIES = setOf(
        // 欧盟 27 国
        "AT", // 奥地利
        "BE", // 比利时
        "BG", // 保加利亚
        "HR", // 克罗地亚
        "CY", // 塞浦路斯
        "CZ", // 捷克
        "DK", // 丹麦
        "EE", // 爱沙尼亚
        "FI", // 芬兰
        "FR", // 法国
        "DE", // 德国
        "GR", // 希腊
        "HU", // 匈牙利
        "IE", // 爱尔兰
        "IT", // 意大利
        "LV", // 拉脱维亚
        "LT", // 立陶宛
        "LU", // 卢森堡
        "MT", // 马耳他
        "NL", // 荷兰
        "PL", // 波兰
        "PT", // 葡萄牙
        "RO", // 罗马尼亚
        "SK", // 斯洛伐克
        "SI", // 斯洛文尼亚
        "ES", // 西班牙
        "SE", // 瑞典
        // 英国（脱欧后仍适用 UK GDPR）
        "GB",
        // 欧洲经济区（EEA）
        "IS", // 冰岛
        "LI", // 列支敦士登
        "NO", // 挪威
        // 瑞士（适用类似 GDPR 的数据保护法）
        "CH"
    )
    
    /**
     * 检查国家代码是否属于 GDPR 覆盖区域
     * 
     * @param countryCode ISO 3166-1 alpha-2 国家代码（如 "DE", "US"）
     * @return true 表示属于 GDPR 区域，需要展示 UMP 同意弹窗
     */
    fun isGdprRegion(countryCode: String?): Boolean {
        if (countryCode.isNullOrBlank()) {
            AdLogger.d("[$TAG] 国家代码为空，判定为非 GDPR 区域")
            return false
        }
        
        val isGdpr = countryCode.uppercase() in GDPR_COUNTRIES
        AdLogger.d("[$TAG] 国家 $countryCode ${if (isGdpr) "属于" else "不属于"} GDPR 区域")
        return isGdpr
    }
    
    /**
     * 获取所有 GDPR 国家代码（用于调试）
     */
    fun getAllGdprCountries(): Set<String> = GDPR_COUNTRIES.toSet()
}
