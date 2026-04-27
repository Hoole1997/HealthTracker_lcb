package com.daily.health.manager.face.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.constants.KEY_HAS_ADD_PROFILE
import com.daily.health.manager.databinding.TrActivityProfileBinding
import com.daily.health.manager.getUserAge
import com.daily.health.manager.isMale
import com.daily.health.manager.face.widget.NumberPickerView
import com.daily.health.manager.saveUserAge
import com.daily.health.manager.saveUserGender
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoRegular
import com.hjq.toast.Toaster
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle

class ProfileActivity: BaseInterActivity<BaseViewModel, TrActivityProfileBinding>() {
    companion object{
        private const val TAG = "ProfileActivity"

        private const val EXTRA_LAUNCH_MODE = "extra_launch_mode"
        const val MODE_SETTINGS = 0
        const val MODE_GUIDE = 1

        fun createGuideIntent(context: Context): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra(EXTRA_LAUNCH_MODE, MODE_GUIDE)
            }
        }

        fun creteEditIntent(context: Context) = Intent(context, ProfileActivity::class.java).apply {
            putExtra(EXTRA_LAUNCH_MODE, MODE_SETTINGS)
        }
    }

    private var age by mutableIntStateOf(getUserAge())
    private var gender by mutableIntStateOf(if(isMale()) 0 else 1)

    private val hasGuide = SpUtils.getBoolean(KEY_HAS_ADD_PROFILE,false)
    
    // 保存初始值，用于比较是否有变化
    private val initialAge = age
    private val initialGender = gender

    private var launchMode by mutableIntStateOf(MODE_SETTINGS)


    override fun getBackAdPosition() = AdPosition.IV_PROFILE_BACK



    override fun createViewBinding() = TrActivityProfileBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        launchMode = resolveLaunchMode(intent)

        with(mViewBind) {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                ProfileScreen(
                    launchMode = launchMode,
                    age = age,
                    gender = gender,
                    hasGuide = hasGuide,
                    initialAge = initialAge,
                    initialGender = initialGender,
                    onAgeChanged = { newAge ->
                        age = newAge
                    },
                    onGenderChanged = { newGender ->
                        gender = newGender
                    },
                    onBack = { onBackPress() },
                    onSave = { handleSaveAndFinish() },
                    onContinue = {
                        reportGuide(10)
                        handleSaveAndFinish()
                    },
                    onSkip = {
                        reportGuide(10)
                        handleSaveAndFinish()
                    },
                    onReportGuideEnter = { reportGuide(9) },
                )
            }

            loadNative(adContainer, AdPosition.NA_SETTINGS_PROFILE_BOTTOM, style = NativeAdStyle.CARD_7)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchMode = resolveLaunchMode(intent)
    }

    private fun resolveLaunchMode(sourceIntent: Intent?): Int {
        return sourceIntent?.getIntExtra(EXTRA_LAUNCH_MODE, MODE_SETTINGS) ?: MODE_SETTINGS
    }
    
    /**
     * 更新保存按钮的启用状态
     * - 如果未保存过配置（hasGuide == false），始终启用
     * - 如果已保存过配置（hasGuide == true），仅在值发生变化时启用
     */
    private fun updateSaveButtonState() {
        // Compose 版本不再依赖 ViewBinding 控制按钮状态
    }

    private fun isSaveEnabled(currentAge: Int, currentGender: Int): Boolean {
        val hasChanges = currentAge != initialAge || currentGender != initialGender
        return !hasGuide || hasChanges
    }

    private fun handleSaveAndFinish() {
        saveUserAge(age)
        saveUserGender(gender)
        SpUtils.putBoolean(KEY_HAS_ADD_PROFILE, true)
        Toaster.show(getString(R.string.tr_save_success))

        when (launchMode) {
            MODE_GUIDE -> {
                setResult(RESULT_OK)
                finish()
            }
            else -> {
                finish()
            }

        }
    }
}

