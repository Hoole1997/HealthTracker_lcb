package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.daily.health.manager.databinding.HtDialogNotificationPermissionV2Binding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment

/**
 * Compose 底部弹窗基类
 *
 * 封装了 BottomSheet 弹窗中 Compose 内容的通用配置：
 * - 窗口定位 (Gravity.BOTTOM) 和动画禁用 (由 Compose 接管)
 * - Compose 生命周期策略
 * - savedInstanceState 恢复时自动关闭 (回调丢失场景)
 * - 导航栏适配
 *
 * 子类只需实现 [Content] 方法提供 Compose UI 即可。
 */
abstract class ComposeBottomSheetFragment :
    BaseBottomSheetDialogFragment<HtDialogNotificationPermissionV2Binding>() {

    /**
     * 子类实现此方法提供 Compose UI 内容
     */
    @Composable
    abstract fun ComposeContent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 恢复时直接关闭（回调已丢失）
        if (savedInstanceState != null) {
            dismissAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // 设置窗口 Gravity 为底部，防止漂移
            setGravity(Gravity.BOTTOM)
            // 彻底禁用窗口动画，动画将由 Compose 接管
            setWindowAnimations(0)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogNotificationPermissionV2Binding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.composeView?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { ComposeContent() }
        }
    }

    override fun isAutoNavigationBarsPadding() = false
}
