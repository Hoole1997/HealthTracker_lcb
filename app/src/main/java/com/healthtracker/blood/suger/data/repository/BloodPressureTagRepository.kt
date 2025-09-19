package com.healthtracker.blood.suger.data.repository

import com.healthtracker.blood.suger.data.dao.BloodPressureTagDao
import com.healthtracker.blood.suger.data.entity.BloodPressureTag
import com.healthtracker.blood.suger.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 血压标签数据仓库
 * 提供标签数据的业务逻辑封装和数据访问
 */
@Singleton
class BloodPressureTagRepository @Inject constructor(
    private val bloodPressureTagDao: BloodPressureTagDao
) {

    /**
     * 初始化预定义标签
     * 在应用首次启动时调用，确保预定义标签存在
     */
    suspend fun initializePredefinedTags() {
        // 检查是否已经有预定义标签
        val predefinedTags = bloodPressureTagDao.getAllTags().first()
        if (predefinedTags.isEmpty()) {
            val predefinedTagList = BloodPressureTag.createAllPredefinedTags()
            bloodPressureTagDao.insertAll(predefinedTagList)
        }
    }

    /**
     * 获取所有血压标签
     * @return Flow形式的标签列表（预定义标签在前）
     */
    fun getAllTags(): Flow<List<BloodPressureTag>> {
        return bloodPressureTagDao.getAllTags()
    }

    /**
     * 根据ID获取血压标签
     * @param tagId 标签ID
     * @return 标签实体，可能为null
     */
    suspend fun getTagById(tagId: Long): BloodPressureTag? {
        return bloodPressureTagDao.getById(tagId)
    }

    /**
     * 根据ID列表获取血压标签
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    suspend fun getTagsByIds(tagIds: List<Long>): List<BloodPressureTag> {
        return if (tagIds.isNotEmpty()) {
            bloodPressureTagDao.getByIds(tagIds)
        } else {
            emptyList()
        }
    }

    /**
     * 根据名称获取血压标签
     * @param name 标签名称
     * @return 标签实体，可能为null
     */
    suspend fun getTagByName(name: String): BloodPressureTag? {
        return bloodPressureTagDao.getByName(name)
    }

    /**
     * 创建自定义血压标签
     * @param name 标签名称
     * @return 创建的标签ID，如果名称无效或已存在则返回-1
     */
    suspend fun createCustomTag(name: String): Long {
        val cleanName = TagUtils.cleanTagName(name) ?: return -1

        // 检查名称长度
        if (cleanName.length > 20) {
            return -1
        }

        // 检查是否已存在
        bloodPressureTagDao.getCustomByName(name)?.run {
            if(isDelete == 1){
                bloodPressureTagDao.update(this.copy(isDelete = 0))
                return id
            }
        }

        val tag = BloodPressureTag.createCustom(cleanName)
        return bloodPressureTagDao.insert(tag)
    }

    /**
     * 删除自定义血压标签（软删除）
     * @param tagId 标签ID
     * @return 是否删除成功
     */
    suspend fun deleteCustomTag(tagId: Long): Boolean {
        val tag = bloodPressureTagDao.getById(tagId) ?: return false

        try {
            bloodPressureTagDao.update(tag.copy(isDelete = 1))
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 检查血压标签名称是否已存在
     * @param name 标签名称
     * @return 是否存在
     */
    suspend fun isTagNameExists(name: String): Boolean {
        val cleanName = TagUtils.cleanTagName(name) ?: return false
        return bloodPressureTagDao.isNameExists(cleanName)
    }

    /**
     * 获取自定义血压标签数量
     * @return 自定义血压标签数量
     */
    suspend fun getCustomTagCount(): Int {
        return bloodPressureTagDao.getCustomTagCount()
    }

    /**
     * 解析标签ID字符串为ID列表
     * @param tagIdsString 逗号分隔的标签ID字符串
     * @return 标签ID列表
     */
    fun parseTagIds(tagIdsString: String?): List<Long> {
        return TagUtils.stringToTagIds(tagIdsString)
    }

    /**
     * 格式化标签ID列表为字符串
     * @param tagIds 标签ID列表
     * @return 格式化后的字符串
     */
    fun formatTagIds(tagIds: List<Long>): String {
        return TagUtils.tagIdsToString(tagIds) ?: ""
    }

    /**
     * 验证标签ID列表的有效性
     * @param tagIds 标签ID列表
     * @return 有效的标签ID列表
     */
    suspend fun validateTagIds(tagIds: List<Long>): List<Long> {
        if (tagIds.isEmpty()) return emptyList()

        val existingTags = bloodPressureTagDao.getByIds(tagIds)
        return existingTags.map { it.id }
    }

    /**
     * 根据类型过滤血压标签
     * @param tags 标签列表
     * @param isPredefined 是否为预定义标签
     * @return 过滤后的标签列表
     */
    fun filterTagsByType(tags: List<BloodPressureTag>, isPredefined: Boolean): List<BloodPressureTag> {
        return if (isPredefined) {
            tags.filter { it.isPredefinedTag() }
        } else {
            tags.filter { it.isCustomTag() }
        }
    }

    /**
     * 将血压标签按类型分组
     * @param tags 标签列表
     * @return 分组后的标签Map
     */
    fun groupTagsByType(tags: List<BloodPressureTag>): Map<Boolean, List<BloodPressureTag>> {
        return tags.groupBy { it.isPredefinedTag() }
    }

    /**
     * 获取血压标签的显示文本
     * @param tags 标签列表
     * @param maxDisplay 最大显示数量
     * @return 显示文本
     */
    fun getTagDisplayText(tags: List<BloodPressureTag>, maxDisplay: Int = 3): String {
        val displayTags = tags.take(maxDisplay)
        val displayText = displayTags.joinToString("、") { it.name }
        return if (tags.size > maxDisplay) {
            "$displayText 等${tags.size}个"
        } else {
            displayText
        }
    }
}