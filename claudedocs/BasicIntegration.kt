/**
 * BasicIntegration.kt
 *
 * 计步SDK基础集成示例
 *
 * 展示内容:
 * 1. Application初始化SDK
 * 2. Activity中启动计步服务
 * 3. 注册监听器获取步数更新
 * 4. 查询当日步数
 * 5. 权限请求处理
 */

package com.example.stepcounter.sample

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stepcounter.sdk.StepCounterSDK
import com.stepcounter.sdk.StepCounterListener
import com.stepcounter.sdk.model.*

/**
 * Application初始化
 *
 * 必须在Application.onCreate中初始化SDK
 */
class MyApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()

        // SDK配置
        val config = StepConfig(
            weight = 65f,        // 用户体重 65kg
            height = 170f,       // 用户身高 170cm
            goalSteps = 10000,   // 每日目标 10000步
            sensitivity = 3,     // 灵敏度 3档 (1-5)
            autoStart = true,    // 开机自启
            foregroundService = true  // 使用前台服务
        )

        try {
            // 初始化SDK
            StepCounterSDK.init(this, config)

            // 启用调试日志 (仅在开发环境)
            if (BuildConfig.DEBUG) {
                StepCounterSDK.setDebugEnabled(true)
            }

            android.util.Log.d("MyApp", "StepCounterSDK initialized successfully")
        } catch (e: Exception) {
            android.util.Log.e("MyApp", "Failed to initialize StepCounterSDK", e)
        }
    }
}

/**
 * MainActivity - 基础集成示例
 */
class MainActivity : AppCompatActivity() {

    // UI组件
    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnRefresh: Button

    // SDK实例
    private val sdk = StepCounterSDK.getInstance()

