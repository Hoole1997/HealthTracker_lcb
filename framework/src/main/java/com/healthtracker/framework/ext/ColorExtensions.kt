
package com.healthtracker.framework.ext

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat


@CheckResult
fun Drawable.tint(@ColorInt color: Int): Drawable {
    val tintedDrawable = DrawableCompat.wrap(this).mutate()
    setTint(color)
    return tintedDrawable
}

@CheckResult
fun Drawable.tint(context: Context, @ColorRes color: Int): Drawable =
    tint(context.getColorCompat(color))

@ColorInt
fun Context.getColorCompat(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}



