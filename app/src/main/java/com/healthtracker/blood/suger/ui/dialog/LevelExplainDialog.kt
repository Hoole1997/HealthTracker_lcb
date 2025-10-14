package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.databinding.DialogLevelExplainBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone

class LevelExplainDialog : BaseBottomSheetDialogFragment<DialogLevelExplainBinding>() {

    companion object {
        private const val KEY_DES = "key_des"
        private const val KEY_ITEMS = "key_items"

        fun show(
            fragmentManager: FragmentManager,

            items: ArrayList<LevelExplainItem>,
            des: String? = null
        ) {
            val dialog = LevelExplainDialog()
            val args = Bundle().apply {
                putString(KEY_DES, des)
                putParcelableArrayList(KEY_ITEMS, items.toParcelableList())
            }
            dialog.arguments = args
            dialog.show(fragmentManager)
        }
    }

    private val adapter = LevelExplainAdapter(emptyList())

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogLevelExplainBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            rvList.layoutManager = LinearLayoutManager(requireContext())
            rvList.adapter = adapter

            val des = arguments?.getString(KEY_DES) ?: ""
            if (des.isEmpty()) {
                tvRangeDes.gone()
            } else {
                tvRangeDes.text = des
            }
            val pList = arguments?.getParcelableArrayList<android.os.Parcelable>(KEY_ITEMS) ?: arrayListOf()
            val items = pList.toExplainItems()
            adapter.submitList(items)

            btnClose.clickWithDuration { dismissAllowingStateLoss() }
        }
    }
}

// 将数据类转换为可放入 Bundle 的 Parcelable 列表（简化处理：直接用 Bundle 序列化字段）
private fun ArrayList<LevelExplainItem>.toParcelableList(): ArrayList<android.os.Parcelable> {
    return ArrayList(this.map { item ->
        Bundle().apply {
            putString("name", item.name)
            putString("desc", item.desc)
            putInt("color", item.colorInt)
        }
    })
}

@Suppress("UNCHECKED_CAST")
private fun ArrayList<android.os.Parcelable>.toExplainItems(): ArrayList<LevelExplainItem> {
    return ArrayList(this.mapNotNull { p ->
        (p as? Bundle)?.let {
            LevelExplainItem(
                it.getString("name") ?: "",
                it.getString("desc") ?: "",
                it.getInt("color")
            )
        }
    })
}