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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.ToastUtils
import com.healthtracker.framework.R
import com.healthtracker.framework.SysBarUtils
import com.healthtracker.framework.SysBarUtils.hideNavigationBar
import com.healthtracker.framework.SysBarUtils.hideStateBar
import com.healthtracker.framework.util.RestoreUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
        if (shouldDisableBackPressed()) {
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // 禁用返回键，什么都不做
                }
            })
        }
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
     * 收集StateFlow的便捷方法
     *
     * 特点：使用 collectLatest，只处理最新的值，会取消之前正在执行的收集块
     *
     * 使用场景：
     * - UI状态更新（如加载状态、错误状态、数据状态）
     * - 只需要最新状态的场景
     * - 避免过时的UI更新
     *
     * 示例：
     * viewModel.isLoading.collectLatestLifecycle { isLoading ->
     *     binding.progressBar.isVisible = isLoading  // 只显示最新状态
     * }
     */
    protected inline fun <T> StateFlow<T>.collectLatestLifecycle(
        crossinline action: suspend (value: T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collectLatest { action(it) }
            }
        }
    }

    /**
     * 收集Flow的便捷方法
     *
     * 特点：使用普通 collect，会处理所有值，包括中间值，按顺序执行每个收集块
     *
     * 与 collectLatestLifecycle 的区别：
     * - collectLatestLifecycle：只处理最新值，会取消之前的操作
     * - collectLifecycle：处理所有值，不丢失任何事件
     *
     * 使用场景：
     * - 收集一次性事件流（如导航事件、错误事件）
     * - 处理用户操作响应流
     * - 监听网络状态变化
     * - 处理来自Repository的数据流
     * - 需要确保不丢失任何事件的场景
     *
     * 示例：
     * viewModel.events.collectLifecycle { event ->
     *     when (event) {
     *         is NavigateEvent -> navigateTo(event.target)  // 每个事件都要处理
     *         is ShowErrorEvent -> showError(event.message)
     *     }
     * }
     */
    protected inline fun <T> Flow<T>.collectLifecycle(
        crossinline action: suspend (value: T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                collect { action(it) }
            }
        }
    }

    /**
     * 收集多个StateFlow的便捷方法
     *
     * 使用场景：
     * - 同时监听多个UI状态变化（如加载状态 + 数据状态）
     * - 组合多个数据源进行UI更新
     * - 处理复杂的表单状态（如输入验证 + 提交状态）
     * - 监听用户权限状态 + 数据加载状态
     *
     * 示例：
     * collectCombined(
     *     viewModel.isLoading,
     *     viewModel.userData
     * ) { isLoading, userData ->
     *     binding.progressBar.isVisible = isLoading
     *     if (!isLoading && userData != null) {
     *         updateUI(userData)
     *     }
     * }
     */
    protected inline fun <T1, T2> collectCombined(
        stateFlow1: StateFlow<T1>,
        stateFlow2: StateFlow<T2>,
        crossinline action: suspend (T1, T2) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                kotlinx.coroutines.flow.combine(stateFlow1, stateFlow2) { value1, value2 ->
                    value1 to value2
                }.collect { (value1, value2) ->
                    action(value1, value2)
                }
            }
        }
    }

    /**
     * 子类可重写，返回true则禁用返回键，默认false
     */
    protected open fun shouldDisableBackPressed(): Boolean = false

    protected open fun hasStatusbarPlaceView() = false
    protected open fun afterAppleyWindowInsets() {

    }


}
