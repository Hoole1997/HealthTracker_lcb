package com.daily.health.manager.face.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.R
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.DialogFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.daily.health.manager.face.dialog.DeleteHydrateReminderDialog

class HydrateReminderTimeAdapter(
    private val times: MutableList<String>,
    private val onDeleteTime: ((Int, Int) -> Unit)? = null,
    private val onToggleEnabled: ((Int, Int, Boolean) -> Unit)? = null
) :
    RecyclerView.Adapter<HydrateReminderTimeAdapter.TimeViewHolder>() {

    private var showDelete: Boolean = false
    private val enabledStates: MutableList<Boolean> = MutableList(times.size) { false }

    /**
     * 保证 enabledStates 与 times 尺寸一致，避免绑定时越界。
     */
    private fun ensureEnabledStatesSize() {
        val diff = times.size - enabledStates.size
        if (diff > 0) {
            repeat(diff) { enabledStates.add(false) }
        } else if (diff < 0) {
            // 若状态数组比数据更长，裁剪掉多余部分
            repeat(-diff) {
                if (enabledStates.isNotEmpty()) enabledStates.removeAt(enabledStates.lastIndex)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fc_item_hydrate_setting_reminder_time, parent, false)
        return TimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TimeViewHolder, position: Int) {
        // 绑定前同步状态数组大小
        ensureEnabledStatesSize()
        val t = times[position]
        holder.tvTime.text = t
        holder.imgDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
        holder.switchReminder.setOnCheckedChangeListener(null)
        holder.switchReminder.isChecked = enabledStates.getOrNull(position) ?: false
        holder.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnCheckedChangeListener
            ensureEnabledStatesSize()
            if (pos >= enabledStates.size) {
                repeat(pos - enabledStates.size + 1) { enabledStates.add(false) }
            }
            enabledStates[pos] = isChecked
            parseTimeToComponents(times[pos])?.let { (h, m) ->
                onToggleEnabled?.invoke(h, m, isChecked)
            }
        }
        holder.switchReminder.setOnTouchListener { _, event ->
            event.actionMasked == MotionEvent.ACTION_MOVE
        }

        // 删除按钮：弹出底部确认对话框
        holder.imgDelete.setOnClickListener {
            val activity = holder.itemView.context as? FragmentActivity ?: return@setOnClickListener
            val position = holder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return@setOnClickListener
            DeleteHydrateReminderDialog(
                message = activity.getString(R.string.fc_hydrate_reminder_delete_confirm_message),
                leftText = activity.getString(R.string.fc_cancel),
                rightText = activity.getString(R.string.fc_confirm),
                onDialogListener = object : DialogListener {
                    override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                        if (which == DeleteHydrateReminderDialog.BUTTON_OK) {
                            // 先持久化删除，再更新UI列表
                            parseTimeToComponents(times[position])?.let { (h, m) ->
                                onDeleteTime?.invoke(h, m)
                            }
                            removeAt(position)
                        }
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
        enabledStates.add(true)
        // 新增项插入后刷新：
        // 1) 通知新增项
        notifyItemInserted(insertIndex)
        // 2) 之前的最后一项现在不再是最后一个，需要显示分割线
        if (previousLastIndex >= 0) notifyItemChanged(previousLastIndex)
    }

    /** 用新列表替换全部提醒时间并刷新 */
    fun replaceAll(newTimes: List<String>) {
        times.clear()
        times.addAll(newTimes)
        enabledStates.clear()
        enabledStates.addAll(List(times.size) { false })
        notifyDataSetChanged()
    }

    fun removeAt(index: Int) {
        if (index !in 0 until times.size) return
        times.removeAt(index)
        if (index < enabledStates.size) {
            enabledStates.removeAt(index)
        }
        notifyItemRemoved(index)
        // 刷新后续项的分割线与内容位置
        if (index <= times.lastIndex) {
            notifyItemRangeChanged(index, times.size - index)
        }
        // 刷新新的最后一项以更新分割线显示状态
        if (times.isNotEmpty()) notifyItemChanged(times.lastIndex)
    }

    /**
     * 外部传入启用状态映射（key为"HH:MM"），以当前times顺序同步开关显示。
     */
    fun setEnabledMap(enabledMap: Map<String, Boolean>) {
        ensureEnabledStatesSize()
        for (i in times.indices) {
            enabledStates[i] = enabledMap[times[i]] ?: false
        }
        notifyDataSetChanged()
    }

    class TimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTime: TextView = itemView.findViewById(R.id.tv_time)
        val divider: View = itemView.findViewById(R.id.divider)
        val imgDelete: AppCompatImageView = itemView.findViewById(R.id.imgDelete)
        val switchReminder: SwitchCompat = itemView.findViewById(R.id.st_alarm)
    }

    private fun parseTimeToComponents(time: String): Pair<Int, Int>? {
        return try {
            val parts = time.split(":")
            val hour = parts.getOrNull(0)?.toInt() ?: return null
            val minute = parts.getOrNull(1)?.toInt() ?: return null
            hour to minute
        } catch (_: Exception) {
            null
        }
    }
}