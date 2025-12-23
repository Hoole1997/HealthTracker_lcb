package com.daily.health.manager.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtDialogStatusSelectBinding
import com.daily.health.manager.databinding.HtItemStatusBinding
import com.daily.health.manager.data.enums.BloodSugarStatus
import com.daily.health.manager.data.enums.getStatusStringRes
import com.daily.health.manager.ui.weight.WrapLayoutLinearLayoutManager
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment

class StatusSelectDialog(
    private val currentStatus: BloodSugarStatus?,
    private val showAllOption: Boolean,
    private val onSelect: ((BloodSugarStatus?) -> Unit)? = null
) : BaseBottomSheetDialogFragment<HtDialogStatusSelectBinding>() {

    companion object{
        fun show(
            fragmentManager: FragmentManager,
            currentStatus: BloodSugarStatus?,
            showAllOption: Boolean = false,
            onSelect: ((BloodSugarStatus?) -> Unit)? = null
        ){
            StatusSelectDialog(currentStatus, showAllOption, onSelect).show(fragmentManager)
        }
    }

    constructor() : this(currentStatus = null, showAllOption = false, onSelect = null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = HtDialogStatusSelectBinding.inflate(inflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.let {
            val dataSource = BloodSugarStatus.entries
                .map { status -> StatuItem(status.statusType, getStatusStringRes(status.statusType)) }
                .toMutableList()
            if (showAllOption) {
                dataSource.add(0, StatuItem(statuType = -1, displayNameRes = R.string.ht_all_types))
            }
            it.rvStatus.adapter = StatusAdapter(dataSource)
            it.rvStatus.layoutManager = WrapLayoutLinearLayoutManager(view.context)


        }

    }


   inner class StatusAdapter(private val list: List<StatuItem>) : RecyclerView.Adapter<StatusAdapter.SatusViewHolder>() {
        var selectIndex: Int = when {
            currentStatus == null && showAllOption -> 0
            currentStatus == null -> 0
            else -> {
                val index = list.indexOfFirst { currentStatus.statusType == it.statuType }
                if (index == -1 && showAllOption) 1 else index.takeIf { it >= 0 } ?: 0
            }
        }


        inner class SatusViewHolder(private val itemBinding: HtItemStatusBinding) : RecyclerView.ViewHolder(itemBinding.root),
            View.OnClickListener {
            init {
                itemBinding.root.setOnClickListener(this@SatusViewHolder)
            }

            fun bind(position: Int) {
                itemBinding.apply {
                    ivSelect.isSelected = selectIndex == position
                    root.isSelected = selectIndex == position
                    tvSatusName.isSelected = selectIndex == position

                    val statusType = list[position].statuType
                    val displayStatusStr = if(statusType == -1){
                        tvSatusName.context.getString(R.string.ht_all_types)
                    }else {
                        tvSatusName.context.getString(getStatusStringRes(statusType))
                    }
                    tvSatusName.text = displayStatusStr
                }
            }

            override fun onClick(v: View?) {
                v?.let {
                    val position = bindingAdapterPosition
                    if (position in 0 until itemCount) {
                        selectIndex = position
                        notifyDataSetChanged()
                        val item = list[position]
                        if(item.statuType == -1){
                            onSelect?.invoke(null)
                        }else{
                            onSelect?.invoke(BloodSugarStatus.entries.first { status -> status.statusType == item.statuType })
                        }
                        dismissAllowingStateLoss()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatusViewHolder {
            val itemBinding = HtItemStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SatusViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SatusViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = list.size
    }


    data class StatuItem(val statuType: Int, val displayNameRes:Int)
}
