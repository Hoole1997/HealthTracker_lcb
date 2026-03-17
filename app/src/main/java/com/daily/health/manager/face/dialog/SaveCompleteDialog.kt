package com.daily.health.manager.face.dialog

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.databinding.TrDialogSaveCompleteBinding
import com.healthtracker.framework.base.fragment.BaseVbDialogFragment

class SaveCompleteDialog(private val onEnd:(() -> Unit)? = null): BaseVbDialogFragment<TrDialogSaveCompleteBinding>() {
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
    ) =  TrDialogSaveCompleteBinding.inflate(inflater,parent,attachToParent)

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
