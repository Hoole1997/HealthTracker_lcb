package com.healthtracker.blood.suger.data.dao

import androidx.room.*
import com.healthtracker.blood.suger.data.entity.HealthTag
import kotlinx.coroutines.flow.Flow

/**
 * 健康标签数据访问接口
 */
@Dao
interface HealthTagDao {

    /**
     * 插入标签
     * @param tag 标签实体
     * @return 插入的标签ID
     */
    @Insert
    suspend fun insert(tag: HealthTag): Long

    /**
     * 批量插入标签
     * @param tags 标签列表
     * @return 插入的标签ID列表
     */
    @Insert
    suspend fun insertAll(tags: List<HealthTag>): List<Long>

    /**
     * 更新标签
     * @param tag 标签实体
     */
    @Update
    suspend fun update(tag: HealthTag)


    /**
     * 根据ID获取标签
     * @param tagId 标签ID
     * @return 标签实体
     */
    @Query("SELECT * FROM health_tags WHERE id = :tagId")
    suspend fun getById(tagId: Long): HealthTag?

    /**
     * 根据ID列表获取标签
     * @param tagIds 标签ID列表
     * @return 标签列表
     */
    @Query("SELECT * FROM health_tags WHERE id IN (:tagIds)")
    suspend fun getByIds(tagIds: List<Long>): List<HealthTag>

    /**
     * 获取所有标签
     * @return 标签流
     */
    @Query("SELECT * FROM health_tags WHERE is_delete = 0 ORDER BY id ASC")
    fun getAllTags(): Flow<List<HealthTag>>

    /**
     * 根据名称查找标签
     * @param name 标签名称
     * @return 标签实体
     */
    @Query("SELECT * FROM health_tags WHERE name = :name AND is_predefined = 0 LIMIT 1")
    suspend fun getCustomByName(name: String): HealthTag?


    /**
     * 根据名称查找标签
     * @param name 标签名称
     * @return 标签实体
     */
    @Query("SELECT * FROM health_tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): HealthTag?


    /**
     * 检查标签名称是否已存在
     * @param name 标签名称
     * @return 是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM health_tags WHERE name = :name AND is_predefined = 0")
    suspend fun isNameExists(name: String): Boolean

    /**
     * 获取自定义标签数量
     * @return 自定义标签数量
     */
    @Query("SELECT COUNT(*) FROM health_tags WHERE is_predefined = 0")
    suspend fun getCustomTagCount(): Int

//    /**
//     * 获取使用频率最高的标签（基于记录关联情况）
//     * 注意：这个查询比较复杂，实际使用时可能需要在业务层实现
//     */
//    @Query("""
//        SELECT h.* FROM health_tags h
//        WHERE h.id IN (
//            SELECT DISTINCT tag_id FROM (
//                SELECT CAST(SUBSTR(tag_ids, 1, INSTR(tag_ids || ',', ',') - 1) AS INTEGER) as tag_id
//                FROM blood_sugar_records
//                WHERE tag_ids IS NOT NULL AND tag_ids != ''
//                UNION ALL
//                SELECT CAST(SUBSTR(tag_ids, 1, INSTR(tag_ids || ',', ',') - 1) AS INTEGER) as tag_id
//                FROM blood_pressure_records
//                WHERE tag_ids IS NOT NULL AND tag_ids != ''
//            )
//        )
//        ORDER BY h.is_predefined DESC, h.id ASC
//        LIMIT :limit
//    """)
//    suspend fun getMostUsedTags(limit: Int = 10): List<HealthTag>

//    /**
//     * 搜索标签（按名称模糊匹配）
//     * @param keyword 搜索关键词
//     * @return 匹配的标签列表
//     */
//    @Query("SELECT * FROM health_tags WHERE name LIKE '%' || :keyword || '%' ORDER BY is_predefined DESC, id ASC")
//    suspend fun searchTags(keyword: String): List<HealthTag>
}