package com.daily.health.manager.ui.widget

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan

class CustomQuoteSpan(
    private val color: Int,
    private val stripeWidth: Int = 10,
    private val gapWidth: Int = 20
) : LeadingMarginSpan {

    override fun getLeadingMargin(first: Boolean): Int {
        return stripeWidth + gapWidth
    }

    override fun drawLeadingMargin(
        c: Canvas,
        p: Paint,
        x: Int,
        dir: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        first: Boolean,
        layout: Layout
    ) {
        val style = p.style
        val originalColor = p.color

        p.style = Paint.Style.FILL
        p.color = this.color

        c.drawRect(
            x.toFloat(),
            top.toFloat(),
            x + dir * stripeWidth.toFloat(),
            bottom.toFloat(),
            p
        )

        p.style = style
        p.color = originalColor
    }
}
