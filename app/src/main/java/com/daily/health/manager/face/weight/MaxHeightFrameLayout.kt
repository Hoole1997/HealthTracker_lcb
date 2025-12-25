package com.daily.health.manager.face.weight

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.healthtracker.framework.R

class MaxHeightFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var maxHeight: Int = context.resources.getDimensionPixelSize(R.dimen.dp_84)

    init {
        context.theme.obtainStyledAttributes(attrs, com.daily.health.manager.R.styleable.MaxHeightFrameLayout, 0, 0).apply {
            maxHeight = getDimensionPixelSize(
                com.daily.health.manager.R.styleable.MaxHeightFrameLayout_maxHeight,
                context.resources.getDimensionPixelSize(R.dimen.dp_84),
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightSpec = heightMeasureSpec
        if (maxHeight > 0) {
            val heightSize = MeasureSpec.getSize(heightMeasureSpec)
            if (heightSize > maxHeight) {
                heightSpec = MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
            }
        }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}