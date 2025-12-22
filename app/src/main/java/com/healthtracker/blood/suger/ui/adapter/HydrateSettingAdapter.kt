package com.healthtracker.blood.suger.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ui.dialog.AlarmTimeSelectDialog
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.ui.weight.RulerView
import com.healthtracker.blood.suger.config.HydrateSettingManager
import com.healthtracker.blood.suger.data.entity.HydrateReminder
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.NumberFormatter

class HydrateSettingAdapter(
    private val onDailyCupsChanged: (Int) -> Unit = { _ -> },
    private val onCupSettingChanged: (Int, Boolean) -> Unit = { _, _ -> },
    private val onCupUnitChanged: (Boolean) -> Unit = { _ -> },
    private val onAddReminderTime: (Int, Int) -> Unit = { _, _ -> },
    private val onDeleteReminderTime: (Int, Int) -> Unit = { _, _ -> },
    private val onToggleReminderEnabled: (Int, Int, Boolean) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isReminderEditMode: Boolean = false
    // 共享的提醒时间列表与内部适配器引用，用于“添加提醒”后刷新列表
    private val reminderTimes = mutableListOf<String>()
    private var timeAdapterRef: HydrateReminderTimeAdapter? = null
    private var enabledMap: Map<String, Boolean> = emptyMap()

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
                inflater.inflate(R.layout.ht_item_hydrate_setting_daily_intake, parent, false)
            )
            TYPE_CUP -> CupViewHolder(
                inflater.inflate(R.layout.ht_item_hydrate_setting_cup_size, parent, false)
            )
            TYPE_REMINDER -> ReminderContainerViewHolder(
                inflater.inflate(R.layout.ht_item_hydrate_setting_reminder_container, parent, false)
            )
            TYPE_ADD_REMINDER -> AddReminderViewHolder(
                inflater.inflate(R.layout.ht_item_hydrate_setting_add_reminder, parent, false)
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
        private val tvSelectUnit: TextView = itemView.findViewById(R.id.tv_select_unit)
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
                    override fun onEndResult(result: Float) {
                        val valueStr = NumberFormatter.formatNumber(
                            result.toDouble(),
                            LanguageUtils.getAppLocale(App.INSTANCE), 1
                        )
                        updateSelectValueText(isMlUnit, valueStr)
                        onSettingChanged(result.toInt(), isMlUnit)
                    }

                    override fun onScrollResult(result: Float) {
                        val valueStr = NumberFormatter.formatNumber(
                            result.toDouble(),
                            LanguageUtils.getAppLocale(App.INSTANCE), 1
                        )
                        updateSelectValueText(isMlUnit, valueStr)
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
            val unit = if (unitIsMl) R.string.ht_ml else R.string.ht_fl_oz
            tvSelectValue.text = valueStr
            tvSelectUnit.text = itemView.context.getString(unit).lowercase()
        }
    }

    inner class ReminderContainerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rcyTimes: RecyclerView = itemView.findViewById(R.id.rcyReminderTimes)
        private val tvEdit: AppCompatTextView = itemView.findViewById(R.id.tv_reminder_edit)
        private val emptyView: View = itemView.findViewById(R.id.emptyReminderView)
        private var timeAdapter: HydrateReminderTimeAdapter? = null

        fun bind() {
            if (rcyTimes.layoutManager == null) {
                rcyTimes.layoutManager = LinearLayoutManager(itemView.context)
            }
            if (timeAdapter == null) {
                timeAdapter = HydrateReminderTimeAdapter(reminderTimes, { h, m ->
                    onDeleteReminderTime(h, m)
                    // 删除后刷新容器以切换空视图
                    notifyItemChanged(items.indexOf(TYPE_REMINDER))
                }, onToggleReminderEnabled)
                rcyTimes.adapter = timeAdapter
                timeAdapterRef = timeAdapter
                // 初始同步开关状态
                timeAdapterRef?.setEnabledMap(enabledMap)
            }

            // 根据编辑模式更新删除图标显示与按钮文案
            timeAdapter?.setDeleteMode(isReminderEditMode)
            tvEdit.setText(if (isReminderEditMode) R.string.ht_cancel else R.string.ht_hydration_reminder_edit)

            // 数据为空时显示空视图，隐藏编辑按钮与列表
            if (reminderTimes.isEmpty()) {
                emptyView.visibility = View.VISIBLE
                rcyTimes.visibility = View.GONE
                tvEdit.visibility = View.GONE
            } else {
                emptyView.visibility = View.GONE
                rcyTimes.visibility = View.VISIBLE
                tvEdit.visibility = View.VISIBLE
            }

            tvEdit.setOnClickListener {
                isReminderEditMode = !isReminderEditMode
                timeAdapter?.setDeleteMode(isReminderEditMode)
                tvEdit.setText(if (isReminderEditMode) R.string.ht_cancel else R.string.ht_hydration_reminder_edit)
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
                    // 防重复：若已存在相同时间，则提示并不添加
                    if (reminderTimes.contains(timeString)) {
                        Toast.makeText(activity, itemView.context.getString(R.string.ht_hydrate_reminder_exist), Toast.LENGTH_SHORT).show()
                        return@show
                    }
                    // 持久化新增后立即更新UI（先乐观更新，再由Flow刷新）
                    onAddReminderTime(pair.first, pair.second)
                    timeAdapterRef?.addTime(timeString)
                    // 切换空视图展示
                    notifyItemChanged(items.indexOf(TYPE_REMINDER))
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

    /**
     * 外部（例如 ViewModel 观察到数据库变更）推送新的提醒时间列表时调用。
     * 会替换内部数据并刷新列表显示。
     */
    fun setReminderTimes(times: List<String>) {
        reminderTimes.clear()
        reminderTimes.addAll(times)
        timeAdapterRef?.replaceAll(times)
        // 同步当前已知的启用状态映射
        timeAdapterRef?.setEnabledMap(enabledMap)
        // 让容器重新绑定以切换空视图/编辑按钮显示
        notifyItemChanged(items.indexOf(TYPE_REMINDER))
    }

    /**
     * 外部提供 HydrateReminder 列表（包含 enabled），用于同步时间项的开关显示。
     */
    fun setReminderStates(reminders: List<HydrateReminder>) {
        enabledMap = reminders.associate { reminder ->
            DateTimeUtils.formatTimeComponents(reminder.hour, reminder.minute) to reminder.enabled
        }
        timeAdapterRef?.setEnabledMap(enabledMap)
    }
}