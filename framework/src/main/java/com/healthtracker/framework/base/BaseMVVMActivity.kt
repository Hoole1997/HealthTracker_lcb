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

    /**
     * 返回键回调处理器
     */
    private var backPressedCallback: OnBackPressedCallback? = null


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

            hideNavigationBar(this)
            afterAppleyWindowInsets()
            insets
        }

        // 设置返回键处理
        setupBackPressedCallback()

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
    }

    /**
     * 设置返回键处理回调
     * 使用现代 OnBackPressedDispatcher API 替代已废弃的 onBackPressed()
     */
    private fun setupBackPressedCallback() {
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 检查是否禁用返回键
                if (shouldDisableBackPressed()) {
                    return
                }

                // 调用子类可重写的方法
                if (!handleBackPress()) {
                    // 子类返回 false，执行默认返回行为
                    performDefaultBackAction()
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback!!)
    }

    /**
     * 执行默认的返回行为（finish Activity）
     */
    private fun performDefaultBackAction() {
        // 临时禁用回调，让系统处理默认行为
        backPressedCallback?.isEnabled = false
        onBackPressedDispatcher.onBackPressed()
        backPressedCallback?.isEnabled = true
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
     * 注意：当返回 true 时，返回键将完全不响应
     */
    protected open fun shouldDisableBackPressed(): Boolean = false

    protected open fun hasStatusbarPlaceView() = false
    protected open fun afterAppleyWindowInsets() {

    }

    /**
     * 处理返回键按下事件
     *
     * 子类可以重写此方法来自定义返回键行为：
     * - 返回 true 表示已处理事件，不会执行默认的 finish() 行为
     * - 返回 false 表示使用默认行为（finish Activity）
     *
     * @return true 如果已处理返回键事件，false 使用默认行为
     */
    protected open fun handleBackPress(): Boolean {
        // 向后兼容：调用旧的 onBackPress() 方法
        return false
    }

    /**
     * @deprecated 使用 handleBackPress() 替代，返回 Boolean 表示是否已处理
     * 此方法保留用于向后兼容，未来版本可能移除
     */
    @Deprecated(
        message = "使用 handleBackPress() 替代，返回 Boolean 表示是否已处理",
        replaceWith = ReplaceWith("handleBackPress()"),
        level = DeprecationLevel.WARNING
    )
    protected open fun onBackPress(){
        handleBackPress()

    }


}
