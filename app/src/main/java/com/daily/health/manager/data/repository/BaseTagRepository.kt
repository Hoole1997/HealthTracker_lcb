package com.daily.health.manager.data.repository

import com.daily.health.manager.data.utils.TagUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 基础标签Repository抽象类
 * 提供通用的标签管理操作
 * @param T 标签实体类型
 * @param D 标签DAO接口类型
 */
abstract class BaseTagRepository<T, D> {
    
    /**
     * 获取标签DAO实例
     */
    protected abstract val tagDao: D
    
    /**
     * 获取预定义标签名称列表的抽象方法
     */
    protected abstract fun getPredefinedTagNames(): List<String>
    
    /**
     * 创建预定义标签的抽象方法
     * @param name 标签名称
     * @return 标签实体
     */
    protected abstract fun createPredefinedTag(name: String): T
    
    /**
     * 创建自定义标签的抽象方法
     * @param name 标签名称
     * @return 标签实体
     */
    protected abstract fun createCustomTag(name: String): T
    
    /**
     * 插入标签的抽象方法
     * @param tag 标签实体
     * @return 插入标签的ID
     */
    protected abstract suspend fun insertTag(tag: T): Long
    
    /**
     * 批量插入标签的抽象方法
     * @param tags 标签列表
     * @return 插入标签的ID列表
     */
    protected abstract suspend fun insertTags(tags: List<T>): List<Long>
    
    /**
     * 更新标签的抽象方法
     * @param tag 标签实体
     * @return 影响的行数
     */
    protected abstract suspend fun updateTag(tag: T): Int
    
    /**
     * 根据ID删除标签的抽象方法
     * @param tagId 标签ID
     * @return 影响的行数
     */
    protected abstract suspend fun deleteTagById(tagId: Long): Int
    
    /**
     * 软删除标签的抽象方法
     * @param tagId 标签ID
     * @return 影响的行数
     */
    protected abstract suspend fun softDeleteTag(tagId: Long): Int
    
    /**
     * 根据ID获取标签的抽象方法
     * @param tagId 标签ID
     * @return 标签实体，可能为null
     */
    protected abstract suspend fun getTagById(tagId: Long): T?
    
    /**
     * 根据ID列表获取标签的抽象方法
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    protected abstract suspend fun getTagsByIds(tagIds: List<Long>): List<T>
    
    /**
     * 根据名称获取标签的抽象方法
     * @param name 标签名称
     * @return 标签实体，可能为null
     */
    protected abstract suspend fun getTagByName(name: String): T?
    
    /**
     * 获取所有标签的抽象方法
     * @return Flow形式的标签列表
     */
    protected abstract fun getAllTags(): Flow<List<T>>
    
    /**
     * 获取所有预定义标签的抽象方法
     * @return Flow形式的预定义标签列表
     */
    protected abstract fun getAllPredefinedTags(): Flow<List<T>>
    
    /**
     * 获取所有自定义标签的抽象方法
     * @return Flow形式的自定义标签列表
     */
    protected abstract fun getAllCustomTags(): Flow<List<T>>
    
    /**
     * 获取标签统计信息的抽象方法
     * @return 标签统计信息
     */
    protected abstract suspend fun getTagStatistics(): TagStatistics
    
    /**
     * 初始化预定义标签
     * 检查数据库中是否已存在预定义标签，如果不存在则创建
     */
    suspend fun initializePredefinedTags() {
        try {
            val existingTags = getAllPredefinedTags().first()
            val existingTagNames = existingTags.map { getTagName(it) }.toSet()
            
            val predefinedNames = getPredefinedTagNames()
            val missingTagNames = predefinedNames.filter { it !in existingTagNames }
            
            if (missingTagNames.isNotEmpty()) {
                val newTags = missingTagNames.map { createPredefinedTag(it) }
                insertTags(newTags)
            }
        } catch (e: Exception) {
            // 记录错误但不抛出异常，避免影响应用启动
            e.printStackTrace()
        }
    }
    
    /**
     * 创建自定义标签
     * @param name 标签名称
     * @return 创建的标签ID，如果标签已存在则返回现有标签ID
     */
    suspend fun createCustomTagIfNotExists(name: String): Long? {
        if (!TagUtils.isValidTagName(name)) {
            return null
        }
        
        val cleanName = TagUtils.cleanTagName(name) ?: return null
        val existingTag = getTagByName(cleanName)
        
        return if (existingTag != null) {
            getTagId(existingTag)
        } else {
            val newTag = createCustomTag(cleanName)
            insertTag(newTag)
        }
    }
    
    /**
     * 获取标签名称的抽象方法
     * @param tag 标签实体
     * @return 标签名称
     */
    protected abstract fun getTagName(tag: T): String
    
    /**
     * 获取标签ID的抽象方法
     * @param tag 标签实体
     * @return 标签ID
     */
    protected abstract fun getTagId(tag: T): Long
    
    /**
     * 批量删除标签
     * @param tagIds 标签ID列表
     * @return 删除的标签数量
     */
    suspend fun deleteTags(tagIds: List<Long>): Int {
        var deletedCount = 0
        tagIds.forEach { tagId ->
            deletedCount += deleteTagById(tagId)
        }
        return deletedCount
    }
    
    /**
     * 批量软删除标签
     * @param tagIds 标签ID列表
     * @return 删除的标签数量
     */
    suspend fun softDeleteTags(tagIds: List<Long>): Int {
        var deletedCount = 0
        tagIds.forEach { tagId ->
            deletedCount += softDeleteTag(tagId)
        }
        return deletedCount
    }
}

/**
 * 标签统计信息数据类
 */
data class TagStatistics(
    val totalCount: Int,
    val predefinedCount: Int,
    val customCount: Int,
    val deletedCount: Int
)