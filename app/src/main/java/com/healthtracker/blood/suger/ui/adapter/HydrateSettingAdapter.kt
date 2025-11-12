package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R

class HydrateSettingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = listOf(
        TYPE_DAILY,
        TYPE_CUP,
        TYPE_REMINDER,
        TYPE_ADD_REMINDER
    )

    override fun getItemViewType(position: Int): Int = items[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DAILY -> DailyViewHolder(
                inflater.inflate(R.layout.item_hydrate_setting_daily_intake, parent, false)
            )
            TYPE_CUP -> CupViewHolder(
                inflater.inflate(R.layout.item_hydrate_setting_cup_size, parent, false)
            )
            TYPE_REMINDER -> ReminderContainerViewHolder(
                inflater.inflate(R.layout.item_hydrate_setting_reminder_container, parent, false)
            )
            TYPE_ADD_REMINDER -> AddReminderViewHolder(
                inflater.inflate(R.layout.item_hydrate_setting_add_reminder, parent, false)
            )
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DailyViewHolder -> holder.bind()
            is CupViewHolder -> holder.bind()
            is ReminderContainerViewHolder -> holder.bind()
            is AddReminderViewHolder -> holder.bind()
        }
    }

    class DailyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgIntakeLess: AppCompatImageView = itemView.findViewById(R.id.intakeLess)
        private val imgIntakeMore: AppCompatImageView = itemView.findViewById(R.id.intakeMore)
        private val tvCupsOfDay: AppCompatTextView = itemView.findViewById(R.id.tvCupsOfDay)
        fun bind() {
            // 仅 UI 展示，默认值
        }
    }

    class CupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rgUnit: RadioGroup = itemView.findViewById(R.id.rg_unit)
        private val tvSelectValue: TextView = itemView.findViewById(R.id.tv_select_value)
        fun bind() {
            // 默认显示 20 ml，仅 UI 文本更新
            updateSelectValueText(unitIsMl = true)

            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                val isMl = checkedId == R.id.rb_ml
                updateSelectValueText(unitIsMl = isMl)
            }
        }

        private fun updateSelectValueText(unitIsMl: Boolean) {
            val unit = if (unitIsMl) R.string.ml else R.string.fl_oz
            // 展示固定数值 20，避免业务逻辑和换算，仅更新 UI 文本
            tvSelectValue.text = "20 " + itemView.context.getString(unit)
        }
    }

    class ReminderContainerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rcyTimes: RecyclerView = itemView.findViewById(R.id.rcyReminderTimes)
        fun bind() {
            rcyTimes.layoutManager = LinearLayoutManager(itemView.context)
            rcyTimes.adapter = HydrateReminderTimeAdapter(listOf("08:00", "09:00", "10:00"))
        }
    }

    class AddReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() { /* 仅展示，无交互 */ }
    }

    companion object {
        private const val TYPE_DAILY = 1
        private const val TYPE_CUP = 2
        private const val TYPE_REMINDER = 3
        private const val TYPE_ADD_REMINDER = 4
    }
}