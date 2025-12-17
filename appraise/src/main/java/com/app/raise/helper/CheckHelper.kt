package com.app.raise.helper

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.app.raise.widget.StarCheckView

class CheckHelper(private val starCheckViews: MutableList<StarCheckView?>?) {
    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                1 -> this@CheckHelper.startItemAnimation(msg.arg1, msg.arg2, msg.obj as Boolean)
                else -> {}
            }
        }
    }
    var rate: Int = -1
        private set
    private var animationEndListener: AnimationEndListener? = null
    private var isClickAfterFirstClick = false
    private var objectAnimator: ObjectAnimator? = null

    fun startFirst() {
        if (this.starCheckViews != null && this.rate < 0) {
            this.isClickAfterFirstClick = false
            this.cleanListCheck()
            this.startItemAnimation(0, starCheckViews.size - 1, true)
        }
    }

    fun setAnimationEndListener(listener: AnimationEndListener?) {
        this.animationEndListener = listener
    }

    fun setCheck(position: Int): Boolean {
        if (this.rate == position) {
            return false
        } else {
            this.rate = position
            handler.removeMessages(1)
            this.isClickAfterFirstClick = true
            if (this.objectAnimator != null) {
                objectAnimator!!.end()
            }

            for (i in starCheckViews!!.indices) {
                starCheckViews[i]?.setCheck(i <= position, false)
            }

            if (this.animationEndListener != null) {
                animationEndListener!!.onEnd(this.rate)
            }

            return true
        }
    }

    private fun startItemAnimation(position: Int, resPosition: Int, isFirst: Boolean) {
        if (resPosition >= position && this.starCheckViews != null && starCheckViews.size > position && position >= 0) {
            val starCheckView =starCheckViews[position]
            starCheckView?.setPosition(position)
            starCheckView?.setCheck(true, true)
            val message = Message()
            message.what = 1
            message.arg1 = position + 1
            message.arg2 = resPosition
            message.obj = isFirst
            handler.sendMessageDelayed(message, 160L)
        } else {
            this.endAnimation(isFirst)
        }
    }

    private fun endAnimation(isFirst: Boolean) {
        if (this.starCheckViews != null && starCheckViews.isNotEmpty()) {
            val starCheckView =starCheckViews[starCheckViews.size - 1]
            starCheckView?.setOnAnimationEnd(object : StarCheckView.AnimationEndListener {
                override fun onAnimationEnd(animation: Animator?) {
                    if (this@CheckHelper.animationEndListener != null) {
                        animationEndListener!!.onEnd(
                            rate
                        )
                    }

                    if (isFirst && !this@CheckHelper.isClickAfterFirstClick) {
                        this@CheckHelper.cleanListCheck()
                        this@CheckHelper.objectAnimator = ObjectAnimator.ofFloat(
                            starCheckView,
                            "rotation",
                            *floatArrayOf(20.0f, -20.0f, 20.0f, -20.0f, 0.0f)
                        )
                        objectAnimator?.setDuration(2000L)
                        objectAnimator?.addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                this@CheckHelper.objectAnimator = null
                            }
                        })
                        objectAnimator?.start()
                    }
                }
            })
        }
    }

    private fun cleanListCheck() {
        if (this.starCheckViews != null) {
            val var1: Iterator<*> = starCheckViews.iterator()

            while (var1.hasNext()) {
                val starCheckView = var1.next() as StarCheckView?
                starCheckView?.setCheck(false)
            }
        }
    }

    interface AnimationEndListener {
        fun onEnd(rate: Int)
    }

}