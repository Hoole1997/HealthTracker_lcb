/**
 * NotificationCustomExample.kt
 *
 * 完整示例：演示如何自定义计步SDK的前台服务通知
 *
 * 功能演示：
 * 1. 自定义通知布局（RemoteViews）
 * 2. 动态更新通知内容（Callback机制）
 * 3. 多种通知样式切换（3种预设样式）
 * 4. Android 13+通知权限处理
 *
 * 文件结构：
 * - NotificationCustomApplication: Application初始化
 * - CustomNotificationCallback: 通知更新回调实现
 * - NotificationCustomActivity: UI交互和样式切换
 * - 3个自定义布局XML（在注释中）
 *
 * @author Claude AI
 * @version 1.0.0
 * @since 2024-XX-XX
 */

package com.example.stepcounter

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stepcounter.sdk.StepConfig
import com.stepcounter.sdk.StepCounterSDK
import com.stepcounter.sdk.StepData
import com.stepcounter.sdk.notification.NotificationConfig
import com.stepcounter.sdk.notification.NotificationUpdateCallback
import java.text.DecimalFormat

// ============================================================
// Application类：SDK初始化和通知配置
// ============================================================

class NotificationCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 创建通知配置
        val notificationConfig = NotificationConfig(
            // 自定义通知布局（紧凑样式）
            customLayout = R.layout.notification_step_minimal,
            // 自定义展开布局（详细样式，可选）
            customBigLayout = R.layout.notification_step_detailed,
            // 小图标（必须是纯白色透明背景）
            smallIcon = R.drawable.ic_notification_shoe,
            // 通知渠道名称
            channelName = "我的计步通知",
            // 通知渠道重要性（LOW = 无声音无震动）
            channelImportance = NotificationManager.IMPORTANCE_LOW,
            // 更新回调（核心：动态更新RemoteViews内容）
            updateCallback = CustomNotificationCallback(),
            // 点击通知时的Intent
            contentIntent = createContentIntent()
        )

        // SDK完整配置
        val config = StepConfig(
            weight = 65f,           // 体重（公斤）
            height = 170f,          // 身高（厘米）
            goalSteps = 10000,      // 目标步数
            notificationConfig = notificationConfig
        )

        // 初始化SDK
        StepCounterSDK.init(this, config)
    }

    /**
     * 创建通知点击Intent
     * 点击通知时打开MainActivity
     */
    private fun createContentIntent(): PendingIntent {
        val intent = Intent(this, NotificationCustomActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

// ============================================================
// 通知更新回调：自定义RemoteViews更新逻辑
// ============================================================

class CustomNotificationCallback : NotificationUpdateCallback {

    private val decimalFormat = DecimalFormat("#.#")

    /**
     * SDK回调此方法来更新通知内容
     *
     * @param remoteViews RemoteViews对象（由SDK创建并传入）
     * @param stepData 当前计步数据
     *
     * @note 此方法在后台线程调用，频率约1秒一次
     * @note 仅更新必要的View，避免过度刷新
     */
    override fun onUpdateRemoteViews(remoteViews: RemoteViews, stepData: StepData) {
        // 1. 更新步数（带千位分隔符）
        val stepsText = formatNumber(stepData.steps)
        remoteViews.setTextViewText(R.id.tv_steps, stepsText)

        // 2. 更新卡路里（保留1位小数）
        val caloriesText = "${decimalFormat.format(stepData.calories)} kcal"
        remoteViews.setTextViewText(R.id.tv_calories, caloriesText)

        // 3. 更新距离（公里，保留2位小数）
        val distanceKm = stepData.distance / 1000.0
        val distanceText = "${String.format("%.2f", distanceKm)} km"
        remoteViews.setTextViewText(R.id.tv_distance, distanceText)

        // 4. 更新进度条
        remoteViews.setProgressBar(
            R.id.pb_progress,
            stepData.goalSteps,  // max
            stepData.steps,      // progress
            false                // indeterminate
        )

        // 5. 根据进度百分比更新文本颜色
        val progressPercent = stepData.steps.toFloat() / stepData.goalSteps
        val textColor = when {
            progressPercent >= 1.0f -> Color.parseColor("#4CAF50")  // Green - 已达标
            progressPercent >= 0.8f -> Color.parseColor("#FF9800")  // Orange - 接近目标
            progressPercent >= 0.5f -> Color.parseColor("#2196F3")  // Blue - 进行中
            else -> Color.parseColor("#666666")                     // Gray - 刚开始
        }
        remoteViews.setTextColor(R.id.tv_steps, textColor)

        // 6. 更新目标达成状态图标
        val iconRes = if (progressPercent >= 1.0f) {
            R.drawable.ic_goal_achieved  // 达标图标
        } else {
            R.drawable.ic_goal_progress  // 进行中图标
        }
        remoteViews.setImageViewResource(R.id.iv_status, iconRes)

        // 7. 更新时间戳（可选，显示最后更新时间）
        val currentTime = System.currentTimeMillis()
        val timeText = android.text.format.DateFormat.format("HH:mm", currentTime).toString()
        remoteViews.setTextViewText(R.id.tv_update_time, timeText)
    }

    /**
     * 格式化数字（添加千位分隔符）
     * 例如：10000 → "10,000"
     */
    private fun formatNumber(number: Int): String {
        return String.format("%,d", number)
    }
}

// ============================================================
// MainActivity：UI交互和通知样式切换
// ============================================================

class NotificationCustomActivity : AppCompatActivity() {

    // Android 13+通知权限请求
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
            startCountingWithPermission()
        } else {
            Toast.makeText(this, "通知权限被拒绝，无法显示前台通知", Toast.LENGTH_LONG).show()
        }
    }

    // UI组件
    private lateinit var btnStartCounting: Button
    private lateinit var btnStopCounting: Button
    private lateinit var btnStyleMinimal: Button
    private lateinit var btnStyleDetailed: Button
    private lateinit var btnStyleCard: Button
    private lateinit var tvCurrentSteps: TextView
    private lateinit var tvCurrentCalories: TextView
    private lateinit var tvCurrentDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_custom)

        initViews()
        setupListeners()
        observeStepData()
    }

    /**
     * 初始化UI组件
     */
    private fun initViews() {
        btnStartCounting = findViewById(R.id.btn_start_counting)
        btnStopCounting = findViewById(R.id.btn_stop_counting)
        btnStyleMinimal = findViewById(R.id.btn_style_minimal)
        btnStyleDetailed = findViewById(R.id.btn_style_detailed)
        btnStyleCard = findViewById(R.id.btn_style_card)
        tvCurrentSteps = findViewById(R.id.tv_current_steps)
        tvCurrentCalories = findViewById(R.id.tv_current_calories)
        tvCurrentDistance = findViewById(R.id.tv_current_distance)

        // 初始状态：停止中
        updateButtonStates(isCounting = false)
    }

    /**
     * 设置按钮点击监听
     */
    private fun setupListeners() {
        // 开始计步按钮
        btnStartCounting.setOnClickListener {
            checkNotificationPermissionAndStart()
        }

        // 停止计步按钮
        btnStopCounting.setOnClickListener {
            StepCounterSDK.stopCounting(this)
            updateButtonStates(isCounting = false)
            Toast.makeText(this, "计步已停止", Toast.LENGTH_SHORT).show()
        }

        // 样式切换：紧凑样式
        btnStyleMinimal.setOnClickListener {
            switchNotificationStyle(NotificationStyle.MINIMAL)
        }

        // 样式切换：详细样式
        btnStyleDetailed.setOnClickListener {
            switchNotificationStyle(NotificationStyle.DETAILED)
        }

        // 样式切换：卡片样式
        btnStyleCard.setOnClickListener {
            switchNotificationStyle(NotificationStyle.CARD)
        }
    }

    /**
     * 观察计步数据变化并更新UI
     */
    private fun observeStepData() {
        StepCounterSDK.getTodayStepsLiveData().observe(this) { stepData ->
            tvCurrentSteps.text = "步数：${stepData.steps}"
            tvCurrentCalories.text = "卡路里：${String.format("%.1f", stepData.calories)} kcal"
            tvCurrentDistance.text = "距离：${String.format("%.2f", stepData.distance / 1000.0)} km"
        }
    }

    /**
     * 检查通知权限并开始计步
     */
    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+需要运行时通知权限
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    startCountingWithPermission()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // 用户之前拒绝过，显示说明对话框
                    showPermissionRationaleDialog()
                }
                else -> {
                    // 首次请求或用户勾选了"不再询问"
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12及以下自动拥有通知权限
            startCountingWithPermission()
        }
    }

    /**
     * 显示权限说明对话框
     */
    private fun showPermissionRationaleDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("需要通知权限")
            .setMessage("计步器需要显示前台通知以保持后台持续运行，这是保证计步准确性的必要条件。")
            .setPositiveButton("授予权限") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "未授予通知权限，无法开始计步", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    /**
     * 权限已授予，开始计步
     */
    private fun startCountingWithPermission() {
        StepCounterSDK.startCounting(this)
        updateButtonStates(isCounting = true)
        Toast.makeText(this, "计步已开始", Toast.LENGTH_SHORT).show()
    }

    /**
     * 更新按钮状态
     */
    private fun updateButtonStates(isCounting: Boolean) {
        btnStartCounting.isEnabled = !isCounting
        btnStopCounting.isEnabled = isCounting
        btnStyleMinimal.isEnabled = isCounting
        btnStyleDetailed.isEnabled = isCounting
        btnStyleCard.isEnabled = isCounting
    }

    /**
     * 切换通知样式
     */
    private fun switchNotificationStyle(style: NotificationStyle) {
        val (layoutResId, bigLayoutResId, styleName) = when (style) {
            NotificationStyle.MINIMAL -> Triple(
                R.layout.notification_step_minimal,
                null,
                "紧凑样式"
            )
            NotificationStyle.DETAILED -> Triple(
                R.layout.notification_step_detailed,
                R.layout.notification_step_detailed_big,
                "详细样式"
            )
            NotificationStyle.CARD -> Triple(
                R.layout.notification_step_card,
                null,
                "卡片样式"
            )
        }

        // 更新通知配置
        val newConfig = NotificationConfig(
            customLayout = layoutResId,
            customBigLayout = bigLayoutResId,
            smallIcon = R.drawable.ic_notification_shoe,
            channelName = "我的计步通知",
            channelImportance = NotificationManager.IMPORTANCE_LOW,
            updateCallback = CustomNotificationCallback(),
            contentIntent = (application as NotificationCustomApplication).createContentIntent()
        )

        // 应用新配置
        StepCounterSDK.updateNotificationConfig(newConfig)

        Toast.makeText(this, "已切换到$styleName", Toast.LENGTH_SHORT).show()

        // 高亮当前选中样式按钮
        highlightSelectedStyleButton(style)
    }

    /**
     * 高亮选中的样式按钮
     */
    private fun highlightSelectedStyleButton(selectedStyle: NotificationStyle) {
        btnStyleMinimal.isSelected = (selectedStyle == NotificationStyle.MINIMAL)
        btnStyleDetailed.isSelected = (selectedStyle == NotificationStyle.DETAILED)
        btnStyleCard.isSelected = (selectedStyle == NotificationStyle.CARD)
    }

    /**
     * 通知样式枚举
     */
    private enum class NotificationStyle {
        MINIMAL,   // 紧凑样式：仅显示步数和进度条
        DETAILED,  // 详细样式：显示步数、卡路里、距离、时间
        CARD       // 卡片样式：卡片背景、圆角、阴影效果
    }
}

