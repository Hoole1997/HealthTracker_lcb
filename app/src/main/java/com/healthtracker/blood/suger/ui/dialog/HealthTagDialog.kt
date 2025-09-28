package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.databinding.DialogLabelSelectBinding
import com.healthtracker.blood.suger.databinding.ItemLabelBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 统一的健康标签选择对话框
 * 支持血糖和血压标签的选择
 */
class HealthTagDialog(
    private val tagType: TagType,
    private val availableTags: List<HealthTag>?,
    private val selectedTags: List<HealthTag>?,
    private val onSave: ((List<HealthTag>) -> Unit)? = null
) : BaseBottomSheetDialogFragment<DialogLabelSelectBinding>() {

    constructor() : this(
        tagType = TagType.BLOOD_SUGAR,
        availableTags = null,
        selectedTags = null,
        onSave = null
    )
    
    private val selectLabels = selectedTags?.toMutableList() ?: mutableListOf()
    
    companion object {
        /**
         * 显示血糖标签选择对话框
         * @param fragmentManager FragmentManager
         * @param availableTags 所有可用标签
         * @param selectedTags 已选中的标签
         * @param onSave 保存回调
         */
        fun showBloodSugarDialog(
            fragmentManager: FragmentManager,
            availableTags: List<HealthTag>,
            selectedTags: List<HealthTag>?,
            onSave: (List<HealthTag>) -> Unit
        ) {
            HealthTagDialog(
                TagType.BLOOD_SUGAR,
                availableTags,
                selectedTags,
                onSave
            ).show(fragmentManager)
        }
        
        /**
         * 显示血压标签选择对话框
         * @param fragmentManager FragmentManager
         * @param availableTags 所有可用标签
         * @param selectedTags 已选中的标签
         * @param onSave 保存回调
         */
        fun showBloodPressureDialog(
            fragmentManager: FragmentManager,
            availableTags: List<HealthTag>,
            selectedTags: List<HealthTag>?,
            onSave: (List<HealthTag>) -> Unit
        ) {
            HealthTagDialog(
                TagType.BLOOD_PRESSURE,
                availableTags,
                selectedTags,
                onSave
            ).show(fragmentManager)
        }
    }
    
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogLabelSelectBinding.inflate(layoutInflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            // 添加标签按钮（暂时不实现）
            ivAdd.clickWithDuration {
                // TODO: 实现添加自定义标签功能
            }

            // 删除标签按钮（暂时不实现）
            ivDelete.clickWithDuration {
                // TODO: 实现删除自定义标签功能
            }

            // 取消按钮
            btnCancel.click {
                dismissAllowingStateLoss()
            }

            // 保存按钮
            btnSave.click {
                onSave?.invoke(selectLabels)
                dismissAllowingStateLoss()
            }
        }

        setupLabelFlex()
    }

    /**
     * 设置标签选择界面
     */
    private fun setupLabelFlex() {
        try {
            mViewBind?.run {
                availableTags?.let { tags ->
                    // 根据标签类型获取对应的字符串数组
                    val labelsArray = when (tagType) {
                        TagType.BLOOD_SUGAR -> resources.getStringArray(R.array.blood_sugar_labels)
                        TagType.BLOOD_PRESSURE -> resources.getStringArray(R.array.blood_pressure_labels)
                    }
                    
                    labelBox.removeAllViews()
                    
                    for (tag in tags) {
                        ItemLabelBinding.inflate(LayoutInflater.from(context)).apply {
                            // 设置标签文本
                            tvLabel.text = if (tag.isPredefined) {
                                // 预定义标签，从字符串数组获取
                                tag.predefinedIndex?.let { index ->
                                    if (index < labelsArray.size) {
                                        labelsArray[index]
                                    } else {
                                        tag.name
                                    }
                                } ?: tag.name
                            } else {
                                // 自定义标签，直接使用name字段
                                tag.name
                            }
                            
                            labelBox.addView(root)
                            labelBox.flexWrap
                            
                            // 设置点击事件
                            root.click {
                                if (selectLabels.contains(tag)) {
                                    selectLabels.remove(tag)
                                } else {
                                    selectLabels.add(tag)
                                }
                                setupLabelFlex()
                            }

                            // 设置选中状态样式
                            if (selectLabels.contains(tag)) {
                                tvLabel.setTextColor(
                                    ContextCompat.getColor(
                                        tvLabel.context,
                                        com.healthtracker.framework.R.color.white
                                    )
                                )
                                labelItem.background = ContextCompat.getDrawable(
                                    labelItem.context,
                                    R.drawable.bg_label_select_selected
                                )
                            } else {
                                tvLabel.setTextColor(
                                    ContextCompat.getColor(
                                        tvLabel.context,
                                        R.color.c5
                                    )
                                )
                                labelItem.background = ContextCompat.getDrawable(
                                    labelItem.context,
                                    R.drawable.bg_label_select_normal
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            dismissAllowingStateLoss()
        }
    }
}