package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.daily.health.manager.databinding.TrDialogImgGetTypeSelectBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration

class ImgGetTypeDialog(
    private val onTakePhoto: (() -> Unit)? = null,
    private val onChoosePhoto: (() -> Unit)? = null
) : BaseBottomSheetDialogFragment<TrDialogImgGetTypeSelectBinding>() {

    constructor():this(null,null)
    companion object {
        fun show(
            fragmentManager: FragmentManager,
            onTakePhoto: (() -> Unit)? = null,
            onChoosePhoto: (() -> Unit)? = null
        ) {
            ImgGetTypeDialog(onTakePhoto,onChoosePhoto).show(fragmentManager)
        }
    }

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = TrDialogImgGetTypeSelectBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            tvTakePhoto.clickWithDuration {
                onTakePhoto?.invoke()
                dismissAllowingStateLoss()
            }

            tvChooseFromGallery.clickWithDuration {
                onChoosePhoto?.invoke()
                dismissAllowingStateLoss()
            }
        }
    }
}