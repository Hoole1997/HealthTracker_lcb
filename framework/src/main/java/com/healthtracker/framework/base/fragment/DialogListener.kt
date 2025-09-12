package com.healthtracker.framework.base.fragment

import androidx.fragment.app.DialogFragment

interface DialogListener {
    fun onItemClick(dialogFragment: DialogFragment, which: Int) {}

    fun onDismiss(dialogFragment: DialogFragment) {}
}