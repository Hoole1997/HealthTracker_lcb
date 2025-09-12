package com.healthtracker.framework.ext

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

inline fun <reified T : Activity> Context.startActivity(vararg params: Pair<String, Any?>) {
    startActivity(Intent(this, T::class.java).putExtras(bundleOf(*params)))
}

inline fun <reified T : Activity> Activity.startActivity(
    requestCode: Int,
    vararg params: Pair<String, Any?>
) {
    startActivityForResult(Intent(this, T::class.java).putExtras(bundleOf(*params)), requestCode)
}

inline fun <reified T : Activity> Activity.startActivity(anim: ActivityOptionsCompat) {
    ActivityCompat.startActivity(this, Intent(this, T::class.java), anim.toBundle())
}

inline fun <reified T : Activity> Context.getIntent(vararg params: Pair<String, Any?>): Intent {
    return Intent(this, T::class.java).putExtras(bundleOf(*params))
}

inline fun <reified T : Activity> FragmentActivity.startActivity(
    vararg params: Pair<String, Any?>,
    isFinishSelf: Boolean = false
) {
    startActivity(Intent(this, T::class.java).putExtras(bundleOf(*params)))
    if (isFinishSelf) {
        finish()
    }
}

inline fun <reified T : Activity> FragmentActivity.startActivity(
    bundle: Bundle,
    isFinishSelf: Boolean = false
) {
    startActivity(Intent(this, T::class.java).putExtras(bundle))
    if (isFinishSelf) {
        finish()
    }
}

inline fun <reified T : Activity> FragmentActivity.startActivity(
    data: Uri,
    isFinishSelf: Boolean = false
) {
    startActivity(Intent(this, T::class.java).setData(data))
    if (isFinishSelf) {
        finish()
    }
}

inline fun <reified T : Activity> FragmentActivity.startActivity(
    data: Uri,
    action: String,
    isFinishSelf: Boolean = false
) {
    startActivity(Intent(this, T::class.java).setData(data).setAction(action))
    if (isFinishSelf) {
        finish()
    }
}



fun BottomSheetDialog.showToast(msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

fun BottomSheetDialog.getString(@StringRes id: Int): String {
    return context.getString(id)
}

fun Fragment.showToast(msg: String) {
    Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
}

fun Activity.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}


fun <T : Activity> T.hideSoftKeyBoard(): Boolean {
    val imm = this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager?
    return imm!!.hideSoftInputFromWindow(this.window.decorView.windowToken, 0)
}

fun <T : Activity> T.showSoftKeyBoard(editText: EditText): Boolean {
    editText.isFocusable = true
    editText.isFocusableInTouchMode = true
    //请求获得焦点
    editText.requestFocus()
    val imm = this.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager?
    return imm!!.showSoftInput(editText, 0)
}


