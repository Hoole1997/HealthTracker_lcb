package com.daily.health.manager.ui.act

import android.content.Context
import android.os.Bundle
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.databinding.HtActivityHydrateCompleteBinding
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.startActivity
import net.corekit.monetize.ui.NativeAdStyle
import com.daily.health.manager.utils.loadNative

class HydrateCompleteActivity : BaseInterActivity<BaseViewModel, HtActivityHydrateCompleteBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateCompleteActivity>()
        }
    }

    override fun createViewBinding(): HtActivityHydrateCompleteBinding =
        HtActivityHydrateCompleteBinding.inflate(layoutInflater)

    override fun getVMModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.apply {
            tvMessage.setText(R.string.ht_hydrate_complete_message)

            // 完成按钮
            btnDone.setOnClickListener { finish() }

            // 加载原生广告（底部卡片区域）
            loadNative(adContainer, style = NativeAdStyle.CARD_6)
        }
    }
}