package com.daily.health.manager.ad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.daily.health.manager.R
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.bidding.FullScreenNativeBiddingManager
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdController
import net.corekit.monetize.ads.pangle.PangleFullScreenNativeAdView
import net.corekit.monetize.ads.topon.TopOnFullScreenNativeAdController
import net.corekit.monetize.ads.topon.ToponFullScreenNativeAdView
import net.corekit.monetize.ui.FullScreenNativeAdView

/**
 * 全屏原生广告展示页面
 * 
 * 支持 AdMob/Pangle/TopOn 三平台全屏原生广告展示
 * 内置失败回退机制：胜出平台失败时依次尝试其他平台
 */
class FullScreenNativeAdActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FullNaActivity"
        private const val EXTRA_PLATFORM = "extra_platform"
        private const val EXTRA_POSITION = "extra_position"
        private const val COUNTDOWN_DURATION = 3000L
        private const val COUNTDOWN_INTERVAL = 1000L

        /**
         * 启动全屏原生广告页面
         * 
         * @param activity 调用方Activity
         * @param platform 指定平台（可选，不指定则自动竞价）
         * @param position 广告位置标识
         */
        fun start(activity: Activity, platform: BiddingPlatform? = null, position: String = "") {
            val intent = Intent(activity, FullScreenNativeAdActivity::class.java).apply {
                putExtra(EXTRA_PLATFORM, platform?.name)
                putExtra(EXTRA_POSITION, position)
            }
            activity.startActivity(intent)
        }
    }

    private lateinit var adContainer: FrameLayout
    private lateinit var btnClose: ImageView
    private lateinit var tvCountdown: TextView
    private lateinit var loadingMask: FrameLayout
    private var countDownTimer: CountDownTimer? = null
    private var currentPlatform: BiddingPlatform? = null
    private val adView = FullScreenNativeAdView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.ht_activity_fullscreen_native_ad)

        initViews()
        setupViews()
        loadAndShowAd()
    }

    private fun initViews() {
        adContainer = findViewById(R.id.ad_container)
        btnClose = findViewById(R.id.btn_close)
        tvCountdown = findViewById(R.id.tv_countdown)
        loadingMask = findViewById(R.id.loading_mask)
    }

    private fun setupViews() {
        btnClose.setOnClickListener {
            triggerCloseEvent()
            finish()
        }
    }

    private fun loadAndShowAd() {
        lifecycleScope.launch {
            showLoading(true)

            // 获取指定平台或执行竞价
            val specifiedPlatform = intent.getStringExtra(EXTRA_PLATFORM)?.let {
                runCatching { BiddingPlatform.valueOf(it) }.getOrNull()
            }

            val orderedPlatforms = if (specifiedPlatform != null) {
                listOf(specifiedPlatform)
            } else {
                // 执行竞价获取排序后的平台列表
                FullScreenNativeBiddingManager.getSortedPlatforms(this@FullScreenNativeAdActivity)
            }

            if (orderedPlatforms.isEmpty()) {
                AdLogger.w("[$TAG] 没有可用的广告平台")
                showLoading(false)
                finish()
                return@launch
            }

            // 依次尝试各平台（失败回退机制）
            var success = false
            for (platform in orderedPlatforms) {
                AdLogger.d("[$TAG] 尝试展示 %s 广告", platform.name)
                val result = showPlatformAd(platform, adContainer)
                if (result is AdResult.Success) {
                    currentPlatform = platform
                    success = true
                    AdLogger.d("[$TAG] ✅ %s 广告展示成功", platform.name)
                    break
                }
                AdLogger.w("[$TAG] %s 广告展示失败，尝试下一平台", platform.name)
            }

            showLoading(false)

            if (success) {
                startCountdown()
            } else {
                AdLogger.e("[$TAG] 所有平台广告展示失败")
                finish()
            }
        }
    }

    private suspend fun showPlatformAd(platform: BiddingPlatform, container: ViewGroup): AdResult<Unit> {
        return try {
            when (platform) {
                BiddingPlatform.ADMOB -> showAdMobAd(container)
                BiddingPlatform.PANGLE -> showPangleAd(container)
                BiddingPlatform.TOPON -> showTopOnAd(container)
            }
        } catch (e: Exception) {
            AdLogger.e("[$TAG] %s 广告展示异常", platform.name, e)
            AdResult.Failure(net.corekit.monetize.ads.AdException(-1, e.message ?: "Unknown error"))
        }
    }

    private fun showAdMobAd(container: ViewGroup): AdResult<Unit> {
        val controller = AdsManager.Controllers.fullScreenNative
        if (!controller.checkCachedAdAvailable()) {
            return AdResult.Failure(AdException(-1, "AdMob 无缓存广告"))
        }

        val cachedAd = controller.peekCachedAd() ?: return AdResult.Failure(
            AdException(-1, "AdMob 获取缓存广告失败")
        )

        val success = adView.bindFullScreenNativeAdToContainer(
            context = this,
            container = container,
            nativeAd = cachedAd,
            lifecycleOwner = this
        )

        return if (success) {
            AdResult.Success(Unit)
        } else {
            AdResult.Failure(AdException(-1, "AdMob 绑定视图失败"))
        }
    }

    private fun showPangleAd(container: ViewGroup): AdResult<Unit> {
        val controller = PangleFullScreenNativeAdController.getInstance()
        if (!controller.hasValidCache()) {
            return AdResult.Failure(AdException(-1, "Pangle 无缓存广告"))
        }

        // Pangle 使用自有渲染逻辑
        val nativeAd = controller.getCurrentAd() ?: return AdResult.Failure(
            AdException(-1, "Pangle 获取缓存广告失败")
        )

        val pangleAdView = PangleFullScreenNativeAdView()
        val success = pangleAdView.bindFullScreenNativeAdToContainer(
            context = this,
            container = container,
            nativeAd = nativeAd,
            lifecycleOwner = this,
            interactionListener = null
        )

        return if (success) {
            AdResult.Success(Unit)
        } else {
            AdResult.Failure(AdException(-1, "Pangle 绑定视图失败"))
        }
    }

    private fun showTopOnAd(container: ViewGroup): AdResult<Unit> {
        val controller = TopOnFullScreenNativeAdController.getInstance()
        if (!controller.hasValidCache()) {
            return AdResult.Failure(AdException(-1, "TopOn 无缓存广告"))
        }

        val nativeAd = controller.getCachedNativeAd() ?: return AdResult.Failure(
            AdException(-1, "TopOn 获取缓存广告失败")
        )

        // TopOn 原生广告渲染
        val toponAdView = ToponFullScreenNativeAdView()
        val success = toponAdView.bindFullScreenNativeAdToContainer(
            context = this,
            container = container,
            nativeAd = nativeAd,
            lifecycleOwner = this
        )

        return if (success) {
            AdResult.Success(Unit)
        } else {
            AdResult.Failure(AdException(-1, "TopOn 绑定视图失败"))
        }
    }

    private fun startCountdown() {
        tvCountdown.visibility = View.VISIBLE
        btnClose.visibility = View.GONE

        countDownTimer = object : CountDownTimer(COUNTDOWN_DURATION, COUNTDOWN_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000) + 1
                tvCountdown.text = "${seconds}s"
            }

            override fun onFinish() {
                tvCountdown.visibility = View.GONE
                btnClose.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun showLoading(show: Boolean) {
        loadingMask.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun triggerCloseEvent() {
        when (currentPlatform) {
            BiddingPlatform.ADMOB -> {
                AdsManager.Controllers.fullScreenNative.triggerCloseEvent()
            }
            BiddingPlatform.PANGLE -> {
                PangleFullScreenNativeAdController.getInstance().closeEvent()
            }
            BiddingPlatform.TOPON -> {
                // TopOn close event handled by SDK
            }
            null -> {}
        }
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        countDownTimer = null
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 倒计时结束前禁止返回
        if (btnClose.visibility == View.VISIBLE) {
            triggerCloseEvent()
            super.onBackPressed()
        }
    }
}
