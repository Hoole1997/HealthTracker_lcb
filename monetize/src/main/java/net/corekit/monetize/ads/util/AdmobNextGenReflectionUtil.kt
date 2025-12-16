package net.corekit.monetize.ads.util

import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.PrecisionType
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import net.corekit.monetize.ads.log.AdLogger
import java.lang.reflect.Field

/**
 * AdMob Next-Gen SDK 反射工具类
 * 用于在广告展示前通过反射获取 eCPM 值，实现客户端竞价
 * 
 * 注意：此工具依赖 SDK 内部实现，SDK 版本升级可能导致反射路径失效
 * 建议添加日志监控反射成功率，并保留递归查找作为降级方案
 */
object AdmobNextGenReflectionUtil {

    private const val TAG = "AdmobReflection"

    // 各广告类型的固定反射路径
    // 插屏广告路径
    private val ivStackV1 = arrayOf("b", "k", "L", "e", "b", "j", "a", "M", "c", "m")
    private val ivStackV2 = arrayOf("b", "k", "M", "c", "m")

    // 开屏广告路径
    private val spStack = arrayOf("b", "k", "M", "c", "m")

    // 原生广告路径
    private val nativeStackV1 = arrayOf("b", "l", "j", "e", "b", "j", "a", "M", "c", "m")
    private val nativeStackV2 = arrayOf("b", "l", "s", "e", "m")

    // Banner广告路径
    private val bannerStack = arrayOf("b", "k", "a", "d", "d", "a", "m")

    // 激励视频路径
    private val rvStack = arrayOf("c", "a", "a", "k", "M", "c", "m")

    /**
     * 通过反射获取任意 AdMob 广告收益信息，当前支持 Banner、开屏、插页、激励、原生。
     * 使用递归查找方式，适用于未知路径的情况。
     * @param ad 广告对象
     * @return [AdValue]，未获取到返回 null
     */
    fun getRevenue(ad: Any?): AdValue? {
        if (ad == null) return null
        return when (ad) {
            is InterstitialAd -> findAdValueRecursively(ad, "插页")
            is AppOpenAd -> findAdValueRecursively(ad, "开屏")
            is RewardedAd -> findAdValueRecursively(ad, "激励")
            is RewardedInterstitialAd -> findAdValueRecursively(ad, "插页激励")
            is NativeAd -> findAdValueRecursively(ad, "原生")
            is BannerAd -> findAdValueRecursively(ad, "Banner")
            else -> null
        } ?: run {
            AdLogger.w("[%s] 未能通过递归反射解析到收益信息，ad=%s", TAG, ad::class.java.simpleName)
            null
        }
    }

    /**
     * 通过固定路径获取任意 AdMob 广告收益信息，当前支持 Banner、开屏、插页、激励、原生。
     * 使用固定路径方式，性能更好，适用于已知路径的情况。
     * @param ad 广告对象
     * @return [AdValue]，未获取到返回 null
     */
    fun getRevenueByPath(ad: Any?): AdValue? {
        if (ad == null) return null
        return when (ad) {
            is InterstitialAd -> findAdValueByPath(ad, "插页", listOf(ivStackV1, ivStackV2))
            is AppOpenAd -> findAdValueByPath(ad, "开屏", listOf(spStack))
            is RewardedAd -> findAdValueByPath(ad, "激励", listOf(rvStack))
            is RewardedInterstitialAd -> findAdValueRecursively(ad, "插页激励")
            is NativeAd -> findAdValueByPath(ad, "原生", listOf(nativeStackV1, nativeStackV2))
            is BannerAd -> findAdValueByPath(ad, "Banner", listOf(bannerStack))
            else -> null
        } ?: run {
            AdLogger.w("[%s] 未能通过固定路径解析到收益信息，ad=%s，尝试递归查找", TAG, ad::class.java.simpleName)
            // 固定路径失败时，降级到递归查找
            getRevenue(ad)
        }
    }

    /**
     * 通过固定路径查找 AdValue
     * 如果第一个路径的价格为0，则尝试第二个路径
     */
    private fun findAdValueByPath(ad: Any, adType: String, pathList: List<Array<String>>): AdValue? {
        var lastAdValue: AdValue? = null
        val hasMultiplePaths = pathList.size > 1
        
        pathList.forEachIndexed { index, stack ->
            val leaf = traverse(ad, stack, adType)
            if (leaf != null) {
                val adValue = parseLeaf(leaf, stack, adType)
                if (adValue != null) {
                    // 如果价格不为0，直接返回
                    if (adValue.valueMicros > 0) {
                        AdLogger.d("[%s] [%s] 通过路径获取到有效价格: %d 微元", TAG, adType, adValue.valueMicros)
                        return adValue
                    }
                    // 如果价格为0，保存并继续尝试下一个路径
                    lastAdValue = adValue
                    if (hasMultiplePaths && index < pathList.size - 1) {
                        AdLogger.d("[%s] [%s] 路径价格为0，尝试下一个路径", TAG, adType)
                    }
                }
            }
        }
        return lastAdValue
    }

    /**
     * 根据路径遍历获取对象
     */
    private fun traverse(target: Any, stack: Array<String>, adType: String): Any? {
        var current: Any? = target
        stack.forEach { fieldName ->
            val fieldValue = current?.getValue(fieldName) ?: return null
            current = fieldValue
        }
        return current
    }

