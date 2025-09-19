package com.healthtracker.blood.suger.data.repository

import android.content.Context
import com.healthtracker.blood.suger.data.dao.HealthTagDao
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.enums.TagType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统一的健康标签仓库
 * 提供标签相关的所有业务逻辑操作
 */
@Singleton
class HealthTagRepository @Inject constructor(
    private val healthTagDao: HealthTagDao,
    @ApplicationContext private val context: Context
) {
    
    /**
     * 初始化预定义标签
     * 检查数据库中是否已存在预定义标签，如果不存在则插入
     */
    suspend fun initializePredefinedTags() {
        try {
            // 检查血糖预定义标签是否已存在
            val existingBloodSugarTags = getPredefinedTagsByType(TagType.BLOOD_SUGAR)
            if (existingBloodSugarTags.isEmpty()) {
                // 插入血糖预定义标签
                val bloodSugarLabels = context.resources.getStringArray(com.healthtracker.blood.suger.R.array.blood_sugar_labels)
                bloodSugarLabels.forEachIndexed { index, label ->
                    val tag = HealthTag.createPredefined(label, TagType.BLOOD_SUGAR, index)
                    healthTagDao.insert(tag)
                }
            }
            
            // 检查血压预定义标签是否已存在
            val existingBloodPressureTags = getPredefinedTagsByType(TagType.BLOOD_PRESSURE)
            if (existingBloodPressureTags.isEmpty()) {
                // 插入血压预定义标签
                val bloodPressureLabels = context.resources.getStringArray(com.healthtracker.blood.suger.R.array.blood_pressure_labels)
                bloodPressureLabels.forEachIndexed { index, label ->
                    val tag = HealthTag.createPredefined(label, TagType.BLOOD_PRESSURE, index)
                    healthTagDao.insert(tag)
                }
            }
            // 输出初始化结果日志
            val bloodSugarCount = getPredefinedTagsByTypeHelper(TagType.BLOOD_SUGAR).size
            val bloodPressureCount = getPredefinedTagsByTypeHelper(TagType.BLOOD_PRESSURE).size
            android.util.Log.d("HealthTagRepository", "Predefined tags initialized - Blood Sugar: $bloodSugarCount, Blood Pressure: $bloodPressureCount")
            
        } catch (e: Exception) {
            android.util.Log.e("HealthTagRepository", "Failed to initialize predefined tags", e)
            e.printStackTrace()
            // 初始化失败时不抛出异常，避免影响应用正常运行
        }
    }
    
    /**
     * 获取指定类型的所有标签
     * @param tagType 标签类型
     * @return 标签列表的Flow
     */
    fun getTagsByType(tagType: TagType): Flow<List<HealthTag>> {
        return healthTagDao.getTagsByType(tagType.value)
    }
    
    /**
     * 获取指定类型的所有标签（同步版本）
     * @param tagType 标签类型
     * @return 标签列表
     */
    suspend fun getTagsByTypeSync(tagType: TagType): List<HealthTag> {
        return healthTagDao.getTagsByTypeSync(tagType.value)
    }
    
    /**
     * 获取指定类型的预定义标签
     * @param tagType 标签类型
     * @return 预定义标签列表
     */
    suspend fun getPredefinedTagsByType(tagType: TagType): List<HealthTag> {
        return healthTagDao.getPredefinedTagsByType(tagType.value)
    }
    
    /**
     * 获取指定类型的预定义标签（私有辅助方法）
     * @param tagType 标签类型
     * @return 预定义标签列表
     */
    private suspend fun getPredefinedTagsByTypeHelper(tagType: TagType): List<HealthTag> {
        return healthTagDao.getTagsByType(tagType.value).first().filter { it.isPredefinedTag() }
    }
    
    /**
     * 获取指定类型的自定义标签
     * @param tagType 标签类型
     * @return 自定义标签列表的Flow
     */
    fun getCustomTagsByType(tagType: TagType): Flow<List<HealthTag>> {
        return healthTagDao.getCustomTagsByType(tagType.value)
    }
    
    /**
     * 根据ID获取标签
     * @param id 标签ID
     * @return 标签实体，如果不存在则返回null
     */
    suspend fun getTagById(id: Long): HealthTag? {
        return healthTagDao.getById(id)
    }
    
    /**
     * 根据ID列表获取标签
     * @param ids 标签ID列表
     * @return 标签列表
     */
    suspend fun getTagsByIds(ids: List<Long>): List<HealthTag> {
        return healthTagDao.getByIds(ids)
    }
    
    /**
     * 创建自定义标签
     * @param name 标签名称
     * @param tagType 标签类型
     * @return 创建的标签实体
     */
    suspend fun createCustomTag(name: String, tagType: TagType): HealthTag {
        val tag = HealthTag.createCustom(name, tagType)
        healthTagDao.insert(tag)
        return tag
    }
    
    /**
     * 更新标签
     * @param tag 要更新的标签
     * @return 更新成功返回true，失败返回false
     */
    suspend fun updateTag(tag: HealthTag): Boolean {
        return try {
            val result = healthTagDao.update(tag)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 删除标签
     * @param tag 要删除的标签
     * @return 删除成功返回true，失败返回false
     */
    suspend fun deleteTag(tag: HealthTag): Boolean {
        return try {
            // 只允许删除自定义标签
            if (tag.isPredefinedTag()) {
                return false
            }
            
            val result = healthTagDao.delete(tag)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 根据ID删除标签
     * @param id 标签ID
     * @return 删除成功返回true，失败返回false
     */
    suspend fun deleteTagById(id: Long): Boolean {
        return try {
            // 先获取标签信息，检查是否为预定义标签
            val tag = healthTagDao.getById(id)
            if (tag?.isPredefinedTag() == true) {
                return false // 不允许删除预定义标签
            }
            
            val result = healthTagDao.deleteById(id)
            result > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 检查标签是否存在
     * @param tagType 标签类型
     * @param name 标签名称
     * @return 是否存在
     */
    suspend fun tagExists(tagType: TagType, name: String): Boolean {
        return try {
            healthTagDao.existsByTypeAndName(tagType.value, name)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取指定类型的标签总数
     * @param tagType 标签类型
     * @return 标签总数
     */
    suspend fun getTagCountByType(tagType: TagType): Int {
        return try {
            healthTagDao.getTagCountByType(tagType.value)
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    /**
     * 获取所有标签
     * @return 所有标签的Flow
     */
    fun getAllTags(): Flow<List<HealthTag>> {
        return healthTagDao.getAllTags()
    }
    
    /**
     * 删除指定类型的所有自定义标签
     * @param tagType 标签类型
     * @return 删除成功返回true，失败返回false
     */
    suspend fun deleteAllCustomTagsByType(tagType: TagType): Boolean {
        return try {
            val result = healthTagDao.deleteCustomTagsByType(tagType.value)
            result >= 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 获取血糖标签（便捷方法）
     * @return 血糖标签列表的Flow
     */
    fun getBloodSugarTags(): Flow<List<HealthTag>> {
        return getTagsByType(TagType.BLOOD_SUGAR)
    }
    
    /**
     * 获取血压标签（便捷方法）
     * @return 血压标签列表的Flow
     */
    fun getBloodPressureTags(): Flow<List<HealthTag>> {
        return getTagsByType(TagType.BLOOD_PRESSURE)
    }
    
    /**
     * 创建血糖自定义标签（便捷方法）
     * @param name 标签名称
     * @return 创建成功返回标签ID，失败返回-1
     */
    suspend fun createBloodSugarCustomTag(name: String): Long {
        return try {
            val tag = createCustomTag(name, TagType.BLOOD_SUGAR)
            tag.id
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * 创建血压自定义标签（便捷方法）
     * @param name 标签名称
     * @return 创建成功返回标签ID，失败返回-1
     */
    suspend fun createBloodPressureCustomTag(name: String): Long {
        return try {
            val tag = createCustomTag(name, TagType.BLOOD_PRESSURE)
            tag.id
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * 检查标签名称是否已存在
     * @param tagType 标签类型
     * @param name 标签名称
     * @return 是否存在
     */
    suspend fun isTagNameExists(tagType: TagType, name: String): Boolean {
        return tagExists(tagType, name)
    }
    
    /**
     * 获取标签显示文本
     * @param tag 标签实体
     * @return 显示文本
     */
    fun getTagDisplayText(tag: HealthTag): String {
        return tag.name
    }
}