// ============================================================
// 自定义通知布局XML（作为注释提供参考）
// ============================================================

/*
 * ============================================================
 * 布局1：notification_step_minimal.xml（紧凑样式）
 * ============================================================
 *
 * res/layout/notification_step_minimal.xml
 *
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:orientation="horizontal"
    android:padding="12dp"
    android:gravity="center_vertical"
    android:background="@android:color/white">

    <!-- 鞋子图标 -->
    <ImageView
        android:id="@+id/iv_status"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:src="@drawable/ic_shoe"
        android:layout_marginEnd="12dp"
        android:contentDescription="@string/app_icon"/>

    <!-- 中间内容区域 -->
    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <!-- 步数 -->
        <TextView
            android:id="@+id/tv_steps"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="#333333"/>

        <!-- 进度条 -->
        <ProgressBar
            android:id="@+id/pb_progress"
            style="?android:attr/progressBarStyleHorizontal"
            android:layout_width="match_parent"
            android:layout_height="4dp"
            android:layout_marginTop="4dp"
            android:max="10000"
            android:progress="0"/>

    </LinearLayout>

    <!-- 更新时间 -->
    <TextView
        android:id="@+id/tv_update_time"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="00:00"
        android:textSize="12sp"
        android:textColor="#999999"
        android:layout_marginStart="8dp"/>

</LinearLayout>

 * ============================================================
 * 布局2：notification_step_detailed.xml（详细样式）
 * ============================================================
 *
 * res/layout/notification_step_detailed.xml
 *
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="96dp"
    android:orientation="vertical"
    android:padding="12dp"
    android:background="@android:color/white">

    <!-- 第一行：标题和状态图标 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="今日步数"
            android:textSize="14sp"
            android:textColor="#666666"/>

        <ImageView
            android:id="@+id/iv_status"
            android:layout_width="20dp"
            android:layout_height="20dp"
            android:src="@drawable/ic_goal_progress"
            android:contentDescription="@string/goal_status"/>

        <TextView
            android:id="@+id/tv_update_time"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="00:00"
            android:textSize="12sp"
            android:textColor="#999999"
            android:layout_marginStart="4dp"/>

    </LinearLayout>

    <!-- 第二行：步数 -->
    <TextView
        android:id="@+id/tv_steps"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="0"
        android:textSize="24sp"
        android:textStyle="bold"
        android:textColor="#333333"
        android:layout_marginTop="4dp"/>

    <!-- 第三行：进度条 -->
    <ProgressBar
        android:id="@+id/pb_progress"
        style="?android:attr/progressBarStyleHorizontal"
        android:layout_width="match_parent"
        android:layout_height="6dp"
        android:layout_marginTop="4dp"
        android:max="10000"
        android:progress="0"/>

    <!-- 第四行：卡路里和距离 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginTop="4dp">

        <TextView
            android:id="@+id/tv_calories"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="0.0 kcal"
            android:textSize="12sp"
            android:textColor="#666666"/>

        <TextView
            android:id="@+id/tv_distance"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0.00 km"
            android:textSize="12sp"
            android:textColor="#666666"/>

    </LinearLayout>

</LinearLayout>

 * ============================================================
 * 布局3：notification_step_card.xml（卡片样式）
 * ============================================================
 *
 * res/layout/notification_step_card.xml
 *
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="120dp"
    app:cardCornerRadius="12dp"
    app:cardElevation="4dp"
    app:cardBackgroundColor="@android:color/white"
    android:layout_margin="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:padding="16dp"
        android:background="@drawable/gradient_background">

        <!-- 第一行：标题和时间 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical">

            <ImageView
                android:id="@+id/iv_status"
                android:layout_width="24dp"
                android:layout_height="24dp"
                android:src="@drawable/ic_shoe"
                android:contentDescription="@string/app_icon"/>

            <TextView
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="今日步数"
                android:textSize="14sp"
                android:textColor="#FFFFFF"
                android:layout_marginStart="8dp"/>

            <TextView
                android:id="@+id/tv_update_time"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="00:00"
                android:textSize="12sp"
                android:textColor="#CCFFFFFF"/>

        </LinearLayout>

        <!-- 第二行：步数大数字 -->
        <TextView
            android:id="@+id/tv_steps"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="0"
            android:textSize="36sp"
            android:textStyle="bold"
            android:textColor="#FFFFFF"
            android:layout_marginTop="8dp"
            android:layout_gravity="center_horizontal"/>

        <!-- 第三行：进度条 -->
        <ProgressBar
            android:id="@+id/pb_progress"
            style="?android:attr/progressBarStyleHorizontal"
            android:layout_width="match_parent"
            android:layout_height="8dp"
            android:layout_marginTop="8dp"
            android:max="10000"
            android:progress="0"
            android:progressTint="#FFFFFF"/>

        <!-- 第四行：卡路里和距离 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="8dp">

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="horizontal"
                android:gravity="center">

                <ImageView
                    android:layout_width="16dp"
                    android:layout_height="16dp"
                    android:src="@drawable/ic_flame"
                    android:contentDescription="@string/calories"/>

                <TextView
                    android:id="@+id/tv_calories"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0.0 kcal"
                    android:textSize="12sp"
                    android:textColor="#FFFFFF"
                    android:layout_marginStart="4dp"/>

            </LinearLayout>

            <LinearLayout
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:orientation="horizontal"
                android:gravity="center">

                <ImageView
                    android:layout_width="16dp"
                    android:layout_height="16dp"
                    android:src="@drawable/ic_distance"
                    android:contentDescription="@string/distance"/>

                <TextView
                    android:id="@+id/tv_distance"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="0.00 km"
                    android:textSize="12sp"
                    android:textColor="#FFFFFF"
                    android:layout_marginStart="4dp"/>

            </LinearLayout>

        </LinearLayout>

    </LinearLayout>

</androidx.cardview.widget.CardView>

*/

