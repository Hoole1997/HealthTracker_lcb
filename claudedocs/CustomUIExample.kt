/**
 * CustomUIExample.kt
 *
 * 计步SDK自定义UI集成示例
 *
 * 展示内容:
 * 1. 自定义圆形进度条UI
 * 2. 查询历史数据并显示图表
 * 3. 获取统计数据(周/月/年)
 * 4. 更新用户配置
 * 5. 调整灵敏度设置
 * 6. 使用ViewModel + LiveData架构
 */

package com.example.stepcounter.customui

import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.stepcounter.sdk.StepCounterSDK
import com.stepcounter.sdk.StepCounterListener
import com.stepcounter.sdk.model.*
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

/**
 * StepViewModel - 使用ViewModel管理步数数据
 */
class StepViewModel : ViewModel() {

    private val sdk = StepCounterSDK.getInstance()

    // LiveData - 步数数据
    private val _stepData = MutableLiveData<StepData>()
    val stepData: LiveData<StepData> = _stepData

    // LiveData - 服务状态
    private val _serviceRunning = MutableLiveData<Boolean>()
    val serviceRunning: LiveData<Boolean> = _serviceRunning

    // LiveData - 历史数据
    private val _historyData = MutableLiveData<List<StepData>>()
    val historyData: LiveData<List<StepData>> = _historyData

    // LiveData - 统计数据
    private val _weekStatistics = MutableLiveData<StepStatistics>()
    val weekStatistics: LiveData<StepStatistics> = _weekStatistics

    // 监听器
    private val stepListener = object : StepCounterListener {
        override fun onStepCountChanged(stepData: StepData) {
            _stepData.postValue(stepData)
        }

        override fun onServiceStatusChanged(isRunning: Boolean) {
            _serviceRunning.postValue(isRunning)
        }

        override fun onGoalAchieved(goalSteps: Int) {
            // 可以发送事件通知UI显示动画
        }

        override fun onError(error: StepError) {
            // 错误处理
        }
    }

    init {
        // 注册监听器
        sdk.registerListener(stepListener)

        // 初始化数据
        loadTodayData()
        loadWeekStatistics()
    }

    /**
     * 加载当日数据
     */
    fun loadTodayData() {
        val today = sdk.getTodaySteps()
        if (today != null) {
            _stepData.postValue(today)
        }
    }

    /**
     * 加载最近7天历史数据
     */
    fun loadHistoryData() {
        val endDate = System.currentTimeMillis()
        val startDate = endDate - 7 * 24 * 3600 * 1000L

        val history = sdk.getHistoryData(startDate, endDate)
        _historyData.postValue(history)
    }

    /**
     * 加载本周统计数据
     */
    fun loadWeekStatistics() {
        val stats = sdk.getStatistics(StatisticsType.WEEK)
        _weekStatistics.postValue(stats)
    }

    /**
     * 更新用户配置
     */
    fun updateUserConfig(weight: Float, height: Float, goalSteps: Int) {
        val currentConfig = sdk.getCurrentConfig()
        val newConfig = currentConfig.copy(
            weight = weight,
            height = height,
            goalSteps = goalSteps
        )
        sdk.updateConfig(newConfig)

        // 重新加载数据
        loadTodayData()
    }

    /**
     * 更新灵敏度
     */
    fun updateSensitivity(sensitivity: Int) {
        sdk.setSensitivity(sensitivity)
    }

    override fun onCleared() {
        super.onCleared()
        // 注销监听器
        sdk.unregisterListener(stepListener)
    }
}

/**
 * CustomUIActivity - 自定义UI示例
 */
class CustomUIActivity : AppCompatActivity() {

    // ViewModel
    private lateinit var viewModel: StepViewModel

