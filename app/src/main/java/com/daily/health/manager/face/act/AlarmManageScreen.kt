package com.daily.health.manager.face.act

import android.os.Bundle
import com.daily.health.manager.databinding.TrActivityAlarmManagerBinding
import com.daily.health.manager.face.viewmodel.AlarmViewModel
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle

import androidx.compose.ui.platform.ViewCompositionStrategy
import com.daily.health.manager.feature.NotificationFeatureSwitch
import com.daily.health.manager.face.compose.AlarmListContent

class AlarmManageScreen : BaseMVVMActivity<AlarmViewModel, TrActivityAlarmManagerBinding>() {

    override fun createViewBinding() = TrActivityAlarmManagerBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AlarmViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        if (!NotificationFeatureSwitch.reminderEntryEnabled) {
            finish()
            return
        }

        mViewBind.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AlarmListContent(
                    viewModel = mViewModel,
                    onBack = { finish() }
                )
            }
        }
        
        loadNative(mViewBind.adContainer, AdPosition.NA_ALARM_MANAGER_BOTTOM, style = NativeAdStyle.CARD_7)
    }
}