// ============================================================
// drawable资源（作为注释提供参考）
// ============================================================

/*
 * ============================================================
 * res/drawable/gradient_background.xml（卡片样式渐变背景）
 * ============================================================
 *
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <gradient
        android:startColor="#4CAF50"
        android:endColor="#2196F3"
        android:angle="135"
        android:type="linear"/>
</shape>

 * ============================================================
 * res/drawable/ic_notification_shoe.xml（通知图标）
 * ============================================================
 *
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M20,8h-3V6c0-1.1-0.9-2-2-2H9C7.9,4,7,4.9,7,6v2H4C2.9,8,2,8.9,2,10v9c0,1.1,0.9,2,2,2h16c1.1,0,2-0.9,2-2v-9C22,8.9,21.1,8,20,8z M9,6h6v2H9V6z M20,19H4v-7h16V19z"/>
</vector>

 * ============================================================
 * res/drawable/ic_goal_achieved.xml（目标达成图标）
 * ============================================================
 *
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#4CAF50"
        android:pathData="M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41-1.41z"/>
</vector>

 * ============================================================
 * res/drawable/ic_goal_progress.xml（进行中图标）
 * ============================================================
 *
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF9800"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM12,20c-4.41,0 -8,-3.59 -8,-8s3.59,-8 8,-8 8,3.59 8,8 -3.59,8 -8,8z"/>
    <path
        android:fillColor="#FF9800"
        android:pathData="M13,7h-2v5.41l4.29,4.29 1.41,-1.41 -3.7,-3.7z"/>
</vector>

*/

