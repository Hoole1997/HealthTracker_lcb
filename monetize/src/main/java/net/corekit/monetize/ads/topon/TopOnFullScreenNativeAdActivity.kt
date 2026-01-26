package net.corekit.monetize.ads.topon

import ads_mobile_sdk.po
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.R
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.bidding.BiddingPlatform
import net.corekit.monetize.ads.log.AdLogger
import net.corekit.monetize.ads.pangle.PangleInterstitialAdController

/**
 * TopOn全屏原生广告展示页
 */
class TopOnFullScreenNativeAdActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SHOW_INTERSTITIAL = "showInterstitial"
        private const val EXTRA_POSITION = "position"
        private const val EXTRA_INTERSTITIAL_PLATFORM = "interstitialPlatform"

        suspend fun start(
            activity: Activity, 
            position: String, 
            showInterstitial: Boolean = true,
            interstitialPlatform: BiddingPlatform? = null
        ): AdResult<Unit> {
            return suspendCancellableCoroutine { continuation ->
                val intent = Intent(activity, TopOnFullScreenNativeAdActivity::class.java)
                intent.putExtra(EXTRA_SHOW_INTERSTITIAL, showInterstitial)
                intent.putExtra(EXTRA_POSITION, position)
                interstitialPlatform?.let { intent.putExtra(EXTRA_INTERSTITIAL_PLATFORM, it.name) }
                activity.startActivity(intent)
                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                this.continuation = continuation
            }
        }

        @Volatile
        private var continuation: CancellableContinuation<AdResult<Unit>>? = null

        fun setResult(result: AdResult<Unit>) {
            continuation?.let { cont ->
                if (cont.isActive) {
                    cont.resume(result, null)
                }
            }
            continuation = null
        }
    }

    private val fullScreenNativeController = TopOnFullScreenNativeAdController.getInstance()

    private val shouldShowInterstitial: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHOW_INTERSTITIAL, true)

    private val position: String
        get() = intent.getStringExtra(EXTRA_POSITION) ?: ""

    private val interstitialPlatform: BiddingPlatform?
        get() = intent.getStringExtra(EXTRA_INTERSTITIAL_PLATFORM)?.let { 
            try { BiddingPlatform.valueOf(it) } catch (e: Exception) { null }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            @Suppress("DEPRECATION")
            navigationBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_topon_full_screen_native_ad)
        loadAndShowFullScreenNativeAd()
        if (shouldShowInterstitial) {
            showInterstitialAd {}
        }
    }

    private fun loadAndShowFullScreenNativeAd() {
        lifecycleScope.launch {
            try {
                when (val result = fullScreenNativeController.showAdInContainer(
                    context = this@TopOnFullScreenNativeAdActivity,
                    container = findViewById(R.id.adContainer),
                    lifecycleOwner = this@TopOnFullScreenNativeAdActivity,
                    position,
                    placementId = BuildConfig.TOPON_FULL_NATIVE_ID
                )) {
                    is AdResult.Success -> {
                        findViewById<View>(R.id.rl_top_buttons)?.apply {
                            isVisible = true
                            findViewById<View>(R.id.btn_close)?.setOnClickListener {
                                TopOnFullScreenNativeAdController.getInstance().closeEvent(placementId = BuildConfig.TOPON_FULL_NATIVE_ID,position = position)
                                closeAdAndFinish()
                            }
                        }
                        AdLogger.d("TopOn全屏原生广告展示成功")
                    }

                    is AdResult.Failure -> {
                        setResult(result)
                        closeAdAndFinish()
                    }

                    AdResult.Loading -> {
                        AdLogger.d("TopOn全屏原生广告加载中")
                    }
                }
            } catch (e: Exception) {
                AdLogger.e("TopOn全屏原生广告展示异常:${e.message}")
                setResult(
                    AdResult.Failure(
                        AdException(
                            code = -2,
                            message = "TopOn全屏原生广告加载异常: ${e.message}",
                            cause = e
                        )
                    )
                )
                closeAdAndFinish()
            }
        }
    }

    private fun showInterstitialAd(onFinished: () -> Unit) {
        lifecycleScope.launch {
            try {
                // 根据传入的平台直接展示对应插页广告，避免重复竞价
                val result = when (interstitialPlatform) {
                    BiddingPlatform.ADMOB -> {
                        AdsManager.Controllers.interstitial.displayAd(
                            this@TopOnFullScreenNativeAdActivity, 
                            position, 
                            ignoreFullNative = true
                        )
                    }
                    BiddingPlatform.PANGLE -> {
                        PangleInterstitialAdController.getInstance().showAd(
                            this@TopOnFullScreenNativeAdActivity, 
                            null, 
                            position
                        )
                    }
                    BiddingPlatform.TOPON -> {
                        TopOnInterstitialAdController.getInstance().showAd(
                            this@TopOnFullScreenNativeAdActivity, 
                            null, 
                            position
                        )
                    }
                    null -> {
                        // 未指定平台时使用同平台插页（TopOn）
                        TopOnInterstitialAdController.getInstance().showAd(
                            this@TopOnFullScreenNativeAdActivity, 
                            null, 
                            position
                        )
                    }
                }
                when (result) {
                    is AdResult.Success, is AdResult.Failure -> onFinished()
                    AdResult.Loading -> Unit
                }
            } catch (e: Exception) {
                AdLogger.e("展示插页广告异常: ${e.message}")
                onFinished()
            }
        }
    }

    private fun closeAdAndFinish() {
        if (continuation != null) {
            setResult(AdResult.Success(Unit))
        }
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (continuation != null) {
            setResult(
                AdResult.Failure(
                    AdException(
                        code = -3,
                        message = "Activity被销毁"
                    )
                )
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 禁用返回键
    }


}
