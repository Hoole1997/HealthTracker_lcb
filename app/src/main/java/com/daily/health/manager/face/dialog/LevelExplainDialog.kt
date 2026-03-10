package com.daily.health.manager.face.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.daily.health.manager.databinding.TrDialogLevelExplainBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone

class LevelExplainDialog(
    private val items: List<LevelExplainItem>,
    private val des: String? = null
) : BaseBottomSheetDialogFragment<TrDialogLevelExplainBinding>() {

    constructor() : this(emptyList(), null)

    companion object {

        fun show(
            fragmentManager: FragmentManager,
            items: ArrayList<LevelExplainItem>,
            des: String? = null
        ) {
            val dialog = LevelExplainDialog(items, des)
            dialog.show(fragmentManager)
        }
    }

    private val adapter = LevelExplainAdapter(emptyList())

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = TrDialogLevelExplainBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.apply {
            rvList.layoutManager = LinearLayoutManager(requireContext())
            rvList.adapter = adapter

            if (des?.isEmpty() ?: true) {
                tvRangeDes.gone()
            } else {
                tvRangeDes.text = des
            }
            adapter.submitList(items)

            btnClose.clickWithDuration { dismissAllowingStateLoss() }
        }
    }
}