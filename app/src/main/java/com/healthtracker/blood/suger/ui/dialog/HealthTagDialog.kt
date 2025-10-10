package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.databinding.DialogLabelSelectBinding
import com.healthtracker.blood.suger.ui.adapter.HealthTagAdapter
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration

/**
 * 统一的健康标签选择对话框
 * 支持血糖和血压标签的选择
 * 使用RecyclerView + FlexboxLayoutManager实现高性能的标签布局
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
    private lateinit var tagAdapter: HealthTagAdapter
    
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
        initRecyclerView()
        initClickListeners()
    }



    /**
     * 初始化RecyclerView和Adapter
     */
    private fun initRecyclerView() {
        mViewBind?.run {
            // 设置FlexboxLayoutManager
            val layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexDirection = FlexDirection.ROW
                flexWrap = FlexWrap.WRAP
                justifyContent = JustifyContent.FLEX_START
            }
            
            // 初始化Adapter
            tagAdapter = HealthTagAdapter(
                tagType = tagType,
                onTagClick = { tag ->
                    handleTagSelection(tag)
                }
            )
            
            // 设置RecyclerView
            labelBox.apply {
                this.layoutManager = layoutManager
                adapter = tagAdapter
                // 禁用嵌套滚动以避免与BottomSheet冲突
                isNestedScrollingEnabled = false
            }
            
            // 初始化标签数据
            updateTagsData()
        }
    }

    /**
     * 更新标签数据
     */
    private fun updateTagsData() {
        availableTags?.let { tags ->
            // 根据标签类型获取对应的字符串数组
            val labelsArray = when (tagType) {
                TagType.BLOOD_SUGAR -> resources.getStringArray(R.array.blood_sugar_labels)
                TagType.BLOOD_PRESSURE -> resources.getStringArray(R.array.blood_pressure_labels)
            }
            
            tagAdapter.updateTags(tags, selectLabels, labelsArray)
        }
    }

    /**
     * 处理标签选择逻辑
     */
    private fun handleTagSelection(tag: HealthTag) {
        val index = selectLabels.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            // 取消选择
            selectLabels.removeAt(index)
        } else {
            // 添加选择
            selectLabels.add(tag)
        }
        
        // 更新Adapter数据
        updateTagsData()
    }

    /**
     * 初始化点击事件监听器
     */
    private fun initClickListeners() {
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
            btnSave.clickWithDuration {
                onSave?.invoke(selectLabels)
                dismissAllowingStateLoss()
            }
        }
    }
}