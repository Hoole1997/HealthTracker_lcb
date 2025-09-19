package com.healthtracker.blood.suger.data.dao

import androidx.room.*
import com.healthtracker.blood.suger.data.entity.BloodPressureTag
import kotlinx.coroutines.flow.Flow

/**
 * 血压标签数据访问接口
 */
@Dao
interface BloodPressureTagDao {

    /**
     * 插入血压标签
     * @param tag 血压标签实体
     * @return 插入的标签ID
     */
    @Insert
    suspend fun insert(tag: BloodPressureTag): Long

    /**
     * 批量插入血压标签
     * @param tags 血压标签列表
     * @return 插入的标签ID列表
     */
    @Insert
    suspend fun insertAll(tags: List<BloodPressureTag>): List<Long>

    /**
     * 更新血压标签
     * @param tag 血压标签实体
     */
    @Update
    suspend fun update(tag: BloodPressureTag)


    /**
     * 根据ID获取血压标签
     * @param tagId 标签ID
     * @return 血压标签实体
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE id = :tagId")
    suspend fun getById(tagId: Long): BloodPressureTag?

    /**
     * 根据ID列表获取血压标签
     * @param tagIds 标签ID列表
     * @return 血压标签列表
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE id IN (:tagIds)")
    suspend fun getByIds(tagIds: List<Long>): List<BloodPressureTag>

    /**
     * 获取所有血压标签
     * @return 血压标签流
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE is_delete = 0 ORDER BY id ASC")
    fun getAllTags(): Flow<List<BloodPressureTag>>

    /**
     * 根据名称查找血压标签
     * @param name 标签名称
     * @return 血压标签实体
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE name = :name AND is_predefined = 0 LIMIT 1")
    suspend fun getCustomByName(name: String): BloodPressureTag?


    /**
     * 根据名称查找血压标签
     * @param name 标签名称
     * @return 血压标签实体
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): BloodPressureTag?


    /**
     * 检查血压标签名称是否已存在
     * @param name 标签名称
     * @return 是否存在
     */
    @Query("SELECT COUNT(*) > 0 FROM blood_pressure_tags WHERE name = :name AND is_predefined = 0")
    suspend fun isNameExists(name: String): Boolean

    /**
     * 获取自定义血压标签数量
     * @return 自定义血压标签数量
     */
    @Query("SELECT COUNT(*) FROM blood_pressure_tags WHERE is_predefined = 0")
    suspend fun getCustomTagCount(): Int

    /**
     * 获取所有预定义标签
     * @return 预定义标签流
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE is_predefined = 1 AND is_delete = 0 ORDER BY id ASC")
    fun getPredefinedTags(): Flow<List<BloodPressureTag>>

    /**
     * 获取所有自定义标签
     * @return 自定义标签流
     */
    @Query("SELECT * FROM blood_pressure_tags WHERE is_predefined = 0 AND is_delete = 0 ORDER BY id ASC")
    fun getCustomTags(): Flow<List<BloodPressureTag>>

    /**
     * 根据ID删除标签
     * @param tagId 标签ID
     * @return 删除的行数
     */
    @Query("DELETE FROM blood_pressure_tags WHERE id = :tagId")
    suspend fun deleteById(tagId: Long): Int

//    /**
//     * 获取使用频率最高的标签（基于记录关联情况）
//     * 注意：这个查询比较复杂，实际使用时可能需要在业务层实现
//     */
//    @Query("""
//        SELECT h.* FROM blood_pressure_tags h
//        WHERE h.id IN (
//            SELECT DISTINCT tag_id FROM (
//                SELECT CAST(SUBSTR(tag_ids, 1, INSTR(tag_ids || ',', ',') - 1) AS INTEGER) as tag_id
//                FROM blood_pressure_records
//                WHERE tag_ids IS NOT NULL AND tag_ids != ''
//            )
//        )
//        ORDER BY h.is_predefined DESC, h.id ASC
//        LIMIT :limit
//    """)
//    suspend fun getMostUsedTags(limit: Int = 10): List<BloodPressureTag>

//    /**
//     * 搜索标签（按名称模糊匹配）
//     * @param keyword 搜索关键词
//     * @return 匹配的标签列表
//     */
//    @Query("SELECT * FROM blood_pressure_tags WHERE name LIKE '%' || :keyword || '%' ORDER BY is_predefined DESC, id ASC")
//    suspend fun searchTags(keyword: String): List<BloodPressureTag>
}