package com.daily.health.manager.face.act

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.config.models.PushConfig
import com.daily.health.manager.databinding.TrActivityLanguageSelectBinding
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.LanguageUtils.getLanguageList
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject
import com.daily.health.manager.face.compose.HealthTopBar


class LanguageScreen: BaseMVVMActivity<BaseViewModel, TrActivityLanguageSelectBinding>() {


    override fun createViewBinding() = TrActivityLanguageSelectBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    private val remoteConfigManager: RemoteConfigManager by inject()

    private var applyChange = false
    private var languageList: List<LanguageUtils.LangBean> = emptyList()
    private var savedSelectIndex: Int = -1
    
    override fun initView(savedInstanceState: Bundle?) {
        applyChange = intent?.getBooleanExtra(KEY_APPLY_CHANGE, false) ?: false
        
        languageList = getLanguageList(this@LanguageScreen)
        savedSelectIndex = savedInstanceState?.getInt(KEY_SELECT_INDEX, -1) ?: -1

        if (savedSelectIndex !in languageList.indices) {
            val currentLangId = LanguageUtils.getAppLanguage(this@LanguageScreen)
            savedSelectIndex = languageList.indexOfFirst { it.id == currentLangId }.takeIf { it >= 0 } ?: 0
        }
        
        with(mViewBind){
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                LanguageSelectScreen(
                    applyChange = applyChange,
                    list = languageList,
                    savedSelectIndex = savedSelectIndex,
                    onSelectedIndexChanged = { newIndex ->
                        this@LanguageScreen.savedSelectIndex = newIndex
                    },
                    onBack = { finish() },
                    onConfirm = { selectedIndex ->
                        onChoiceLangDone(selectedIndex)
                    },
                    onReportGuide = {
                        reportGuide(1)
                    }
                )
            }

            if(AdConfigManager.shouldShowBottomNativeOnLanguageSelection()){
                loadNative(adContainer, AdPosition.NA_SETTINGS_LANGUAGE_BOTTOM, NativeAdStyle.CARD_7)
            }

        }
    }


    private fun onChoiceLangDone(selectedIndex: Int) {
        val selectedLangId = languageList.getOrNull(selectedIndex)?.id ?: return
        LanguageUtils.setAppLanguage(selectedLangId)
        // 语言改变后，清除 PushConfig 缓存，以便下次获取时使用新语言重新解析
        remoteConfigManager.clearCache<PushConfig>()
        if (applyChange) {
            // 通知设置页面需要重建以应用语言变更
            setResult(RESULT_OK)
        } else {
            val targetPage = if(AdConfigManager.showNewGuide()) GuideScreen::class.java else MainScreen::class.java
            this.startActivity(Intent(this, targetPage).apply {
                putExtras(intent)
            })

        }
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECT_INDEX, savedSelectIndex)
    }

    override fun shouldDisableBackPressed() = true
    
    companion object {
        private const val KEY_SELECT_INDEX = "select_index"
        private const val TAG = "LanguageActivity"

        const val KEY_APPLY_CHANGE = "apply_change"
    }
}

@Composable
private fun LanguageSelectScreen(
    applyChange: Boolean,
    list: List<LanguageUtils.LangBean>,
    savedSelectIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onConfirm: (selectedIndex: Int) -> Unit,
    onReportGuide: () -> Unit,
) {
    val context = LocalContext.current
    val resolvedInitialIndex = remember(list, savedSelectIndex) {
        when {
            savedSelectIndex in list.indices -> savedSelectIndex
            else -> {
                val current = LanguageUtils.getAppLanguage(context)
                list.indexOfFirst { it.id == current }.takeIf { it >= 0 } ?: 0
            }
        }
    }

    var selectedIndex by rememberSaveable {
        mutableIntStateOf(resolvedInitialIndex)
    }

    LaunchedEffect(selectedIndex) {
        onSelectedIndexChanged(selectedIndex)
    }

    LaunchedEffect(applyChange) {
        if (!applyChange) {
            onReportGuide()
        }
    }

    val isFirstSelection by remember {
        derivedStateOf { LanguageUtils.getSavedLanguage().isEmpty() }
    }
    val selectedLangId by remember(list, selectedIndex) {
        derivedStateOf { list.getOrNull(selectedIndex)?.id }
    }
    val currentLangId by remember {
        derivedStateOf { LanguageUtils.getAppLanguage(context) }
    }
    val confirmEnabled by remember {
        derivedStateOf {
            val selected = selectedLangId ?: return@derivedStateOf false
            selected != currentLangId || isFirstSelection
        }
    }

    val bgColor = colorResource(R.color.c1)
    val titleColor = colorResource(R.color.t1)
    val primaryColor = colorResource(R.color.c5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        HealthTopBar(
            title = stringResource(R.string.tr_choose_language),
            onBack = if (applyChange) onBack else null,
            rightAction = {
                TextButton(
                    onClick = { onConfirm(selectedIndex) },
                    enabled = confirmEnabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(R.color.c5),
                        disabledContentColor = Color(android.graphics.Color.DKGRAY)
                    ),
                    contentPadding = ButtonDefaults.TextButtonContentPadding
                ) {
                    val confirmColor = if (confirmEnabled) colorResource(R.color.c5) else Color(android.graphics.Color.DKGRAY)
                    Text(
                        text = stringResource(R.string.tr_confirm),
                        color = confirmColor,
                        fontSize = 16.sp,
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = colorResource(R.color.bg_window))
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(list) { index, item ->
                val isSelected = index == selectedIndex
                LanguageItem(
                    title = item.displayName,
                    isSelected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            selectedIndex = index
                        }
                    },
                    titleColor = if (isSelected) primaryColor else titleColor,
                    titleFontFamily = FontFamily.Default,
                    titleFontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    titleColor: Color,
    titleFontFamily: FontFamily,
    titleFontWeight: FontWeight,
) {
    val shape = RoundedCornerShape(8.dp)
    val bgColor = if (isSelected) colorResource(R.color.color_EFFBF7) else Color.White
    val borderColor = if (isSelected) colorResource(R.color.c5) else Color.Transparent

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        color = bgColor,
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        onClick = onClick,
        enabled = !isSelected,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp,
                fontFamily = titleFontFamily,
                fontWeight = titleFontWeight,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Image(
                    painter = painterResource(R.drawable.tr_ic_checked),
                    contentDescription = "selected",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}