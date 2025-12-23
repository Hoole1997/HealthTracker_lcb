package com.daily.health.manager.ui.weight

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

class CustomViewPager @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    ViewPager(context, attrs) {
    var isEnableScroll = false
    var isSmoothScroll = false

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        try {
            if (isEnableScroll) return super.onInterceptTouchEvent(motionEvent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return try {
            if (isEnableScroll) {
                super.onTouchEvent(motionEvent)
            } else false
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }

    override fun setCurrentItem(item: Int) {
        if (isSmoothScroll) {
            if (isEnableScroll) {
                super.setCurrentItem(item)
            } else {
                setCurrentItem(item, false)
            }
        } else {
            setCurrentItem(item, false)
        }
    }
}