package com.healthtracker.blood.suger.data.utils

import com.healthtracker.blood.suger.data.entity.BloodSugarTag

/**
 * 标签工具类
 * 提供标签相关的实用方法
 */
object TagUtils {

    /**
     * 将标签ID列表转换为字符串
     * @param tagIds 标签ID列表
     * @return 逗号分隔的字符串，如 "1,3,5"
     */
    fun tagIdsToString(tagIds: List<Long>?): String? {
        return tagIds?.takeIf { it.isNotEmpty() }?.joinToString(",")
    }

    /**
     * 将标签ID字符串转换为列表
     * @param tagIdsString 逗号分隔的标签ID字符串
     * @return 标签ID列表
     */
    fun stringToTagIds(tagIdsString: String?): List<Long> {
        return if (tagIdsString.isNullOrBlank()) {
            emptyList()
        } else {
            tagIdsString.split(",").mapNotNull { it.trim().toLongOrNull() }
        }
    }

    /**
     * 验证标签名称是否有效
     * @param name 标签名称
     * @return 是否有效
     */
    fun isValidTagName(name: String?): Boolean {
        return !name.isNullOrBlank() &&
               name.trim().length in 1..20 &&
               !name.contains(",") // 避免与分隔符冲突
    }

    /**
     * 清理标签名称（去除首尾空格，限制长度）
     * @param name 原始标签名称
     * @return 清理后的标签名称
     */
    fun cleanTagName(name: String?): String? {
        return name?.trim()?.take(20)?.takeIf { it.isNotEmpty() }
    }

    /**
     * 合并标签ID列表（去重并排序）
     * @param existingTagIds 现有标签ID列表
     * @param newTagIds 新增标签ID列表
     * @return 合并后的标签ID列表
     */
    fun mergeTagIds(existingTagIds: List<Long>, newTagIds: List<Long>): List<Long> {
        return (existingTagIds + newTagIds).distinct().sorted()
    }

    /**
     * 从标签ID列表中移除指定标签
     * @param tagIds 标签ID列表
     * @param tagIdToRemove 要移除的标签ID
     * @return 移除后的标签ID列表
     */
    fun removeTagId(tagIds: List<Long>, tagIdToRemove: Long): List<Long> {
        return tagIds.filter { it != tagIdToRemove }
    }

    /**
     * 检查标签ID列表是否包含指定标签
     * @param tagIds 标签ID列表
     * @param tagId 要检查的标签ID
     * @return 是否包含
     */
    fun containsTag(tagIds: List<Long>, tagId: Long): Boolean {
        return tagIds.contains(tagId)
    }

    /**
     * 获取标签显示文本（用于UI展示）
     * @param tags 标签列表
     * @param maxDisplay 最大显示数量
     * @param separator 分隔符
     * @return 显示文本
     */
    fun getTagDisplayText(
        tags: List<BloodSugarTag>,
        maxDisplay: Int = 3,
        separator: String = "、"
    ): String {
        return when {
            tags.isEmpty() -> ""
            tags.size <= maxDisplay -> tags.joinToString(separator) { it.name }
            else -> {
                val displayTags = tags.take(maxDisplay).joinToString(separator) { it.name }
                val remainingCount = tags.size - maxDisplay
                "$displayTags 等${tags.size}个标签"
            }
        }
    }

    /**
     * 过滤预定义标签
     * @param tags 标签列表
     * @return 预定义标签列表
     */
    fun filterPredefinedTags(tags: List<BloodSugarTag>): List<BloodSugarTag> {
        return tags.filter { it.isPredefinedTag() }
    }

    /**
     * 过滤自定义标签
     * @param tags 标签列表
     * @return 自定义标签列表
     */
    fun filterCustomTags(tags: List<BloodSugarTag>): List<BloodSugarTag> {
        return tags.filter { it.isCustomTag() }
    }

    /**
     * 按类型分组标签
     * @param tags 标签列表
     * @return 分组后的标签Map，key为是否预定义(true/false)
     */
    fun groupTagsByType(tags: List<BloodSugarTag>): Map<Boolean, List<BloodSugarTag>> {
        return tags.groupBy { it.isPredefinedTag() }
    }

}