package com.daily.health.manager.face.act

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.app.raise.AppraiseManager
import com.app.raise.config.EvaluateConfig
import com.daily.health.manager.BuildConfig
import com.daily.health.manager.R
import com.daily.health.manager.databinding.TrActivitySettingsBinding
import com.daily.health.manager.face.dialog.ComingSoonDialog
import com.daily.health.manager.face.fragment.SettingsAction
import com.daily.health.manager.face.fragment.SettingsScreen
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.helper.HealthTrackerEvaluateListener
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ui.debug.AdDebugPanel

class SettingsAct : BaseMVVMActivity<BaseViewModel, TrActivitySettingsBinding>() {

    private var hasRatedState = mutableStateOf(false)

    private val languageSelectLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                recreate()
            }
        }

    override fun createViewBinding() = TrActivitySettingsBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind) {
            actionBar.tvTitle.text = getString(R.string.tr_settings)
            actionBar.btnBack.clickWithDuration {
                finish()
            }
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                HealthTrackerTheme {
                    SettingsScreen(
                        hasRated = hasRatedState.value,
                        onAction = ::handleAction
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasRatedState.value = SpUtils.getBoolean(HealthTrackerEvaluateListener.KEY_HAS_RATED, false)
    }

    private fun handleAction(action: SettingsAction) {
        when (action) {
            SettingsAction.AlarmManagement -> {
                startActivity(Intent(this, AlarmManageScreen::class.java))
            }

            SettingsAction.UnitSettings -> {
                ComingSoonDialog.show(supportFragmentManager)
            }

            SettingsAction.TargetRangeSettings -> {
                startActivity(Intent(this, TargetRangeAct::class.java))
            }

            SettingsAction.PersonalInfo -> {
                startActivity(ProfileActivity.creteEditIntent(this))
            }

            SettingsAction.Language -> {
                languageSelectLauncher.launch(Intent(this, LanguageAct::class.java).apply {
                    putExtra(LanguageAct.KEY_APPLY_CHANGE, true)
                })
            }

            SettingsAction.Feedback -> {
                startActivity(Intent(this, FeedbackAct::class.java))
            }

            SettingsAction.Disclaimers -> {
                ComingSoonDialog.show(supportFragmentManager)
            }

            SettingsAction.PrivacyPolicy -> {
                InnerWebAct.start(this, BuildConfig.PRIVACY_POLICY)
            }

            SettingsAction.TermsOfService -> {
                ComingSoonDialog.show(supportFragmentManager)
            }

            SettingsAction.AdDebugPanel -> {
                AdDebugPanel.showDebugDialog(this)
            }

            SettingsAction.RateUs -> {
                ReportDataManager.reportData("rate_us_show", mapOf("source" to "settings"))
                val hasRated = SpUtils.getBoolean(HealthTrackerEvaluateListener.KEY_HAS_RATED, false)
                if (!hasRated) {
                    val manager = AppraiseManager(this, star5GoMarket = false)
                    manager.showAppraiseDialog(
                        HealthTrackerEvaluateListener(this, "settings") {
                            hasRatedState.value = true
                        }
                    )
                } else {
                    AppraiseManager.goToMarket(this, EvaluateConfig())
                }
            }
        }
    }
}
