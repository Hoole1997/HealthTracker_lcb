package com.healthtracker.blood.suger.ui.act

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.config.models.PushConfig
import com.healthtracker.blood.suger.databinding.ActivityLanguageSelectBinding
import com.healthtracker.blood.suger.databinding.ItemAppLanguageBinding
import com.healthtracker.blood.suger.ui.weight.WrapLayoutLinearLayoutManager
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.config.core.RemoteConfigManager
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.LanguageUtils.getLanguageList
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoMedium
import net.corekit.monetize.ads.config.AdConfigManager
import net.corekit.monetize.ui.NativeAdStyle
import org.koin.android.ext.android.inject


class LanguageActivity: BaseMVVMActivity<BaseViewModel, ActivityLanguageSelectBinding>() {


    override fun createViewBinding() = ActivityLanguageSelectBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    private val remoteConfigManager: RemoteConfigManager by inject()

    private var applyChange = false
    private var languageAdapter: LanguageAdapter? = null
    
    override fun initView(savedInstanceState: Bundle?) {
        applyChange = intent?.getBooleanExtra(KEY_APPLY_CHANGE, false) ?: false
        
        val languageList = getLanguageList(this@LanguageActivity)
        val savedSelectIndex = savedInstanceState?.getInt(KEY_SELECT_INDEX, -1) ?: -1
        
        with(mViewBind){
            rvLanguage.layoutManager = WrapLayoutLinearLayoutManager(this@LanguageActivity)
            rvLanguage.adapter = LanguageAdapter(languageList, savedSelectIndex).also { 
                languageAdapter = it 
            }
            onSelectChanged()

            if (applyChange) {
                btnBack.isVisible = true
                btnBack.click {
                    finish()
                }
            }else{
                reportGuide(1)
            }
            tvConfirm.clickWithDuration {
                onChoiceLangDone()
            }
            loadNative(adContainer, NativeAdStyle.CARD_7)
        }
    }


    private fun onSelectChanged() {
        val selectedLang = languageAdapter?.getSelectedLang()?.id ?: return
        val currentLang = LanguageUtils.getAppLanguage(this)
        val isFirstSelection = LanguageUtils.getSavedLanguage().isEmpty()
        
        mViewBind.tvConfirm.isEnabled = selectedLang != currentLang || isFirstSelection
    }

    private fun onChoiceLangDone() {
        languageAdapter?.let {
            LanguageUtils.setAppLanguage(it.getSelectedLang().id)
            // 语言改变后，清除 PushConfig 缓存，以便下次获取时使用新语言重新解析
            remoteConfigManager.clearCache<PushConfig>()
        }
        if (applyChange) {
            // 通知设置页面需要重建以应用语言变更
            setResult(RESULT_OK)
        } else {
            val targetPage = if(AdConfigManager.showNewGuide()) GuideActivity::class.java else MainActivity::class.java
            this.startActivity(Intent(this, targetPage).apply {
                putExtras(intent)
            })

        }
        finish()
    }


    private inner class LanguageAdapter(
        private val list: List<LanguageUtils.LangBean>,
        savedSelectIndex: Int = -1
    ) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {
        
        var selectIndex: Int = if (savedSelectIndex >= 0 && savedSelectIndex < list.size) {
            savedSelectIndex
        } else {
            // 查找当前语言在列表中的位置
            val currentLang = LanguageUtils.getAppLanguage(this@LanguageActivity)
            list.indexOfFirst { it.id == currentLang }.takeIf { it >= 0 } ?: 0
        }
            private set

        fun getSelectedLang() = list[selectIndex]
        
        fun updateSelectIndex(newIndex: Int) {
            val oldIndex = selectIndex
            selectIndex = newIndex
            notifyItemChanged(oldIndex)
            notifyItemChanged(selectIndex)
            onSelectChanged() // Notify activity about selection change
        }

        inner class LanguageViewHolder(private val itemBinding: ItemAppLanguageBinding) : RecyclerView.ViewHolder(itemBinding.root) {

            fun bind(position: Int) {
                itemBinding.apply {
                    val isSelected = selectIndex == position
                    
                    if (isSelected) {
                        ivSelect.visible()
                        itemBinding.root.background = ContextCompat.getDrawable(
                            this@LanguageActivity,
                            R.drawable.bg_rect_language_selected
                        )
                        tvLang.setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.c5))
                        tvLang.typeface = getRobotoBold(this@LanguageActivity)
                    } else {
                        ivSelect.gone()
                        itemBinding.root.background = ContextCompat.getDrawable(
                            this@LanguageActivity,
                            R.drawable.bg_rect_white_8
                        )
                        tvLang.setTextColor(ContextCompat.getColor(this@LanguageActivity, R.color.t1))
                        tvLang.typeface = getRobotoMedium(this@LanguageActivity)
                    }
                    tvLang.text = list[position].displayName
                    root.isEnabled = !isSelected
                    root.click {
                        if (selectIndex == position) {
                            return@click
                        }
                        updateSelectIndex(position)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val itemBinding = ItemAppLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return LanguageViewHolder(itemBinding)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            holder.bind(position)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        languageAdapter?.let {
            outState.putInt(KEY_SELECT_INDEX, it.selectIndex)
        }
    }

    override fun shouldDisableBackPressed() = true
    
    companion object {
        private const val KEY_SELECT_INDEX = "select_index"
        private const val TAG = "LanguageActivity"

        const val KEY_APPLY_CHANGE = "apply_change"
    }
}