package com.app.raise.base

import android.app.Dialog
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import com.app.raise.AppraiseManager.Companion.goToMarket
import com.app.raise.R
import com.app.raise.config.EvaluateConfig
import com.app.raise.helper.CheckHelper
import com.app.raise.listeners.EvaluateListener
import com.app.raise.widget.StarCheckView
import java.util.Locale

abstract class BaseAppraiseDialog {
    protected var evaluateStar1: StarCheckView? = null
    protected var evaluateStar2: StarCheckView? = null
    protected var evaluateStar3: StarCheckView? = null
    protected var evaluateStar4: StarCheckView? = null
    protected var evaluateStar5: StarCheckView? = null
    protected var evaluateResultTitle: TextView? = null
    protected var evaluateResultTip: TextView? = null
    protected var evaluateHandLayout: LinearLayout? = null
    protected var evaluateEmoji: LottieAnimationView? = null
    protected var btnEvaluate: TextView? = null
    private var dialog: Dialog? = null
    private var checkHelper: CheckHelper? = null
    protected var evaluateScore: Int = 0

    fun show(
        context: Context, evaluateConfig: EvaluateConfig, evaluateListener: EvaluateListener?
    ) {
        try {
            if (this.isEvaluateForbidden(context, evaluateConfig.allowIndonesia)) {
                return
            }

            evaluateListener?.sendEvent("Appraise_new", "Show", "")

            val list: MutableList<StarCheckView?> = mutableListOf()
            this.checkHelper = CheckHelper(list)
            this.dialog =
                this.getDialog(context, evaluateConfig, this.checkHelper!!, evaluateListener)

            dialog?.setCanceledOnTouchOutside(evaluateConfig.canceledOnTouchOutside)
            if (evaluateConfig.isRtl && !evaluateConfig.isSupportRTL) {
                list.add(this.evaluateStar5)
                list.add(this.evaluateStar4)
                list.add(this.evaluateStar3)
                list.add(this.evaluateStar2)
                list.add(this.evaluateStar1)
            } else {
                list.add(this.evaluateStar1)
                list.add(this.evaluateStar2)
                list.add(this.evaluateStar3)
                list.add(this.evaluateStar4)
                list.add(this.evaluateStar5)
            }

            dialog?.setOnCancelListener { listener ->
                if (evaluateListener != null) {
                    evaluateListener.cancelDialog(dialog!!)
                    evaluateListener.sendEvent("Appraise_new", "Show", "cancel")
                }
                listener.dismiss()
            }
            btnEvaluate?.setOnClickListener {
                dialog?.dismiss()
                if (this@BaseAppraiseDialog.evaluateScore > 4) {
                    if (evaluateConfig.star5GoMarket) {
                        goToMarket(context, evaluateConfig)
                    }
                    if (evaluateListener != null) {
                        evaluateListener.evaluateUs(this@BaseAppraiseDialog.evaluateScore)
                        evaluateListener.sendEvent(
                            "Appraise_new",
                            "Like",
                            "Review:" + this@BaseAppraiseDialog.evaluateScore
                        )
                    }

                    if (this@BaseAppraiseDialog.dialog != null && dialog?.isShowing == true) {
                        dialog?.dismiss()
                    }
                } else if (evaluateListener != null) {
                    evaluateListener.feedback(this@BaseAppraiseDialog.evaluateScore)
                    evaluateListener.sendEvent(
                        "Appraise_new", "UnLike", "Review:" + this@BaseAppraiseDialog.evaluateScore
                    )
                }
            }
            dialog?.setOnDismissListener { evaluateListener?.dismissDialog(dialog!!) }
        } catch (e: Exception) {
            evaluateListener?.sendException(e)
            e.printStackTrace()
        }
    }

    abstract fun getDialog(
        context: Context,
        evaluateConfig: EvaluateConfig,
        checkHelper: CheckHelper,
        evaluateListener: EvaluateListener?
    ): Dialog

