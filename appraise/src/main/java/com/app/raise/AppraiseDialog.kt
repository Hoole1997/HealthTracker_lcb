package com.app.raise

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import com.app.raise.base.BaseAppraiseDialog
import com.app.raise.base.BaseDialog
import com.app.raise.config.EvaluateConfig
import com.app.raise.helper.CheckHelper
import com.app.raise.listeners.EvaluateListener
import java.util.Locale

class AppraiseDialog(@StyleRes private val theme: Int = com.healthtracker.framework.R.style.BottomSheetDialog) :
    BaseAppraiseDialog() {

    var dialog: Dialog? = null

    override fun getDialog(
        context: Context,
        evaluateConfig: EvaluateConfig,
        checkHelper: CheckHelper,
        evaluateListener: EvaluateListener?
    ): Dialog {

        dialog = BaseDialog(context, theme)
        val view: View?
        if (evaluateConfig.isRtl && !evaluateConfig.isSupportRTL) {
            view = LayoutInflater.from(context)
                .inflate(R.layout.lib_appraise_dialog_rtl, null as ViewGroup?)
        } else {
            view =
                LayoutInflater.from(context)
                    .inflate(R.layout.lib_appraise_dialog, null as ViewGroup?)
            if (evaluateConfig.isRtl) {
                (view.findViewById<View>(R.id.rate_hand) as ImageView).scaleX = -1.0f
                view.findViewById<View>(R.id.evaluate_star_5).scaleX = -1.0f
            }
        }

        val mainLayout = view.findViewById<View>(R.id.main_layout) as ConstraintLayout
        if (evaluateConfig.canceledOnTouchOutside) {
            dialog?.setCanceledOnTouchOutside(true)
            view.setOnClickListener(View.OnClickListener {
                if (dialog?.isShowing == true) {
                    dialog?.dismiss()
                }
            })
            mainLayout.isClickable = true
        }

        this.evaluateEmoji = view.findViewById<View>(R.id.rate_emoji) as LottieAnimationView
        this.btnEvaluate = view.findViewById<View>(R.id.lib_rate_button) as TextView
        this.evaluateResultTitle = view.findViewById<View>(R.id.rate_result_title) as TextView
        this.evaluateResultTip = view.findViewById<View>(R.id.rate_result_tip) as TextView
        btnEvaluate?.isEnabled = false
        btnEvaluate?.text =
            context.getString(evaluateConfig.evaluateStringRes).uppercase(Locale.getDefault())
        this.evaluateStar1 = view.findViewById(R.id.evaluate_star_1)
        this.evaluateStar2 = view.findViewById(R.id.evaluate_star_2)
        this.evaluateStar3 = view.findViewById(R.id.evaluate_star_3)
        this.evaluateStar4 = view.findViewById(R.id.evaluate_star_4)
        this.evaluateStar5 = view.findViewById(R.id.evaluate_star_5)
        this.evaluateHandLayout = view.findViewById(R.id.rate_hand_layout)

        val appraiseClickListener = AppraiseClickListener(evaluateConfig, evaluateListener)
        evaluateStar1?.setOnClickListener(appraiseClickListener)
        evaluateStar2?.setOnClickListener(appraiseClickListener)
        evaluateStar3?.setOnClickListener(appraiseClickListener)
        evaluateStar4?.setOnClickListener(appraiseClickListener)
        evaluateStar5?.setOnClickListener(appraiseClickListener)
        dialog?.window?.requestFeature(1)
        dialog?.setContentView(view)
        dialog?.show()
        val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val originalBottomSheetPaddingLeft = bottomSheet.paddingLeft
            val originalBottomSheetPaddingTop = bottomSheet.paddingTop
            val originalBottomSheetPaddingRight = bottomSheet.paddingRight
            val originalBottomSheetPaddingBottom = bottomSheet.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet) { v, insets ->
                val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                v.setPadding(
                    originalBottomSheetPaddingLeft,
                    originalBottomSheetPaddingTop,
                    originalBottomSheetPaddingRight,
                    originalBottomSheetPaddingBottom + navBars.bottom
                )
                insets
            }
            bottomSheet.post { ViewCompat.requestApplyInsets(bottomSheet) }
        }
        val decorView = dialog?.window?.decorView
        decorView?.post { ViewCompat.requestApplyInsets(decorView) }
        dialog?.window?.setBackgroundDrawable(ColorDrawable(0))
        dialog?.window?.setLayout(-1, -1)
        dialog?.window?.let {
            com.healthtracker.framework.BarUtils.setNavBarColor(it, android.graphics.Color.WHITE)
            com.healthtracker.framework.BarUtils.setNavBarLightMode(it, true)
        }
        view.postDelayed(Runnable { checkHelper.startFirst() }, 1200L)
        return dialog as BaseDialog
    }

    fun getAppraiseDialog(): Dialog? {
        return dialog
    }
}