package com.daily.health.manager.face.weight

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.daily.health.manager.databinding.FcViewGenericStatusBinding

/**
 * 通用状态视图基类
 * 适用于血压和血糖状态显示，只需传入不同的进度条组件
 * @param T 状态类型，需要实现LevelCategory接口
 */
abstract class GenericStatusView<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) where T : Enum<T>, T : LevelCategory {

    protected val binding: FcViewGenericStatusBinding =
        FcViewGenericStatusBinding.inflate(LayoutInflater.from(context), this, true)

    // 当前状态
    protected var currentLevelValue: T? = null

    // 子类需要提供的抽象方法
    abstract fun createLevelBar(): View
    abstract fun getLevelTexts(): Map<T, Int>
    abstract fun getStatusTextRes(level: T): Int
    abstract fun getRangeText(level: T): String
    abstract fun getDefaultLevel(): T

    private var levelBar: View? = null

    private var isInitialized = false

    init {
        setupLevelBar()
        // 延迟UI更新，等待子类完全初始化
        post {
            isInitialized = true
            updateUI()
        }
    }

    /**
     * 设置进度条
     */
    private fun setupLevelBar() {
        levelBar = createLevelBar()
        binding.levelBarContainer.addView(levelBar)
    }

    /**
     * 更新UI显示
     */
    protected fun updateUI() {
        if (!isInitialized) return // 避免在初始化过程中调用
        updateLevelText()
        updateRangeText()
        updateLevelBar()
    }

    /**
     * 更新等级文本
     */
    private fun updateLevelText() {
        val level = currentLevelValue ?: getDefaultLevel()
        val levelTextRes = getStatusTextRes(level)
        val levelText = context.getString(levelTextRes)

        binding.levelText.text = levelText
        binding.levelText.setTextColor(ContextCompat.getColor(context, level.colorRes))
    }

    /**
     * 更新范围文本
     */
    private fun updateRangeText() {
        val level = currentLevelValue ?: getDefaultLevel()
        val rangeText = getRangeText(level)
        binding.rangeText.text = rangeText
    }

    /**
     * 更新等级进度条
     */
    protected abstract fun updateLevelBar()

    /**
     * 设置等级
     * @param level 等级
     */
    fun setLevel(level: T) {
        this.currentLevelValue = level
        updateUI()
    }

    /**
     * 获取当前等级
     */
    fun getCurrentLevel(): T? = currentLevelValue
}