    protected fun updateRateResultView(evaluateListener: EvaluateListener?) {
        var emojiId = R.raw.json_emoji_celebrate
        var rateTextId = R.string.btn_rate_on_google_play
        var rateResultTipId = R.string.lib_rate_dialog_tip
        if (this.evaluateScore != 0) {
            when (this.evaluateScore) {
                1 -> {
                    checkHelper?.setCheck(0)
                    emojiId = R.raw.json_emoji_cry
                    rateTextId = R.string.btn_feedback
                    rateResultTipId = R.string.evaluate_txt_1guide
                }

                2 -> {
                    checkHelper?.setCheck(1)
                    emojiId = R.raw.json_emoji_sad
                    rateTextId = R.string.btn_feedback
                    rateResultTipId = R.string.evaluate_txt_2guide
                }

                3 -> {
                    checkHelper?.setCheck(2)
                    emojiId = R.raw.json_emoji_sad
                    rateTextId = R.string.btn_feedback
                    rateResultTipId = R.string.evaluate_txt_3guide
                }

                4 -> {
                    checkHelper?.setCheck(3)
                    emojiId = R.raw.json_emoji_smile
                    rateTextId = R.string.btn_feedback
                    rateResultTipId = R.string.evaluate_txt_4guide
                }

                5 -> {
                    checkHelper?.setCheck(4)
                    emojiId = R.raw.json_emoji_celebrate
                    rateTextId = R.string.btn_rate_on_google_play
                    rateResultTipId = R.string.evaluate_txt_5guide
                }
            }

            this.iconImageAnimation(emojiId)
            evaluateResultTitle?.visibility = View.VISIBLE
            evaluateResultTip?.visibility = View.VISIBLE
            btnEvaluate?.visibility = View.VISIBLE
            evaluateHandLayout?.visibility = View.INVISIBLE
            btnEvaluate?.visibility = View.VISIBLE
            evaluateResultTip?.setText(rateResultTipId)
            btnEvaluate?.setText(rateTextId)
            btnEvaluate?.isEnabled = true
           /* if (this.evaluateScore == 5) {
                if (evaluateListener != null) {
                    evaluateListener.evaluateUs(this.evaluateScore)
                    evaluateListener.sendEvent(
                        "Appraise_new", "Like", "Review:" + this.evaluateScore
                    )
                }
            }*/
        } else {
            this.iconImageAnimation(emojiId)
            evaluateResultTip?.setText(rateResultTipId)
            btnEvaluate?.setText(rateTextId)
            evaluateHandLayout?.visibility = View.VISIBLE
            btnEvaluate?.visibility = View.INVISIBLE
            btnEvaluate?.isEnabled = false
        }
    }

    private fun iconImageAnimation(imageId: Int) {
        if (this.evaluateEmoji != null) {
            evaluateEmoji?.setAnimation(imageId)
        }
    }

    private fun isEvaluateForbidden(context: Context, allowIndonesia: Boolean): Boolean {
        if (allowIndonesia) {
            return false
        } else {
            val locale = Locale.getDefault()
            if (this.isIndonesia(locale)) {
                return true
            } else {
                val configuration = context.resources.configuration
                return configuration != null && this.isIndonesia(configuration.locale)
            }
        }
    }

    fun isEvaluateForbidden(context: Context): Boolean {
        return this.isEvaluateForbidden(context, false)
    }

