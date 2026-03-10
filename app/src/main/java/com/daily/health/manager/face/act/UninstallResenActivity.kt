package com.daily.health.manager.face.act

import android.content.Intent
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.daily.health.manager.R
import com.daily.health.manager.databinding.TrActivityUninstallResenBinding
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.utils.loadInterstitial
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import com.daily.health.manager.face.tracker.trackPage1DontClick
import com.daily.health.manager.face.tracker.trackPage1UninstallClick

class UninstallResenActivity : BaseMVVMActivity<BaseViewModel, TrActivityUninstallResenBinding>() {

    override fun createViewBinding() = TrActivityUninstallResenBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind) {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                HealthTrackerTheme {
                    UninstallResenScreen(
                        onBack = { navigateToMain() },
                        onDontUninstall = { 
                            trackPage1DontClick()
                            navigateToMain() 
                        },
                        onUninstall = { 
                            trackPage1UninstallClick()
                            handleUninstallClick() 
                        }
                    )
                }
            }

            // 加载底部Native广告
            loadNativeAdIfEnabled()
        }
    }

    private fun loadNativeAdIfEnabled() {
        if (AdConfigManager.shouldShowUninstall1Native()) {
            loadNative(mViewBind.adContainer, AdPosition.NA_UNINSTALL_1_BOTTOM, NativeAdStyle.CARD_7)
        }
    }

    private fun navigateToMain() {
        // 返回主界面并重启应用以展示开屏广告
        // 不传递任何extras，避免再次触发卸载拦截
        val intent = Intent(this, SplashScreen::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun handleUninstallClick() {
        if (AdConfigManager.shouldShowUninstall1Interstitial()) {
            loadInterstitial(position = AdPosition.IV_UNINSTALL_1) {
                navigateToConfirmPage()
            }
        } else {
            navigateToConfirmPage()
        }
    }

    private fun navigateToConfirmPage() {
        startActivity(Intent(this, UninstallConfirmActivity::class.java))
        finish()
    }

    override fun shouldDisableBackPressed() = true

    companion object {
        private const val TAG = "UninstallResenActivity"
    }
}

@Immutable
private data class FeatureItem(
    @DrawableRes val iconRes: Int,
    @StringRes val textRes: Int
)

@Composable
private fun UninstallResenScreen(
    onBack: () -> Unit,
    onDontUninstall: () -> Unit,
    onUninstall: () -> Unit
) {
    val bgColor = colorResource(R.color.c1)
    val primaryColor = colorResource(R.color.c5)

    val features = listOf(
        FeatureItem(R.drawable.tr_ic_bs_primery, R.string.tr_uninstall_feature_blood_sugar),
        FeatureItem(R.drawable.tr_ic_statistical, R.string.tr_uninstall_feature_trend),
        FeatureItem(R.drawable.tr_ic_meds, R.string.tr_uninstall_feature_reminder),
        FeatureItem(R.drawable.tr_ic_tips, R.string.tr_uninstall_feature_insights)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // TopBar with back button
        TopBar(onBack = onBack)

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Illustration
            Image(
                painter = painterResource(R.drawable.tr_ic_placeholder_uninstall),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(R.string.tr_uninstall_lose_access_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.t1),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Feature list
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                features.forEach { feature ->
                    FeatureRow(
                        iconRes = feature.iconRes,
                        text = stringResource(feature.textRes)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Don't uninstall button (primary)
            Button(
                onClick = onDontUninstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                )
            ) {
                Text(
                    text = stringResource(R.string.tr_uninstall_btn_dont_uninstall),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Uninstall button (secondary - text only)
            Text(
                text = stringResource(R.string.tr_uninstall_btn_uninstall),
                fontSize = 14.sp,
                color = colorResource(com.android.common.weather.R.color.color_999),
                modifier = Modifier
                    .clickable { onUninstall() }
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val bgColor = colorResource(R.color.c1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bgColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.tr_ic_back),
                    contentDescription = "back",
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    @DrawableRes iconRes: Int,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )

        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorResource(R.color.t1),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}