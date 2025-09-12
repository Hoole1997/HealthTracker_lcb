package com.healthtracker.framework.ext

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


/**
 * dp to px
 */
val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )


private var lastClickTime: Long = 0

/***
 * 判断是否快速点击
 */
fun isShortClick(): Boolean {
    val currentTime = System.currentTimeMillis()
    val step: Long = currentTime - lastClickTime
    if (step in 1..299) {
        return true
    }
    lastClickTime = currentTime
    return false
}

/**
 * toast short
 * @param message 消息
 */
fun Context.toastShort(message: CharSequence) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Context.toastShort(resId: Int) = toastShort(getString(resId))

fun String.highlightText(keyword: String): CharSequence {
    if (keyword.isNotEmpty()) {
        val highlightColor = Color.parseColor("#EE1D47")
        val spannableString = SpannableString(this)
        val lowerOriginalText = this.lowercase()
        val lowerKeyword = keyword.lowercase()

        var startIndex = lowerOriginalText.indexOf(lowerKeyword)
        while (startIndex != -1) {
            val endIndex = startIndex + keyword.length
            if (endIndex > spannableString.length) {
                break
            }
            spannableString.setSpan(
                ForegroundColorSpan(highlightColor),
                startIndex,
                endIndex,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            startIndex = lowerOriginalText.indexOf(lowerKeyword, endIndex)
        }
        return spannableString
    }
    return this
}

/**
 * toast Long
 * @param message 消息
 */
@Suppress("unused")
fun Context.toastLong(message: CharSequence) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun Context.toastLong(resId: Int) = toastLong(getString(resId))

inline fun <reified T> Gson.fromJson(json: String): T =
    fromJson(json, object : TypeToken<T>() {}.type)
