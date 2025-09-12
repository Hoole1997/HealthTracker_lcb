package com.healthtracker.framework.base.fragment

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

open class BaseDialogFragment : DialogFragment() {
    override fun show(manager: FragmentManager, tag: String?) {
        try {
            super.show(manager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
            showAllowingStateLoss(manager, tag)
        }
    }

    override fun show(transaction: FragmentTransaction, tag: String?): Int {
        return try {
            super.show(transaction, tag)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                transaction.commitAllowingStateLoss()
            } catch (e1: Exception) {
                e1.printStackTrace()
                0
            }
        }
    }

    override fun showNow(manager: FragmentManager, tag: String?) {
        try {
            super.showNow(manager, tag)
        } catch (e: Exception) {
            e.printStackTrace()
            showAllowingStateLoss(manager, tag)
        }
    }

    fun show(manager: FragmentManager?) {
        if (manager != null) {
            this.show(manager, this.javaClass.name)
        }
    }

    fun close() {
        if (isEnable()) {
            super.dismissAllowingStateLoss()
        }
    }

    fun isShow(): Boolean {
        return if (!isEnable()) {
            false
        } else this.dialog != null && this.dialog!!.isShowing
    }

    override fun dismiss() {
        super.dismissAllowingStateLoss()
    }


    private fun showAllowingStateLoss(manager: FragmentManager, tag: String?) {
        try {
            val ft: FragmentTransaction = manager.beginTransaction()
            ft.add(this, tag)
            ft.commitAllowingStateLoss()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isEnable(): Boolean {
        return if (activity == null || requireActivity().isDestroyed || requireActivity().isFinishing) {
            false
        } else this.isAdded && !this.isDetached && !this.isRemoving
    }
}