@Composable
private fun ProfileScreen(
    launchMode: Int,
    age: Int,
    gender: Int,
    hasGuide: Boolean,
    initialAge: Int,
    initialGender: Int,
    onAgeChanged: (Int) -> Unit,
    onGenderChanged: (Int) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    onReportGuideEnter: () -> Unit,
) {
    val isGuideMode = launchMode == ProfileActivity.MODE_GUIDE
    val saveEnabled = remember(age, gender, hasGuide, initialAge, initialGender) {
        val hasChanges = age != initialAge || gender != initialGender
        !hasGuide || hasChanges
    }

    LaunchedEffect(isGuideMode) {
        if (isGuideMode) {
            onReportGuideEnter()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.c1))
    ) {
        ProfileTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.tr_choose_your_gender),
                color = colorResource(R.color.t1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 20.dp)
            )

            Text(
                text = stringResource(R.string.tr_txt_profile_des),
                color = colorResource(R.color.color_666),
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 14.dp, end = 16.dp, top = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GenderCard(
                    modifier = Modifier.weight(1f),
                    selected = gender == 0,
                    iconRes = R.mipmap.tr_ic_male,
                    labelRes = R.string.tr_male,
                    onClick = { onGenderChanged(0) }
                )
                GenderCard(
                    modifier = Modifier.weight(1f),
                    selected = gender == 1,
                    iconRes = R.mipmap.tr_ic_female,
                    labelRes = R.string.tr_female,
                    onClick = { onGenderChanged(1) }
                )
            }

            Text(
                text = stringResource(R.string.tr_choose_your_age),
                color = colorResource(R.color.t1),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        val root = LayoutInflater.from(ctx)
                            .inflate(R.layout.tr_layout_profile_age_picker, null, false)
                        val picker = root.findViewById<NumberPickerView>(R.id.numberPicker)

                        picker.apply {
                            val selectFont = getRobotoBold(ctx)
                            val normalFont = getRobotoRegular(ctx)
                            setContentSelectedTextTypeface(selectFont)
                            setContentNormalTextTypeface(normalFont)

                            val ages = (1..110).map { it.toString() }.toTypedArray()
                            displayedValues = ages
                            minValue = 0
                            maxValue = ages.lastIndex

                            val normalizedAge = age.coerceIn(1, 110)
                            val currentIndex = ages.indexOf(normalizedAge.toString()).takeIf { it >= 0 } ?: 0
                            value = currentIndex

                            setOnValueChangedListener { _, _, newVal ->
                                val newAge = ages[newVal].toInt()
                                onAgeChanged(newAge)
                            }
                        }

                        root
                    },
                    update = { rootView: View ->
                        val picker = rootView.findViewById<NumberPickerView>(R.id.numberPicker)
                        val ages = (1..110).map { it.toString() }.toTypedArray()
                        val normalizedAge = age.coerceIn(1, 110)
                        val targetIndex = ages.indexOf(normalizedAge.toString()).takeIf { it >= 0 } ?: 0
                        if (picker.value != targetIndex) {
                            picker.value = targetIndex
                        }
                    }
                )
            }
        }

        if (isGuideMode) {
            PrimaryActionButton(
                textRes = R.string.tr_text_continue,
                enabled = true,
                onClick = onContinue,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Text(
                text = stringResource(R.string.tr_skip),
                color = colorResource(R.color.color_999),
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSkip() }
                    .padding(16.dp)
            )
        } else {
            PrimaryActionButton(
                textRes = R.string.tr_save,
                enabled = saveEnabled,
                onClick = onSave,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

@Composable
private fun ProfileTopBar(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colorResource(R.color.c1))
            .padding(end = 8.dp),
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
                    tint = Color.Unspecified,
                )
            }
        }
    }
}

@Composable
private fun GenderCard(
    modifier: Modifier = Modifier,
    selected: Boolean,
    iconRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val labelColor = if (selected) colorResource(R.color.c5) else colorResource(R.color.t1)
    val backgroundColor = if (selected) Color(0x1F1D6BF2) else Color(0xFFF9F9FA)
    val borderColor = colorResource(R.color.c5)

    Surface(
        modifier = modifier.height(179.dp),
        shape = shape,
        color = backgroundColor,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null,
        onClick = onClick,
        enabled = true,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(width = 102.dp, height = 127.dp)
                    .align(Alignment.BottomCenter),
            )
            Text(
                text = stringResource(labelRes),
                color = labelColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            )
        }
    }
}

@Composable
private fun PrimaryActionButton(
    textRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(24.dp)
    val bg = if (enabled) colorResource(R.color.c5) else colorResource(R.color.color_C7C7CC)
    val textColor = Color.White

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = shape,
        color = bg,
        onClick = onClick,
        enabled = enabled,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(textRes),
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