    // UI组件
    private lateinit var circularProgressView: CircularProgressView
    private lateinit var lineChart: LineChart
    private lateinit var tvTotalSteps: TextView
    private lateinit var tvAvgSteps: TextView
    private lateinit var tvMaxSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvDistance: TextView
    private lateinit var seekBarSensitivity: SeekBar
    private lateinit var etWeight: EditText
    private lateinit var etHeight: EditText
    private lateinit var etGoal: EditText
    private lateinit var btnSaveConfig: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_ui)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[StepViewModel::class.java]

        // 初始化UI
        initViews()

        // 观察数据变化
        observeData()
    }

    private fun initViews() {
        circularProgressView = findViewById(R.id.circular_progress)
        lineChart = findViewById(R.id.line_chart)
        tvTotalSteps = findViewById(R.id.tv_total_steps)
        tvAvgSteps = findViewById(R.id.tv_avg_steps)
        tvMaxSteps = findViewById(R.id.tv_max_steps)
        tvCalories = findViewById(R.id.tv_calories)
        tvDistance = findViewById(R.id.tv_distance)
        seekBarSensitivity = findViewById(R.id.seekbar_sensitivity)
        etWeight = findViewById(R.id.et_weight)
        etHeight = findViewById(R.id.et_height)
        etGoal = findViewById(R.id.et_goal)
        btnSaveConfig = findViewById(R.id.btn_save_config)

        // 配置灵敏度SeekBar
        seekBarSensitivity.max = 4  // 1-5档,0-4对应1-5
        seekBarSensitivity.progress = 2  // 默认3档
        seekBarSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val sensitivity = progress + 1  // 转换为1-5
                    viewModel.updateSensitivity(sensitivity)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // 保存配置按钮
        btnSaveConfig.setOnClickListener {
            saveUserConfig()
        }

        // 配置图表样式
        configureLineChart()

        // 加载历史数据
        viewModel.loadHistoryData()
    }

    private fun observeData() {
        // 观察步数数据变化
        viewModel.stepData.observe(this) { stepData ->
            updateStepUI(stepData)
        }

        // 观察历史数据变化
        viewModel.historyData.observe(this) { historyData ->
            updateChartData(historyData)
        }

        // 观察统计数据变化
        viewModel.weekStatistics.observe(this) { statistics ->
            updateStatisticsUI(statistics)
        }
    }

    /**
     * 更新步数UI
     */
    private fun updateStepUI(stepData: StepData) {
        // 更新圆形进度条
        val goalSteps = 10000  // 从配置获取
        val progress = (stepData.steps * 100 / goalSteps).coerceAtMost(100)
        circularProgressView.setProgress(progress)
        circularProgressView.setCenterText("${stepData.steps}\n步")

        // 更新卡路里和距离
        tvCalories.text = String.format("%.1f kcal", stepData.calories)
        tvDistance.text = String.format("%.2f km", stepData.distance)
    }

    /**
     * 更新图表数据
     */
    private fun updateChartData(historyData: List<StepData>) {
        val entries = mutableListOf<Entry>()

        historyData.reversed().forEachIndexed { index, stepData ->
            entries.add(Entry(index.toFloat(), stepData.steps.toFloat()))
        }

        val dataSet = LineDataSet(entries, "最近7天步数").apply {
            color = getColor(R.color.colorPrimary)
            lineWidth = 2f
            setCircleColor(getColor(R.color.colorPrimary))
            circleRadius = 4f
            setDrawValues(true)
            valueTextSize = 10f
        }

        val lineData = LineData(dataSet)
        lineChart.data = lineData
        lineChart.invalidate()
    }

    /**
     * 更新统计UI
     */
    private fun updateStatisticsUI(statistics: StepStatistics) {
        tvTotalSteps.text = "${statistics.totalSteps} 步"
        tvAvgSteps.text = "平均 ${statistics.avgSteps} 步/天"
        tvMaxSteps.text = "最高 ${statistics.maxSteps} 步"
    }

    /**
     * 保存用户配置
     */
    private fun saveUserConfig() {
        try {
            val weight = etWeight.text.toString().toFloat()
            val height = etHeight.text.toString().toFloat()
            val goal = etGoal.text.toString().toInt()

            // 验证输入
            if (weight in 30f..200f && height in 100f..250f && goal in 1000..100000) {
                viewModel.updateUserConfig(weight, height, goal)
                Toast.makeText(this, "配置已保存", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "输入参数超出范围", Toast.LENGTH_SHORT).show()
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "请输入有效数字", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 配置图表样式
     */
    private fun configureLineChart() {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)

            // X轴配置
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
            }

            // 左Y轴配置
            axisLeft.apply {
                setDrawGridLines(true)
                axisMinimum = 0f
            }

            // 右Y轴禁用
            axisRight.isEnabled = false

            // 图例配置
            legend.isEnabled = true
        }
    }
}

