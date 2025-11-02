package net.corekit.adsdk.event

/**
 * 广告事件监听器接口
 * 实现此接口以接收广告事件通知
 */
fun interface AdEventListener {
    /**
     * 处理广告事件
     *
     * @param event 广告事件
     */
    fun onEvent(event: AdEvent)
}

/**
 * 简化的事件监听器 DSL
 */
class AdEventListenerBuilder {
    private val handlers = mutableMapOf<Class<out AdEvent>, (AdEvent) -> Unit>()

    /**
     * 监听加载成功事件
     */
    fun onLoadSuccess(handler: (AdEvent.LoadSuccess) -> Unit) {
        handlers[AdEvent.LoadSuccess::class.java] = { handler(it as AdEvent.LoadSuccess) }
    }

    /**
     * 监听加载失败事件
     */
    fun onLoadFailure(handler: (AdEvent.LoadFailure) -> Unit) {
        handlers[AdEvent.LoadFailure::class.java] = { handler(it as AdEvent.LoadFailure) }
    }

    /**
     * 监听展示成功事件
     */
    fun onShowSuccess(handler: (AdEvent.ShowSuccess) -> Unit) {
        handlers[AdEvent.ShowSuccess::class.java] = { handler(it as AdEvent.ShowSuccess) }
    }

    /**
     * 监听展示失败事件
     */
    fun onShowFailure(handler: (AdEvent.ShowFailure) -> Unit) {
        handlers[AdEvent.ShowFailure::class.java] = { handler(it as AdEvent.ShowFailure) }
    }

    /**
     * 监听点击事件
     */
    fun onClick(handler: (AdEvent.Click) -> Unit) {
        handlers[AdEvent.Click::class.java] = { handler(it as AdEvent.Click) }
    }

    /**
     * 监听曝光事件
     */
    fun onImpression(handler: (AdEvent.Impression) -> Unit) {
        handlers[AdEvent.Impression::class.java] = { handler(it as AdEvent.Impression) }
    }

    /**
     * 监听关闭事件
     */
    fun onClose(handler: (AdEvent.Close) -> Unit) {
        handlers[AdEvent.Close::class.java] = { handler(it as AdEvent.Close) }
    }

    /**
     * 监听激励完成事件
     */
    fun onRewarded(handler: (AdEvent.Rewarded) -> Unit) {
        handlers[AdEvent.Rewarded::class.java] = { handler(it as AdEvent.Rewarded) }
    }

    /**
     * 构建监听器
     */
    fun build(): AdEventListener {
        return AdEventListener { event ->
            handlers[event::class.java]?.invoke(event)
        }
    }
}

/**
 * DSL 方式创建事件监听器
 *
 * 示例：
 * ```
 * val listener = adEventListener {
 *     onLoadSuccess { event ->
 *         println("Ad loaded: ${event.adUnitId}")
 *     }
 *     onImpression { event ->
 *         println("Revenue: ${event.valueMicros / 1_000_000.0} ${event.currencyCode}")
 *     }
 * }
 * ```
 */
fun adEventListener(builder: AdEventListenerBuilder.() -> Unit): AdEventListener {
    return AdEventListenerBuilder().apply(builder).build()
}
