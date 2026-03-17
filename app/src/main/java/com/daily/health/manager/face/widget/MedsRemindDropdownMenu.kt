package com.daily.health.manager.face.widget

import android.content.Context
import android.view.LayoutInflater
import android.widget.PopupWindow
import com.daily.health.manager.databinding.FcLayoutMedsRemindMenuBinding
import com.healthtracker.framework.ext.click


class MedsRemindDropdownMenu(
    private val context: Context,

    private val onSelect: ((MenuAction) -> Unit)? = null
) : PopupWindow(context) {

    private val binding = FcLayoutMedsRemindMenuBinding.inflate(LayoutInflater.from(context))

    init {
        setupView()
    }

    private fun setupView() {
        with(binding) {
            tvTakeNow.click {
                onSelect?.invoke(MenuAction.TAKE_NOW)
                dismiss()
            }

            tvEdit.click {
                onSelect?.invoke(MenuAction.EDIT)
                dismiss()
            }

            tvDelete.click {
                onSelect?.invoke(MenuAction.DELETE)
                dismiss()
            }

            contentView = root
        }
    }


}

enum class MenuAction{
    TAKE_NOW,EDIT,DELETE
}