package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.TagType
import com.healthtracker.blood.suger.databinding.DialogLabelSelectBinding
import com.healthtracker.blood.suger.ui.adapter.HealthTagAdapter
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog.Companion.BUTTON_OK
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 统一的健康标签选择对话框
 * 支持血糖和血压标签的选择
 * 使用RecyclerView + FlexboxLayoutManager实现高性能的标签布局
 */
class HealthTagDialog(
    private val tagType: TagType,
    private val tagsFlow: Flow<List<HealthTag>>?,
    private val selectedTags: List<HealthTag>?,
    private val onSave: ((List<HealthTag>) -> Unit)? = null,
    private val onDelete: ((HealthTag) -> Unit)? = null,
    private val onAdd: ((String) -> Unit)? = null
) : BaseBottomSheetDialogFragment<DialogLabelSelectBinding>() {

    constructor() : this(
        tagType = TagType.BLOOD_SUGAR,
        tagsFlow = null,
        selectedTags = null,
        onSave = null,
        onDelete = null
    )

    private val selectLabels = selectedTags?.toMutableList() ?: mutableListOf()
    private lateinit var tagAdapter: HealthTagAdapter

    private var pendingNewTagName: String? = null
    private var isDeleteMode = false

    companion object {
        /**
         * 显示血糖标签选择对话框（响应式）
         * @param fragmentManager FragmentManager
         * @param tagsFlow 标签Flow（单一数据源）
         * @param selectedTags 已选中的标签
         * @param onSave 保存回调
         * @param onDelete 删除回调（软删除）
         */
        fun showBloodSugarDialog(
            fragmentManager: FragmentManager,
            tagsFlow: Flow<List<HealthTag>>,
            selectedTags: List<HealthTag>?,
            onSave: (List<HealthTag>) -> Unit,
            onDelete: (HealthTag) -> Unit,
            onAdd: (String) -> Unit
        ) {
            HealthTagDialog(
                TagType.BLOOD_SUGAR,
                tagsFlow,
                selectedTags,
                onSave,
                onDelete,
                onAdd
            ).show(fragmentManager)
        }

        /**
         * 显示血压标签选择对话框（响应式）
         * @param fragmentManager FragmentManager
         * @param tagsFlow 标签Flow（单一数据源）
         * @param selectedTags 已选中的标签
         * @param onSave 保存回调
         * @param onDelete 删除回调（软删除）
         */
        fun showBloodPressureDialog(
            fragmentManager: FragmentManager,
            tagsFlow: Flow<List<HealthTag>>,
            selectedTags: List<HealthTag>?,
            onSave: (List<HealthTag>) -> Unit,
            onDelete: (HealthTag) -> Unit,
            onAdd: (String) -> Unit
        ) {
            HealthTagDialog(
                TagType.BLOOD_PRESSURE,
                tagsFlow,
                selectedTags,
                onSave,
                onDelete,
                onAdd
            ).show(fragmentManager)
        }

        /**
         * 显示心率标签选择对话框
         */
        fun showHeartRateDialog(
            fragmentManager: FragmentManager,
            tagsFlow: Flow<List<HealthTag>>,
            selectedTags: List<HealthTag>?,
            onSave: (List<HealthTag>) -> Unit,
            onDelete: (HealthTag) -> Unit,
            onAdd: (String) -> Unit
        ) {
            HealthTagDialog(
                TagType.HEART_RATE,
                tagsFlow,
                selectedTags,
                onSave,
                onDelete,
                onAdd
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

            // 初始化标签数据（订阅Flow）
            updateTagsData()
        }
    }

    /**
     * 响应式更新标签数据
     */
    private fun updateTagsData() {
        val labelsArray = when (tagType) {
            TagType.BLOOD_SUGAR -> resources.getStringArray(R.array.blood_sugar_labels)
            TagType.BLOOD_PRESSURE -> resources.getStringArray(R.array.blood_pressure_labels)
            TagType.BMI -> {
                val resId = resources.getIdentifier("bmi_labels", "array", requireContext().packageName)
                if (resId != 0) resources.getStringArray(resId) else emptyArray()
            }
            TagType.HEART_RATE -> {
                val resId = resources.getIdentifier("heart_rate_labels", "array", requireContext().packageName)
                if (resId != 0) resources.getStringArray(resId) else emptyArray()
            }
        }

        tagsFlow?.let { flow ->
            viewLifecycleOwner.lifecycleScope.launch {
                flow.collect { tags ->
                    pendingNewTagName?.let { name ->
                        val newlyCreated = tags.find { it.name == name }
                        if (newlyCreated != null && selectLabels.none { it.id == newlyCreated.id }) {
                            selectLabels.add(newlyCreated)
                        }
                        pendingNewTagName = null
                    }
                    tagAdapter.updateTags(tags, selectLabels, labelsArray)
                }
            }
        }
    }

    /**
     * 处理标签选择或删除逻辑
     */
    private fun handleTagSelection(tag: HealthTag) {
        if (isDeleteMode) {
            ConfirmDialog(
                getString(R.string.confirm_delete_title),
                getString(R.string.confirm_delete_message),
                object : DialogListener {
                    override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                        super.onItemClick(dialogFragment, which)
                        when (which) {
                            BUTTON_OK -> {
                                // 删除模式：触发删除并移除本地选中列表中的该标签
                                val index = selectLabels.indexOfFirst { it.id == tag.id }
                                if (index >= 0) {
                                    selectLabels.removeAt(index)
                                }
                                onDelete?.invoke(tag)
                            }

                            else -> {

                            }
                        }
                    }

                },
                getString(R.string.cancel),
                getString(R.string.delete)
            ).show(childFragmentManager)
            return
        }

        // 普通模式：切换选择
        val index = selectLabels.indexOfFirst { it.id == tag.id }
        if (index >= 0) {
            // 取消选择
            selectLabels.removeAt(index)
        } else {
            // 添加选择
            selectLabels.add(tag)
        }

        // 更新Adapter数据（展示新选中状态）
        updateTagsData()
    }

    /**
     * 初始化点击事件监听器
     */
    private fun initClickListeners() {
        mViewBind?.run {
            ivAdd.clickWithDuration {
                AddTagDialog.show(childFragmentManager) { input ->
                    pendingNewTagName = input
                    onAdd?.invoke(input)
                }
            }

            // 删除标签按钮：切换删除模式
            ivDelete.clickWithDuration {
                isDeleteMode = !isDeleteMode
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
