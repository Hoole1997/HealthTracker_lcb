package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.BloodPressureTag
import com.healthtracker.blood.suger.databinding.DialogLabelSelectBinding
import com.healthtracker.blood.suger.databinding.ItemLabelBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 血压标签选择对话框
 */
class BpLabelDialog(
    private val datas: List<BloodPressureTag>?, 
    private val addTags: List<BloodPressureTag>?, 
    private val onSave: ((List<BloodPressureTag>) -> Unit)? = null
) : BaseBottomSheetDialogFragment<DialogLabelSelectBinding>() {

    constructor() : this(datas = null, addTags = null, onSave = null)
    
    private val selectLabels = addTags?.toMutableList() ?: mutableListOf()
    
    companion object {
        /**
         * 显示血压标签选择对话框
         * @param fragmentManager FragmentManager
         * @param healthTags 所有可用标签
         * @param addTags 已选中的标签
         * @param onSave 保存回调
         */
        fun show(
            fragmentManager: FragmentManager, 
            healthTags: List<BloodPressureTag>, 
            addTags: List<BloodPressureTag>?, 
            onSave: (List<BloodPressureTag>) -> Unit
        ) {
            BpLabelDialog(healthTags, addTags, onSave).show(fragmentManager)
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
                datas?.let { tags ->
                    val labels = resources.getStringArray(R.array.blood_pressure_labels)
                    labelBox.removeAllViews()
                    
                    for (tag in tags) {
                        ItemLabelBinding.inflate(LayoutInflater.from(context)).apply {
                            // 设置标签文本：预定义标签使用数组中的文本，自定义标签使用name字段
                            tvLabel.text = if (tag.isPreDefined == 1) {
                                // 预定义标签，从字符串数组获取
                                if (tag.id.toInt() - 1 < labels.size) {
                                    labels[tag.id.toInt() - 1]
                                } else {
                                    tag.name
                                }
                            } else {
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
                                        com.peppa.widget.picker.R.color.white
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