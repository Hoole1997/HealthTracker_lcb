package com.healthtracker.blood.suger.ui.widget

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.PopupWindow
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.HtLayoutStatisticDimensionMenuBinding
import com.healthtracker.blood.suger.viewmodel.StatisticDimension
import com.healthtracker.framework.ext.click

class StatisticDimensionMenu(
    private val context: Context,
    private val onSelect: ((StatisticDimension) -> Unit)? = null
) : PopupWindow(context) {

    private val binding = HtLayoutStatisticDimensionMenuBinding.inflate(LayoutInflater.from(context))

    init {
        setupView()
    }

    private fun setupView() {
        with(binding) {
            tvAvg.click {
                onSelect?.invoke(StatisticDimension.AVG)
                dismiss()
            }

            tvMin.click {
                onSelect?.invoke(StatisticDimension.MIN)
                dismiss()
            }

            tvMax.click {
                onSelect?.invoke(StatisticDimension.MAX)
                dismiss()
            }

            contentView = root
            isOutsideTouchable = true
            isFocusable = true
        }
    }

    fun show(anchor: android.view.View) {
        showAsDropDown(anchor, 0, 0, Gravity.BOTTOM)
    }
}
