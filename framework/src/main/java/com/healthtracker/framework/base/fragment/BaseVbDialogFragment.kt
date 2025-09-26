package com.healthtracker.framework.base.fragment

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.healthtracker.framework.R
import com.healthtracker.framework.ext.loge

/**
 * 描述　: ViewModelFragment基类，自动把ViewModel注入Fragment和 ViewBinding 注入进来了
 * 需要使用 ViewBinding 的清继承它
 */
abstract class BaseVbDialogFragment<VB : ViewBinding> : DialogFragment() {
    //该类绑定的 ViewBinding
    private var _binding: VB? = null

    val mViewBind: VB? get() = _binding

    fun isCreated(): Boolean {
        return _binding != null
    }

    /**
     * 弹性弹出效果
     */
//    protected var springIn = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, onDialogStyle())
    }

    open fun onDialogStyle() = R.style.CommonDialogStyle

    open fun onAnimationEnd() {

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return createViewBinding(inflater, container).also { _binding = it }.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return super.onCreateDialog(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView(view, savedInstanceState)

    }

    protected abstract fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean = false,
    ): VB

    abstract fun initView(view: View, savedInstanceState: Bundle?)

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt() // 设置宽度为屏幕的 85%
            val height = WindowManager.LayoutParams.WRAP_CONTENT
            setLayout(width, height)
            setGravity(Gravity.CENTER)
        }
    }

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

    open fun show(manager: FragmentManager) {
        try {
            if (manager.isDestroyed) {
                return
            }
            if (dialog?.isShowing == true || this.isAdded) return

            show(manager, this::class.simpleName)
        } catch (e: Exception) {
            "dialog show exception ${e.message}".loge()
        }
    }

    override fun dismiss() {
        try {
            super.dismissAllowingStateLoss()
        } catch (_: Exception) {
        }
    }
}