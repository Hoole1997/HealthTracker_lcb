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
import com.healthtracker.blood.suger.config.HydrateSettingManager

class HydrateSettingAdapter(
    private val onDailyCupsChanged: (Int) -> Unit = { _ -> },
    private val onCupSettingChanged: (Int, Boolean) -> Unit = { _, _ -> },
    private val onCupUnitChanged: (Boolean) -> Unit = { _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
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
            is DailyViewHolder -> holder.bind(onChanged = onDailyCupsChanged)
            is CupViewHolder -> holder.bind(onSettingChanged = onCupSettingChanged, onUnitChanged = onCupUnitChanged)
            is ReminderContainerViewHolder -> holder.bind()
            is AddReminderViewHolder -> holder.bind()
        }
    }

    class DailyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgIntakeLess: AppCompatImageView = itemView.findViewById(R.id.intakeLess)
        private val imgIntakeMore: AppCompatImageView = itemView.findViewById(R.id.intakeMore)
        private val tvCupsOfDay: AppCompatTextView = itemView.findViewById(R.id.tvCupsOfDay)
        fun bind(onChanged: (Int) -> Unit = {}) {
            // 读取持久化的每日杯数，默认 8
            val saved = HydrateSettingManager.getDailyCups()
            tvCupsOfDay.text = saved.toString()

            imgIntakeMore.setOnClickListener {
                val cur = tvCupsOfDay.text.toString().toIntOrNull() ?: saved
                val next = cur + 1
                tvCupsOfDay.text = next.toString()
                onChanged(next)
            }
            imgIntakeLess.setOnClickListener {
                val cur = tvCupsOfDay.text.toString().toIntOrNull() ?: saved
                val next = (cur - 1).coerceAtLeast(1)
                tvCupsOfDay.text = next.toString()
                onChanged(next)
            }
        }
    }

    class CupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rgUnit: RadioGroup = itemView.findViewById(R.id.rg_unit)
        private val tvSelectValue: TextView = itemView.findViewById(R.id.tv_select_value)
        private val rulerView: RulerView = itemView.findViewById(R.id.rulerView)
        private var isMlUnit: Boolean = true

        fun bind(
            onSettingChanged: (Int, Boolean) -> Unit = { _, _ -> },
            onUnitChanged: (Boolean) -> Unit = { _ -> }
        ) {
            // 先读取单位偏好，配置标尺范围再设置初始值
            isMlUnit = HydrateSettingManager.getCupUnit() == HydrateSettingManager.CupUnit.ML
            rgUnit.check(if (isMlUnit) R.id.rb_ml else R.id.rb_floz)

            rulerView.apply {
                setRulerUnit(RulerView.RulerUnit.INTEGER_PRECISION)
                setScaleStep(1f)
                setDecimalPlaces(0)
                val min = if (isMlUnit) 10f else 1f
                val max = if (isMlUnit) 10000f else HydrateSettingManager.fromMl(10000, HydrateSettingManager.CupUnit.FL_OZ).toFloat()
                setScaleRange(min, max)
                setScrollableRange(min, max)
                // 读取持久化的杯子容积（用于展示的数值）并设置到标尺
                val volDisplay = HydrateSettingManager.getCupDisplayVolume()
                val clamped = volDisplay.coerceIn(min.toInt(), max.toInt()).toFloat()
                val action = {
                    setScaleImmediately(clamped, suppressCallback = true)
                    updateSelectValueText(unitIsMl = isMlUnit, valueStr = volDisplay.toString())
                }
                if (width == 0 || height == 0) {
                    post(action)
                } else {
                    action()
                }
                setOnChooseResultListener(object : RulerView.OnChooseResultListener {
                    override fun onEndResult(result: String) {
                        updateSelectValueText(isMlUnit, result)
                        result.toIntOrNull()?.let { onSettingChanged(it, isMlUnit) }
                    }

                    override fun onScrollResult(result: String) {
                        updateSelectValueText(isMlUnit, result)
                    }
                })
            }

            updateSelectValueText(unitIsMl = isMlUnit, valueStr = HydrateSettingManager.getCupDisplayVolume().toString())

            rgUnit.setOnCheckedChangeListener { _, checkedId ->
                isMlUnit = checkedId == R.id.rb_ml
                val unit = if (isMlUnit) HydrateSettingManager.CupUnit.ML else HydrateSettingManager.CupUnit.FL_OZ
                val min = if (isMlUnit) 10f else 1f
                val max = if (isMlUnit) 10000f else HydrateSettingManager.fromMl(10000, HydrateSettingManager.CupUnit.FL_OZ).toFloat()
                rulerView.setScaleRange(min, max)
                rulerView.setScrollableRange(min, max)
                // 基于存储的 ml 值，计算新单位下的展示值，避免链式取整导致的漂移
                val ml = HydrateSettingManager.getCupVolume()
                val newDisplayValue = HydrateSettingManager.fromMl(ml, unit).coerceIn(min.toInt(), max.toInt())
                // 更新标尺与文案（不触发选择回调）
                rulerView.setScaleImmediately(newDisplayValue.toFloat(), suppressCallback = true)
                updateSelectValueText(unitIsMl = isMlUnit, valueStr = newDisplayValue.toString())
                // 仅持久化单位偏好，不改动存储的 ml
                onUnitChanged(isMlUnit)
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