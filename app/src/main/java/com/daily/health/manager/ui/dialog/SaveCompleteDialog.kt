package com.daily.health.manager.ui.dialog

import android.animation.Animator
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.databinding.HtDialogSaveCompleteBinding
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment

class SaveCompleteDialog(private val onEnd:(() -> Unit)? = null): BaseVbDialogFragment<HtDialogSaveCompleteBinding>() {
    constructor():this(null)
    companion object{
        fun show(fragmentManager: FragmentManager,onEnd:() -> Unit){
            SaveCompleteDialog(onEnd).show(fragmentManager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) =  HtDialogSaveCompleteBinding.inflate(inflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            lottie.addAnimatorListener(object : Animator.AnimatorListener{
                override fun onAnimationStart(animation: Animator) {

                }

                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                    dismissAllowingStateLoss()

                }

                override fun onAnimationCancel(animation: Animator) {

                }

                override fun onAnimationRepeat(animation: Animator) {

                }

            })
        }
    }


}