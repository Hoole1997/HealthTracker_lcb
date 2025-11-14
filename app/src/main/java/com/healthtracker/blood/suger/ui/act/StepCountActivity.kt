package com.healthtracker.blood.suger.ui.act

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityStepCountBinding
import com.healthtracker.blood.suger.service.HealthService
import com.healthtracker.framework.ext.collect
import android.widget.Toast

class StepCountActivity : BaseInterActivity<StepCountViewModel, ActivityStepCountBinding>() {

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startHealthService() else Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    override fun createViewBinding() = ActivityStepCountBinding.inflate(layoutInflater)
    override fun getVMModelClass() = StepCountViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.btnBack.setOnClickListener { onBackPress() }
        checkPermissionAndStart()
        createObserver()
    }

    override fun createObserver() {
        this.collect(mViewModel.todayStatFlow) { stat ->
            stat?.let {
                mViewBind.tvStepCount.text = it.steps.toString()
                mViewBind.tvDistance.text = String.format("%.2f", it.distanceKm)
                mViewBind.tvKcal.text = String.format("%.1f", it.kcal)
                val totalSeconds = it.durationSeconds
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                mViewBind.tvTime.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                val goal = 6000
                val p = (it.steps * 100 / goal).coerceAtMost(100)
                mViewBind.progressBar.setProgress(p)
            }
        }

        this.collect(mViewModel.recent7DaysFlow()) { list ->
            // 如需扩展图表/历史预览，可在此处理 list
        }
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            if (granted) startHealthService() else permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        } else {
            startHealthService()
        }
    }

    private fun startHealthService() {
        val intent = Intent(this, HealthService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}