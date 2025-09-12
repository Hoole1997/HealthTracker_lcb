package com.healthtracker.framework.base.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.healthtracker.framework.base.BaseViewModel

/**
 * 描述　: ViewModelFragment基类，自动把ViewModel注入Fragment和 ViewBinding 注入进来了
 * 需要使用 ViewBinding 的清继承它
 */
abstract class BaseMVVMFragment<VM : BaseViewModel, VB : ViewBinding> : Fragment() {

    lateinit var mViewModel: VM

    //该类绑定的 ViewBinding
    private var _binding: VB? = null

    val mViewBind: VB? get() = _binding

    private var isPaused = true
    private var isAttached = false

    abstract fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ): VB

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = createViewModel()
        initView(savedInstanceState)
        createObserver()
        initData()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return createViewBinding(inflater, container, false).also { _binding = it }.root
    }

    abstract fun getVMModelClass(): Class<VM>

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        isAttached = true
    }

    override fun onDetach() {
        isAttached = false
        super.onDetach()
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
    }

    override fun onPause() {
        super.onPause()
        isPaused = true
    }

    protected fun isResume() = !isPaused

    protected fun isAttached() = isAttached

    protected fun isCreated() = _binding != null

    /**
     * 创建viewModel
     */
    private fun createViewModel(): VM {
        return ViewModelProvider(this)[getVMModelClass()]
    }

    /**
     * 初始化view
     */
    abstract fun initView(savedInstanceState: Bundle?)

    /**
     * 创建观察者
     */
    open fun createObserver() {}

    /**
     * Fragment执行onCreate后触发的方法
     */
    open fun initData() {}

}