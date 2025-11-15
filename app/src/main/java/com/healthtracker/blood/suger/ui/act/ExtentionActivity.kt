package com.healthtracker.blood.suger.ui.act

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.showToast
import kotlinx.coroutines.launch

inline fun AppCompatActivity.showDeleteConfirm(crossinline onConfirm: suspend () -> Boolean) {
    ConfirmDialog(
        title = getString(R.string.delete_record_remind_title),
        message = getString(R.string.delete_record_remind),
        leftText = getString(R.string.cancel),
        rightText = getString(R.string.confirm),
        onDialogListener = object : DialogListener {
            override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                super.onItemClick(dialogFragment, which)
                if (which == R.id.btn_ok) {
                    lifecycleScope.launch {
                        if (onConfirm.invoke()) {
                            finish()
                        } else {
                            showToast(getString(R.string.delete_record_failed))
                        }
                    }
                }
            }
        }
    ).show(supportFragmentManager)
}


inline fun AppCompatActivity.showFreeLockConfirm(crossinline onConfirm: () -> Unit,crossinline onCancel:() -> Unit) {
    //TODO 这个最好只展示异常，不然会影响下面原生广告有效展示
    ConfirmDialog(
        title = getString(R.string.kindly_note),
        message = getString(R.string.your_health_data_requires),
        leftText = getString(R.string.cancel),
        rightText = getString(R.string.free_unlock),
        onDialogListener = object : DialogListener {
            override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                super.onItemClick(dialogFragment, which)
                if (which == R.id.btn_ok) {
                    onConfirm.invoke()
                }else{
                    onCancel.invoke()
                }
            }
        }
    ).apply {
        isCancelable = false
    }.show(supportFragmentManager)
}