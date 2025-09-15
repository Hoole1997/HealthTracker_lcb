package com.healthtracker.framework

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.Window
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.blankj.utilcode.util.BarUtils
import com.healthtracker.framework.BarUtils.setStatusBarColor
import kotlin.also
import kotlin.apply
import kotlin.let

/**
 * 需要在attachedWindow之后调用，否则可能会出错
 */
object SysBarUtils {
    private var statusBarHeight = 0
    private var navigationBarHeight = 0

    private var isInit = true

    /**
     * 需要在attachedWindow之后调用，用于初始化系统栏高度
     */
    fun initBarHeight(activity: Activity) {
        if (!isInit) {
            return
        }
        isInit = false
        getStatusBarHeight(activity)
    }

    /**
     * 获取状态栏高度，首次获取需要在attachWindow之后调用
     */
    fun getStatusBarHeight(context: Context): Int {
        getActivity(context)?.let { act ->
            try {
                val rootWindowInsets = ViewCompat.getRootWindowInsets(act.window.decorView)
                if (rootWindowInsets != null) {
                    return rootWindowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars()).top.also { statusBarHeight = it }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return BarUtils.getStatusBarHeight() //不赋值给statusBarHeight，这个方法不准
    }


    /**
     * 获取状态栏高度，首次获取需要在attachWindow之后调用
     */
    fun getNavigationBarHeight(context: Context): Int {
        if (navigationBarHeight > 0) {
            return navigationBarHeight
        }

        getActivity(context)?.let { act ->
            try {
                val rootWindowInsets = ViewCompat.getRootWindowInsets(act.window.decorView)
                if (rootWindowInsets != null) {
                    return rootWindowInsets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.navigationBars()).bottom.also { navigationBarHeight = it }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return BarUtils.getNavBarHeight()
    }

    private fun getActivity(context: Context?): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }


    fun hideNavigationBar(activity: Activity) {
        hideNavigationBar(activity.window)
    }

    fun hideNavigationBar(window: Window) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
    }


    fun hideStateBar(activity: Activity) {
        hideStateBar(activity.window)
    }

    fun hideStateBar(window: Window) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }


    /**
     * 设置状态栏颜色


     */
    fun setStatusBarColor(activity : Activity,colorRes:Int) {
        val window = activity.window
        val color = ContextCompat.getColor(activity, colorRes)

        val windowInsetsControllerCompat  = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsControllerCompat.apply {
            isAppearanceLightNavigationBars = isLightColor(color)
            isAppearanceLightStatusBars = isLightColor(color)
            setStatusBarColor(window, color,true)
        }
    }

    /**
     * 判断颜色是否为浅色
     */
    private fun isLightColor(@ColorInt color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // 使用HSP颜色空间计算亮度
        val brightness = (red * 0.299 + green * 0.587 + blue * 0.114)
        return brightness > 127.5
    }

}
