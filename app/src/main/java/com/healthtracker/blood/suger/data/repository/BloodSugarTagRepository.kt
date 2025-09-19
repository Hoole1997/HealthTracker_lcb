package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BloodSugarTagDao
import com.healthtracker.blood.suger.data.entity.BloodSugarTag
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 血糖标签数据仓库
 * 提供标签数据的业务逻辑封装和数据访问
 */
@Singleton
class BloodSugarTagRepository @Inject constructor(
    private val bloodSugarTagDao: BloodSugarTagDao
) {

    /**
     * 初始化预定义标签
     * 在应用首次启动时调用，确保预定义标签存在
     */
    suspend fun initializePredefinedTags() {
        // 检查是否已经有预定义标签
        val predefinedTags = bloodSugarTagDao.getAllTags().first()
        if (predefinedTags.isEmpty()) {
            val predefinedTagList = BloodSugarTag.createAllPredefinedTags()
            bloodSugarTagDao.insertAll(predefinedTagList)
        }
    }

    /**
     * 获取所有标签
     * @return Flow形式的标签列表（预定义标签在前）
     */
    fun getAllTags(): Flow<List<BloodSugarTag>> {
        return bloodSugarTagDao.getAllTags()
    }

    /**
     * 根据ID获取标签
     * @param tagId 标签ID
     * @return 标签实体，可能为null
     */
    suspend fun getTagById(tagId: Long): BloodSugarTag? {
        return bloodSugarTagDao.getById(tagId)
    }

    /**
     * 根据ID列表获取标签
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    suspend fun getTagsByIds(tagIds: List<Long>): List<BloodSugarTag> {
        return if (tagIds.isNotEmpty()) {
            bloodSugarTagDao.getByIds(tagIds)
        } else {
            emptyList()
        }
    }

    /**
     * 根据名称获取标签
     * @param name 标签名称
     * @return 标签实体，可能为null
     */
    suspend fun getTagByName(name: String): BloodSugarTag? {
        return bloodSugarTagDao.getByName(name)
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
        bloodSugarTagDao.getCustomByName(name)?.run {
            if(isDelete == 1){
                bloodSugarTagDao.update(this.copy(isDelete = 0))
                return id
            }
        }

        val tag = BloodSugarTag.createCustom(cleanName)
        return bloodSugarTagDao.insert(tag)
    }

    /**
     * 删除自定义标签
     * @param tagId 标签ID
     * @return 是否成功删除
     */
    suspend fun deleteCustomTag(tagId: Long): Boolean {
        val tag = bloodSugarTagDao.getById(tagId) ?: return false

        try {
            bloodSugarTagDao.update(tag.copy(isDelete = 1))
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
        return bloodSugarTagDao.isNameExists(cleanName)
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
    fun filterTagsByType(tags: List<BloodSugarTag>, isPredefined: Boolean): List<BloodSugarTag> {
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
    fun groupTagsByType(tags: List<BloodSugarTag>): Map<Boolean, List<BloodSugarTag>> {
        return TagUtils.groupTagsByType(tags)
    }

    /**
     * 生成标签显示文本
     * @param tags 标签列表
     * @param maxDisplay 最大显示数量
     * @return 显示文本
     */
    fun getTagDisplayText(tags: List<BloodSugarTag>, maxDisplay: Int = 3): String {
        return TagUtils.getTagDisplayText(tags, maxDisplay)
    }

    /**
     * 验证标签ID列表的有效性
     * @param tagIds 标签ID列表
     * @return 有效的标签ID列表
     */
    suspend fun validateTagIds(tagIds: List<Long>): List<Long> {
        if (tagIds.isEmpty()) return emptyList()

        val existingTags = bloodSugarTagDao.getByIds(tagIds)
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