    private fun isIndonesia(locale: Locale?): Boolean {
        try {
            if (locale != null) {
                val country = locale.country
                val language = locale.language
                if (!TextUtils.isEmpty(country) && country.equals("ID", ignoreCase = true)) {
                    return true
                }

                if (!TextUtils.isEmpty(language) && language.lowercase(Locale.getDefault())
                        .startsWith("in")
                ) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return false
    }

    protected inner class AppraiseClickListener(
        private var evaluateConfig: EvaluateConfig, private var evaluateListener: EvaluateListener?
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            val id = v.id
            if (evaluateConfig.isRtl && !evaluateConfig.isSupportRTL) {
                evaluateStar1?.setStarDrawableToDefault()
                when (id) {
                    R.id.evaluate_star_1 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 5) {
                            this@BaseAppraiseDialog.evaluateScore = 4
                            evaluateStar1?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 5
                            allCheck()
                        }
                    }

                    R.id.evaluate_star_2 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 4) {
                            this@BaseAppraiseDialog.evaluateScore = 3
                            evaluateStar2?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 4
                            checkOne(true)
                        }
                    }

                    R.id.evaluate_star_3 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 3) {
                            this@BaseAppraiseDialog.evaluateScore = 2
                            evaluateStar3?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 3
                            checkTwo(true)
                        }
                    }

                    R.id.evaluate_star_4 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 2) {
                            this@BaseAppraiseDialog.evaluateScore = 1
                            evaluateStar4?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 2
                            checkThree(true)
                        }
                    }

                    R.id.evaluate_star_5 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 1) {
                            this@BaseAppraiseDialog.evaluateScore = 0
                            evaluateStar5?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 1
                            checkFour(true)
                        }
                    }
                }
            } else {
                evaluateStar5?.setStarDrawableToDefault()
                when (id) {
                    R.id.evaluate_star_1 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 1) {
                            this@BaseAppraiseDialog.evaluateScore = 0
                            evaluateStar1?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 1
                            checkOne(false)
                        }
                    }

                    R.id.evaluate_star_2 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 2) {
                            this@BaseAppraiseDialog.evaluateScore = 1
                            evaluateStar2?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 2
                            checkTwo(false)
                        }
                    }

                    R.id.evaluate_star_3 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 3) {
                            this@BaseAppraiseDialog.evaluateScore = 2
                            evaluateStar3?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 3
                            checkThree(false)
                        }
                    }

                    R.id.evaluate_star_4 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 4) {
                            this@BaseAppraiseDialog.evaluateScore = 3
                            evaluateStar4?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 4
                            checkFour(false)
                        }
                    }

                    R.id.evaluate_star_5 -> {
                        if (this@BaseAppraiseDialog.evaluateScore == 5) {
                            this@BaseAppraiseDialog.evaluateScore = 4
                            evaluateStar5?.setCheck(false)
                        } else {
                            this@BaseAppraiseDialog.evaluateScore = 5
                            allCheck()
                        }
                    }
                }
            }
            this@BaseAppraiseDialog.updateRateResultView(this.evaluateListener)
        }
    }


    private fun checkOne(isRtl: Boolean) {
        evaluateStar1?.setCheck(isRtl.not())
        evaluateStar2?.setCheck(isRtl)
        evaluateStar3?.setCheck(isRtl)
        evaluateStar4?.setCheck(isRtl)
        evaluateStar5?.setCheck(isRtl)
    }


    private fun checkTwo(isRtl: Boolean) {
        evaluateStar1?.setCheck(isRtl.not())
        evaluateStar2?.setCheck(isRtl.not())
        evaluateStar3?.setCheck(isRtl)
        evaluateStar4?.setCheck(isRtl)
        evaluateStar5?.setCheck(isRtl)
    }

    private fun checkThree(isRtl: Boolean) {
        evaluateStar1?.setCheck(isRtl.not())
        evaluateStar2?.setCheck(isRtl.not())
        evaluateStar3?.setCheck(isRtl.not())
        evaluateStar4?.setCheck(isRtl)
        evaluateStar5?.setCheck(isRtl)
    }

    private fun checkFour(isRtl: Boolean) {
        evaluateStar1?.setCheck(isRtl.not())
        evaluateStar2?.setCheck(isRtl.not())
        evaluateStar3?.setCheck(isRtl.not())
        evaluateStar4?.setCheck(isRtl.not())
        evaluateStar5?.setCheck(isRtl)
    }

    private fun allCheck() {
        evaluateStar1?.setCheck(true)
        evaluateStar2?.setCheck(true)
        evaluateStar3?.setCheck(true)
        evaluateStar4?.setCheck(true)
        evaluateStar5?.setCheck(true)
    }
}