package net.corekit.adsdk.service

import net.corekit.adsdk.core.AdResult

/**
 * 广告加载器接口
 * 负责从平台 SDK 加载广告对象
 *
 * @param T 平台广告对象类型
 */
internal interface AdLoader<T> {
    /**
     * 加载广告
     *
     * @param adUnitId 广告位 ID
     * @return 加载结果，成功时包含广告对象
     */
    suspend fun load(adUnitId: String): AdResult<T>

    /**
     * 取消正在进行的加载（可选实现）
     */
    fun cancel() {}
}
