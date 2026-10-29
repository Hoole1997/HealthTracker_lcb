package com.daily.health.manager.face.launch

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.ContextThemeWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.daily.health.manager.constants.KEY_HAS_GUIDE_NEW
import com.daily.health.manager.face.act.GuideAct
import com.daily.health.manager.face.act.MainAct
import com.healthtracker.framework.util.SpUtils
import com.leafmotivation.quizguessoncolor.unityact.LocaleInspectActivity
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 验证真实 SDK 中转链、重复进入，以及 windowNoDisplay 路由的失败退出行为。 */
@RunWith(AndroidJUnit4::class)
class LaunchRoutingTest {

    @Test
    fun firstEntryOpensGuideOnceAndFinishesRouters() {
        verifyRoute(guideCompleted = false, targetClass = GuideAct::class.java)
    }

    @Test
    fun completedGuideOpensMainOnceAndFinishesRouters() {
        verifyRoute(guideCompleted = true, targetClass = MainAct::class.java)
    }

    @Test
    fun reentryDoesNotRecreateExistingMain() {
        verifyRoute(guideCompleted = true, targetClass = MainAct::class.java, repeatEntry = true)
    }

    @Test
    fun missingDestinationFinishesRouterWithoutCrashing() {
        verifyRoute(false, GuideAct::class.java, launchFailure = ActivityNotFoundException("test destination missing"))
    }

    @Test
    fun deniedDestinationFinishesRouterWithoutCrashing() {
        verifyRoute(false, GuideAct::class.java, launchFailure = SecurityException("test launch denied"))
    }

    @Test
    fun sdkProxyThemeAllowsItsVisibleFallback() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val activityInfo = context.packageManager.getActivityInfo(ComponentName(context, LocaleInspectActivity::class.java), 0)
        val themeContext = ContextThemeWrapper(context, activityInfo.themeResource)
        val attributes = themeContext.obtainStyledAttributes(intArrayOf(
            android.R.attr.windowNoDisplay,
            android.R.attr.windowDisablePreview,
            android.R.attr.windowIsTranslucent,
        ))
        try {
            // SDK 的兜底页面可能正常停留，不应继承无窗口 Activity 的强制 finish 约束。
            assertFalse(attributes.getBoolean(0, false))
            assertTrue(attributes.getBoolean(1, false))
            assertFalse(attributes.getBoolean(2, false))
        } finally {
            attributes.recycle()
        }
    }

    private fun verifyRoute(
        guideCompleted: Boolean,
        targetClass: Class<out Activity>,
        repeatEntry: Boolean = false,
        launchFailure: Exception? = null,
    ) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val originalGuideCompleted = SpUtils.getBoolean(KEY_HAS_GUIDE_NEW, false)
        val routeCompleted = CountDownLatch(1)
        val repeatedRouteCompleted = CountDownLatch(1)
        val createdActivities = CopyOnWriteArrayList<Activity>()
        val failureInjected = AtomicBoolean(false)
        // 通过系统测试监视器注入真实 startActivity 异常，不在生产路由中添加测试分支。
        val monitor = launchFailure?.let { failure ->
            object : Instrumentation.ActivityMonitor() {
                override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                    if (intent.component?.className == targetClass.name) {
                        failureInjected.set(true)
                        throw failure
                    }
                    return null
                }
            }
        }
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) {
                createdActivities.add(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                if (targetClass.isInstance(activity)) routeCompleted.countDown()
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) {
                if (activity is LaunchGateActivity) {
                    if (launchFailure != null) routeCompleted.countDown()
                    if (createdActivities.count { it is LaunchGateActivity } == 2) {
                        repeatedRouteCompleted.countDown()
                    }
                }
            }
        }

        try {
            if (monitor != null) instrumentation.addMonitor(monitor)
            instrumentation.runOnMainSync {
                SpUtils.putBoolean(KEY_HAS_GUIDE_NEW, guideCompleted)
                application.registerActivityLifecycleCallbacks(callbacks)
                application.startActivity(Intent(application, LocaleInspectActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }

            assertTrue("启动路由未完成", routeCompleted.await(15, TimeUnit.SECONDS))
            instrumentation.waitForIdleSync()

            if (repeatEntry) {
                instrumentation.runOnMainSync {
                    application.startActivity(Intent(application, LocaleInspectActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
                assertTrue("重复进入的路由未结束", repeatedRouteCompleted.await(15, TimeUnit.SECONDS))
                instrumentation.waitForIdleSync()
            }

            instrumentation.runOnMainSync {
                assertEquals(launchFailure != null, failureInjected.get())
                assertEquals(if (launchFailure == null) 1 else 0, createdActivities.count { targetClass.isInstance(it) })
                val routerClasses = setOf(LocaleInspectActivity::class.java, LaunchGateActivity::class.java)
                routerClasses.forEach { routerClass ->
                    val routers = createdActivities.filter { routerClass.isInstance(it) }
                    assertEquals("中转页创建次数异常：${routerClass.simpleName}", if (repeatEntry) 2 else 1, routers.size)
                    routers.forEach { router ->
                        val attributes = router.obtainStyledAttributes(intArrayOf(android.R.attr.windowNoDisplay))
                        try {
                            assertEquals("仅应用自己的路由使用无窗口主题", router is LaunchGateActivity, attributes.getBoolean(0, false))
                        } finally {
                            attributes.recycle()
                        }
                        assertTrue("中转页未结束：${routerClass.simpleName}", router.isFinishing || router.isDestroyed)
                        assertFalse("中转页不应残留可见窗口", router.window.peekDecorView()?.isAttachedToWindow == true)
                    }
                }
            }
        } finally {
            if (monitor != null) instrumentation.removeMonitor(monitor)
            // 仅切换本次测试需要的引导标记；保留设备上的业务数据和 SDK 引导设置。
            instrumentation.runOnMainSync {
                application.unregisterActivityLifecycleCallbacks(callbacks)
                createdActivities.asReversed().forEach { activity ->
                    if (!activity.isFinishing && !activity.isDestroyed) activity.finish()
                }
                SpUtils.putBoolean(KEY_HAS_GUIDE_NEW, originalGuideCompleted)
            }
            instrumentation.waitForIdleSync()
        }
    }
}
