package com.daily.health.manager.data.dao

import androidx.room.*
import com.daily.health.manager.data.entity.HealthTag
import com.daily.health.manager.data.enums.TagType
import kotlinx.coroutines.flow.Flow

/**
 * 统一的健康标签数据访问对象
 * 提供对health_tags表的所有数据库操作
 */
@Dao
interface HealthTagDao {
    
    /**
     * 插入单个标签
     * @param tag 要插入的标签
     * @return 插入后的标签ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: HealthTag): Long
    
    /**
     * 批量插入标签
     * @param tags 要插入的标签列表
     * @return 插入后的标签ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<HealthTag>): List<Long>
    
    /**
     * 更新标签
     * @param tag 要更新的标签
     * @return 受影响的行数
     */
    @Update
    suspend fun update(tag: HealthTag): Int
    
    /**
     * 删除标签（软删除）
     * @param tag 要删除的标签
     * @return 受影响的行数
     */
    @Query("UPDATE health_tags SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long): Int
    
    /**
     * 删除标签（物理删除，保留原方法）
     * @param tag 要删除的标签
     * @return 受影响的行数
     */
    @Delete
    suspend fun delete(tag: HealthTag): Int
    
    /**
     * 根据ID删除标签（软删除）
     * @param id 标签ID
     * @return 受影响的行数
     */
    @Query("UPDATE health_tags SET is_deleted = 1 WHERE id = :id")
    suspend fun softDeleteById(id: Long): Int
    
    /**
     * 根据ID删除标签（物理删除，保留原方法）
     * @param id 标签ID
     * @return 受影响的行数
     */
    @Query("DELETE FROM health_tags WHERE id = :id")
    suspend fun deleteById(id: Long): Int
    
    /**
     * 根据ID获取标签（过滤软删除）
     * @param id 标签ID
     * @return 标签实体，如果不存在或已软删除则返回null
     */
    @Query("SELECT * FROM health_tags WHERE id = :id AND is_deleted = 0")
    suspend fun getById(id: Long): HealthTag?
    
    /**
     * 根据ID列表获取标签（过滤软删除）
     * @param ids 标签ID列表
     * @return 标签列表
     */
    @Query("SELECT * FROM health_tags WHERE id IN (:ids) AND is_deleted = 0")
    suspend fun getByIds(ids: List<Long>): List<HealthTag>
    
    /**
     * 根据标签类型获取所有标签（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 标签列表的Flow，按预定义标签优先、创建时间升序排列
     */
    @Query("SELECT * FROM health_tags WHERE tag_type = :tagType AND is_deleted = 0 ORDER BY is_predefined DESC, create_time ASC")
    fun getTagsByType(tagType: Int): Flow<List<HealthTag>>
    
    /**
     * 根据标签类型获取所有标签（同步版本，过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 标签列表，按预定义标签优先、创建时间升序排列
     */
    @Query("SELECT * FROM health_tags WHERE tag_type = :tagType AND is_deleted = 0 ORDER BY is_predefined DESC, create_time ASC")
    suspend fun getTagsByTypeSync(tagType: Int): List<HealthTag>
    
    /**
     * 根据标签类型获取预定义标签（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 预定义标签列表，按预定义索引升序排列
     */
    @Query("SELECT * FROM health_tags WHERE tag_type = :tagType AND is_predefined = 1 AND is_deleted = 0 ORDER BY predefined_index ASC")
    suspend fun getPredefinedTagsByType(tagType: Int): List<HealthTag>
    
    /**
     * 根据标签类型获取自定义标签（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 自定义标签列表的Flow，按创建时间升序排列
     */
    @Query("SELECT * FROM health_tags WHERE tag_type = :tagType AND is_predefined = 0 AND is_deleted = 0 ORDER BY create_time ASC")
    fun getCustomTagsByType(tagType: Int): Flow<List<HealthTag>>
    
    /**
     * 获取所有标签（过滤软删除）
     * @return 所有标签的Flow
     */
    @Query("SELECT * FROM health_tags WHERE is_deleted = 0 ORDER BY tag_type ASC, is_predefined DESC, create_time ASC")
    fun getAllTags(): Flow<List<HealthTag>>
    
    /**
     * 检查指定类型和名称的标签是否存在（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @param name 标签名称
     * @return 是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM health_tags WHERE tag_type = :tagType AND name = :name AND is_deleted = 0")
    suspend fun existsByTypeAndName(tagType: Int, name: String): Boolean
    
    /**
     * 获取指定类型的预定义标签数量（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 预定义标签数量
     */
    @Query("SELECT COUNT(*) FROM health_tags WHERE tag_type = :tagType AND is_predefined = 1 AND is_deleted = 0")
    suspend fun getPredefinedTagCount(tagType: Int): Int
    
    /**
     * 获取指定类型的标签总数（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 标签总数
     */
    @Query("SELECT COUNT(*) FROM health_tags WHERE tag_type = :tagType AND is_deleted = 0")
    suspend fun getTagCountByType(tagType: Int): Int
    
    /**
     * 删除指定类型的所有标签（软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("UPDATE health_tags SET is_deleted = 1 WHERE tag_type = :tagType AND is_deleted = 0")
    suspend fun softDeleteAllByType(tagType: Int): Int
    
    /**
     * 删除指定类型的自定义标签（软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("UPDATE health_tags SET is_deleted = 1 WHERE tag_type = :tagType AND is_predefined = 0 AND is_deleted = 0")
    suspend fun softDeleteCustomTagsByType(tagType: Int): Int
    
    /**
     * 恢复软删除的标签
     * @param id 标签ID
     * @return 受影响的行数
     */
    @Query("UPDATE health_tags SET is_deleted = 0 WHERE id = :id")
    suspend fun restoreById(id: Long): Int
    
    /**
     * 删除指定类型的所有标签（物理删除，保留原方法）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("DELETE FROM health_tags WHERE tag_type = :tagType")
    suspend fun deleteAllByType(tagType: Int): Int
    
    /**
     * 删除指定类型的自定义标签（物理删除，保留原方法）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("DELETE FROM health_tags WHERE tag_type = :tagType AND is_predefined = 0")
    suspend fun deleteCustomTagsByType(tagType: Int): Int
    
    /**
     * 清空所有标签
     * @return 受影响的行数
     */
    @Query("DELETE FROM health_tags")
    suspend fun deleteAll(): Int
}
