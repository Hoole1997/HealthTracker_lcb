package com.daily.health.manager.face.act

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daily.health.manager.R
import com.daily.health.manager.config.models.PushConfig
import com.daily.health.manager.face.compose.HealthTopBar
import com.daily.health.manager.databinding.TrActivityLanguageSelectBinding
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.LanguageUtils.getLanguageList
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject


class LanguageAct: BaseMVVMActivity<BaseViewModel, TrActivityLanguageSelectBinding>() {


    override fun createViewBinding() = TrActivityLanguageSelectBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    private val remoteConfigManager: RemoteConfigManager by inject()

    private var applyChange = false
    private var languageList: List<LanguageUtils.LangBean> = emptyList()
    private var savedSelectIndex: Int = -1
    
    override fun initView(savedInstanceState: Bundle?) {
        applyChange = intent?.getBooleanExtra(KEY_APPLY_CHANGE, false) ?: false
        
        languageList = getLanguageList(this@LanguageAct)
        savedSelectIndex = savedInstanceState?.getInt(KEY_SELECT_INDEX, -1) ?: -1

        if (savedSelectIndex !in languageList.indices) {
            val currentLangId = LanguageUtils.getAppLanguage(this@LanguageAct)
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
                        this@LanguageAct.savedSelectIndex = newIndex
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
            val targetPage = if(AdConfigManager.showNewGuide()) GuideAct::class.java else MainAct::class.java
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
    val itemBackgroundColor = colorResource(R.color.tr_language_item_bg)
    val itemSelectedBackgroundColor = colorResource(R.color.tr_language_item_selected_bg)
    val itemSelectedBorderColor = colorResource(R.color.tr_language_item_selected_border)
    val radioUnselectedColor = colorResource(R.color.tr_language_item_radio_unselected)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        HealthTopBar(
            title = stringResource(R.string.tr_choose_language),
            onBack = if (applyChange) onBack else null,
            rightAction = {
                IconButton(
                    onClick = { onConfirm(selectedIndex) },
                    enabled = confirmEnabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.tr_ic_confirm),
                        contentDescription = stringResource(R.string.tr_confirm),
                        tint = if (confirmEnabled) titleColor else colorResource(R.color.color_BFBFBF),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = bgColor)
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    backgroundColor = if (isSelected) itemSelectedBackgroundColor else itemBackgroundColor,
                    borderColor = if (isSelected) itemSelectedBorderColor else Color.Transparent,
                    indicatorColor = if (isSelected) itemSelectedBorderColor else radioUnselectedColor,
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
    backgroundColor: Color,
    borderColor: Color,
    indicatorColor: Color,
) {
    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(57.dp),
        shape = shape,
        color = backgroundColor,
        border = if (isSelected) BorderStroke(1.dp, borderColor) else null,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp)
                .padding(start = 8.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageSelectionIndicator(
                isSelected = isSelected,
                selectedColor = borderColor,
                unselectedColor = indicatorColor,
            )

            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun LanguageSelectionIndicator(
    isSelected: Boolean,
    selectedColor: Color,
    unselectedColor: Color,
) {
    Box(
        modifier = Modifier
            .size(19.dp)
            .border(
                width = 1.5.dp,
                color = if (isSelected) selectedColor else unselectedColor,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(selectedColor, CircleShape)
            )
        }
    }
}
