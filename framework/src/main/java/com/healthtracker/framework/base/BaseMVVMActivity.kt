package com.healthtracker.framework.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ToastUtils
import com.healthtracker.framework.R
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.SysBarUtils.hideNavigationBar
import com.healthtracker.framework.SysBarUtils.hideStateBar
import com.healthtracker.framework.util.RestoreUtils
import kotlin.math.max


/**
 * 描述　: 包含 ViewModel 和 ViewBinding ViewModelActivity基类，把ViewModel 和 ViewBinding 注入进来了
 * 需要使用 ViewBinding 的清继承它
 */
abstract class BaseMVVMActivity<VM : BaseViewModel, VB : ViewBinding> : AppCompatActivity() {

    lateinit var mViewBind: VB

    lateinit var mViewModel: VM

    open var isSaveInstance = true


    abstract fun createViewBinding(): VB

    abstract fun getVMModelClass(): Class<VM>

    /**
     * 创建DataBinding
     */
    open fun initViewBinding(): View {
        return createViewBinding().also { mViewBind = it }.root
    }

    open fun hasViewModel() = true

    /**
     * 创建viewModel
     */
    open fun createViewModel(): VM {
        return ViewModelProvider(this)[getVMModelClass()]
    }

    open fun isFullscreen() = false

    open fun isFullscreenWithNavigationBar() = false

    open fun preSetView(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        isSaveInstance = false
        if (isFullscreen()) {
            hideStateBar(this)
            if(isFullscreenWithNavigationBar()){
                hideNavigationBar(this)
            }
        }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContentView(initViewBinding())
        ViewCompat.setOnApplyWindowInsetsListener(mViewBind.root){
                view,insets ->
            if(!isFullscreen() && !hasStatusbarPlaceView()){
                val statuBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                val captionBas = insets.getInsets(WindowInsetsCompat.Type.captionBar())
                mViewBind.root.updatePadding(top = max(statuBars.top,captionBas.top))
                setStatusBarColor2(getStatusBarColor())
            }
            if(!isFullscreenWithNavigationBar()){
                val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                if (navigationBars.right > 0) {
                    mViewBind.root.updatePadding(right = navigationBars.right)
                } else {
                    mViewBind.root.updatePadding(bottom = navigationBars.bottom)
                }
            }

            afterAppleyWindowInsets()
            insets
        }

        init(savedInstanceState)
    }

    override fun onResume() {
        isSaveInstance = false
        super.onResume()
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
        if(!isFullscreen()){
            setStatusBarColor2(getStatusBarColor())
        }
        initView(savedInstanceState)
        initData()
        // 统一处理返回键禁用逻辑
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 禁用返回键，什么都不做
                onBackPress()
            }
        })
    }

    /**
     * 创建LiveData数据观察者
     */
    open fun createObserver() {
        mViewModel.toastStr.observe(this) {
            if (it == null) return@observe
            kotlin.runCatching {  ToastUtils.getDefaultMaker().show(getString(it)) }
        }
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
            mViewBind.root as ViewGroup,
            AutoTransition().apply {
                duration = time
            })
    }

    protected fun setStatusBarColor2(@ColorRes colorAppBar: Int) {
        SysBarUtils.setStatusBarColor(this, colorAppBar)
    }

    protected open fun getStatusBarColor() = R.color.white

    /**
     * 注意：Flow扩展函数已迁移到 FlowExtensions.kt
     *
     * 现在可以使用更简洁的语法：
     * - collectLatest(viewModel.isLoading) { ... }
     * - collect(viewModel.events) { ... }
     * - collectCombined(flow1, flow2) { ... }
     *
     * 这些扩展函数现在可以在任何LifecycleOwner中使用，不仅限于Activity
     */

    /**
     * 子类可重写，返回true则禁用返回键，默认false
     */
    protected open fun shouldDisableBackPressed(): Boolean = false

    protected open fun hasStatusbarPlaceView() = false
    protected open fun afterAppleyWindowInsets() {

    }

    protected open fun onBackPress(){

    }


}
