package com.android.common.weather

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupWindow
import com.android.common.weather.databinding.LayoutUnitMenuBinding
import com.healthtracker.framework.ext.click

class UnitMenu(
    private val context: Context,
    private val onSelect: ((Int) -> Unit)? = null
) : PopupWindow(context) {

    private val binding = LayoutUnitMenuBinding.inflate(LayoutInflater.from(context))

    init {
        setupView()
    }

    private fun setupView() {
        with(binding) {
            tvUnitF.click {
                onSelect?.invoke(1)
                dismiss()
            }



            tvUnitC.click {
                onSelect?.invoke(0)
                dismiss()
            }

            contentView = root
            isOutsideTouchable = true
            isFocusable = true
        }
    }

    fun show(anchor: View) {
        showAsDropDown(anchor, 0, 0, Gravity.BOTTOM)
    }
}
