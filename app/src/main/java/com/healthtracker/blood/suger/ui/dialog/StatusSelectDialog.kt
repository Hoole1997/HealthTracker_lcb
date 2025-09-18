package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.databinding.DialogStatusSelectBinding
import com.healthtracker.blood.suger.databinding.ItemStatusBinding
import com.healthtracker.blood.suger.enum.BloodSugarStatus
import com.healthtracker.blood.suger.enum.getStatusStringRes
import com.healthtracker.blood.suger.ui.weight.WrapLayoutLinearLayoutManager
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment

class StatusSelectDialog(private val currentStatus: BloodSugarStatus?,private val onSelect: ((BloodSugarStatus) -> Unit)? = null): BaseBottomSheetDialogFragment<DialogStatusSelectBinding>() {

    companion object{
        fun show(fragmentManager: FragmentManager,currentStatus: BloodSugarStatus,onSelect: ((BloodSugarStatus) -> Unit)? = null){
            StatusSelectDialog(currentStatus,onSelect).show(fragmentManager)
        }
    }

    constructor() : this(currentStatus = null, onSelect = null)

    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogStatusSelectBinding.inflate(inflater,parent,attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.let {
            it.rvStatus.adapter = StatusAdapter(BloodSugarStatus.entries)
            it.rvStatus.layoutManager = WrapLayoutLinearLayoutManager(view.context)


        }

    }


   inner class StatusAdapter(private val list: List<BloodSugarStatus>) : RecyclerView.Adapter<StatusAdapter.SatusViewHolder>() {
        var selectIndex: Int = list.indexOfFirst { currentStatus?.statusType == it.statusType }


        inner class SatusViewHolder(private val itemBinding: ItemStatusBinding) : RecyclerView.ViewHolder(itemBinding.root),
            View.OnClickListener {
            init {
                itemBinding.root.setOnClickListener(this@SatusViewHolder)
            }

            fun bind(position: Int) {
                itemBinding.apply {
                    ivSelect.isSelected = selectIndex == position
                    root.isSelected = selectIndex == position
                    tvSatusName.isSelected = selectIndex == position

                    tvSatusName.text = tvSatusName.context.getString(getStatusStringRes(list[position].statusType))
                }
            }

            override fun onClick(v: View?) {
                v?.let {
                    val position = bindingAdapterPosition
                    if (position in 0 until itemCount) {
                        selectIndex = position
                        notifyDataSetChanged()
                        onSelect?.invoke(list[position])
                        dismissAllowingStateLoss()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SatusViewHolder {
            val itemBinding = ItemStatusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return SatusViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: SatusViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = list.size
    }
}