    /**
     * 解析叶子节点
     */
    private fun parseLeaf(leaf: Any, stack: Array<String>, adType: String): AdValue? {
        // 如果是 AdValue 类型，直接返回
        if (leaf is AdValue) {
            return leaf
        }
        // 检查当前对象是否包含 AdValue 的特征字段
        return checkAndCreateAdValue(leaf, adType)
    }

    /**
     * 递归查找 AdValue 对象
     */
    private fun findAdValueRecursively(
        obj: Any?, 
        adType: String, 
        visited: MutableSet<Any> = mutableSetOf(), 
        depth: Int = 0
    ): AdValue? {
        if (obj == null || depth > 10) return null
        
        val identity = System.identityHashCode(obj)
        if (visited.any { System.identityHashCode(it) == identity }) return null
        visited.add(obj)

        return try {
            if (obj is AdValue) return obj
            
            checkAndCreateAdValue(obj, adType)?.let { return it }

            var clazz: Class<*>? = obj::class.java
            while (clazz != null) {
                val fields = clazz.declaredFields
                for (field in fields) {
                    try {
                        field.isAccessible = true
                        val fieldValue = field.get(obj) ?: continue
                        if (isPrimitiveOrBasicType(field.type)) continue
                        findAdValueRecursively(fieldValue, adType, visited, depth + 1)?.let { return it }
                    } catch (e: Exception) {
                        continue
                    }
                }
                clazz = clazz.superclass
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查对象是否包含 AdValue 的特征字段，并尝试创建 AdValue
     */
    private fun checkAndCreateAdValue(obj: Any, adType: String): AdValue? {
        return try {
            var precision: PrecisionType? = null
            var valueMicros: Long? = null
            var currencyCode: String? = null

            var clazz: Class<*>? = obj::class.java
            while (clazz != null) {
                val fields = clazz.declaredFields
                for (field in fields) {
                    try {
                        field.isAccessible = true
                        val fieldValue = field.get(obj) ?: continue

                        when {
                            field.type == PrecisionType::class.java && fieldValue is PrecisionType -> {
                                precision = fieldValue
                            }
                            (field.type == Long::class.javaPrimitiveType || field.type == Long::class.javaObjectType) 
                                    && fieldValue is Long -> {
                                if (valueMicros == null || (fieldValue > 0 && fieldValue > (valueMicros ?: 0))) {
                                    valueMicros = fieldValue
                                }
                            }
                            field.type == String::class.java && fieldValue is String && fieldValue.isNotBlank() -> {
                                if (currencyCode == null || (fieldValue.length <= 5 && fieldValue.length >= 2)) {
                                    currencyCode = fieldValue
                                }
                            }
                        }
                    } catch (e: Exception) {
                        continue
                    }
                }
                clazz = clazz.superclass
            }

            if (precision != null && valueMicros != null && currencyCode != null) {
                createAdValue(precision, valueMicros, currencyCode)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 通过字段名获取对象的值
     */
    private fun Any?.getValue(fieldName: String): Any? {
        if (this == null) return null
        return try {
            var clazz: Class<*>? = this::class.java
            var field: Field? = null
            while (clazz != null) {
                try {
                    field = clazz.getDeclaredField(fieldName).apply { isAccessible = true }
                    break
                } catch (ignored: NoSuchFieldException) {
                    clazz = clazz.superclass
                }
            }
            field?.get(this)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 判断是否为基础类型
     */
    private fun isPrimitiveOrBasicType(type: Class<*>): Boolean {
        val componentType = type.componentType
        return when {
            type.isPrimitive -> true
            type == Boolean::class.javaObjectType || type == Boolean::class.javaPrimitiveType -> true
            type == Byte::class.javaObjectType || type == Byte::class.javaPrimitiveType -> true
            type == Character::class.javaObjectType || type == Char::class.javaPrimitiveType -> true
            type == Short::class.javaObjectType || type == Short::class.javaPrimitiveType -> true
            type == Int::class.javaObjectType || type == Int::class.javaPrimitiveType -> true
            type == Long::class.javaObjectType || type == Long::class.javaPrimitiveType -> true
            type == Float::class.javaObjectType || type == Float::class.javaPrimitiveType -> true
            type == Double::class.javaObjectType || type == Double::class.javaPrimitiveType -> true
            type == String::class.java -> true
            type.isArray && componentType != null && isPrimitiveOrBasicType(componentType) -> true
            type.name.startsWith("java.lang.") -> true
            else -> false
        }
    }

    /**
     * 通过反射创建 AdValue 实例
     */
    private fun createAdValue(precision: PrecisionType, valueMicros: Long, currencyCode: String): AdValue? {
        return try {
            val constructor = AdValue::class.java.getDeclaredConstructor(
                PrecisionType::class.java,
                Long::class.javaPrimitiveType,
                String::class.java
            )
            constructor.isAccessible = true
            constructor.newInstance(precision, valueMicros, currencyCode) as AdValue
        } catch (e: Exception) {
            AdLogger.e("[%s] 创建 AdValue 失败: %s", TAG, e.message)
            null
        }
    }
}