// ============================================================
// 使用说明和最佳实践
// ============================================================

/**
 * ## 使用说明
 *
 * ### 1. 基础集成
 * ```kotlin
 * // 在Application中初始化
 * class MyApp : Application() {
 *     override fun onCreate() {
 *         super.onCreate()
 *         // 使用NotificationCustomApplication的代码
 *     }
 * }
 * ```
 *
 * ### 2. 自定义布局要点
 *
 * #### 2.1 View ID约定
 * SDK通过View ID识别需要更新的组件，必须遵守以下约定：
 * - `R.id.tv_steps`: 步数TextView
 * - `R.id.tv_calories`: 卡路里TextView
 * - `R.id.tv_distance`: 距离TextView
 * - `R.id.pb_progress`: 进度条ProgressBar
 * - `R.id.iv_status`: 状态图标ImageView
 * - `R.id.tv_update_time`: 更新时间TextView
 *
 * #### 2.2 RemoteViews限制
 * RemoteViews仅支持部分View类型和方法：
 * - 支持的View: TextView, ImageView, Button, ProgressBar, ImageButton
 * - 不支持：自定义View, RecyclerView, WebView
 * - 支持的方法：setTextViewText, setImageViewResource, setProgressBar等
 * - 不支持：复杂的布局动画, 触摸事件监听
 *
 * ### 3. 通知权限处理（Android 13+）
 *
 * #### 3.1 在AndroidManifest.xml中声明
 * ```xml
 * <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
 * ```
 *
 * #### 3.2 运行时请求（参考NotificationCustomActivity）
 * ```kotlin
 * if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
 *     requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE)
 * }
 * ```
 *
 * ### 4. 样式切换最佳实践
 *
 * #### 4.1 何时切换样式
 * - 用户偏好设置：允许用户选择喜欢的样式
 * - 场景自适应：运动时显示详细信息，日常显示紧凑信息
 * - 性能优化：低电量模式使用紧凑样式减少刷新开销
 *
 * #### 4.2 样式设计原则
 * - 紧凑样式（MINIMAL）：高度≤64dp，信息密度高，适合日常
 * - 详细样式（DETAILED）：高度≤128dp，信息完整，适合运动中
 * - 卡片样式（CARD）：视觉丰富，品牌突出，适合吸引用户关注
 *
 * ### 5. 性能优化建议
 *
 * #### 5.1 减少更新频率
 * ```kotlin
 * override fun onUpdateRemoteViews(remoteViews: RemoteViews, stepData: StepData) {
 *     // 仅在步数变化时更新
 *     if (stepData.steps != lastSteps) {
 *         remoteViews.setTextViewText(R.id.tv_steps, stepData.steps.toString())
 *         lastSteps = stepData.steps
 *     }
 * }
 * ```
 *
 * #### 5.2 避免复杂计算
 * ```kotlin
 * // 不推荐：每次更新都重新计算
 * val color = calculateColorByProgress(stepData.steps, stepData.goalSteps)
 *
 * // 推荐：使用预定义的颜色映射
 * val color = COLOR_MAP[progressPercent.toInt() / 10]
 * ```
 *
 * ### 6. 调试技巧
 *
 * #### 6.1 查看RemoteViews更新日志
 * ```kotlin
 * override fun onUpdateRemoteViews(remoteViews: RemoteViews, stepData: StepData) {
 *     Log.d("Notification", "Update: steps=${stepData.steps}, time=${System.currentTimeMillis()}")
 *     // ... 更新逻辑
 * }
 * ```
 *
 * #### 6.2 测试不同通知样式
 * - 使用Activity Result测试通知点击跳转
 * - 在不同Android版本设备上测试兼容性
 * - 测试长时间运行后的通知显示状态
 *
 * ## 常见问题
 *
 * ### Q1: 通知不显示？
 * A: 检查以下几点：
 * 1. Android 13+是否授予POST_NOTIFICATIONS权限
 * 2. 通知渠道是否正确创建（SDK会自动创建）
 * 3. 是否调用了startCounting()方法
 * 4. 检查系统通知设置，确保应用通知未被禁用
 *
 * ### Q2: RemoteViews更新不生效？
 * A: 可能的原因：
 * 1. View ID不符合约定（必须是R.id.tv_steps等）
 * 2. 使用了RemoteViews不支持的View类型
 * 3. 布局层级过深（RemoteViews有性能限制）
 *
 * ### Q3: 通知点击无响应？
 * A: 检查contentIntent是否正确设置：
 * ```kotlin
 * val intent = Intent(context, MainActivity::class.java).apply {
 *     flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
 * }
 * val pendingIntent = PendingIntent.getActivity(
 *     context, 0, intent, PendingIntent.FLAG_IMMUTABLE
 * )
 * ```
 *
 * ### Q4: 样式切换后通知消失？
 * A: 切换样式时SDK会重新创建通知，正常现象。确保：
 * 1. updateNotificationConfig()在计步服务运行时调用
 * 2. 新布局XML文件存在且没有语法错误
 *
 * ## 版本兼容性
 *
 * - Android 6.0 (API 23)+: 基础功能支持
 * - Android 8.0 (API 26)+: 通知渠道支持
 * - Android 13 (API 33)+: 需要POST_NOTIFICATIONS运行时权限
 * - Android 14 (API 34)+: FOREGROUND_SERVICE_HEALTH类型
 *
 * ## 参考资料
 *
 * - [Android官方文档 - 通知](https://developer.android.com/develop/ui/views/notifications)
 * - [RemoteViews使用指南](https://developer.android.com/reference/android/widget/RemoteViews)
 * - [前台服务最佳实践](https://developer.android.com/develop/background-work/services/foreground-services)
 */