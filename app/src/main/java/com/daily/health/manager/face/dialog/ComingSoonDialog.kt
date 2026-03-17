package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.databinding.FcDialogComingSoonBinding
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment
import com.healthtracker.framework.ext.click

/**
 * ComingSoon 弹窗
 * 当用户选择今天之后的日期时提示，并提供返回到今天的按钮。
 */
class ComingSoonDialog(
    private val onBackToToday: (() -> Unit)? = null
) : BaseVbDialogFragment<FcDialogComingSoonBinding>() {

    companion object {
        fun show(
            fragmentManager: FragmentManager,
            onBackToToday: (() -> Unit)? = null
        ) {
            ComingSoonDialog(onBackToToday).show(fragmentManager)
        }
    }

    constructor() : this(null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FcDialogComingSoonBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            tvTitle.text = context?.getString(com.daily.health.manager.R.string.fc_coming_soon_title)
            tvMessage.text = context?.getString(com.daily.health.manager.R.string.fc_coming_soon_message)
            btnBackToday.text = context?.getString(com.daily.health.manager.R.string.fc_back_to_today)

            btnBackToday.click {
                onBackToToday?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
}