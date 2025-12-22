package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.ethanhua.skeleton.ViewSkeletonScreen
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.HtDialogExitBinding
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import net.corekit.monetize.ui.NativeAdStyle

class ExitDialog(private val onExit:(() -> Unit)? = null): BaseBottomSheetDialogFragment<HtDialogExitBinding>() {

    constructor():this(null)

    companion object{
        fun show(fragmentManager: FragmentManager,onExit:() -> Unit){
            ExitDialog(onExit).show(fragmentManager)
        }
    }
    private lateinit var skeleton: ViewSkeletonScreen
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogExitBinding.inflate(layoutInflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            btnExit.clickWithDuration {
                onExit?.invoke()
                dismissAllowingStateLoss()
            }

            btnCancel.clickWithDuration {
                dismissAllowingStateLoss()
            }

            skeleton = ViewSkeletonScreen.Builder(adContainer)
                .load(R.layout.ht_layout_skeleton_ad)
                .shimmer(true)
                .angle(30)
                .duration(1200)
                .color(R.color.color_f7f7f7)
                .show()

            if(context is FragmentActivity){
                (context as FragmentActivity).loadNative(adContainer, style = NativeAdStyle.CARD_3, call = {
                    skeleton.hide()
                    if(!it){
                        adContainer.gone()
                    }
                })
            }

        }
    }
}