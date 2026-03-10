package com.daily.health.manager.face.act

import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.daily.health.manager.databinding.TrActivityAlarmManagerBinding
import com.daily.health.manager.face.compose.AiAssistantScreen
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel

/**
 * AI 助手宿主页面
 */
class AiAssistantActivity : BaseMVVMActivity<BaseViewModel, TrActivityAlarmManagerBinding>() {


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

    override fun createViewBinding() = TrActivityAlarmManagerBinding.inflate(layoutInflater)


    override fun getVMModelClass() = BaseViewModel::class.java
}
