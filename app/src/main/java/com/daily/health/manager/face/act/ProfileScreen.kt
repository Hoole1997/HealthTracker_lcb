package com.daily.health.manager.face.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.layout.ContentScale
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
import com.daily.health.manager.databinding.FcActivityProfileBinding
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

class ProfileActivity: BaseInterActivity<BaseViewModel, FcActivityProfileBinding>() {
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

    override fun getStatusBarColor() = com.healthtracker.framework.R.color.transparent

    override fun hasStatusbarPlaceView() = true



    override fun createViewBinding() = FcActivityProfileBinding.inflate(layoutInflater)

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
                    onBack = { handleBackPress() },
                    onSave = { handleSaveAndFinish() },
                    onContinue = {
                        reportGuide(10)
                        handleSaveAndFinish()
                    },
                    onReportGuideEnter = { reportGuide(9) },
                )
            }

            loadNative(adContainer, AdPosition.NA_SETTINGS_PROFILE_BOTTOM, style = NativeAdStyle.STANDARD)
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
        Toaster.show(getString(R.string.fc_save_success))

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
    onReportGuideEnter: () -> Unit,
) {
    val isGuideMode = launchMode == ProfileActivity.MODE_GUIDE
    val saveEnabled = remember(age, gender, hasGuide, initialAge, initialGender) {
        val hasChanges = age != initialAge || gender != initialGender
        !hasGuide || hasChanges
    }
    val actionEnabled = if (isGuideMode) true else saveEnabled
    val actionTextRes = if (isGuideMode) R.string.fc_text_continue else R.string.fc_save
    val listState = rememberLazyListState()

    LaunchedEffect(isGuideMode) {
        if (isGuideMode) {
            onReportGuideEnter()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.c1))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(304.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFCF5E8),
                            Color(0xFFFFFFFF)
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            item {
                ProfileTopBar(onBack = onBack)
            }

            item {
                Column {
                    Box(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.fc_choose_your_gender),
                        color = Color(0xFF222222),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 20.dp)
                    )

                    Text(
                        text = stringResource(R.string.fc_txt_profile_des),
                        color = Color(0xFF666666),
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GenderCard(
                            modifier = Modifier.weight(1f),
                            selected = gender == 0,
                            illustrationRes = R.mipmap.fc_profile_gender_male,
                            labelRes = R.string.fc_male,
                            onClick = { onGenderChanged(0) }
                        )
                        GenderCard(
                            modifier = Modifier.weight(1f),
                            selected = gender == 1,
                            illustrationRes = R.mipmap.fc_profile_gender_female,
                            labelRes = R.string.fc_female,
                            onClick = { onGenderChanged(1) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.fc_choose_your_age),
                    color = Color(0xFF222222),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxWidth(),
                        factory = { ctx ->
                            val root = LayoutInflater.from(ctx)
                                .inflate(R.layout.fc_layout_profile_age_picker, null, false)
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

            item {
                Box(modifier = Modifier.height(52.dp))
            }

            item {
                PrimaryActionButton(
                    textRes = actionTextRes,
                    enabled = actionEnabled,
                    onClick = if (isGuideMode) onContinue else onSave,
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .padding(bottom = 16.dp)
                )
            }
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
            .statusBarsPadding()
            .height(40.dp)
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.fc_ic_back),
                contentDescription = "back",
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private val GenderCardShape = GenericShape { size, _ ->
    moveTo(size.width * 0.058f, size.height * 0.084f)
    cubicTo(
        size.width * 0.061f,
        size.height * 0.037f,
        size.width * 0.094f,
        0f,
        size.width * 0.134f,
        0f
    )
    lineTo(size.width * 0.924f, 0f)
    cubicTo(
        size.width * 0.968f,
        0f,
        size.width,
        size.height * 0.046f,
        size.width * 0.981f,
        size.height * 0.099f
    )
    lineTo(size.width * 0.931f, size.height * 0.915f)
    cubicTo(
        size.width * 0.919f,
        size.height * 0.963f,
        size.width * 0.895f,
        size.height,
        size.width * 0.873f,
        size.height
    )
    lineTo(size.width * 0.076f, size.height)
    cubicTo(
        size.width * 0.031f,
        size.height,
        -size.width * 0.004f,
        size.height * 0.954f,
        0f,
        size.height * 0.900f
    )
    close()
}

@Composable
private fun GenderCard(
    modifier: Modifier = Modifier,
    selected: Boolean,
    illustrationRes: Int,
    labelRes: Int,
    onClick: () -> Unit,
) {
    val labelColor = if (selected) colorResource(R.color.c5) else Color(0xFF999999)
    val backgroundColor = if (selected) Color(0xFFFCF6F1) else Color(0xFFF9F9FA)
    val borderColor = colorResource(R.color.c5)
    val grayscaleFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix().apply { setToSaturation(0f) }
        )
    }

    Surface(
        modifier = modifier.height(118.dp),
        shape = GenderCardShape,
        color = backgroundColor,
        border = if (selected) BorderStroke(2.dp, borderColor) else null,
        onClick = onClick,
        enabled = true,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val accentPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.35f, size.height * 0.76f)
                        cubicTo(
                            size.width * 0.24f,
                            size.height * 0.69f,
                            size.width * 0.06f,
                            size.height * 0.83f,
                            -size.width * 0.01f,
                            size.height * 0.92f
                        )
                        lineTo(-size.width * 0.012f, size.height)
                        lineTo(size.width * 0.96f, size.height)
                        lineTo(size.width, size.height * 0.28f)
                        cubicTo(
                            size.width * 0.92f,
                            size.height * 0.26f,
                            size.width * 0.79f,
                            size.height * 0.41f,
                            size.width * 0.73f,
                            size.height * 0.49f
                        )
                        cubicTo(
                            size.width * 0.64f,
                            size.height * 0.61f,
                            size.width * 0.46f,
                            size.height * 0.82f,
                            size.width * 0.35f,
                            size.height * 0.76f
                        )
                        close()
                    }
                    val cornerPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.78f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height * 0.58f)
                        lineTo(size.width * 0.61f, size.height * 0.58f)
                        close()
                    }

                    onDrawBehind {
                        drawPath(
                            path = accentPath,
                            color = if (selected) Color(0xFFFBECE6) else Color(0xFFF0F0F0)
                        )
                        drawPath(
                            path = cornerPath,
                            color = if (selected) Color(0xFFF7EFE9) else Color(0xFFF3F3F4)
                        )
                    }
                }
        ) {
            Text(
                text = stringResource(labelRes),
                color = labelColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 18.dp)
            )

            Image(
                painter = painterResource(illustrationRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (selected) null else grayscaleFilter,
                alpha = if (selected) 1f else 0.92f,
                modifier = Modifier
                    .size(width = 94.dp, height = 74.dp)
                    .align(Alignment.BottomCenter)
                    .offset(x = 11.dp, y = 2.dp)
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
    val shape = RoundedCornerShape(10.dp)
    val bg = if (enabled) colorResource(R.color.c5) else colorResource(R.color.c5).copy(alpha = 0.35f)
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
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
