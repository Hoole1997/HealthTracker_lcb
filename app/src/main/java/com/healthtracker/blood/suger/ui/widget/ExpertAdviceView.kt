package com.healthtracker.blood.suger.ui.widget

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.LayoutExpertAdviceBinding
import com.healthtracker.blood.suger.ui.act.showFreeLockConfirm
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.R as FrameworkR
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 专家建议自定义控件
 *
 * 功能：
 * 1. 显示专家建议内容
 * 2. 支持遮罩层（自动启用模糊效果）
 * 3. 倒计时功能（外部控制开始）
 * 4. 交互回调
 * 5. 动态高度控制
 */
class ExpertAdviceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // ViewBinding
    private val binding: LayoutExpertAdviceBinding

    // 状态
    private var isMaskVisible = false
    private var countdownSeconds = 5
    private var blurRadius = 20f
    private var minHeightWithMask = 0

    // 倒计时协程
    private var countdownJob: Job? = null

    // 回调监听器
    private var listener: OnExpertAdviceListener? = null

    /**
     * 交互回调接口
     */
    interface OnExpertAdviceListener {
        /**
         * 倒计时结束
         */
        fun onCountdownFinished()

        /**
         * 点击获取提示按钮
         */
        fun onGetTipClicked()

        /**
         * 点击取消按钮
         */
        fun onCancelClicked()
    }

    init {
        // 加载布局
        val inflater = LayoutInflater.from(context)
        binding = LayoutExpertAdviceBinding.inflate(inflater, this)

        // 读取自定义属性
        context.obtainStyledAttributes(attrs, R.styleable.ExpertAdviceView).apply {
            try {
                isMaskVisible = getBoolean(R.styleable.ExpertAdviceView_eav_showMask, false)
                countdownSeconds = getInt(R.styleable.ExpertAdviceView_eav_countdownSeconds, 2)
                blurRadius = getFloat(R.styleable.ExpertAdviceView_eav_blurRadius, 20f)
                minHeightWithMask = getDimensionPixelSize(
                    R.styleable.ExpertAdviceView_eav_minHeightWithMask,
                    resources.getDimensionPixelSize(FrameworkR.dimen.dp_237)
                )

                // 设置初始文本（如果提供）
                getString(R.styleable.ExpertAdviceView_eav_adviceText)?.let {
                    setAdviceText(it)
                }
            } finally {
                recycle()
            }
        }

        // 初始化视图状态
        initViews()
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        // 设置按钮点击事件
        binding.btnCancel.click {
            onCancelClicked()
        }

        binding.btnGetTip.clickWithDuration {
            listener?.onGetTipClicked()
        }

        // 初始化遮罩状态（自动控制模糊）
        setMaskVisible(isMaskVisible)
    }

    /**
     * 设置专家建议文本（支持 HTML 格式）
     */
    fun setAdviceText(text: String) {
        binding.tvAdviceContent.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
    }

    /**
     * 设置遮罩层可见性（自动控制模糊效果）
     *
     * @param visible true 显示遮罩和模糊，false 隐藏遮罩和模糊
     */
    fun setMaskVisible(visible: Boolean) {
        isMaskVisible = visible

        // 同步控制模糊效果
        binding.blurView.isVisible = visible
        if (visible) {
            setupBlurEffect()
        }
        // 同步控制遮罩层
        binding.clMask.isVisible = visible

        // 更新容器高度
        updateContainerHeight(visible)

        // 控制按钮显示（不自动开始倒计时）
        if (visible) {
            "${countdownSeconds}s".also { binding.btnCountdown.text = it }
            // 显示倒计时按钮，隐藏获取提示按钮
            binding.llCountdownButtons.visible()
            binding.btnGetTip.gone()
            // 移除自动开始倒计时，由外部控制
            if(context is AppCompatActivity){
                (context as AppCompatActivity).showFreeLockConfirm({
                    listener?.onGetTipClicked()
                },{
                    startCountdown()
                })
            }
        } else {
            // 隐藏遮罩时停止倒计时
            stopCountdown()
        }
    }

    /**
     * 设置倒计时秒数
     */
    fun setCountdownSeconds(seconds: Int) {
        require(seconds > 0) { "Countdown seconds must be positive" }
        countdownSeconds = seconds
    }

    /**
     * 设置模糊半径
     */
    fun setBlurRadius(radius: Float) {
        require(radius > 0) { "Blur radius must be positive" }
        blurRadius = radius
        if (isMaskVisible) {
            setupBlurEffect()
        }
    }

    /**
     * 设置监听器
     */
    fun setOnExpertAdviceListener(listener: OnExpertAdviceListener?) {
        this.listener = listener
    }

    /**
     * 手动开始倒计时
     */
    fun startCountdown() {
        stopCountdown() // 先停止之前的倒计时

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            for (i in countdownSeconds downTo 1) {
                "${i}s".also { binding.btnCountdown.text = it }
                delay(1000)
            }
            // 倒计时结束
            onCountdownFinished()
        }
    }

    /**
     * 手动停止倒计时
     */
    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    /**
     * 取消按钮点击处理
     */
    private fun onCancelClicked() {
        // 停止倒计时
        stopCountdown()

        // 隐藏倒计时按钮，显示获取提示按钮
        binding.llCountdownButtons.gone()
        binding.btnGetTip.visible()

        // 回调通知
        listener?.onCancelClicked()
    }

    /**
     * 倒计时结束处理
     */
    private fun onCountdownFinished() {
        onCancelClicked()
        listener?.onCountdownFinished()
    }

    /**
     * 设置模糊效果
     */
    private fun setupBlurEffect() {
        try {
            val decorView = (context as? android.app.Activity)?.window?.decorView as? ViewGroup
            val rootView = decorView?.findViewById<ViewGroup>(android.R.id.content)
            if (rootView != null) {
                binding.blurView.apply {
                    outlineProvider = ViewOutlineProvider.BACKGROUND
                    clipToOutline = true
                    setupWith(decorView)
                        .setFrameClearDrawable(decorView.background)
                        .setBlurRadius(blurRadius)
                }
            }
        } catch (e: Exception) {
            // 模糊设置失败，静默处理
            binding.blurView.gone()
        }
    }

    /**
     * 更新容器高度
     *
     * @param showMask true 有遮罩（固定高度），false 无遮罩（包裹内容）
     */
    private fun updateContainerHeight(showMask: Boolean) {
        binding.clAdviceContainer.apply {
            val layoutParams = this.layoutParams
            if (showMask) {
                // 有遮罩：固定高度 = minHeightWithMask
                layoutParams.height = minHeightWithMask
                minHeight = 0  // 清除 minHeight，使用固定 height
            } else {
                // 无遮罩：包裹内容
                layoutParams.height = LayoutParams.WRAP_CONTENT
                minHeight = 0
            }
            this.layoutParams = layoutParams
            requestLayout()
        }
    }

    /**
     * View 从窗口分离时清理资源
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopCountdown()
    }
}
