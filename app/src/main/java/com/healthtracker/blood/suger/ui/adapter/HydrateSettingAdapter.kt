package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.dialog.AlarmTimeSelectDialog
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.ui.weight.RulerView

class HydrateSettingAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isReminderEditMode: Boolean = false
    // 共享的提醒时间列表与内部适配器引用，用于“添加提醒”后刷新列表
    private val reminderTimes = mutableListOf("08:00", "09:00", "10:00")
    private var timeAdapterRef: HydrateReminderTimeAdapter? = null

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
            // 默认显示 8 杯水
            tvCupsOfDay.text = "8"

            imgIntakeMore.setOnClickListener {
                val cur = tvCupsOfDay.text.toString().toIntOrNull() ?: 8
                tvCupsOfDay.text = (cur + 1).toString()
            }
            imgIntakeLess.setOnClickListener {
                val cur = tvCupsOfDay.text.toString().toIntOrNull() ?: 8
                val next = (cur - 1).coerceAtLeast(1)
                tvCupsOfDay.text = next.toString()
            }
        }
    }

    class CupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rgUnit: RadioGroup = itemView.findViewById(R.id.rg_unit)
        private val tvSelectValue: TextView = itemView.findViewById(R.id.tv_select_value)
        private val rulerView: RulerView = itemView.findViewById(R.id.rulerView)
        private var isMlUnit: Boolean = true

        fun bind() {
            // 配置标尺：整数精度，范围 10 到 10000，步长为 1，显示整数
            rulerView.apply {
                setRulerUnit(RulerView.RulerUnit.INTEGER_PRECISION)
                setScaleStep(1f)
                setDecimalPlaces(0)
                setScaleRange(10f, 10000f)
                setScrollableRange(10f, 10000f)
                setScaleImmediately(20f, suppressCallback = true)
                setOnChooseResultListener(object : RulerView.OnChooseResultListener {
                    override fun onEndResult(result: String) {
                        updateSelectValueText(isMlUnit, result)
                    }

                    override fun onScrollResult(result: String) {
                        updateSelectValueText(isMlUnit, result)
                    }
                })
            }

            // 默认单位与显示：ML
            rgUnit.check(R.id.rb_ml)
            updateSelectValueText(unitIsMl = true, valueStr = "20")

            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                isMlUnit = checkedId == R.id.rb_ml
                val currentValue = rulerView.getCurrentScale().toInt().toString()
                updateSelectValueText(unitIsMl = isMlUnit, valueStr = currentValue)
            }
        }

        private fun updateSelectValueText(unitIsMl: Boolean, valueStr: String) {
            val unit = if (unitIsMl) R.string.ml else R.string.fl_oz
            tvSelectValue.text = valueStr + " " + itemView.context.getString(unit)
        }
    }

    inner class ReminderContainerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rcyTimes: RecyclerView = itemView.findViewById(R.id.rcyReminderTimes)
        private val tvEdit: AppCompatTextView = itemView.findViewById(R.id.tv_reminder_edit)
        private var timeAdapter: HydrateReminderTimeAdapter? = null

        fun bind() {
            if (rcyTimes.layoutManager == null) {
                rcyTimes.layoutManager = LinearLayoutManager(itemView.context)
            }
            if (timeAdapter == null) {
                timeAdapter = HydrateReminderTimeAdapter(reminderTimes)
                rcyTimes.adapter = timeAdapter
                timeAdapterRef = timeAdapter
            }

            // 根据编辑模式更新删除图标显示与按钮文案
            timeAdapter?.setDeleteMode(isReminderEditMode)
            tvEdit.setText(if (isReminderEditMode) R.string.cancel else R.string.hydration_reminder_edit)

            tvEdit.setOnClickListener {
                isReminderEditMode = !isReminderEditMode
                timeAdapter?.setDeleteMode(isReminderEditMode)
                tvEdit.setText(if (isReminderEditMode) R.string.cancel else R.string.hydration_reminder_edit)
            }
        }
    }

    inner class AddReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val btnAdd: View = itemView.findViewById(R.id.btnAddReminder)
            btnAdd.setOnClickListener {
                val activity = itemView.context as? FragmentActivity ?: return@setOnClickListener
                // 底部弹出时间选择器，默认当前时间
                AlarmTimeSelectDialog.show(activity.supportFragmentManager, null) { pair ->
                    val timeString = DateTimeUtils.formatTimeComponents(pair.first, pair.second)
                    // 通过适配器方法新增并刷新，避免与内部状态不同步
                    timeAdapterRef?.addTime(timeString)
                }
            }
        }
    }

    companion object {
        private const val TYPE_DAILY = 1
        private const val TYPE_CUP = 2
        private const val TYPE_REMINDER = 3
        private const val TYPE_ADD_REMINDER = 4
    }
}