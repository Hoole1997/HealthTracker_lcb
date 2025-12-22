package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.ActivityHydrateCompleteBinding
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.startActivity
import net.corekit.monetize.ui.NativeAdStyle
import com.healthtracker.blood.suger.utils.loadNative

class HydrateCompleteActivity : BaseInterActivity<BaseViewModel, ActivityHydrateCompleteBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity<HydrateCompleteActivity>()
        }
    }

    override fun createViewBinding(): ActivityHydrateCompleteBinding =
        ActivityHydrateCompleteBinding.inflate(layoutInflater)

    override fun getVMModelClass(): Class<BaseViewModel> = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        mViewBind.apply {
            tvMessage.setText(R.string.hydrate_complete_message)

            // 完成按钮
            btnDone.setOnClickListener { finish() }

            // 加载原生广告（底部卡片区域）
            loadNative(adContainer, style = NativeAdStyle.CARD_6)
        }
    }
}