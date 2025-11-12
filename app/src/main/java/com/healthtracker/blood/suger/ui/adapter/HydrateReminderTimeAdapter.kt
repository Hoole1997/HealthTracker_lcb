package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.DialogFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.blood.suger.ui.dialog.DeleteHydrateReminderDialog

class HydrateReminderTimeAdapter(private val times: MutableList<String>) :
    RecyclerView.Adapter<HydrateReminderTimeAdapter.TimeViewHolder>() {

    private var showDelete: Boolean = false
    private val enabledStates: MutableList<Boolean> = MutableList(times.size) { false }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hydrate_setting_reminder_time, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        val t = times[position]
        holder.tvTime.text = t
        holder.imgDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
        // 根据当前启用状态更新开关图片
        holder.switchReminder.isSelected = enabledStates[position]
        holder.switchReminder.setOnClickListener {
            val newState = !enabledStates[position]
            enabledStates[position] = newState
            holder.switchReminder.isSelected = newState
        }

        // 删除按钮：弹出底部确认对话框
        holder.imgDelete.setOnClickListener {
            val activity = holder.itemView.context as? FragmentActivity ?: return@setOnClickListener
            DeleteHydrateReminderDialog(
                // 底部弹窗不展示标题
                message = activity.getString(R.string.hydrate_reminder_delete_confirm_message),
                leftText = activity.getString(R.string.cancel),
                rightText = activity.getString(R.string.confirm),
                onDialogListener = object : DialogListener {
                    override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                        super.onItemClick(dialogFragment, which)
                        // 此处仅展示对话框，实际删除逻辑可在确认后回调中实现
                        // if (which == R.id.btn_ok) { /* 删除对应提醒 */ }
                    }
                }
            ).show(activity.supportFragmentManager)
        }
        // 最后一项不显示分割线，其余显示
        holder.divider.visibility = if (position == times.lastIndex) View.GONE else View.VISIBLE
    }

    override fun getItemCount(): Int = times.size

    fun setDeleteMode(enabled: Boolean) {
        showDelete = enabled
        notifyDataSetChanged()
    }

    fun addTime(time: String) {
        val previousLastIndex = times.size - 1
        val insertIndex = times.size
        times.add(time)
        enabledStates.add(false)
        // 新增项插入后刷新：
        // 1) 通知新增项
        notifyItemInserted(insertIndex)
        // 2) 之前的最后一项现在不再是最后一个，需要显示分割线
        if (previousLastIndex >= 0) notifyItemChanged(previousLastIndex)
    }

    class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val divider: View = itemView.findViewById(R.id.divider)
        val imgDelete: AppCompatImageView = itemView.findViewById(R.id.imgDelete)
        val switchReminder: AppCompatImageView = itemView.findViewById(R.id.switch_reminder)
    }
}