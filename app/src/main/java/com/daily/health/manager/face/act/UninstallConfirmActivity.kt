package com.daily.health.manager.face.act

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtActivityUninstallConfirmBinding
import com.daily.health.manager.face.theme.HealthTrackerTheme
import com.daily.health.manager.utils.loadInterstitial
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ui.NativeAdStyle
import com.daily.health.manager.face.tracker.trackPage2DontClick
import com.daily.health.manager.face.tracker.trackPage2UninstallClick

class UninstallConfirmActivity : BaseMVVMActivity<BaseViewModel, HtActivityUninstallConfirmBinding>() {

    override fun createViewBinding() = HtActivityUninstallConfirmBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind) {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                HealthTrackerTheme {
                    UninstallConfirmScreen(
                        onBack = { navigateToMain() },
                        onDontUninstall = { 
                            trackPage2DontClick()
                            navigateToMain() 
                        },
                        onUninstall = { selectedReason, otherText ->
                            trackPage2UninstallClick()
                            handleUninstallClick(selectedReason, otherText)
                        }
                    )
                }
            }

            // 加载底部Native广告
            loadNativeAdIfEnabled()
        }
    }

    private fun loadNativeAdIfEnabled() {
        if (AdConfigManager.shouldShowUninstall2Native()) {
            loadNative(mViewBind.adContainer, NativeAdStyle.CARD_7)
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

    private fun handleUninstallClick(selectedReason: Int, otherText: String) {
        // 收集卸载原因数据（暂不处理）
        // TODO: 后续可上报到服务器或Analytics

        if (AdConfigManager.shouldShowUninstall2Interstitial()) {
            loadInterstitial {
                navigateToSystemUninstall()
            }
        } else {
            navigateToSystemUninstall()
        }
    }

    private fun navigateToSystemUninstall() {
        // 跳转到系统应用详情页面
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
        finish()
    }

    override fun shouldDisableBackPressed() = true

    companion object {
        private const val TAG = "UninstallConfirmActivity"
    }
}

@Immutable
private data class ReasonItem(
    val index: Int,
    @param:StringRes val textRes: Int
)

@Composable
private fun UninstallConfirmScreen(
    onBack: () -> Unit,
    onDontUninstall: () -> Unit,
    onUninstall: (selectedReason: Int, otherText: String) -> Unit
) {
    val bgColor = colorResource(R.color.c1)
    val primaryColor = colorResource(R.color.c5)

    var selectedReason by rememberSaveable { mutableIntStateOf(0) }
    var otherReasonText by rememberSaveable { mutableStateOf("") }

    val reasons = listOf(
        ReasonItem(0, R.string.ht_uninstall_reason_difficult),
        ReasonItem(1, R.string.ht_uninstall_reason_too_many_ads),
        ReasonItem(2, R.string.ht_uninstall_reason_poor_experience),
        ReasonItem(3, R.string.ht_uninstall_reason_similar_installed),
        ReasonItem(4, R.string.ht_uninstall_reason_unable_meet_demand),
        ReasonItem(5, R.string.ht_uninstall_reason_others)
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
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Title
            val appName = stringResource(R.string.app_name)
            Text(
                text = stringResource(R.string.ht_uninstall_reason_title, appName),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.t1),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Reason options
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                reasons.forEach { reason ->
                    ReasonOptionItem(
                        text = stringResource(reason.textRes),
                        isSelected = selectedReason == reason.index,
                        onClick = { selectedReason = reason.index }
                    )
                }
            }

            // "Others" input field - only show when "Others" is selected
            if (selectedReason == 5) {
                Spacer(modifier = Modifier.height(16.dp))
                OtherReasonTextField(
                    value = otherReasonText,
                    onValueChange = { otherReasonText = it },
                    hint = stringResource(R.string.ht_uninstall_reason_hint, appName)
                )
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
                    text = stringResource(R.string.ht_uninstall_btn_dont_uninstall),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Uninstall button (secondary - text only)
            Text(
                text = stringResource(R.string.ht_uninstall_btn_uninstall),
                fontSize = 14.sp,
                color = colorResource(com.android.common.weather.R.color.color_999),
                modifier = Modifier
                    .clickable { onUninstall(selectedReason, otherReasonText) }
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
                    painter = painterResource(R.drawable.ht_ic_back),
                    contentDescription = "back",
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun ReasonOptionItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = colorResource(R.color.c5)
    val textColor = colorResource(R.color.t1)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        // Radio button style indicator
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.background(primaryColor)
                    } else {
                        Modifier.border(1.dp, colorResource(R.color.color_E0E0E0), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.ht_ic_checked),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun OtherReasonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String
) {
    val bgColor = colorResource(R.color.color_F9F9FA)
    val hintColor = colorResource(R.color.color_B0B0B0)
    val textColor = colorResource(R.color.t1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 14.sp,
                color = textColor
            ),
            cursorBrush = SolidColor(colorResource(R.color.c5)),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            fontSize = 13.sp,
                            color = hintColor
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}