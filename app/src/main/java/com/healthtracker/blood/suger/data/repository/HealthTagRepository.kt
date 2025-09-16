package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.HealthTagDao
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 健康标签数据仓库
 * 提供标签数据的业务逻辑封装和数据访问
 */
@Singleton
class HealthTagRepository @Inject constructor(
    private val healthTagDao: HealthTagDao
) {

    /**
     * 初始化预定义标签
     * 在应用首次启动时调用，确保预定义标签存在
     */
    suspend fun initializePredefinedTags() {
        // 检查是否已经有预定义标签
        val predefinedTags = healthTagDao.getPredefinedTags().first()
        if (predefinedTags.isEmpty()) {
            val predefinedTagList = HealthTag.createAllPredefinedTags()
            healthTagDao.insertAll(predefinedTagList)
        }
    }

    /**
     * 获取所有标签
     * @return Flow形式的标签列表（预定义标签在前）
     */
    fun getAllTags(): Flow<List<HealthTag>> {
        return healthTagDao.getAllTags()
    }

    /**
     * 获取预定义标签
     * @return Flow形式的预定义标签列表
     */
    fun getPredefinedTags(): Flow<List<HealthTag>> {
        return healthTagDao.getPredefinedTags()
    }

    /**
     * 获取自定义标签
     * @return Flow形式的自定义标签列表
     */
    fun getCustomTags(): Flow<List<HealthTag>> {
        return healthTagDao.getCustomTags()
    }

    /**
     * 根据ID获取标签
     * @param tagId 标签ID
     * @return 标签实体，可能为null
     */
    suspend fun getTagById(tagId: Long): HealthTag? {
        return healthTagDao.getById(tagId)
    }

    /**
     * 根据ID列表获取标签
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    suspend fun getTagsByIds(tagIds: List<Long>): List<HealthTag> {
        return if (tagIds.isNotEmpty()) {
            healthTagDao.getByIds(tagIds)
        } else {
            emptyList()
        }
    }

    /**
     * 根据名称获取标签
     * @param name 标签名称
     * @return 标签实体，可能为null
     */
    suspend fun getTagByName(name: String): HealthTag? {
        return healthTagDao.getByName(name)
    }

    /**
     * 创建自定义标签
     * @param name 标签名称
     * @return 创建的标签ID，如果失败返回null
     */
    suspend fun createCustomTag(name: String): Long? {
        val cleanName = TagUtils.cleanTagName(name)

        // 验证标签名称
        if (cleanName == null || !TagUtils.isValidTagName(cleanName)) {
            return null
        }

        // 检查是否已存在
        if (healthTagDao.isNameExists(cleanName)) {
            return null
        }

        val tag = HealthTag.createCustom(cleanName)
        return healthTagDao.insert(tag)
    }

    /**
     * 更新标签
     * @param tag 标签实体
     * @return 是否成功
     */
    suspend fun updateTag(tag: HealthTag): Boolean {
        // 只允许更新自定义标签
        if (tag.isPredefinedTag()) {
            return false
        }

        // 验证标签名称
        val cleanName = TagUtils.cleanTagName(tag.name)
        if (cleanName == null || !TagUtils.isValidTagName(cleanName)) {
            return false
        }

        // 检查名称是否与其他标签冲突
        val existingTag = healthTagDao.getByName(cleanName)
        if (existingTag != null && existingTag.id != tag.id) {
            return false
        }

        try {
            healthTagDao.update(tag.copy(name = cleanName))
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 删除自定义标签
     * @param tagId 标签ID
     * @return 是否成功删除
     */
    suspend fun deleteCustomTag(tagId: Long): Boolean {
        val tag = healthTagDao.getById(tagId) ?: return false

        // 只允许删除自定义标签
        if (tag.isPredefinedTag()) {
            return false
        }

        try {
            healthTagDao.deleteById(tagId)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 检查标签名称是否已存在
     * @param name 标签名称
     * @return 是否存在
     */
    suspend fun isTagNameExists(name: String): Boolean {
        val cleanName = TagUtils.cleanTagName(name) ?: return false
        return healthTagDao.isNameExists(cleanName)
    }

    /**
     * 搜索标签
     * @param keyword 搜索关键词
     * @return 匹配的标签列表
     */
    suspend fun searchTags(keyword: String): List<HealthTag> {
        return if (keyword.isBlank()) {
            emptyList()
        } else {
            healthTagDao.searchTags(keyword.trim())
        }
    }

    /**
     * 获取最常用的标签
     * @param limit 返回数量限制
     * @return 最常用的标签列表
     */
    suspend fun getMostUsedTags(limit: Int = 10): List<HealthTag> {
        return healthTagDao.getMostUsedTags(limit)
    }

    /**
     * 获取自定义标签数量
     * @return 自定义标签数量
     */
    suspend fun getCustomTagCount(): Int {
        return healthTagDao.getCustomTagCount()
    }

    /**
     * 删除所有自定义标签（谨慎使用）
     * @return 是否成功
     */
    suspend fun deleteAllCustomTags(): Boolean {
        return try {
            healthTagDao.deleteAllCustomTags()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 批量创建自定义标签
     * @param names 标签名称列表
     * @return 成功创建的标签ID列表
     */
    suspend fun createCustomTags(names: List<String>): List<Long> {
        val successIds = mutableListOf<Long>()

        for (name in names) {
            val tagId = createCustomTag(name)
            if (tagId != null) {
                successIds.add(tagId)
            }
        }

        return successIds
    }

    /**
     * 获取标签统计信息
     * @return 标签统计信息
     */
    suspend fun getTagStatistics(): TagStatistics {
        val allTags = getAllTags().first()
        val predefinedCount = allTags.count { it.isPredefinedTag() }
        val customCount = allTags.count { it.isCustomTag() }

        return TagStatistics(
            totalCount = allTags.size,
            predefinedCount = predefinedCount,
            customCount = customCount
        )
    }

    /**
     * 根据标签类型过滤标签
     * @param tags 标签列表
     * @param isPredefined 是否为预定义标签
     * @return 过滤后的标签列表
     */
    fun filterTagsByType(tags: List<HealthTag>, isPredefined: Boolean): List<HealthTag> {
        return if (isPredefined) {
            TagUtils.filterPredefinedTags(tags)
        } else {
            TagUtils.filterCustomTags(tags)
        }
    }

    /**
     * 按类型分组标签
     * @param tags 标签列表
     * @return 分组后的标签Map
     */
    fun groupTagsByType(tags: List<HealthTag>): Map<Boolean, List<HealthTag>> {
        return TagUtils.groupTagsByType(tags)
    }

    /**
     * 生成标签显示文本
     * @param tags 标签列表
     * @param maxDisplay 最大显示数量
     * @return 显示文本
     */
    fun getTagDisplayText(tags: List<HealthTag>, maxDisplay: Int = 3): String {
        return TagUtils.getTagDisplayText(tags, maxDisplay)
    }

    /**
     * 验证标签ID列表的有效性
     * @param tagIds 标签ID列表
     * @return 有效的标签ID列表
     */
    suspend fun validateTagIds(tagIds: List<Long>): List<Long> {
        if (tagIds.isEmpty()) return emptyList()

        val existingTags = healthTagDao.getByIds(tagIds)
        return existingTags.map { it.id }
    }
}

/**
 * 标签统计信息数据类
 */
data class TagStatistics(
    val totalCount: Int,
    val predefinedCount: Int,
    val customCount: Int
)