package net.corekit.monetize.ump

import android.app.Activity
import android.content.Context
import com.healthtracker.framework.util.SpUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.corekit.monetize.ads.log.AdLogger

/**
 * UMP 同意管理统一控制器
 * 
 * 职责：
 * 1. 检测用户 IP 地理位置
 * 2. 判断是否属于 GDPR 覆盖区域
 * 3. 仅对 GDPR 区域用户展示 UMP 同意弹窗
 * 4. 缓存国家代码，避免重复查询
 *
 * 流程说明：
 * - IP 查询 → GDPR 区域判断 → 展示 UMP（仅 GDPR 区域）
 * - IP 查询失败时，默认不显示 UMP（保护非 GDPR 用户体验）
 * - UMP 结果不影响广告加载（仅用于合规）
 *
 * 兜底策略：
 * - IP 查询超时（3秒）：使用缓存或默认不显示 UMP
 * - IP 查询失败：默认不显示 UMP
 * - UMP SDK 加载失败：跳过 UMP，允许广告
 * - 用户拒绝 UMP：仍允许广告（Google 自动处理非个性化广告）
 */
object UmpConsentController {
    
    private const val TAG = "UmpController"
    
    /** 缓存的国家代码 SP Key */
    private const val SP_KEY_COUNTRY_CODE = "ump_cached_country_code"
    
    /** 缓存时间 SP Key */
    private const val SP_KEY_CACHE_TIME = "ump_country_code_cache_time"
    
    /** 缓存过期时长（1小时） */
    private const val CACHE_EXPIRY_MS = 1 * 60 * 60 * 1000L
    
    /** 当前会话是否已检查过（避免同一次启动重复检查） */
    @Volatile
    private var hasCheckedThisSession = false
    
    /** 当前会话是否已预取国家代码 */
    @Volatile
    private var hasPrefetchedCountryCode = false
    
    /**
     * 预取国家代码（启动时并行调用）
     * 
     * 在启动时立即调用此方法，与通知权限和广告加载并行执行。
     * 结果会缓存，后续 checkAndShowConsentIfNeeded 直接使用缓存。
     */
    suspend fun prefetchCountryCode() {
        if (hasPrefetchedCountryCode) {
            AdLogger.d("[$TAG] 当前会话已预取国家代码，跳过")
            return
        }
        
        try {
            AdLogger.d("[$TAG] 开始预取国家代码...")
            val countryCode = fetchCountryCode()
            hasPrefetchedCountryCode = true
            AdLogger.d("[$TAG] 预取国家代码完成: ${countryCode ?: "未知"}")
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 预取国家代码异常: ${e.message}")
            hasPrefetchedCountryCode = true
        }
    }
    
    /**
     * 检查并处理 UMP 同意流程
     * 
     * 此方法使用已缓存的国家代码，不会阻塞查询 IP。
     * 应在 prefetchCountryCode() 完成后调用。
     * 
     * 流程：
     * 1. 使用缓存的国家代码判断 GDPR 区域
     * 2. 仅对 GDPR 区域用户展示 UMP 弹窗
     * 
     * @param activity 当前 Activity（用于展示 UMP 弹窗）
     * @return true 表示检查完成（无论是否弹窗），false 表示检查失败
     */
    suspend fun checkAndShowConsentIfNeeded(activity: Activity): Boolean = withContext(Dispatchers.Main) {
        try {
            AdLogger.d("[$TAG] ========== 开始 UMP 同意检查流程 ==========")
            
            // 1. 检查当前会话是否已检查过
            if (hasCheckedThisSession) {
                AdLogger.d("[$TAG] 当前会话已检查过 UMP，跳过")
                return@withContext true
            }
            
            // 2. 使用缓存的国家代码（prefetchCountryCode 已提前查询）
            val countryCode = getCachedCountryCode()
            AdLogger.d("[$TAG] 用户国家代码: ${countryCode ?: "未知"}")
            
            // 3. 判断是否属于 GDPR 区域
            val isGdprRegion = GdprRegionChecker.isGdprRegion(countryCode)
            
            if (!isGdprRegion) {
                // 非 GDPR 区域，跳过 UMP
                AdLogger.d("[$TAG] 非 GDPR 区域，跳过 UMP 弹窗")
                hasCheckedThisSession = true
                AdLogger.d("[$TAG] ========== UMP 同意检查流程结束（跳过）==========")
                return@withContext true
            }
            
            // 4. GDPR 区域，展示 UMP 弹窗
            AdLogger.d("[$TAG] GDPR 区域用户，开始展示 UMP 弹窗")
            val consentManager = GoogleMobileAdsConsentManager.getInstance(activity)
            val canRequestAds = consentManager.gatherConsent(activity)
            
            // 5. 标记当前会话已检查
            hasCheckedThisSession = true
            
            AdLogger.d("[$TAG] UMP 弹窗完成，canRequestAds: $canRequestAds")
            AdLogger.d("[$TAG] isPrivacyOptionsRequired: ${consentManager.isPrivacyOptionsRequired}")
            AdLogger.d("[$TAG] ========== UMP 同意检查流程结束 ==========")
            
            true
            
        } catch (e: Exception) {
            AdLogger.e("[$TAG] UMP 同意检查异常: ${e.message}")
            e.printStackTrace()
            // 出错时标记当前会话已检查，避免重复尝试
            hasCheckedThisSession = true
            true
        }
    }
    
