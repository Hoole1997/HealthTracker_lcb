package com.daily.health.manager.face.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import androidx.core.content.ContextCompat
import com.daily.health.manager.R
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView

/** Restyles the existing pager title without replacing paging or category tracking. */
class InsightCategoryTitleView(context: Context) : ColorTransitionPagerTitleView(context) {
    private val density = resources.displayMetrics.density

    init {
        normalColor = ContextCompat.getColor(context, R.color.tr_insights_chip_text)
        selectedColor = Color.WHITE
        maxLines = 1
        setPadding((16 * density).toInt(), 0, (16 * density).toInt(), 0)
        updateSurface(false)
    }

    override fun onSelected(index: Int, totalCount: Int) {
        super.onSelected(index, totalCount)
        updateSurface(true)
    }

    override fun onDeselected(index: Int, totalCount: Int) {
        super.onDeselected(index, totalCount)
        updateSurface(false)
    }

    private fun updateSurface(selected: Boolean) {
        isSelected = selected
        val shape = GradientDrawable().apply {
            cornerRadius = 24 * density
            setColor(ContextCompat.getColor(context,
                if (selected) R.color.tr_entry_primary else R.color.tr_insights_chip_surface))
        }
        background = InsetDrawable(shape, (4 * density).toInt(), (2 * density).toInt(), (4 * density).toInt(), (2 * density).toInt())
    }
}
