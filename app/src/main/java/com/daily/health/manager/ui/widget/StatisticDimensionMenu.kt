package com.daily.health.manager.ui.widget

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.PopupWindow
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtLayoutStatisticDimensionMenuBinding
import com.daily.health.manager.viewmodel.StatisticDimension
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
