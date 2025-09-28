package com.healthtracker.framework.ext

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.healthtracker.framework.util.hasOreo
import java.util.regex.Pattern
import androidx.core.net.toUri

/**
 * 获取屏幕宽度
 */
val Context.screenWidth
    get() = Resources.getSystem().displayMetrics.widthPixels

/**
 * 获取屏幕宽度
 */
val Fragment.screenWidth
    get() = Resources.getSystem().displayMetrics.widthPixels

/**
 * 获取屏幕高度
 */
val Context.screenHeight
    get() = Resources.getSystem().displayMetrics.heightPixels


/**
 * 获取屏幕宽度
 */
val Fragment.screenHeight
    get() = Resources.getSystem().displayMetrics.heightPixels

/**
 * 判断是否为空 并传入相关操作
 */
inline fun <reified T> T?.notNull(notNullAction: (T) -> Unit, nullAction: () -> Unit = {}) {
    if (this != null) {
        notNullAction.invoke(this)
    } else {
        nullAction.invoke()
    }
}



/**
 * dp值转换为px
 */
fun Context.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

/**
 * dp值转换为px
 */
fun Fragment.dp2px(dp: Int): Int {
    return try {
        val scale = resources.displayMetrics.density
        (dp * scale + 0.5f).toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * px值转换成dp
 */
fun Context.px2dp(px: Int): Int {
    return try {
        val scale = resources.displayMetrics.density
        (px / scale + 0.5f).toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * dp值转换为px
 */
fun View.dp2px(dp: Int): Int {
    return try {
        val scale = resources.displayMetrics.density
        (dp * scale + 0.5f).toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * px值转换成dp
 */
fun View.px2dp(px: Int): Int {
    return try {
        val scale = resources.displayMetrics.density
        (px / scale + 0.5f).toInt()
    } catch (e: Exception) {
        0
    }
}

/**
 * 复制文本到粘贴板
 */
fun Context.copyToClipboard(text: String, label: String = "JetpackMvvm") {
    val clipData = ClipData.newPlainText(label, text)
    ContextCompat.getSystemService(this, ClipboardManager::class.java)?.setPrimaryClip(clipData)
}


/**
 * 设置防止重复点击事件
 * @param views 需要设置点击事件的view集合
 * @param interval 时间间隔 默认0.5秒
 * @param onClick 点击触发的方法
 */
fun setOnclickNoRepeat(vararg views: View?, interval: Long = 500, onClick: (View) -> Unit) {
    views.forEach {
        it?.clickNoRepeat(interval = interval) { view ->
            onClick.invoke(view)
        }
    }
}

/**
 * 防止重复点击事件 默认0.5秒内不可重复点击
 * @param interval 时间间隔 默认0.5秒
 * @param action 执行方法
 */
fun View.clickNoRepeat(interval: Long = 500, action: (view: View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastClickTime
        if (lastClickTime != 0L && diff > 0 && diff < interval) {
            return@setOnClickListener
        }
        lastClickTime = currentTime
        action(it)
    }
}



fun getActivity(context: Context?): Activity? {
    var context = context
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}


/**
 * 是否为邮箱号
 */
fun String?.isEmail(): Boolean {
    return this?.let {
        Pattern.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*\$", this)
    } ?: let {
        false
    }
}



/**
 * 删除符合条件的和Null，但null不触发true
 */
fun <E> MutableList<E>.removeIfOrNull(filter: ((E) -> Boolean)): Boolean {
    var removed = false
    val iterator = iterator()
    while (iterator.hasNext()) {
        val next = iterator.next()
        if (next != null) {
            if (filter(next)) {
                iterator.remove()
                removed = true
            }
        } else {
            iterator.remove()
        }
    }
    return removed
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.gone() {
    this.visibility = View.GONE
}

fun <T : View> T.click(block: (T) -> Unit) {
    setOnClickListener {
        block(this)
    }
}

//私有扩展属性，允许2次点击的间隔时间
private var <T : View> T.delayTime: Long
    get() = getTag(0x7FFF0001) as? Long ?: 0
    set(value) {
        setTag(0x7FFF0001, value)
    }

//私有扩展属性，记录点击时的时间戳
private var <T : View> T.lastClickTime: Long
    get() = getTag(0x7FFF0002) as? Long ?: 0
    set(value) {
        setTag(0x7FFF0002, value)
    }

//私有扩展方法，判断能否触发点击事件
private fun <T : View> T.canClick(): Boolean {
    var flag = false
    var now = System.currentTimeMillis()
    if (now - this.lastClickTime >= this.delayTime) {
        flag = true
        this.lastClickTime = now
    }
    return flag
}

//扩展点击事件，默认 500ms 内不能触发 2 次点击
fun <T : View> T.clickWithDuration(time: Long = 500, block: (T) -> Unit) {
    delayTime = time
    setOnClickListener {
        if (canClick()) {
            block(this)
        }
    }
}


/**
 * 设置View高度，限制在min和max范围之内
 * @param h
 * @param min 最小高度
 * @param max 最大高度
 */
fun View.limitHeight(h: Int, min: Int, max: Int): View {
    val params = layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    when {
        h < min -> params.height = min
        h > max -> params.height = max
        else -> params.height = h
    }
    layoutParams = params
    return this
}

/**
 * 设置View宽度，限制在min和max范围之内
 * @param w
 * @param min 最小宽度
 * @param max 最大宽度
 */
fun View.limitWidth(w: Int, min: Int, max: Int): View {
    val params = layoutParams ?: ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
    when {
        w < min -> params.width = min
        w > max -> params.width = max
        else -> params.width = w
    }
    layoutParams = params
    return this
}


/**
 * 设置View的margin
 * @param startMargin 默认保留原来的
 * @param topMargin 默认是保留原来的
 * @param endMargin 默认是保留原来的
 * @param bottomMargin 默认是保留原来的
 * @param rtl
 */
fun View.margin(
    startMargin: Int = Int.MAX_VALUE,
    topMargin: Int = Int.MAX_VALUE,
    endMargin: Int = Int.MAX_VALUE,
    bottomMargin: Int = Int.MAX_VALUE,
    supportRTL: Boolean = true
): View {
    val params = layoutParams as? ViewGroup.MarginLayoutParams
    if (startMargin != Int.MAX_VALUE) {
        if (supportRTL)
            params?.marginStart = startMargin
        else
            params?.leftMargin = startMargin
    }
    if (topMargin != Int.MAX_VALUE)
        params?.topMargin = topMargin
    if (endMargin != Int.MAX_VALUE) {
        if (supportRTL)
            params?.marginEnd = endMargin
        else
            params?.rightMargin = endMargin
    }
    if (bottomMargin != Int.MAX_VALUE)
        params?.bottomMargin = bottomMargin
    params?.let { layoutParams = it }
    return this
}

private const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
private const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"
fun Intent.highlightSettingsTo(string: String): Intent {
    putExtra(EXTRA_FRAGMENT_ARG_KEY, string)
    val bundle = bundleOf(EXTRA_FRAGMENT_ARG_KEY to string)
    putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    return this
}

fun Intent.applicationSettingsTo(context: Context): Intent {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    data = "package:${context.packageName}".toUri()
    return this
}

//系统设置页
fun Activity.toSetting(requestCode: Int) {
    val intent = Intent().applicationSettingsTo(this)
    startActivityForResult(intent, requestCode)
}

fun Activity.toSetting() {
    val intent = Intent().applicationSettingsTo(this)
    startActivity(intent)
}

//应用通知设置页
fun Activity.toNotificationSetting(requestCode: Int) {
    if (hasOreo()) {
        startActivityForResult(Intent().apply {
            action = ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(EXTRA_APP_PACKAGE, packageName)
        }, requestCode)
    } else {
        toSetting(requestCode)
    }
}

fun Activity.toNotificationSetting() {
    if (hasOreo()) {
        startActivity(Intent().apply {
            action = ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(EXTRA_APP_PACKAGE, packageName)
        })
    } else {
        toSetting()
    }
}

fun openBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(url.toUri())
        context.startActivity(intent)
    } catch (e: Exception) {
        e.toString().logd()
    }
}