package com.daily.health.manager.face.act

import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.daily.health.manager.databinding.HtActivityAlarmManagerBinding
import com.daily.health.manager.face.compose.AiAssistantScreen
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel

/**
 * AI 助手宿主页面
 */
class AiAssistantActivity : BaseMVVMActivity<BaseViewModel, HtActivityAlarmManagerBinding>() {


    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AiAssistantScreen(
                    onBack = { finish() }
                )
            }
        }
    }

    override fun createViewBinding() = HtActivityAlarmManagerBinding.inflate(layoutInflater)


    override fun getVMModelClass() = BaseViewModel::class.java
}
