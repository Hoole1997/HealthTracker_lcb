package com.daily.health.manager.face.widget

import android.content.Context
import android.content.ContextWrapper
import android.text.Html
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.daily.health.manager.R
import com.daily.health.manager.databinding.FcLayoutExpertAdviceBinding
import com.daily.health.manager.face.act.showFreeLockConfirm
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible
import com.daily.health.manager.face.tracker.HealthType
import com.daily.health.manager.face.tracker.HealthTypeProvider
import com.daily.health.manager.face.tracker.trackAdAutoCancel
import com.daily.health.manager.face.tracker.trackAdAutoPlay
import com.daily.health.manager.face.tracker.trackRecommCancel
import com.daily.health.manager.face.tracker.trackRecommFreeUnlock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.config.AdConfigManager
import com.healthtracker.framework.R as FrameworkR

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
    private val binding: FcLayoutExpertAdviceBinding

    // 状态
    private var isMaskVisible = false
    private var countdownSeconds = 5
    private var blurRadius = 20f
    private var minHeightWithMask = 0

    // 倒计时协程
    private var countdownJob: Job? = null

    // 生命周期管理
    private var lifecycleOwner: LifecycleOwner? = null

    // 倒计时状态（支持暂停/恢复）
    private var remainingSeconds = 0  // 剩余秒数
    private var isPaused = false      // 是否暂停状态

    // 回调监听器
    private var listener: OnExpertAdviceListener? = null

    // 生命周期观察者（监听页面可见性）
    private val lifecycleObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_PAUSE -> {
                // 页面不可见，暂停倒计时
                if (countdownJob?.isActive == true && !isPaused) {
                    pauseCountdown()
                }
            }
            Lifecycle.Event.ON_RESUME -> {
                // 页面恢复，继续倒计时
                if (isPaused && remainingSeconds > 0) {
                    unpauseCountdown()
                }
            }
            Lifecycle.Event.ON_DESTROY -> {
                // 页面销毁，停止倒计时并清理
                stopCountdown()
            }
            else -> {}
        }
    }

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
        binding = FcLayoutExpertAdviceBinding.inflate(inflater, this)

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
            context.trackAdAutoCancel(getHealthType(context))
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
                    context.trackRecommFreeUnlock(getHealthType(context))
                    listener?.onGetTipClicked()
                },{
                    context.trackRecommCancel(getHealthType(context))
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
     * 设置生命周期所有者
     * 用于支持 Fragment 等非 Activity 场景
     *
     * @param owner LifecycleOwner（Activity 或 Fragment）
     */
    fun setLifecycleOwner(owner: LifecycleOwner?) {
        // 移除旧的观察者
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)

        // 设置新的
        lifecycleOwner = owner

        // 注册新的观察者
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
    }

    /**
     * 手动开始倒计时
     */
    fun startCountdown() {
        stopCountdown() // 先停止之前的倒计时

        // 初始化倒计时状态
        remainingSeconds = countdownSeconds
        isPaused = false

        // 启动倒计时
        resumeCountdown()
    }

    /**
     * 从当前剩余秒数继续倒计时
     */
    private fun resumeCountdown() {
        // 如果没有剩余秒数或已有倒计时在运行，直接返回
        if (remainingSeconds <= 0 || countdownJob?.isActive == true) {
            return
        }

        countdownJob = CoroutineScope(Dispatchers.Main).launch {
            while (remainingSeconds > 0 && !isPaused) {
                // 更新显示
                "${remainingSeconds}s".also { binding.btnCountdown.text = it }

                // 等待 1 秒
                delay(1000)

                // 减少剩余秒数
                remainingSeconds--
            }

            // 检查是否正常结束（非暂停导致的退出）
            if (remainingSeconds <= 0 && !isPaused) {
                context.trackAdAutoPlay(getHealthType(context))
                onCountdownFinished()
            }
        }
    }

    /**
     * 暂停倒计时（保存当前进度）
     */
    private fun pauseCountdown() {
        isPaused = true

        // 取消协程（但保留 remainingSeconds）
        countdownJob?.cancel()
        countdownJob = null
    }

    /**
     * 恢复倒计时（从暂停位置继续）
     */
    private fun unpauseCountdown() {
        // 只有在暂停状态且有剩余秒数时才恢复
        if (!isPaused || remainingSeconds <= 0) {
            return
        }

        isPaused = false

        // 从剩余秒数继续倒计时
        resumeCountdown()
    }

    /**
     * 手动停止倒计时
     */
    fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null

        // 重置状态，防止意外恢复
        remainingSeconds = 0
        isPaused = false
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
        if(AdConfigManager.autoPlayReward()){
            listener?.onCountdownFinished()
        }

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
     * View 附加到窗口时自动注册生命周期观察者
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // 如果 Context 是 AppCompatActivity，自动获取 LifecycleOwner
        if (lifecycleOwner == null && context is AppCompatActivity) {
            lifecycleOwner = context as AppCompatActivity
        }

        // 注册生命周期观察者
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
    }

    /**
     * View 从窗口分离时清理资源
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 停止倒计时
        stopCountdown()

        // 移除生命周期观察者
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
    }

    private fun getHealthType(activity: Context): HealthType {
        var ctx: Context? = activity
        while (ctx is ContextWrapper) {
            if (ctx is HealthTypeProvider) {
                return ctx.getHealthType()
            }
            ctx = ctx.baseContext
        }
        return (ctx as? HealthTypeProvider)?.getHealthType() ?: HealthType.OTHER
    }
}
