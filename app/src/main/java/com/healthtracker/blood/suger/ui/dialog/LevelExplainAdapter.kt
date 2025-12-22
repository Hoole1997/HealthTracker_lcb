package com.healthtracker.blood.suger.ui.dialog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.databinding.HtItemLevelExplainBinding

class LevelExplainAdapter(private var items: List<LevelExplainItem>) :
    RecyclerView.Adapter<LevelExplainAdapter.VH>() {

    inner class VH(val binding: HtItemLevelExplainBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = HtItemLevelExplainBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvLeve.text = item.name
            tvValueRange.text = item.desc
            vFlag.backgroundTintList = android.content.res.ColorStateList.valueOf(item.colorInt)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<LevelExplainItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}