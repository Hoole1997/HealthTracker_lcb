package com.healthtracker.framework.ext

/**
 * Flow扩展函数使用示例
 *
 * 演示如何在不同场景下使用Flow扩展函数
 */

/*
=================================================================
1. 在Activity中使用
=================================================================

class MainActivity : BaseMVVMActivity<MainViewModel, ActivityMainBinding>() {

    override fun initView(savedInstanceState: Bundle?) {
        // 方式1：使用LifecycleOwner扩展（推荐）
        collectLatest(mViewModel.isLoading) { isLoading ->
            mViewBind.progressBar.isVisible = isLoading
        }

        collect(mViewModel.events) { event ->
            when (event) {
                is NavigateEvent -> navigateTo(event.destination)
                is ShowErrorEvent -> showError(event.message)
            }
        }

        collectCombined(mViewModel.isLoading, mViewModel.userData) { isLoading, userData ->
            mViewBind.progressBar.isVisible = isLoading
            if (!isLoading && userData != null) {
                updateUI(userData)
            }
        }

        // 方式2：使用Flow扩展（显式指定LifecycleOwner）
        mViewModel.isLoading.collectLatestLifecycle(this) { isLoading ->
            mViewBind.progressBar.isVisible = isLoading
        }
    }
}

=================================================================
2. 在Fragment中使用
=================================================================

class HomeFragment : BaseMVVMFragment<HomeViewModel, FragmentHomeBinding>() {

    override fun initView(savedInstanceState: Bundle?) {
        // 使用Fragment扩展（自动使用viewLifecycleOwner）
        collectLatest(mViewModel.bloodSugarRecord) { record ->
            updateBloodSugarUI(record)
        }

        collect(mViewModel.navigationEvents) { event ->
            findNavController().navigate(event.destination)
        }

        // 组合多个状态
        collectCombined(
            mViewModel.isLoading,
            mViewModel.hasPermission,
            mViewModel.userData
        ) { isLoading, hasPermission, userData ->
            when {
                isLoading -> showLoading()
                !hasPermission -> showPermissionRequest()
                userData != null -> showUserData(userData)
                else -> showEmptyState()
            }
        }
    }
}

=================================================================
3. 在自定义View中使用
=================================================================

class HealthStatusView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    fun bindViewModel(viewModel: HealthViewModel, lifecycleOwner: LifecycleOwner) {
        // 在自定义View中需要显式传入LifecycleOwner
        viewModel.healthStatus.collectLatestLifecycle(lifecycleOwner) { status ->
            updateHealthIndicator(status)
        }

        collectCombined(
            lifecycleOwner,
            viewModel.bloodSugar,
            viewModel.bloodPressure
        ) { sugar, pressure ->
            updateComprehensiveDisplay(sugar, pressure)
        }
    }
}

=================================================================
4. 在Repository中使用（配合其他组件）
=================================================================

class HealthDataRepository {

    // Repository通常不直接使用这些扩展函数
    // 但可以为外部提供便捷的收集方法

    fun observeHealthData(lifecycleOwner: LifecycleOwner, callback: (HealthData) -> Unit) {
        healthDataFlow.collectLifecycle(lifecycleOwner) { data ->
            callback(data)
        }
    }
}

=================================================================
5. 在Compose中使用
=================================================================

@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 在Compose中，通常使用collectAsState，但也可以使用这些扩展
    LaunchedEffect(Unit) {
        viewModel.events.collectLifecycle(lifecycleOwner) { event ->
            when (event) {
                is ShowToastEvent -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is NavigateEvent -> {
                    // 处理导航
                }
            }
        }
    }
}

=================================================================
6. 错误处理和生命周期控制
=================================================================

class MainActivity : BaseMVVMActivity<MainViewModel, ActivityMainBinding>() {

    override fun initView(savedInstanceState: Bundle?) {
        // 自定义生命周期状态
        collectLatest(
            mViewModel.isLoading,
            minActiveState = Lifecycle.State.RESUMED  // 只在RESUMED状态收集
        ) { isLoading ->
            // 只有在页面完全可见时才更新UI
            mViewBind.progressBar.isVisible = isLoading
        }

        // 错误处理
        collect(mViewModel.errors) { error ->
            try {
                handleError(error)
            } catch (e: Exception) {
                // 处理错误处理中的异常
                logError(e)
            }
        }
    }
}

=================================================================
7. 性能优化场景
=================================================================

class DataIntensiveActivity : BaseMVVMActivity<DataViewModel, ActivityDataBinding>() {

    override fun initView(savedInstanceState: Bundle?) {
        // 对于频繁更新的数据，使用collectLatest避免UI卡顿
        collectLatest(mViewModel.realtimeData) { data ->
            updateRealtimeDisplay(data)  // 只处理最新数据
        }

        // 对于重要事件，使用collect确保不丢失
        collect(mViewModel.importantEvents) { event ->
            processImportantEvent(event)  // 确保处理每个事件
        }

        // 组合多个流时的性能考虑
        collectCombined(
            mViewModel.configuration,     // 很少变化
            mViewModel.userPreferences,   // 偶尔变化
            mViewModel.realtimeStatus     // 频繁变化
        ) { config, prefs, status ->
            // 任何一个变化都会触发，但只会处理最新的组合值
            updateComplexUI(config, prefs, status)
        }
    }
}

=================================================================
8. 测试友好的设计
=================================================================

class TestableFragment : Fragment() {

    // 为了便于测试，可以将Flow收集逻辑提取到方法中
    fun setupObservers(viewModel: TestViewModel) {
        collectLatest(viewModel.state) { state ->
            updateUI(state)
        }
    }

    // 在测试中可以方便地验证
    // @Test
    // fun testObservers() {
    //     val viewModel = TestViewModel()
    //     fragment.setupObservers(viewModel)
    //     viewModel.updateState(newState)
    //     // 验证UI更新
    // }
}
*/