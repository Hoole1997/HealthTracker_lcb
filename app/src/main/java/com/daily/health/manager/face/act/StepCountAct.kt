package com.daily.health.manager.face.act

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import android.widget.Toast
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.constants.KEY_STEP_COUNT_GOLE
import com.daily.health.manager.databinding.TrActivityStepCountBinding
import com.daily.health.manager.steps.StepTrackingManager
import com.daily.health.manager.steps.StepTrackingStatus
import com.daily.health.manager.face.chart.HealthLineChartManager
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collect
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter
import com.healthtracker.framework.util.SpUtils
import org.koin.android.ext.android.inject

class StepCountAct : BaseInterActivity<StepCountViewModel, TrActivityStepCountBinding>() {

    private val chartManagerFactory: HealthLineChartManager.Factory by inject()
    private val stepTrackingManager: StepTrackingManager by inject()

    private var chartManager: HealthLineChartManager? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        stepTrackingManager.refresh()
        if (!granted) Toast.makeText(this, getString(R.string.tr_permission_denied), Toast.LENGTH_SHORT).show()
    }

    override fun createViewBinding() = TrActivityStepCountBinding.inflate(layoutInflater)
    override fun getVMModelClass() = StepCountViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.btnBack.clickWithDuration { handleBackPress() }
        mViewBind.ivSetting.clickWithDuration {
            startActivity<StepSettingAct>()
        }

        mViewBind.vGoalBg.clickWithDuration {
            startActivity<StepSettingAct>()
        }

        chartManager = chartManagerFactory.create(mViewBind.chartView, this)
        checkPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        // 权限设置页返回时重新检查，不依赖进程生命周期是否恰好触发过 onStop。
        stepTrackingManager.refresh()
        mViewBind.tvGoal.text = SpUtils.getInt(KEY_STEP_COUNT_GOLE,6000).toString()
    }

    override fun createObserver() {
        this.collect(stepTrackingManager.status) { status ->
            val message = when (status) {
                StepTrackingStatus.PERMISSION_REQUIRED -> R.string.tr_enable_step_count
                StepTrackingStatus.SENSOR_UNAVAILABLE -> R.string.tr_step_sensor_unavailable
                StepTrackingStatus.ERROR -> R.string.tr_step_tracking_unavailable
                StepTrackingStatus.ACTIVE, StepTrackingStatus.STOPPED -> null
            }
            mViewBind.tvStepStatus.isVisible = message != null
            if (message != null) mViewBind.tvStepStatus.setText(message)
        }

        this.collect(mViewModel.todayStatFlow) { stat ->
            stat?.let {
                mViewBind.tvStepCount.text = it.steps.toString()
                mViewBind.tvDistance.text =  NumberFormatter.formatNumber(it.distanceKm,LanguageUtils.getAppLocale(App.INSTANCE))
                mViewBind.tvKcal.text =  NumberFormatter.formatNumber(it.kcal, LanguageUtils.getAppLocale(App.INSTANCE),1)
                val totalSeconds = it.durationSeconds
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                mViewBind.tvTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                val goal = 6000
                val rawProgress = (it.steps * 100 / goal).coerceAtMost(100)
                val adjustedProgress = if (it.steps > 0) rawProgress.coerceAtLeast(1) else 0
                mViewBind.progressBar.setProgress(adjustedProgress)
            }
        }

        this.collect(mViewModel.chartUiStateFlow) { chartState ->
            chartManager?.renderColumn(chartState, isShowLabel = true)
            mViewBind.chartView.setAnimationDuration(0)
            mViewBind.chartView.animateIn = false
        }

        this.collect(mViewModel.statsFlow) { stats ->
            mViewBind.tvMaxValue.text = stats.max.toString()
            mViewBind.tvMinValue.text = stats.min.toString()
            mViewBind.tvAvgValue.text = stats.average.toString()
        }
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            if (granted) stepTrackingManager.refresh() else permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            stepTrackingManager.refresh()
        }
    }
}
