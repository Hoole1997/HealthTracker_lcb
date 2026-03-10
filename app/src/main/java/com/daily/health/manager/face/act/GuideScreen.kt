package com.daily.health.manager.face.act

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.daily.health.manager.databinding.TrActivityGuideBinding
import com.daily.health.manager.face.compose.OnboardingRoute
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.saveHasNewGuide
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import net.corekit.core.report.ReportDataManager

class GuideScreen : BaseMVVMActivity<BaseViewModel, TrActivityGuideBinding>() {

    override fun createViewBinding() = TrActivityGuideBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        mViewBind.composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind.composeView.setContent {
            HealthTrackerTheme {
                OnboardingRoute(
                    onFinish = ::goNext
                )
            }
        }
    }

    private fun goNext() {
        saveHasNewGuide()
        startActivity(Intent(this, MainScreen::class.java).apply {
            putExtras(intent)
        })
        finish()
    }
}

fun reportGuide(step: Int) {
    ReportDataManager.reportData("guide_page_show", mapOf("step" to step))
}
