package com.daily.health.manager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.daily.health.manager.data.entity.HealthTag
import kotlinx.coroutines.flow.Flow

/**
 * 统一的健康标签数据访问对象
 * 提供对health_tags表的所有数据库操作
 */
@Dao
interface LocalDao03 {
    
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
    @Query("UPDATE t03 SET c07 = 1 WHERE c01 = :id")
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
    @Query("UPDATE t03 SET c07 = 1 WHERE c01 = :id")
    suspend fun softDeleteById(id: Long): Int
    
    /**
     * 根据ID删除标签（物理删除，保留原方法）
     * @param id 标签ID
     * @return 受影响的行数
     */
    @Query("DELETE FROM t03 WHERE c01 = :id")
    suspend fun deleteById(id: Long): Int
    
    /**
     * 根据ID获取标签（过滤软删除）
     * @param id 标签ID
     * @return 标签实体，如果不存在或已软删除则返回null
     */
    @Query("SELECT * FROM t03 WHERE c01 = :id AND c07 = 0")
    suspend fun getById(id: Long): HealthTag?
    
    /**
     * 根据ID列表获取标签（过滤软删除）
     * @param ids 标签ID列表
     * @return 标签列表
     */
    @Query("SELECT * FROM t03 WHERE c01 IN (:ids) AND c07 = 0")
    suspend fun getByIds(ids: List<Long>): List<HealthTag>
    
    /**
     * 根据标签类型获取所有标签（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 标签列表的Flow，按预定义标签优先、创建时间升序排列
     */
    @Query("SELECT * FROM t03 WHERE c03 = :tagType AND c07 = 0 ORDER BY c04 DESC, c06 ASC")
    fun getTagsByType(tagType: Int): Flow<List<HealthTag>>
    
    /**
     * 根据标签类型获取所有标签（同步版本，过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 标签列表，按预定义标签优先、创建时间升序排列
     */
    @Query("SELECT * FROM t03 WHERE c03 = :tagType AND c07 = 0 ORDER BY c04 DESC, c06 ASC")
    suspend fun getTagsByTypeSync(tagType: Int): List<HealthTag>
    
    /**
     * 根据标签类型获取预定义标签（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 预定义标签列表，按预定义索引升序排列
     */
    @Query("SELECT * FROM t03 WHERE c03 = :tagType AND c04 = 1 AND c07 = 0 ORDER BY c05 ASC")
    suspend fun getPredefinedTagsByType(tagType: Int): List<HealthTag>
    
    /**
     * 根据标签类型获取自定义标签（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 自定义标签列表的Flow，按创建时间升序排列
     */
    @Query("SELECT * FROM t03 WHERE c03 = :tagType AND c04 = 0 AND c07 = 0 ORDER BY c06 ASC")
    fun getCustomTagsByType(tagType: Int): Flow<List<HealthTag>>
    
    /**
     * 获取所有标签（过滤软删除）
     * @return 所有标签的Flow
     */
    @Query("SELECT * FROM t03 WHERE c07 = 0 ORDER BY c03 ASC, c04 DESC, c06 ASC")
    fun getAllTags(): Flow<List<HealthTag>>
    
    /**
     * 检查指定类型和名称的标签是否存在（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @param name 标签名称
     * @return 是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM t03 WHERE c03 = :tagType AND c02 = :name AND c07 = 0")
    suspend fun existsByTypeAndName(tagType: Int, name: String): Boolean
    
    /**
     * 获取指定类型的预定义标签数量（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 预定义标签数量
     */
    @Query("SELECT COUNT(*) FROM t03 WHERE c03 = :tagType AND c04 = 1 AND c07 = 0")
    suspend fun getPredefinedTagCount(tagType: Int): Int
    
    /**
     * 获取指定类型的标签总数（过滤软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 标签总数
     */
    @Query("SELECT COUNT(*) FROM t03 WHERE c03 = :tagType AND c07 = 0")
    suspend fun getTagCountByType(tagType: Int): Int
    
    /**
     * 删除指定类型的所有标签（软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("UPDATE t03 SET c07 = 1 WHERE c03 = :tagType AND c07 = 0")
    suspend fun softDeleteAllByType(tagType: Int): Int
    
    /**
     * 删除指定类型的自定义标签（软删除）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("UPDATE t03 SET c07 = 1 WHERE c03 = :tagType AND c04 = 0 AND c07 = 0")
    suspend fun softDeleteCustomTagsByType(tagType: Int): Int
    
    /**
     * 恢复软删除的标签
     * @param id 标签ID
     * @return 受影响的行数
     */
    @Query("UPDATE t03 SET c07 = 0 WHERE c01 = :id")
    suspend fun restoreById(id: Long): Int
    
    /**
     * 删除指定类型的所有标签（物理删除，保留原方法）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("DELETE FROM t03 WHERE c03 = :tagType")
    suspend fun deleteAllByType(tagType: Int): Int
    
    /**
     * 删除指定类型的自定义标签（物理删除，保留原方法）
     * @param tagType 标签类型的int值，参考 TagType 枚举
     * @return 删除的标签数量
     */
    @Query("DELETE FROM t03 WHERE c03 = :tagType AND c04 = 0")
    suspend fun deleteCustomTagsByType(tagType: Int): Int
    
    /**
     * 清空所有标签
     * @return 受影响的行数
     */
    @Query("DELETE FROM t03")
    suspend fun deleteAll(): Int
}

typealias HealthTagDao = LocalDao03
