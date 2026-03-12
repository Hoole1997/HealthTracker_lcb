package com.daily.health.manager.face.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.BuildConfig
import com.daily.health.manager.R
import com.daily.health.manager.databinding.TrFragmentSettingsBinding
import com.daily.health.manager.face.act.AlarmManageScreen
import com.daily.health.manager.face.act.FeedbackAct
import com.daily.health.manager.face.act.InnerWebAct
import com.daily.health.manager.face.act.LanguageAct
import com.daily.health.manager.face.act.ProfileActivity
import com.daily.health.manager.face.act.TargetRangeAct
import com.daily.health.manager.face.dialog.ComingSoonDialog
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.helper.HealthTrackerEvaluateListener
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.app.raise.AppraiseManager
import com.app.raise.config.EvaluateConfig
import com.google.android.gms.ads.MobileAds
import com.healthtracker.framework.util.SpUtils
import net.corekit.core.report.ReportDataManager
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import net.corekit.monetize.ui.debug.AdDebugPanel


class SettingsFrg : BaseMVVMFragment<BaseViewModel, TrFragmentSettingsBinding>() {

    private var hasRatedState = androidx.compose.runtime.mutableStateOf(false)

    private val languageSelectLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                activity?.recreate()
            }
        }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = TrFragmentSettingsBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind?.composeView?.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        mViewBind?.composeView?.setContent {
            HealthTrackerTheme {
                SettingsScreen(
                    hasRated = hasRatedState.value,
                    onAction = ::handleAction
                )
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
                startActivity(Intent(requireContext(), AlarmManageScreen::class.java))
            }

            SettingsAction.UnitSettings -> {
                activity?.let { ComingSoonDialog.show(it.supportFragmentManager) }
            }

            SettingsAction.TargetRangeSettings -> {
                startActivity(Intent(requireContext(), TargetRangeAct::class.java))
            }

            SettingsAction.PersonalInfo -> {
                startActivity(ProfileActivity.creteEditIntent(requireContext()))
            }

            SettingsAction.Language -> {
                languageSelectLauncher.launch(Intent(requireContext(), LanguageAct::class.java).apply {
                    putExtra(LanguageAct.KEY_APPLY_CHANGE, true)
                })
            }

            SettingsAction.Feedback -> {
                startActivity(Intent(requireContext(), FeedbackAct::class.java))
            }

            SettingsAction.Disclaimers -> {
                activity?.let { ComingSoonDialog.show(it.supportFragmentManager) }
            }

            SettingsAction.PrivacyPolicy -> {
                InnerWebAct.start(requireContext(), BuildConfig.PRIVACY_POLICY)
            }

            SettingsAction.TermsOfService -> {
                MobileAds.openAdInspector(requireContext()) { }
            }

            SettingsAction.AdDebugPanel -> {
                AdDebugPanel.showDebugDialog(requireActivity())
            }

            SettingsAction.RateUs -> {
                ReportDataManager.reportData("rate_us_show", mapOf("source" to "settings"))
                val hasRated = SpUtils.getBoolean(HealthTrackerEvaluateListener.KEY_HAS_RATED, false)
                if (!hasRated) {
                    // 未评分，显示评分弹窗
                    val manager = AppraiseManager(requireContext(), star5GoMarket = false)
                    manager.showAppraiseDialog(
                        HealthTrackerEvaluateListener(
                            requireActivity() as androidx.fragment.app.FragmentActivity,
                            "settings"
                        ) {
                            // 评分完成后刷新状态，隐藏 Rate Us 入口
                            hasRatedState.value = true
                        }
                    )
                } else {
                    // 已评分，直接跳转 Play Store
                    AppraiseManager.goToMarket(requireContext(), EvaluateConfig())
                }
            }
        }
    }
}

private enum class SettingsAction {
    AlarmManagement,
    UnitSettings,
    TargetRangeSettings,
    PersonalInfo,
    Language,
    RateUs,
    Feedback,
    Disclaimers,
    PrivacyPolicy,
    TermsOfService,
    AdDebugPanel
}

@Immutable
private data class SettingsItemUi(
    val action: SettingsAction,
    val titleRes: Int,
    val iconRes: Int,
    val isLegal: Boolean = false
)

