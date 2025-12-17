package com.app.raise

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.annotation.StringRes
import com.app.raise.config.EvaluateConfig
import com.app.raise.listeners.EvaluateListener
import java.util.Locale

class AppraiseManager(private val context: Context, star5GoMarket: Boolean = false) {

    private var evaluateConfig: EvaluateConfig = EvaluateConfig()

    init {
        evaluateConfig.isRtl = this.isRtl(context)
        evaluateConfig.isSupportRTL = this.isSupportRTL(context)
        evaluateConfig.star5GoMarket = star5GoMarket
        evaluateConfig.marketUrl =
            "https://play.google.com/store/apps/details?id=" + context.packageName
        evaluateConfig.marketPackage = MARKET_GOOGLE
    }

    fun setMarketUrl(marketUrl: String, marketPackage: String) {
        if (!TextUtils.isEmpty(marketUrl)) {
            evaluateConfig.marketUrl = marketUrl
        }

        if (!TextUtils.isEmpty(marketPackage)) {
            evaluateConfig.marketPackage = marketPackage
        }
    }

    fun setBtnRateText(@StringRes rateStringRes: Int) {
        evaluateConfig.evaluateStringRes = rateStringRes
    }

    fun setCanceledOnTouchOutside(canceledOnTouchOutside: Boolean) {
        evaluateConfig.canceledOnTouchOutside = canceledOnTouchOutside
    }

    fun allowIndonesia(allow: Boolean) {
        evaluateConfig.allowIndonesia = allow
    }

    fun showAppraiseDialog(evaluateListener: EvaluateListener): AppraiseDialog {
        val appraiseDialog = AppraiseDialog()
        appraiseDialog.show(context, this.evaluateConfig, evaluateListener)
        return appraiseDialog
    }

    private fun isSupportRTL(context: Context): Boolean {
        val applicationInfo = context.applicationInfo
        val hasRtlSupport = (applicationInfo.flags and 4194304) == 4194304
        return hasRtlSupport
    }

    private fun isRtl(context: Context): Boolean {
        try {
            val locale = context.resources.configuration.locale
            val language = locale.language.lowercase(Locale.getDefault())
            if (language == "ar" || language == "iw" || language == "fa" || language == "ur") {
                return true
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return false
    }

    companion object {

        const val MARKET_GOOGLE: String = "com.android.vending"

        @JvmStatic
        fun goToMarket(context: Context, evaluateConfig: EvaluateConfig) {
            try {
                val intent =
                    Intent("android.intent.action.VIEW", Uri.parse(evaluateConfig.marketUrl))
                if (!TextUtils.isEmpty(evaluateConfig.marketPackage)) {
                    intent.setPackage(evaluateConfig.marketPackage)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    val intent =
                        Intent("android.intent.action.VIEW", Uri.parse(evaluateConfig.marketUrl))
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
