package com.daily.health.manager.face.widget

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.withStyledAttributes
import com.daily.health.manager.R
import com.daily.health.manager.face.compose.HealthDisclaimer
import com.daily.health.manager.face.theme.HealthTrackerTheme

/** XML 页面只负责定位，文案、展开状态和交互全部复用同一个 Compose 组件。 */
class HealthDisclaimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var disclaimerTextRes by mutableIntStateOf(R.string.tr_health_disclaimer)

    init {
        // 专用页面可在 XML 中选文案；共享历史/统计页可随指标动态更新。
        context.withStyledAttributes(attrs, R.styleable.HealthDisclaimerView, defStyleAttr, 0) {
            disclaimerTextRes = getResourceId(
                R.styleable.HealthDisclaimerView_healthDisclaimerText,
                R.string.tr_health_disclaimer,
            )
        }
        // 与宿主视图生命周期绑定，支持 Fragment 及 AndroidView 内嵌的旧页面。
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun setTextResource(@StringRes textRes: Int) {
        disclaimerTextRes = textRes
    }

    @Composable
    override fun Content() {
        HealthTrackerTheme {
            HealthDisclaimer(textRes = disclaimerTextRes)
        }
    }
}
