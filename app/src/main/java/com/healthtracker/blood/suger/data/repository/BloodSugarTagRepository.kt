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
) : BaseTagRepository<BloodSugarTag, BloodSugarTagDao>() {
    
    override val tagDao: BloodSugarTagDao = bloodSugarTagDao
    
    // 实现BaseTagRepository的抽象方法
    override fun getPredefinedTagNames(): List<String> {
        return BloodSugarTag.getPredefinedTagNames().toList()
    }
    
    override fun createPredefinedTag(name: String): BloodSugarTag {
        return BloodSugarTag.createPredefined(name)
    }
    
    override fun createCustomTag(name: String): BloodSugarTag {
        return BloodSugarTag.createCustom(name)
    }
    
    override suspend fun insertTag(tag: BloodSugarTag): Long {
        return bloodSugarTagDao.insert(tag)
    }
    
    override suspend fun insertTags(tags: List<BloodSugarTag>): List<Long> {
        return bloodSugarTagDao.insertAll(tags)
    }
    
    override suspend fun updateTag(tag: BloodSugarTag): Int {
        bloodSugarTagDao.update(tag)
        return 1
    }
    
    override suspend fun deleteTagById(tagId: Long): Int {
        return bloodSugarTagDao.deleteById(tagId)
    }
    
    override suspend fun softDeleteTag(tagId: Long): Int {
        val tag = bloodSugarTagDao.getById(tagId) ?: return 0
        bloodSugarTagDao.update(tag.copy(isDelete = 1))
        return 1
    }
    
    override suspend fun getTagById(tagId: Long): BloodSugarTag? {
        return bloodSugarTagDao.getById(tagId)
    }
    
    override suspend fun getTagsByIds(tagIds: List<Long>): List<BloodSugarTag> {
        return bloodSugarTagDao.getByIds(tagIds)
    }
    
    override suspend fun getTagByName(name: String): BloodSugarTag? {
        return bloodSugarTagDao.getByName(name)
    }
    
    override fun getAllTags(): Flow<List<BloodSugarTag>> {
        return bloodSugarTagDao.getAllTags()
    }
    
    override fun getAllPredefinedTags(): Flow<List<BloodSugarTag>> {
        return bloodSugarTagDao.getPredefinedTags()
    }
    
    override fun getAllCustomTags(): Flow<List<BloodSugarTag>> {
        return bloodSugarTagDao.getCustomTags()
    }
    
    override suspend fun getTagStatistics(): TagStatistics {
        val allTags = getAllTags().first()
        val predefinedCount = allTags.count { it.isPredefinedTag() }
        val customCount = allTags.count { it.isCustomTag() }
        val deletedCount = allTags.count { it.isDelete == 1 }
        
        return TagStatistics(
            totalCount = allTags.size,
            predefinedCount = predefinedCount,
            customCount = customCount,
            deletedCount = deletedCount
        )
    }
    
    override fun getTagName(tag: BloodSugarTag): String {
        return tag.name
    }
    
    override fun getTagId(tag: BloodSugarTag): Long {
        return tag.id
    }

    /**
     * 获取所有血糖标签（公共方法）
     * @return Flow形式的标签列表
     */
    fun getAllBloodSugarTags(): Flow<List<BloodSugarTag>> {
        return getAllTags()
    }

    /**
     * 创建自定义标签（业务方法）
     * @param name 标签名称
     * @return 创建的标签ID，如果失败返回null
     */
    suspend fun createCustomTagBusiness(name: String): Long? {
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
            val tagId = createCustomTagBusiness(name)
            if (tagId != null) {
                successIds.add(tagId)
            }
        }

        return successIds
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