    /**
     * 获取国家代码（查询 IP 并缓存）
     * 
     * 优先使用缓存的国家代码，如果没有缓存则查询 IP
     * 
     * @return 国家代码（如 "US", "DE"），失败返回 null
     */
    private suspend fun fetchCountryCode(): String? = withContext(Dispatchers.IO) {
        // 1. 检查缓存是否有效（未过期）
        val cachedCountryCode = SpUtils.getString(SP_KEY_COUNTRY_CODE, "")
        val cacheTime = SpUtils.getLong(SP_KEY_CACHE_TIME, 0L)
        val isExpired = System.currentTimeMillis() - cacheTime > CACHE_EXPIRY_MS
        
        if (!cachedCountryCode.isNullOrBlank() && !isExpired) {
            val remainingMinutes = (CACHE_EXPIRY_MS - (System.currentTimeMillis() - cacheTime)) / 60000
            AdLogger.d("[$TAG] 使用缓存的国家代码: $cachedCountryCode，剩余有效时间: ${remainingMinutes}分钟")
            return@withContext cachedCountryCode
        }
        
        if (isExpired && !cachedCountryCode.isNullOrBlank()) {
            AdLogger.d("[$TAG] 国家代码缓存已过期，重新查询...")
        }
        
        // 2. 查询 IP 获取国家代码
        AdLogger.d("[$TAG] 开始 IP 地理位置查询...")
        val countryCode = GeoLocationService.getCountryCode()
        
        // 3. 缓存结果（仅在成功时缓存）
        if (!countryCode.isNullOrBlank()) {
            SpUtils.putString(SP_KEY_COUNTRY_CODE, countryCode)
            SpUtils.putLong(SP_KEY_CACHE_TIME, System.currentTimeMillis())
            AdLogger.d("[$TAG] 国家代码已缓存: $countryCode，有效期: 1小时")
        } else {
            AdLogger.w("[$TAG] IP 查询失败，无法获取国家代码")
        }
        
        countryCode
    }
    
    /**
     * 检查是否需要展示隐私选项入口
     * 用于设置页面显示"隐私设置"选项
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        return try {
            GoogleMobileAdsConsentManager.getInstance(context).isPrivacyOptionsRequired
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 检查隐私选项需求异常: ${e.message}")
            false
        }
    }
    
    /**
     * 显示隐私选项表单（供设置页面调用）
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        try {
            if (activity is androidx.fragment.app.FragmentActivity) {
                GoogleMobileAdsConsentManager.getInstance(activity)
                    .showPrivacyOptionsForm(activity) { _ ->
                        AdLogger.d("[$TAG] 隐私选项表单已关闭")
                        onDismissed()
                    }
            } else {
                AdLogger.w("[$TAG] Activity 不是 FragmentActivity，无法显示隐私选项表单")
                onDismissed()
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 显示隐私选项表单异常: ${e.message}")
            onDismissed()
        }
    }
    
    /**
     * 获取缓存的国家代码
     */
    fun getCachedCountryCode(): String? {
        val code = SpUtils.getString(SP_KEY_COUNTRY_CODE, "")
        return if (code.isNullOrBlank()) null else code
    }
    
    /**
     * 判断缓存的国家是否属于 GDPR 区域
     */
    fun isCachedCountryGdprRegion(): Boolean {
        return GdprRegionChecker.isGdprRegion(getCachedCountryCode())
    }
    
    /**
     * 重置当前会话的检查状态（用于测试）
     */
    fun resetSessionCheckState() {
        hasCheckedThisSession = false
        hasPrefetchedCountryCode = false
        AdLogger.d("[$TAG] 当前会话 UMP 检查状态已重置")
    }
    
    /**
     * 重置 UMP SDK 的同意状态（用于测试）
     * 警告：此方法会清除用户之前的同意选择
     */
    fun resetUmpConsentState(context: Context) {
        try {
            com.google.android.ump.UserMessagingPlatform.getConsentInformation(context).reset()
            hasCheckedThisSession = false
            AdLogger.d("[$TAG] UMP 同意状态已完全重置")
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 重置 UMP 同意状态异常: ${e.message}")
        }
    }
    
    /**
     * 清除国家代码缓存（用于测试或用户切换地区）
     */
    fun clearCountryCodeCache() {
        SpUtils.putString(SP_KEY_COUNTRY_CODE, "")
        SpUtils.putLong(SP_KEY_CACHE_TIME, 0L)
        AdLogger.d("[$TAG] 国家代码缓存已清除")
    }
    
    /**
     * 获取当前是否可以请求广告
     * 注意：UMP 状态不影响广告请求，此方法仅用于查询
     */
    fun canRequestAds(context: Context): Boolean {
        return try {
            GoogleMobileAdsConsentManager.getInstance(context).canRequestAds
        } catch (e: Exception) {
            AdLogger.e("[$TAG] 查询广告请求状态异常: ${e.message}")
            true // 默认允许
        }
    }
}
