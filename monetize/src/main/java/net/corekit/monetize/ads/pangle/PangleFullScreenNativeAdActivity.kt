package net.corekit.monetize.ads.pangle

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
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
import net.corekit.monetize.ads.ext.AdShowExt
import net.corekit.monetize.ads.log.AdLogger

/**
 * Pangle全屏原生广告展示页
 */
class PangleFullScreenNativeAdActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_SHOW_INTERSTITIAL = "showInterstitial"
        private const val EXTRA_POSITION = "position"

        suspend fun start(activity: Activity, position: String, showInterstitial: Boolean = true): AdResult<Unit> {
            return suspendCancellableCoroutine { continuation ->
                val intent = Intent(activity, PangleFullScreenNativeAdActivity::class.java)
                intent.putExtra(EXTRA_SHOW_INTERSTITIAL, showInterstitial)
                intent.putExtra(EXTRA_POSITION, position)
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

    private val fullScreenNativeController = PangleFullScreenNativeAdController.getInstance()
    private val interstitialController = PangleInterstitialAdController.getInstance()

    private val shouldShowInterstitial: Boolean
        get() = intent.getBooleanExtra(EXTRA_SHOW_INTERSTITIAL, true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            @Suppress("DEPRECATION")
            navigationBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_pangle_full_screen_native_ad)
        loadAndShowFullScreenNativeAd()
        if (shouldShowInterstitial) {
            showInterstitialAd {}
        }
    }

    private fun loadAndShowFullScreenNativeAd() {
        lifecycleScope.launch {
            try {
                when (val result = fullScreenNativeController.showAdInContainer(
                    context = this@PangleFullScreenNativeAdActivity,
                    container = findViewById(R.id.adContainer),
                    lifecycleOwner = this@PangleFullScreenNativeAdActivity,
                    adUnitId = BuildConfig.PANGLE_FULL_NATIVE_ID
                )) {
                    is AdResult.Success -> {
                        findViewById<View>(R.id.rl_top_buttons)?.apply {
                            isVisible = true
                            findViewById<View>(R.id.btn_close)?.setOnClickListener {
                                PangleFullScreenNativeAdController.getInstance().closeEvent(adUnitId = BuildConfig.PANGLE_FULL_NATIVE_ID)
                                closeAdAndFinish()
                            }
                        }
                        AdLogger.d("Pangle全屏原生广告展示成功")
                    }

                    is AdResult.Failure -> {
                        setResult(result)
                        closeAdAndFinish()
                    }

                    AdResult.Loading -> {
                        AdLogger.d("Pangle全屏原生广告加载中")
                    }
                }
            } catch (e: Exception) {
                AdLogger.e("Pangle全屏原生广告展示异常:${e.message}")
                setResult(
                    AdResult.Failure(
                        AdException(
                            code = -2,
                            message = "Pangle全屏原生广告加载异常: ${e.message}",
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
                when (val result =  AdShowExt.showInterstitialAd(
                    activity = this@PangleFullScreenNativeAdActivity,
                    ignoreFullNative = true
                )) {
                    is AdResult.Success, is AdResult.Failure -> onFinished()
                    AdResult.Loading -> Unit
                }
                onFinished()
            } catch (e: Exception) {
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

    override fun onBackPressed() {
        // 禁用返回键
    }
}
