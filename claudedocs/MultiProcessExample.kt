/**
 * MultiProcessExample.kt
 *
 * 计步SDK多进程集成示例
 *
 * 展示内容:
 * 1. 多进程架构下的初始化策略
 * 2. 跨进程通信机制验证
 * 3. 数据一致性保证
 * 4. ContentProvider数据查询
 * 5. Broadcast跨进程通知处理
 * 6. 进程生命周期管理
 */

package com.example.stepcounter.multiprocess

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import com.stepcounter.sdk.StepCounterSDK
import com.stepcounter.sdk.StepCounterListener
import com.stepcounter.sdk.model.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.FileReader

/**
 * MultiProcessApplication - 多进程应用初始化
 *
 * 关键点:
 * 1. 判断当前进程名称
 * 2. 只在主进程初始化SDK公开API
 * 3. SDK内部服务在独立进程运行
 */
class MultiProcessApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val processName = getCurrentProcessName()
        val packageName = packageName

        android.util.Log.d("MultiProcess", "Process started: $processName")

        when {
            // 主进程: 业务方应用进程
            processName == packageName -> {
                initMainProcess()
            }

            // SDK进程: 计步服务进程 (`:stepcounter`)
            processName.endsWith(":stepcounter") -> {
                initSDKProcess()
            }

            else -> {
                // 其他进程
                android.util.Log.d("MultiProcess", "Unknown process: $processName")
            }
        }
    }

    /**
     * 主进程初始化
     */
    private fun initMainProcess() {
        android.util.Log.d("MultiProcess", "Initializing main process")

        // SDK配置
        val config = StepConfig(
            weight = 65f,
            height = 170f,
            goalSteps = 10000,
            sensitivity = 3,
            autoStart = true,
            foregroundService = true
        )

        try {
            // 初始化SDK (会启动`:stepcounter`进程)
            StepCounterSDK.init(this, config)
            android.util.Log.d("MultiProcess", "StepCounterSDK initialized in main process")
        } catch (e: Exception) {
            android.util.Log.e("MultiProcess", "Failed to initialize SDK", e)
        }
    }

    /**
     * SDK进程初始化
     *
     * 注意: SDK进程由SDK内部管理,业务方无需关心
     * 此方法仅用于演示和日志记录
     */
    private fun initSDKProcess() {
        android.util.Log.d("MultiProcess", "SDK process started")
        // SDK进程初始化由SDK内部处理
        // 业务方无需在此进行任何操作
    }

    /**
     * 获取当前进程名称
     */
    private fun getCurrentProcessName(): String {
        return try {
            BufferedReader(FileReader("/proc/self/cmdline")).use { reader ->
                reader.readLine().trim('\u0000')
            }
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * MultiProcessActivity - 多进程场景演示
 */
class MultiProcessActivity : AppCompatActivity() {

    private val sdk = StepCounterSDK.getInstance()

    // UI组件
    private lateinit var tvProcessInfo: TextView
    private lateinit var tvMainProcessData: TextView
    private lateinit var tvSDKProcessData: TextView
    private lateinit var tvDataConsistency: TextView
    private lateinit var btnQueryViaContentProvider: Button
    private lateinit var btnTriggerBroadcast: Button
    private lateinit var btnCheckConsistency: Button

    // ContentObserver - 监听ContentProvider数据变化
    private lateinit var stepDataObserver: ContentObserver

    // BroadcastReceiver - 接收跨进程广播
    private lateinit var stepBroadcastReceiver: BroadcastReceiver

    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_multiprocess)

        initViews()
        showProcessInfo()
        setupContentObserver()
        setupBroadcastReceiver()
    }

    private fun initViews() {
        tvProcessInfo = findViewById(R.id.tv_process_info)
        tvMainProcessData = findViewById(R.id.tv_main_process_data)
        tvSDKProcessData = findViewById(R.id.tv_sdk_process_data)
        tvDataConsistency = findViewById(R.id.tv_data_consistency)
        btnQueryViaContentProvider = findViewById(R.id.btn_query_content_provider)
        btnTriggerBroadcast = findViewById(R.id.btn_trigger_broadcast)
        btnCheckConsistency = findViewById(R.id.btn_check_consistency)

        btnQueryViaContentProvider.setOnClickListener {
            queryViaContentProvider()
        }

        btnTriggerBroadcast.setOnClickListener {
            triggerStepCountUpdate()
        }

        btnCheckConsistency.setOnClickListener {
            checkDataConsistency()
        }
    }

    /**
     * 显示进程信息
     */
    private fun showProcessInfo() {
        val processId = Process.myPid()
        val processName = getCurrentProcessName()

        val info = """
            当前进程信息:
            - 进程名: $processName
            - 进程ID: $processId
            - 线程ID: ${Thread.currentThread().id}

            多进程架构:
            - 主进程: ${packageName}
            - SDK进程: ${packageName}:stepcounter
        """.trimIndent()

        tvProcessInfo.text = info
    }

    /**
     * 设置ContentProvider观察者
     *
     * 监听SDK进程通过ContentProvider发布的数据变化
     */
    private fun setupContentObserver() {
        stepDataObserver = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                android.util.Log.d("MultiProcess", "ContentProvider data changed: $uri")

                // 数据变化时重新查询
                queryViaContentProvider()
            }
        }

        // 注册观察者 (假设SDK暴露了ContentProvider URI)
        // 注意: 实际SDK可能不暴露ContentProvider,这里仅作演示
        val contentUri = Uri.parse("content://${packageName}.stepcounter.provider/step_data")
        contentResolver.registerContentObserver(
            contentUri,
            true,
            stepDataObserver
        )
    }

    /**
     * 设置广播接收器
     *
     * 接收SDK进程发送的跨进程广播通知
     */
    private fun setupBroadcastReceiver() {
        stepBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    "${packageName}.STEP_CHANGED" -> {
                        val steps = intent.getIntExtra("steps", 0)
                        val calories = intent.getDoubleExtra("calories", 0.0)

                        android.util.Log.d(
                            "MultiProcess",
                            "Received broadcast: steps=$steps, calories=$calories"
                        )

                        tvSDKProcessData.text = """
                            SDK进程广播数据:
                            - 步数: $steps
                            - 卡路里: ${"%.1f".format(calories)} kcal
                            - 时间: ${System.currentTimeMillis()}
                        """.trimIndent()
                    }
                }
            }
        }

        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction("${packageName}.STEP_CHANGED")
        }

        registerReceiver(stepBroadcastReceiver, filter)
    }

    /**
     * 通过ContentProvider查询数据
     *
     * 演示跨进程数据查询机制
     */
    private fun queryViaContentProvider() {
        coroutineScope.launch {
            try {
                // 方式1: 通过SDK API查询 (推荐)
                val stepData = withContext(Dispatchers.IO) {
                    sdk.getTodaySteps()
                }

                if (stepData != null) {
                    tvMainProcessData.text = """
                        主进程查询数据 (SDK API):
                        - 步数: ${stepData.steps}
                        - 卡路里: ${"%.1f".format(stepData.calories)} kcal
                        - 距离: ${"%.2f".format(stepData.distance)} km
                        - 时间: ${stepData.date}
                    """.trimIndent()
                }

                // 方式2: 直接查询ContentProvider (仅供演示)
                // 实际使用中应该只通过SDK API查询
                queryContentProviderDirect()

            } catch (e: Exception) {
                android.util.Log.e("MultiProcess", "Query failed", e)
                tvMainProcessData.text = "查询失败: ${e.message}"
            }
        }
    }

    /**
     * 直接查询ContentProvider (仅供演示)
     */
    private fun queryContentProviderDirect() {
        // 注意: 这里仅作演示,实际SDK不应暴露ContentProvider给外部
        val uri = Uri.parse("content://${packageName}.stepcounter.provider/step_data/today")

        val cursor = contentResolver.query(
            uri,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val steps = it.getInt(it.getColumnIndexOrThrow("steps"))
                val calories = it.getDouble(it.getColumnIndexOrThrow("calories"))

                android.util.Log.d(
                    "MultiProcess",
                    "ContentProvider direct query: steps=$steps, calories=$calories"
                )
            }
        }
    }

    /**
     * 触发步数更新
     *
     * 模拟SDK进程更新步数并触发广播
     */
    private fun triggerStepCountUpdate() {
        // 启动计步服务 (会在SDK进程中运行)
        val success = sdk.startCounting()

        if (success) {
            Toast.makeText(this, "已触发步数更新", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "启动失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 检查数据一致性
     *
     * 验证主进程和SDK进程的数据是否一致
     */
    private fun checkDataConsistency() {
        coroutineScope.launch {
            try {
                // 获取当前步数数据
                val stepData = withContext(Dispatchers.IO) {
                    sdk.getTodaySteps()
                }

                // 查询历史数据
                val historyData = withContext(Dispatchers.IO) {
                    val today = System.currentTimeMillis()
                    sdk.getHistoryData(today, today)
                }

                // 比较数据一致性
                val isConsistent = if (stepData != null && historyData.isNotEmpty()) {
                    val historySteps = historyData[0].steps
                    stepData.steps == historySteps
                } else {
                    false
                }

                val consistencyText = if (isConsistent) {
                    """
                        ✅ 数据一致性检查通过
                        - 主进程数据: ${stepData?.steps ?: 0} 步
                        - 历史数据: ${historyData.firstOrNull()?.steps ?: 0} 步
                        - 状态: 一致
                    """.trimIndent()
                } else {
                    """
                        ⚠️ 数据不一致
                        - 主进程数据: ${stepData?.steps ?: 0} 步
                        - 历史数据: ${historyData.firstOrNull()?.steps ?: 0} 步
                        - 建议: 重新同步数据
                    """.trimIndent()
                }

                tvDataConsistency.text = consistencyText

            } catch (e: Exception) {
                android.util.Log.e("MultiProcess", "Consistency check failed", e)
                tvDataConsistency.text = "检查失败: ${e.message}"
            }
        }
    }

    /**
     * 获取当前进程名称
     */
    private fun getCurrentProcessName(): String {
        return try {
            BufferedReader(FileReader("/proc/self/cmdline")).use { reader ->
                reader.readLine().trim('\u0000')
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // 注销ContentObserver
        contentResolver.unregisterContentObserver(stepDataObserver)

        // 注销BroadcastReceiver
        unregisterReceiver(stepBroadcastReceiver)

        // 取消协程
        coroutineScope.cancel()
    }
}

/**
 * ProcessMonitorFragment - 进程监控Fragment
 *
 * 实时监控多进程状态和数据同步
 */
class ProcessMonitorFragment : Fragment() {

    private lateinit var tvMainProcessStatus: TextView
    private lateinit var tvSDKProcessStatus: TextView
    private lateinit var tvIPCLatency: TextView
    private lateinit var recyclerView: RecyclerView

    private val monitorJob = Job()
    private val monitorScope = CoroutineScope(Dispatchers.Main + monitorJob)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_process_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvMainProcessStatus = view.findViewById(R.id.tv_main_process_status)
        tvSDKProcessStatus = view.findViewById(R.id.tv_sdk_process_status)
        tvIPCLatency = view.findViewById(R.id.tv_ipc_latency)
        recyclerView = view.findViewById(R.id.recycler_view)

        startMonitoring()
    }

    /**
     * 启动监控
     */
    private fun startMonitoring() {
        monitorScope.launch {
            while (isActive) {
                updateProcessStatus()
                delay(1000)  // 每秒更新一次
            }
        }
    }

    /**
     * 更新进程状态
     */
    private suspend fun updateProcessStatus() {
        withContext(Dispatchers.IO) {
            // 检查主进程状态
            val mainProcessAlive = checkProcessAlive(requireContext().packageName)

            // 检查SDK进程状态
            val sdkProcessAlive = checkProcessAlive("${requireContext().packageName}:stepcounter")

            withContext(Dispatchers.Main) {
                tvMainProcessStatus.text = "主进程: ${if (mainProcessAlive) "运行中" else "未运行"}"
                tvSDKProcessStatus.text = "SDK进程: ${if (sdkProcessAlive) "运行中" else "未运行"}"
            }
        }
    }

    /**
     * 检查进程是否存活
     */
    private fun checkProcessAlive(processName: String): Boolean {
        return try {
            val activityManager = requireContext().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = activityManager.runningAppProcesses

            processes?.any { it.processName == processName } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 测量IPC延迟
     */
    private fun measureIPCLatency() {
        monitorScope.launch {
            val startTime = System.nanoTime()

            // 通过SDK查询数据 (涉及跨进程通信)
            withContext(Dispatchers.IO) {
                StepCounterSDK.getInstance().getTodaySteps()
            }

            val endTime = System.nanoTime()
            val latency = (endTime - startTime) / 1_000_000.0  // 转换为毫秒

            tvIPCLatency.text = "IPC延迟: ${"%.2f".format(latency)} ms"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        monitorJob.cancel()
    }
}

/**
 * activity_multiprocess.xml布局文件示例
 */
/*
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:padding="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <!-- 进程信息 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="进程信息"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tv_process_info"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_rounded_card"
            android:padding="16dp"
            android:textSize="12sp"
            android:fontFamily="monospace"
            android:layout_marginBottom="24dp" />

        <!-- 主进程数据 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="主进程数据"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tv_main_process_data"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_rounded_card"
            android:padding="16dp"
            android:textSize="12sp"
            android:fontFamily="monospace"
            android:layout_marginBottom="16dp" />

        <!-- SDK进程数据 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="SDK进程数据"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tv_sdk_process_data"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_rounded_card"
            android:padding="16dp"
            android:textSize="12sp"
            android:fontFamily="monospace"
            android:layout_marginBottom="16dp" />

        <!-- 数据一致性 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="数据一致性"
            android:textSize="18sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <TextView
            android:id="@+id/tv_data_consistency"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_rounded_card"
            android:padding="16dp"
            android:textSize="12sp"
            android:fontFamily="monospace"
            android:layout_marginBottom="24dp" />

        <!-- 操作按钮 -->
        <Button
            android:id="@+id/btn_query_content_provider"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="查询ContentProvider"
            android:layout_marginBottom="8dp" />

        <Button
            android:id="@+id/btn_trigger_broadcast"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="触发广播通知"
            android:layout_marginBottom="8dp" />

        <Button
            android:id="@+id/btn_check_consistency"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="检查数据一致性" />

    </LinearLayout>
</ScrollView>
*/

/**
 * AndroidManifest.xml配置示例
 */
/*
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.stepcounter.multiprocess">

    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />

    <application
        android:name=".MultiProcessApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <!-- 主Activity -->
        <activity
            android:name=".MultiProcessActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- SDK服务运行在独立进程 (由SDK内部管理) -->
        <!-- 业务方无需配置 -->

    </application>

</manifest>
*/

/**
 * build.gradle依赖配置
 */
/*
dependencies {
    // 计步SDK
    implementation 'com.stepcounter.sdk:stepcounter:1.0.0'

    // Kotlin协程
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
}
*/
