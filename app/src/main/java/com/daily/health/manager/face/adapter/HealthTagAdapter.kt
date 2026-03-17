package com.daily.health.manager.face.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daily.health.manager.R
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.enums.TagType
import com.daily.health.manager.databinding.FcItemLabelFlexBinding
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible

/**
 * 健康标签RecyclerView适配器
 * 支持血糖和血压标签的显示和选择
 *
 * @param tagType 标签类型（血糖或血压）
 * @param onTagClick 标签点击回调
 */
class HealthTagAdapter(
    private val tagType: TagType,
    private val onTagClick: (HealthTag) -> Unit
) : ListAdapter<HealthTagAdapter.TagItem, HealthTagAdapter.TagViewHolder>(TagDiffCallback()) {

    private var isDelectMode = false

    fun switchDelectMode(isDelete: Boolean){
        isDelectMode = isDelete
        notifyItemRangeChanged(0,itemCount)
    }

    /**
     * 标签项数据类
     * @param tag 健康标签
     * @param isSelected 是否被选中
     * @param displayText 显示文本
     */
    data class TagItem(
        val tag: HealthTag,
        val isSelected: Boolean,
        val displayText: String
    )

    /**
     * 标签ViewHolder
     */
    inner class TagViewHolder(
        private val binding: FcItemLabelFlexBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TagItem) {
            binding.apply {
                // 设置标签文本
                tvLabel.text = item.displayText

                if (isDelectMode) {
                    ivLabelDelete.visible()
                } else {
                    ivLabelDelete.gone()
                }

                // 设置选中状态样式
                if (item.isSelected && !isDelectMode) {
                    tvLabel.setTextColor(
                        ContextCompat.getColor(
                            tvLabel.context,
                            com.healthtracker.framework.R.color.white
                        )
                    )
                    labelItem.background = ContextCompat.getDrawable(
                        labelItem.context,
                        R.drawable.fc_bg_label_select_selected
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
                        R.drawable.fc_bg_label_select_normal
                    )
                }

                // 设置点击事件
                root.click {
                    onTagClick(item.tag)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val binding = FcItemLabelFlexBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * 更新标签列表和选中状态
     * @param availableTags 所有可用标签
     * @param selectedTags 已选中的标签
     * @param labelsArray 标签显示文本数组
     */
    fun updateTags(
        availableTags: List<HealthTag>,
        selectedTags: List<HealthTag>,
        labelsArray: Array<String>
    ) {
        val tagItems = availableTags.map { tag ->
            val displayText = if (tag.isPredefined) {
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

            TagItem(
                tag = tag,
                isSelected = selectedTags.contains(tag),
                displayText = displayText
            )
        }

        submitList(tagItems)
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class TagDiffCallback : DiffUtil.ItemCallback<TagItem>() {
        override fun areItemsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
            return oldItem.tag.id == newItem.tag.id
        }

        override fun areContentsTheSame(oldItem: TagItem, newItem: TagItem): Boolean {
            return oldItem == newItem
        }
    }
}