package com.daily.health.manager.helper

import android.app.Dialog
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.app.raise.AppraiseManager
import com.app.raise.config.EvaluateConfig
import com.app.raise.listeners.EvaluateListener
import com.daily.health.manager.face.act.FeedbackScreen
import com.google.android.play.core.review.ReviewManagerFactory
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager

/**
 * Health Tracker 应用评分监听器
 * 
 * 实现 EvaluateListener 接口，处理用户评分行为：
 * - 4-5星：使用 Google Play In-App Review API
 * - 1-3星：跳转到 FeedbackScreen 收集用户反馈
 */
class HealthTrackerEvaluateListener(
    private val activity: FragmentActivity,
    private val source: String = "unknown",
    private val onRated: (() -> Unit)? = null
) : EvaluateListener {

    companion object {
        /** 用户是否已评分的存储 Key */
        const val KEY_HAS_RATED = "has_rated"
        /** 新手引导后待评分标记的存储 Key */
        const val KEY_PENDING_RATE_AFTER_ONBOARDING = "pending_rate_after_onboarding"
    }

    /**
     * 用户点击评分按钮（4-5星）
     * 使用 In-App Review API 请求用户评分
     */
    override fun evaluateUs(evaluateScore: Int) {
        ReportDataManager.reportData(
            "rate_us_submit",
            mapOf("score" to evaluateScore, "source" to source)
        )

        if (evaluateScore >= 4) {
            // 使用 In-App Review API
            try {
                val reviewManager = ReviewManagerFactory.create(activity)
                val requestFlow = reviewManager.requestReviewFlow()
                requestFlow.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val reviewInfo = task.result
                        reviewManager.launchReviewFlow(activity, reviewInfo)
                            .addOnCompleteListener {
                                // 标记已评分
                                markAsRated()
                            }
                    } else {
                        // In-App Review 失败，回退到 Play Store
                        fallbackToPlayStore()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fallbackToPlayStore()
            }
        }
        
        // 标记已评分
        markAsRated()
    }

    /**
     * 用户点击反馈按钮（1-3星）
     * 跳转到反馈页面
     */
    override fun feedback(evaluateScore: Int) {
        ReportDataManager.reportData(
            "rate_us_submit",
            mapOf("score" to evaluateScore, "source" to source, "action" to "feedback")
        )
        activity.startActivity(Intent(activity, FeedbackScreen::class.java))
        // 标记已评分
        markAsRated()
    }

    /**
     * 用户取消弹窗
     */
    override fun cancelDialog(dialog: Dialog) {
        ReportDataManager.reportData("rate_us_dismiss", mapOf("source" to source))
    }

    /**
     * 弹窗消失
     */
    override fun dismissDialog(dialog: Dialog) {
        // 弹窗消失时的处理
    }

    /**
     * 发送事件埋点
     */
    override fun sendEvent(category: String?, action: String?, label: String?) {
        val eventName = "${category ?: "appraise"}_${action ?: "unknown"}"
        ReportDataManager.reportData(eventName, mapOf("label" to (label ?: "")))
    }

    /**
     * 发送异常信息
     */
    override fun sendException(throwable: Throwable?) {
        throwable?.printStackTrace()
    }

    /**
     * 标记用户已评分
     */
    private fun markAsRated() {
        SpUtils.putBoolean(KEY_HAS_RATED, true)
        // 通知调用方评分已完成
        onRated?.invoke()
    }

    /**
     * 回退到 Play Store
     */
    private fun fallbackToPlayStore() {
        try {
            AppraiseManager.goToMarket(activity, EvaluateConfig())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 检查用户是否已评分
     */
    fun hasRated(): Boolean {
        return SpUtils.getBoolean(KEY_HAS_RATED, false)
    }
}
