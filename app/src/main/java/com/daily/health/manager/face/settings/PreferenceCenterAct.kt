package com.daily.health.manager.face.settings

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
import com.daily.health.manager.face.act.AlarmManageScreen
import com.daily.health.manager.face.act.FeedbackAct
import com.daily.health.manager.face.act.InnerWebAct
import com.daily.health.manager.face.act.LanguageAct
import com.daily.health.manager.face.act.ProfileActivity
import com.daily.health.manager.face.act.TargetRangeAct
import com.daily.health.manager.face.dialog.ComingSoonDialog
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.helper.HealthTrackerEvaluateListener
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ui.debug.AdDebugPanel

class PreferenceCenterAct : BaseMVVMActivity<BaseViewModel, TrActivitySettingsBinding>() {

    private var hasRatedState = mutableStateOf(false)

    private val languageSelectLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // AppCompatDelegate.setApplicationLocales handles activity recreation.
        }

    override fun onResume() {
        super.onResume()
        hasRatedState.value = SpUtils.getBoolean(HealthTrackerEvaluateListener.KEY_HAS_RATED, false)
    }

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
                    PreferenceMenuSurface(
                        hasRated = hasRatedState.value,
                        onAction = ::handleAction
                    )
                }
            }
        }
    }

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun createViewBinding() = TrActivitySettingsBinding.inflate(layoutInflater)

    private fun handleAction(action: PreferenceAction) {
        when (action) {
            PreferenceAction.AlarmManagement -> {
                startActivity(Intent(this, AlarmManageScreen::class.java))
            }

            PreferenceAction.UnitSettings -> {
                ComingSoonDialog.show(supportFragmentManager)
            }

            PreferenceAction.TargetRangeSettings -> {
                startActivity(Intent(this, TargetRangeAct::class.java))
            }

            PreferenceAction.PersonalInfo -> {
                startActivity(ProfileActivity.creteEditIntent(this))
            }

            PreferenceAction.Language -> {
                languageSelectLauncher.launch(Intent(this, LanguageAct::class.java).apply {
                    putExtra(LanguageAct.KEY_APPLY_CHANGE, true)
                })
            }

            PreferenceAction.Feedback -> {
                startActivity(Intent(this, FeedbackAct::class.java))
            }

            PreferenceAction.Disclaimers -> {
                ComingSoonDialog.show(supportFragmentManager)
            }

            PreferenceAction.PrivacyPolicy -> {
                InnerWebAct.start(this, BuildConfig.PRIVACY_POLICY)
            }

            PreferenceAction.TermsOfService -> {
                ComingSoonDialog.show(supportFragmentManager)
            }

            PreferenceAction.AdDebugPanel -> {
                AdDebugPanel.showDebugDialog(this)
            }

            PreferenceAction.RateUs -> {
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
