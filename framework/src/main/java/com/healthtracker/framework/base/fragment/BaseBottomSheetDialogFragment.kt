package com.healthtracker.framework.base.fragment

import android.content.DialogInterface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.healthtracker.framework.ext.loge
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.healthtracker.framework.BarUtils
import com.healthtracker.framework.R
import kotlin.math.max


/**
 * 描述　: ViewModelFragment基类，自动把ViewModel注入Fragment和 ViewBinding 注入进来了
 * 需要使用 ViewBinding 的清继承它
 */
abstract class BaseBottomSheetDialogFragment<VB : ViewBinding>(var dialogListener: DialogListener? = null) : BottomSheetDialogFragment() {
    //该类绑定的 ViewBinding
    private var _binding: VB? = null

    val mViewBind: VB? get() = _binding

    /**
     * 弹性弹出效果
     */
    protected var springIn = true
    private var defPadding = 0
    /**
     * 适配高度
     */
    protected var isAdjustHeight = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
    }

    override fun onStart() {
        super.onStart()
        if (isAdjustHeight) {
            //适配小屏手机显示不全
            view?.let {
                val parent = it.parent
                it.measure(0, 0)
                BottomSheetBehavior.from(parent as View).peekHeight = it.measuredHeight

                parent.updateLayoutParams<CoordinatorLayout.LayoutParams> {
                    gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                }
            }
        }

        view?.post {
            dialog?.let { dialog ->
                val d = dialog as BottomSheetDialog
                d.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.let {
                    BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return createViewBinding(inflater, container).also { _binding = it }.root.apply {
            defPadding = paddingBottom
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //软键盘弹出时为底部添加padding
        setOnApplyWindowInsetsListener(view) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val inputBar = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            v.updatePadding(bottom = max(navigationBars,inputBar) + defPadding)
            insets
        }
        initView(view, savedInstanceState)
        dialog?.window?.let {
            BarUtils.setNavBarLightMode(it,true)
        }

    }

    protected abstract fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean = false,
    ): VB

    abstract fun initView(view: View, savedInstanceState: Bundle?)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 去除背景阴影
     */
    protected fun disableDim() {
        dialog?.window?.setDimAmount(0f)
    }

    fun show(manager: FragmentManager): Boolean {
        try {
            show(manager, this::class.simpleName)
            return true
        } catch (e: Exception) {
            "dialog show exception ${e.message}".loge()
        }
        return false
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        dialogListener?.onDismiss(this)
    }

    override fun dismiss() {
        try {
            super.dismissAllowingStateLoss()
        } catch (_: Throwable) {
        }
    }
}
