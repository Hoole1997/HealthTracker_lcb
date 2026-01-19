package net.corekit.monetize.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import net.corekit.monetize.BuildConfig
import net.corekit.monetize.R
import net.corekit.monetize.ads.AdException
import net.corekit.monetize.ads.AdsManager
import net.corekit.monetize.ads.AdResult
import net.corekit.monetize.ads.FullNativeAds
import net.corekit.monetize.ads.log.AdLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 全屏原生广告Activity
 * 展示全屏的原生广告内容，通常用于应用启动或重要操作前
 */
class AdmobFullScreenNativeAdActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "FullScreenNativeAdActivity"

        /**
         * 启动全屏原生广告Activity
         * @param activity 启动Activity
         * @return AdResult<Unit> 广告显示结果
         */
        suspend fun start(activity: Activity,position:String, showInterstitial: Boolean = true): AdResult<Unit> {
            return suspendCancellableCoroutine { continuation ->

                val intent = Intent(activity, AdmobFullScreenNativeAdActivity::class.java)
                intent.putExtra("showInterstitial", showInterstitial)
                intent.putExtra("position", position)
                activity.startActivity(intent)
                activity.overridePendingTransition(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )

                // 存储continuation以便在Activity中调用
                AdmobFullScreenNativeAdActivity.continuation = continuation
            }
        }

        // 用于存储continuation的变量
        @Volatile
        private var continuation: kotlinx.coroutines.CancellableContinuation<AdResult<Unit>>? = null

        /**
         * 设置结果并恢复continuation
         */
        fun setResult(result: AdResult<Unit>) {
            continuation?.let { cont ->
                if (cont.isActive) {
                    cont.resume(result)
                }
            }
            continuation = null
        }
    }

    private val fullScreenNativeController = AdsManager.Controllers.fullScreenNative
    private val interstitialController = AdsManager.Controllers.interstitial
    private val isShowInterstitial: Boolean
        get() = intent.getBooleanExtra("showInterstitial", true)

    private val position: String
        get() = intent.getStringExtra("position") ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.apply {
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            @Suppress("DEPRECATION")
            navigationBarColor = Color.TRANSPARENT
        }
        setContentView(R.layout.activity_full_screen_native_ad)
        loadAndShowFullScreenNativeAd()
        if (isShowInterstitial) {
            showInterstitialAdAndNavigate {}
        }
    }

    /**
     * 加载并显示全屏原生广告
     */
    private fun loadAndShowFullScreenNativeAd() {
        lifecycleScope.launch {
            try {
                when (val result = fullScreenNativeController.displayAdInView(
                    context = this@AdmobFullScreenNativeAdActivity,
                    container = findViewById<ViewGroup>(R.id.ads_container),
                    lifecycleOwner = this@AdmobFullScreenNativeAdActivity,
                    position = position,
                    adUnitId = BuildConfig.ADMOB_FULL_NATIVE_ID
                )) {
                    is AdResult.Success -> {
                        findViewById<View>(R.id.ads_rl_top_buttons).apply {
                            isVisible = true
                            findViewById<View>(R.id.ads_btn_close).setOnClickListener {
                                FullNativeAds.getInstance().triggerCloseEvent(
                                    adUnitId = BuildConfig.ADMOB_FULL_NATIVE_ID,
                                    position = position
                                )
                                closeAdAndFinish()
                            }
                        }
                        AdLogger.d("全屏原生广告页面加载成功")
                        // 广告加载成功，展示页面，等待用户关闭时回调结果
                        // 不在这里设置结果，而是在页面关闭时设置
                    }

                    is AdResult.Failure -> {
                        // 广告加载失败，立即返回失败结果
                        setResult(result)
                        closeAdAndFinish()
                    }

                    AdResult.Loading -> {
                        // 广告正在加载中，等待结果
                        AdLogger.d("全屏原生广告正在加载中")
                    }
                }
            } catch (e: Exception) {
                // 异常情况，立即返回失败结果
                AdLogger.e("全屏原生广告页面加载失败:${e.message}")
                setResult(
                    AdResult.Failure(
                        AdException(
                            code = -2,
                            message = "全屏原生广告加载异常: ${e.message}",
                            cause = e
                        )
                    )
                )
                closeAdAndFinish()
            }
        }
    }

    private fun showInterstitialAdAndNavigate(call: () -> Unit) {
        lifecycleScope.launch {
            try {
                // 直接显示广告（自动处理加载）
                when (val result = interstitialController.displayAd(
                    this@AdmobFullScreenNativeAdActivity, position, BuildConfig.ADMOB_INTERSTITIAL_ID, ignoreFullNative = true
                )) {
                    is AdResult.Success -> {
                        call.invoke()
                    }

                    is AdResult.Failure -> {
                        call.invoke()
                    }

                    AdResult.Loading -> {

                    }
                }

            } catch (e: Exception) {

            }
        }
    }

    /**
     * 关闭广告并结束Activity
     */
    private fun closeAdAndFinish() {
        // 如果还没有设置结果（说明是用户主动关闭），设置成功结果
        if (continuation != null) {
            setResult(AdResult.Success(Unit))
        }
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 如果Activity被销毁但还没有设置结果，设置失败结果
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
        // 禁用返回键，只能通过广告关闭按钮关闭
    }
} 