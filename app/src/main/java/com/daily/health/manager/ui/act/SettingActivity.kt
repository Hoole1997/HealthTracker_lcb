package com.daily.health.manager.ui.act

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdInspectorError
import com.google.android.libraries.ads.mobile.sdk.common.OnAdInspectorClosedListener
import com.daily.health.manager.BuildConfig
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtActivitySettingBinding
import com.daily.health.manager.databinding.HtItemSettingBinding
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.startActivity

class SettingActivity : BaseMVVMActivity<BaseViewModel, HtActivitySettingBinding>() {

    companion object {
        private const val KEY_IS_LANGUAGE_CHANGED = "key_is_language_changed"
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, SettingActivity::class.java))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_LANGUAGE_CHANGED, isLanguageChanged)
    }

    private var isLanguageChanged = false

    /**
     * 语言选择页面启动器
     */
    private val languageSelectLauncher: ActivityResultLauncher<Intent> = 
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // 语言已变更，标记状态并重启设置页面以应用新语言
                isLanguageChanged = true
                recreate()
            }
        }

    override fun createViewBinding() = HtActivitySettingBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java


    override fun initView(savedInstanceState: Bundle?) {
        isLanguageChanged = savedInstanceState?.getBoolean(KEY_IS_LANGUAGE_CHANGED, false)
            ?: intent.getBooleanExtra(KEY_IS_LANGUAGE_CHANGED, false)
        with(mViewBind) {
            // 设置返回按钮
            btnBack.click {
                finish()
            }

            // 初始化设置列表
            rvSetting.layoutManager = LinearLayoutManager(this@SettingActivity)
            rvSetting.addItemDecoration(SettingItemDecoration(resources.getDimensionPixelSize(com.healthtracker.framework.R.dimen.dp_12)))
            rvSetting.adapter = SettingAdapter(getSettingItems()) { item ->
                handleSettingItemClick(item)
            }
        }
    }


    override fun finish() {
        if (isLanguageChanged) {
            setResult(RESULT_OK)
        }
        super.finish()
    }

    /**
     * 获取设置项列表数据
     */
    private fun getSettingItems(): List<SettingItem> {
        return listOf(
//            SettingItem(
//                icon = R.drawable.ht_ic_setting_alarm,
//                title = R.string.ht_alarm_management,
//                type = SettingType.ALARM_MANAGEMENT
//            ),
//            SettingItem(
//                icon = R.drawable.ht_ic_setting_unit,
//                title = R.string.ht_unit_settings,
//                type = SettingType.UNIT_SETTINGS
//            ),
//            SettingItem(
//                icon = R.drawable.ht_ic_setting_target,
//                title = R.string.ht_target_range_settings,
//                type = SettingType.TARGET_RANGE
//            ),
//            SettingItem(
//                icon = R.drawable.ht_ic_setting_profile,
//                title = R.string.ht_personal_info,
//                type = SettingType.PERSONAL_INFO
//            ),
            SettingItem(
                icon = R.drawable.ht_ic_setting_language,
                title = R.string.ht_language,
                type = SettingType.LANGUAGE
            ),
            SettingItem(
                icon = R.drawable.ht_ic_setting_feedback,
                title = R.string.ht_feedback,
                type = SettingType.FEEDBACK
            ),
//            SettingItem(
//                icon = R.drawable.ht_ic_setting_disclaimers,
//                title = R.string.ht_disclaimers,
//                type = SettingType.DISCLAIMERS
//            ),
            SettingItem(
                icon = R.drawable.ht_ic_setting_privacy,
                title = R.string.ht_privacy_policy,
                type = SettingType.PRIVACY_POLICY
            ),
            SettingItem(
                icon = R.drawable.ht_ic_setting_terms,
                title = R.string.ht_terms_of_service,
                type = SettingType.TERMS_OF_SERVICE
            )
        )
    }

    /**
     * 处理设置项点击事件
     */
    private fun handleSettingItemClick(item: SettingItem) {
        when (item.type) {
            SettingType.LANGUAGE -> {
                // 使用 launcher 启动语言选择页面，以便接收结果
                languageSelectLauncher.launch(Intent(this, LanguageActivity::class.java).apply {
                    putExtra(LanguageActivity.KEY_APPLY_CHANGE, true)
                })
            }
            SettingType.ALARM_MANAGEMENT -> {
                startActivity<AlarmManageActivity>()
            }
            SettingType.UNIT_SETTINGS -> {
                // TODO: 单位设置页面
            }
            SettingType.TARGET_RANGE -> {
                // TODO: 目标范围设置页面
            }
            SettingType.PERSONAL_INFO -> {
                startActivity(ProfileActivity.creteEditIntent(this))
            }
            SettingType.FEEDBACK -> {
                startActivity<FeedbackActivity>()
            }
            SettingType.DISCLAIMERS -> {
                // TODO: 免责声明页面
            }
            SettingType.PRIVACY_POLICY -> {
                InnerWebActivity.start(this@SettingActivity, BuildConfig.PRIVACY_POLICY)
            }
            SettingType.TERMS_OF_SERVICE -> {
                MobileAds.openAdInspector { }
            }
        }
    }

    /**
     * 设置项数据类
     */
    data class SettingItem(
        @param:DrawableRes val icon: Int,
        @param:StringRes val title: Int,
        val type: SettingType
    )

    /**
     * 设置项类型枚举
     */
    enum class SettingType {
        ALARM_MANAGEMENT,
        UNIT_SETTINGS,
        TARGET_RANGE,
        PERSONAL_INFO,
        LANGUAGE,
        FEEDBACK,
        DISCLAIMERS,
        PRIVACY_POLICY,
        TERMS_OF_SERVICE
    }

    /**
     * 设置列表适配器
     */
    private inner class SettingAdapter(
        private val items: List<SettingItem>,
        private val onItemClick: (SettingItem) -> Unit
    ) : RecyclerView.Adapter<SettingAdapter.ViewHolder>() {

        inner class ViewHolder(private val binding: HtItemSettingBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(item: SettingItem) {
                binding.apply {
                    ivIcon.setImageResource(item.icon)
                    tvAction.setText(item.title)
                    root.clickWithDuration {
                        onItemClick(item)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = HtItemSettingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

    }

    /**
     * 设置项间距装饰器
     */
    private class SettingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            // 不是第一个 item 时添加顶部间距
            if (parent.getChildAdapterPosition(view) != 0) {
                outRect.top = spacing
            }
        }
    }

    override fun getStatusBarColor() = R.color.color_e2ffea


}