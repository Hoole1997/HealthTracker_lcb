package com.app.raise.base

import android.content.Context
import android.view.ViewGroup
import androidx.annotation.StyleRes
import com.app.raise.R
import com.google.android.material.bottomsheet.BottomSheetDialog


class BaseDialog(context: Context, @StyleRes theme: Int) : BottomSheetDialog(context, theme) {

    override fun onStart() {
        super.onStart()
        val window = window
        window?.run {
            val wl = attributes
            wl.width = ViewGroup.LayoutParams.MATCH_PARENT
            wl.height = ViewGroup.LayoutParams.WRAP_CONTENT
            attributes = wl
            //setGravity(Gravity.BOTTOM)
            setWindowAnimations(R.style.BottomStyle)
        }
    }
}