/**
 * CircularProgressView - 自定义圆形进度条
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0
    private var centerText = ""

    private val progressPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = Color.parseColor("#FF6200EE")
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 20f
        color = Color.parseColor("#E0E0E0")
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 48f
        color = Color.BLACK
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (min(width, height) / 2f) - 30f

        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // 绘制背景圆环
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // 绘制进度圆弧
        val sweepAngle = 360f * progress / 100f
        canvas.drawArc(rect, -90f, sweepAngle, false, progressPaint)

        // 绘制中心文字
        if (centerText.isNotEmpty()) {
            val lines = centerText.split("\n")
            val lineHeight = textPaint.textSize + 10f
            val totalHeight = lines.size * lineHeight
            var y = centerY - (totalHeight / 2) + textPaint.textSize

            lines.forEach { line ->
                canvas.drawText(line, centerX, y, textPaint)
                y += lineHeight
            }
        }
    }

    /**
     * 设置进度 (0-100)
     */
    fun setProgress(progress: Int) {
        this.progress = progress.coerceIn(0, 100)
        invalidate()
    }

    /**
     * 设置中心文字
     */
    fun setCenterText(text: String) {
        this.centerText = text
        invalidate()
    }
}

/**
 * activity_custom_ui.xml布局文件示例
 */
/*
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <!-- 圆形进度条 -->
        <com.example.stepcounter.customui.CircularProgressView
            android:id="@+id/circular_progress"
            android:layout_width="200dp"
            android:layout_height="200dp"
            android:layout_gravity="center"
            android:layout_marginBottom="24dp" />

        <!-- 卡路里和距离 -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginBottom="24dp">

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
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="卡路里"
                    android:textSize="14sp"
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
                    android:textSize="18sp"
                    android:textStyle="bold" />

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="距离"
                    android:textSize="14sp"
                    android:textColor="#666666" />
            </LinearLayout>
        </LinearLayout>

        <!-- 历史数据图表 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="最近7天"
            android:textSize="16sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <com.github.mikephil.charting.charts.LineChart
            android:id="@+id/line_chart"
            android:layout_width="match_parent"
            android:layout_height="200dp"
            android:layout_marginBottom="24dp" />

        <!-- 本周统计 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="本周统计"
            android:textSize="16sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:padding="16dp"
            android:background="@drawable/bg_rounded_card"
            android:layout_marginBottom="24dp">

            <TextView
                android:id="@+id/tv_total_steps"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="总步数: 0 步"
                android:textSize="14sp"
                android:layout_marginBottom="4dp" />

            <TextView
                android:id="@+id/tv_avg_steps"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="平均: 0 步/天"
                android:textSize="14sp"
                android:layout_marginBottom="4dp" />

            <TextView
                android:id="@+id/tv_max_steps"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:text="最高: 0 步"
                android:textSize="14sp" />
        </LinearLayout>

        <!-- 灵敏度设置 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="灵敏度设置"
            android:textSize="16sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <SeekBar
            android:id="@+id/seekbar_sensitivity"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="24dp" />

        <!-- 用户配置 -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="用户配置"
            android:textSize="16sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp" />

        <EditText
            android:id="@+id/et_weight"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="体重 (kg)"
            android:inputType="numberDecimal"
            android:layout_marginBottom="8dp" />

        <EditText
            android:id="@+id/et_height"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="身高 (cm)"
            android:inputType="numberDecimal"
            android:layout_marginBottom="8dp" />

        <EditText
            android:id="@+id/et_goal"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="每日目标步数"
            android:inputType="number"
            android:layout_marginBottom="16dp" />

        <Button
            android:id="@+id/btn_save_config"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="保存配置" />

    </LinearLayout>
</ScrollView>
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

    // ViewModel and LiveData
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.6.2'

    // MPAndroidChart for charts
    implementation 'com.github.PhilJay:MPAndroidChart:v3.1.0'
}
*/
