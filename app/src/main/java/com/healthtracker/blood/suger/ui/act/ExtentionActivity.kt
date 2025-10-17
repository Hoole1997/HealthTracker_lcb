package com.healthtracker.blood.suger.ui.act

import android.app.Activity
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