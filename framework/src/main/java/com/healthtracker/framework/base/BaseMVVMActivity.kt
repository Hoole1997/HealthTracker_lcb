package com.healthtracker.framework.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewbinding.ViewBinding
import com.healthtracker.framework.util.RestoreUtils
import com.healthtracker.framework.util.requestOrientation

/**
 * 描述　: 包含 ViewModel 和 ViewBinding ViewModelActivity基类，把ViewModel 和 ViewBinding 注入进来了
 * 需要使用 ViewBinding 的清继承它
 */
abstract class BaseMVVMActivity<VM : BaseViewModel, VB : ViewBinding> : AppCompatActivity() {

    lateinit var mBinding: VB
    lateinit var mViewModel: VM

    abstract fun createViewBinding(): VB

    abstract fun getVMModelClass(): Class<VM>

    /**
     * 创建DataBinding
     */
    open fun initViewBinding(): View {
        return createViewBinding().also { mBinding = it }.root
    }

    open fun hasViewModel() = true

    /**
     * 创建viewModel
     */
    open fun createViewModel(): VM {
        return ViewModelProvider(this)[getVMModelClass()]
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        requestOrientation()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(initViewBinding())
        init(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        RestoreUtils.onSaveInstanceState(this, outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        RestoreUtils.onRestoreInstanceState(this, savedInstanceState)
        super.onRestoreInstanceState(savedInstanceState)
    }

    abstract fun initView(savedInstanceState: Bundle?)

    open fun initData() {}

    private fun init(savedInstanceState: Bundle?) {
        if (hasViewModel()) {
            mViewModel = createViewModel()
        }
        createObserver()
        initView(savedInstanceState)
        initData()
    }

    /**
     * 创建LiveData数据观察者
     */
    open fun createObserver() {

    }

    open fun showFragment(layoutId: Int, fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            if (!fragment.isAdded) {
                add(layoutId, fragment)
            } else {
                show(fragment)
            }

            setMaxLifecycle(fragment, Lifecycle.State.RESUMED).commitAllowingStateLoss()
        }
        supportFragmentManager
    }

    open fun showFragment(fragment: Fragment?) {
        if (fragment == null) return
        supportFragmentManager.beginTransaction().show(fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.RESUMED).commitAllowingStateLoss()
    }

    open fun hideFragment(fragment: Fragment?) {
        if (fragment == null) return
        supportFragmentManager.beginTransaction().hide(fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
            .commitAllowingStateLoss()
    }

    open fun removeFragment(fragment: Fragment?) {
        if (fragment == null || !fragment.isAdded) return
        supportFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
    }

    protected fun startTransition(time: Long = 150L) {
        TransitionManager.beginDelayedTransition(
            mBinding.root as ViewGroup,
            AutoTransition().apply {
                duration = time
            })
    }
}
