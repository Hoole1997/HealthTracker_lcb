package net.corekit.adsdk.service

/**
 * 广告缓存接口
 * 提供广告对象的缓存管理功能
 *
 * @param T 平台广告对象类型
 */
internal interface AdCache<T> : AutoCloseable {
    /**
     * 缓存广告
     *
     * @param adUnitId 广告位 ID
     * @param ad 广告对象
     */
    fun cache(adUnitId: String, ad: T)

    /**
     * 获取缓存的广告
     * 自动移除已过期的广告
     *
     * @param adUnitId 广告位 ID
     * @return 广告对象，如果没有可用广告则返回 null
     */
    suspend fun get(adUnitId: String): T?

    /**
     * 获取指定广告位的缓存数量
     *
     * @param adUnitId 广告位 ID
     * @return 缓存数量
     */
    fun size(adUnitId: String): Int

    /**
     * 清空指定广告位的缓存
     *
     * @param adUnitId 广告位 ID
     */
    fun clear(adUnitId: String)

    /**
     * 清空所有缓存
     */
    fun clearAll()

    /**
     * 执行缓存清理（移除过期广告）
     */
    suspend fun cleanup()

    /**
     * 🔧 FIX: 关闭缓存并释放所有资源
     * 实现 AutoCloseable 接口，便于资源管理
     */
    override fun close()
}
