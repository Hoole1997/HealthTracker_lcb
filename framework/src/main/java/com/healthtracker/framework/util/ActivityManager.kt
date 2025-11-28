package com.healthtracker.framework.util

import android.app.Activity
import com.healthtracker.framework.ext.logd
import java.lang.ref.WeakReference
import java.util.Stack
import kotlin.system.exitProcess

/**
 * Activity 栈管理器
 * 
 * 使用 WeakReference 管理 Activity 引用，避免内存泄漏
 * 采用单例模式确保全局唯一
 */
object ActivityManager {
    
    private var stack: Stack<WeakReference<Activity>>? = null
    
    /**
     * 添加 Activity 到栈中
     */
    fun addActivity(activity: Activity) {
        if (stack == null) {
            stack = Stack()
        }
        stack?.add(WeakReference(activity))
    }
    
    /**
     * 从栈中移除指定 Activity
     */
    fun removeActivity(activity: Activity) {
        stack?.removeAll { it.get() == null || it.get() == activity }
    }
    
    /**
     * 清理已释放的 Activity 弱引用
     */
    fun checkWeakReference() {
        stack?.removeAll { it.get() == null }
    }
    
    /**
     * 获取当前可见的 Activity
     * @return 当前未 finishing 且未 destroyed 的 Activity，若无则返回 null
     */
    fun currentVisibleActivity(): Activity? {
        checkWeakReference()
        val currentStack = stack ?: return null
        
        if (currentStack.isEmpty()) return null
        
        // 先检查栈顶 Activity
        currentStack.lastElement()?.get()?.let { activity ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                return activity
            }
        }
        
        // 栈顶不可用，遍历查找第一个可用的 Activity
        return currentStack.firstNotNullOfOrNull { ref ->
            ref.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
        }
    }
    
    /**
     * 获取栈顶 Activity（可能已销毁）
     */
    fun lastActivity(): Activity? {
        checkWeakReference()
        return stack?.takeIf { it.isNotEmpty() }?.lastElement()?.get()
    }
    
    /**
     * 结束栈顶 Activity
     */
    fun finishActivity() {
        lastActivity()?.let { finishActivity(it) }
    }
    
    /**
     * 结束指定 Activity 实例
     */
    fun finishActivity(activity: Activity) {
        stack?.removeAll { it.get() == null || it.get() == activity }
        activity.finish()
    }
    
    /**
     * 检查栈中是否包含指定类型的 Activity
     */
    fun containsActivity(cls: Class<*>): Boolean {
        return stack?.any { it.get()?.javaClass == cls } == true
    }
    
    /**
     * 检查栈中是否包含指定类型列表中的任意 Activity
     */
    fun containsActivity(classList: List<Class<*>>?): Boolean {
        if (classList == null) return false
        return stack?.any { ref ->
            ref.get()?.let { classList.contains(it.javaClass) } == true
        } == true
    }
    
    /**
     * 结束指定类型的所有 Activity
     */
    fun finishActivity(cls: Class<*>) {
        stack?.removeAll { ref ->
            ref.get()?.let { activity ->
                if (activity.javaClass == cls) {
                    "finish:${cls.name}".logd("OpenAdLifeImpl")
                    activity.finish()
                    true
                } else {
                    false
                }
            } ?: true // 移除已释放的引用
        }
    }
    
    /**
     * 关闭所有 Activity
     */
    fun finishAllActivity() {
        stack?.forEach { ref ->
            ref.get()?.finish()
        }
        stack?.clear()
    }
    
    /**
     * 关闭除指定类型外的所有 Activity
     */
    fun finishOtherActivity(cls: Class<*>) {
        stack?.removeAll { ref ->
            ref.get()?.let { activity ->
                if (activity.javaClass != cls) {
                    activity.finish()
                    true
                } else {
                    false
                }
            } ?: true // 移除已释放的引用
        }
    }
    
    /**
     * 重新创建所有 Activity（除语言页面）
     */
    fun recreateAllActivityExcludeLangPage() {
        stack?.removeAll { ref ->
            ref.get()?.let { activity ->
                activity.recreate()
                false
            } ?: true // 移除已释放的引用
        }
    }
    
    /**
     * 检查是否存在指定类型的 Activity
     */
    fun hasActivity(cls: Class<*>): Boolean {
        checkWeakReference()
        return stack?.any { it.get()?.javaClass == cls } == true
    }
    
    /**
     * 统计指定类型 Activity 的数量
     */
    fun hasActivityNum(cls: Class<*>): Int {
        checkWeakReference()
        return stack?.count { it.get()?.javaClass == cls } ?: 0
    }

    /**
     * 退出应用程序
     */
    fun exitApp() {
        try {
            finishAllActivity()
            // 退出 JVM
            exitProcess(0)
            // 从系统中 kill 掉
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 获取当前栈中 Activity 的数量
     */
    fun getPageSize(): Int = stack?.size ?: 0
}