    // 权限请求
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 权限授予,启动计步
            startStepCounting()
        } else {
            // 权限被拒绝
            Toast.makeText(this, "需要运动识别权限才能计步", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化UI
        initViews()

        // 检查并请求权限
        checkAndRequestPermission()
    }

    private fun initViews() {
        tvSteps = findViewById(R.id.tv_steps)
        tvCalories = findViewById(R.id.tv_calories)
        tvDistance = findViewById(R.id.tv_distance)
        tvStatus = findViewById(R.id.tv_status)
        btnStart = findViewById(R.id.btn_start)
        btnStop = findViewById(R.id.btn_stop)
        btnRefresh = findViewById(R.id.btn_refresh)

        // 按钮点击事件
        btnStart.setOnClickListener {
            if (checkPermission()) {
                startStepCounting()
            } else {
                requestPermission()
            }
        }

        btnStop.setOnClickListener {
            stopStepCounting()
        }

        btnRefresh.setOnClickListener {
            refreshStepData()
        }
    }

    /**
     * 检查权限
     */
    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 需要ACTIVITY_RECOGNITION权限
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 10以下不需要此权限
            true
        }
    }

    /**
     * 请求权限
     */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
    }

    /**
     * 检查并请求权限
     */
    private fun checkAndRequestPermission() {
        if (checkPermission()) {
            // 权限已授予
            startStepCounting()
        } else {
            // 请求权限
            requestPermission()
        }
    }

    /**
     * 启动计步服务
     */
    private fun startStepCounting() {
        val success = sdk.startCounting()

        if (success) {
            Toast.makeText(this, "计步已启动", Toast.LENGTH_SHORT).show()
            tvStatus.text = "运行中"
            btnStart.isEnabled = false
            btnStop.isEnabled = true

            // 注册监听器
            sdk.registerListener(stepListener)

            // 刷新显示当前数据
            refreshStepData()
        } else {
            Toast.makeText(this, "计步启动失败", Toast.LENGTH_SHORT).show()
            tvStatus.text = "启动失败"
        }
    }

    /**
     * 停止计步服务
     */
    private fun stopStepCounting() {
        sdk.stopCounting()
        Toast.makeText(this, "计步已停止", Toast.LENGTH_SHORT).show()
        tvStatus.text = "已停止"
        btnStart.isEnabled = true
        btnStop.isEnabled = false
    }

    /**
     * 刷新步数数据
     */
    private fun refreshStepData() {
        val todayData = sdk.getTodaySteps()

        if (todayData != null) {
            updateUI(todayData)
        } else {
            tvSteps.text = "0"
            tvCalories.text = "0.0 kcal"
            tvDistance.text = "0.00 km"
        }
    }

    /**
     * 步数监听器
     */
    private val stepListener = object : StepCounterListener {
        /**
         * 步数变化回调 (主线程)
         */
        override fun onStepCountChanged(stepData: StepData) {
            // 直接更新UI (已在主线程)
            updateUI(stepData)
        }

        /**
         * 服务状态变化回调 (主线程)
         */
        override fun onServiceStatusChanged(isRunning: Boolean) {
            tvStatus.text = if (isRunning) "运行中" else "已停止"
            btnStart.isEnabled = !isRunning
            btnStop.isEnabled = isRunning
        }

        /**
         * 达成目标回调 (主线程)
         */
        override fun onGoalAchieved(goalSteps: Int) {
            // 显示祝贺消息
            Toast.makeText(
                this@MainActivity,
                "恭喜!您已完成今日目标: $goalSteps 步",
                Toast.LENGTH_LONG
            ).show()
        }

        /**
         * 错误回调 (主线程)
         */
        override fun onError(error: StepError) {
            when (error.code) {
                1001 -> {
                    // 权限被拒绝
                    Toast.makeText(
                        this@MainActivity,
                        "需要运动识别权限",
                        Toast.LENGTH_LONG
                    ).show()
                }
                1002 -> {
                    // 传感器不可用
                    Toast.makeText(
                        this@MainActivity,
                        "您的设备不支持计步功能",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    // 其他错误
                    Toast.makeText(
                        this@MainActivity,
                        "错误: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * 更新UI显示
     */
    private fun updateUI(stepData: StepData) {
        // 步数
        tvSteps.text = stepData.steps.toString()

        // 卡路里
        tvCalories.text = String.format("%.1f kcal", stepData.calories)

        // 距离
        tvDistance.text = String.format("%.2f km", stepData.distance)
    }

    override fun onDestroy() {
        super.onDestroy()

        // 注销监听器,避免内存泄漏
        sdk.unregisterListener(stepListener)
    }
}

/**
 * activity_main.xml布局文件
 *
 * 注意: 这里只是布局示例,实际项目中需创建对应的XML文件
 */
/*
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <TextView
        android:id="@+id/tv_status"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="未启动"
        android:textSize="18sp"
        android:gravity="center"
        android:layout_marginBottom="24dp" />

    <TextView
        android:id="@+id/tv_steps"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="0"
        android:textSize="48sp"
        android:gravity="center"
        android:textStyle="bold"
        android:layout_marginBottom="8dp" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="步"
        android:textSize="16sp"
        android:gravity="center"
        android:layout_marginBottom="32dp" />

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="32dp">

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:gravity="center">

            <TextView
                android:id="@+id/tv_calories"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0.0 kcal"
                android:textSize="16sp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="卡路里"
                android:textSize="12sp"
                android:textColor="#666666" />
        </LinearLayout>

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:gravity="center">

            <TextView
                android:id="@+id/tv_distance"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="0.00 km"
                android:textSize="16sp" />

            <TextView
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="距离"
                android:textSize="12sp"
                android:textColor="#666666" />
        </LinearLayout>
    </LinearLayout>

    <Button
        android:id="@+id/btn_start"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="开始计步"
        android:layout_marginBottom="8dp" />

    <Button
        android:id="@+id/btn_stop"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="停止计步"
        android:enabled="false"
        android:layout_marginBottom="8dp" />

    <Button
        android:id="@+id/btn_refresh"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="刷新数据" />

</LinearLayout>
*/

/**
 * AndroidManifest.xml配置
 *
 * 注意: 需要在AndroidManifest.xml中注册Application
 */
/*
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.stepcounter.sample">

    <!-- 必须权限 -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_HEALTH" />

    <!-- Android 13+ 通知权限 -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".MyApplication"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

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

    // AndroidX
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
}
*/