@Composable
private fun SettingsScreen(
    hasRated: Boolean,
    onAction: (SettingsAction) -> Unit
) {

    val iconTintCore = colorResource(R.color.c5)
    val iconTintLegal = iconTintCore.copy()
    val chevronTint = colorResource(R.color.color_C7C7CC).copy(alpha = 0.9f)

    val remindersAndGoalsItems = buildList {
        if (NotificationFeatureSwitch.reminderEntryEnabled) {
            add(SettingsItemUi(SettingsAction.AlarmManagement, R.string.tr_alarm_management, R.drawable.tr_ic_setting_alarm))
        }
//        add(SettingsItemUi(SettingsAction.UnitSettings, R.string.tr_unit_settings, R.drawable.tr_ic_setting_unit))
        add(SettingsItemUi(SettingsAction.TargetRangeSettings, R.string.tr_target_range_settings, R.drawable.tr_ic_setting_target))
    }
    val generalItems = listOf(
        SettingsItemUi(SettingsAction.PersonalInfo, R.string.tr_personal_info, R.drawable.tr_ic_setting_profile),
        SettingsItemUi(SettingsAction.Language, R.string.tr_language, R.drawable.tr_ic_setting_language)
    )
    val helpAndFeedbackItems = mutableListOf<SettingsItemUi>()
    if (!hasRated) {
        helpAndFeedbackItems.add(
            SettingsItemUi(SettingsAction.RateUs, R.string.tr_rate_us, R.drawable.tr_ic_setting_rate)
        )
    }
    helpAndFeedbackItems.add(
        SettingsItemUi(SettingsAction.Feedback, R.string.tr_feedback, R.drawable.tr_ic_setting_feedback)
    )
    val legalItems = listOf(
//        SettingsItemUi(SettingsAction.Disclaimers, R.string.tr_disclaimers, R.drawable.tr_ic_setting_disclaimers, isLegal = true),
        SettingsItemUi(SettingsAction.PrivacyPolicy, R.string.tr_privacy_policy, R.drawable.tr_ic_setting_privacy, isLegal = true),
//        SettingsItemUi(SettingsAction.TermsOfService, R.string.tr_terms_of_service, R.drawable.tr_ic_setting_terms, isLegal = true)
    )

    // 开发者工具（仅 DEBUG 版本显示）
    val developerItems = if (BuildConfig.DEBUG) {
        listOf(
            SettingsItemUi(SettingsAction.AdDebugPanel, R.string.tr_ad_debug_panel, R.drawable.tr_ic_setting)
        )
    } else {
        emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.tr_subpage_bg))
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SettingsSection(
                titleRes = R.string.tr_settings_section_tracking_and_targets,
                items = remindersAndGoalsItems,
                iconTintCore = iconTintCore,
                iconTintLegal = iconTintLegal,
                chevronTint = chevronTint,
                onAction = onAction
            )
        }
        item {
            SettingsSection(
                titleRes = R.string.tr_settings_section_preferences,
                items = generalItems,
                iconTintCore = iconTintCore,
                iconTintLegal = iconTintLegal,
                chevronTint = chevronTint,
                onAction = onAction
            )
        }
        item {
            SettingsSection(
                titleRes = R.string.tr_settings_section_help_and_feedback,
                items = helpAndFeedbackItems,
                iconTintCore = iconTintCore,
                iconTintLegal = iconTintLegal,
                chevronTint = chevronTint,
                onAction = onAction
            )
        }
        item {
            SettingsSection(
                titleRes = R.string.tr_settings_section_legal_and_policies,
                items = legalItems,
                iconTintCore = iconTintCore,
                iconTintLegal = iconTintLegal,
                chevronTint = chevronTint,
                onAction = onAction
            )
        }
        // 开发者工具 section（仅 DEBUG 版本显示）
        if (developerItems.isNotEmpty()) {
            item {
                SettingsSection(
                    titleRes = R.string.tr_settings_section_developer,
                    items = developerItems,
                    iconTintCore = iconTintCore,
                    iconTintLegal = iconTintLegal,
                    chevronTint = chevronTint,
                    onAction = onAction
                )
            }
        }
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.tr_settings_version_format,
                    BuildConfig.VERSION_NAME
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = colorResource(R.color.color_B0B0B0),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SettingsSection(
    titleRes: Int,
    items: List<SettingsItemUi>,
    iconTintCore: Color,
    iconTintLegal: Color,
    chevronTint: Color,
    onAction: (SettingsAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(titleRes),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = colorResource(R.color.color_999).copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            items.forEachIndexed { index, item ->
                SettingsRow(
                    item = item,
                    iconTintCore = iconTintCore,
                    iconTintLegal = iconTintLegal,
                    chevronTint = chevronTint,
                    onClick = { onAction(item.action) }
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    item: SettingsItemUi,
    iconTintCore: Color,
    iconTintLegal: Color,
    chevronTint: Color,
    onClick: () -> Unit
) {
    val iconTint = if (item.isLegal) iconTintLegal else iconTintCore
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(
            painter = painterResource(item.iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(item.titleRes),
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.t1)
        )
        androidx.compose.material3.Icon(
            painter = painterResource(R.drawable.tr_ic_status_arrow),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = chevronTint
        )
    }
}
