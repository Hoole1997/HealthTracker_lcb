package com.daily.health.manager.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.daily.health.manager.data.enums.TagType
import java.util.Date

/**
 * 统一的健康标签实体类
 * 用于存储血糖和血压的标签信息
 */
@Entity(tableName = "t03")
data class LocalEntity03(
    /**
     * 标签ID，自动生成
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "c01")
    val id: Long = 0,
    
    /**
     * 标签名称
     * 对于预定义标签，这个字段存储显示名称
     * 对于自定义标签，这个字段存储用户输入的名称
     */
    @ColumnInfo(name = "c02")
    val name: String,
    
    /**
     * 标签类型
     * 区分是血糖标签还是血压标签
     * 存储TagType枚举的value值: 0-血糖, 1-血压
     */
    @ColumnInfo(name = "c03")
    val tagType: Int,
    
    /**
     * 是否为预定义标签
     * true: 预定义标签（来自字符串资源）
     * false: 用户自定义标签
     */
    @ColumnInfo(name = "c04")
    val isPredefined: Boolean,
    
    /**
     * 预定义标签在字符串数组中的索引位置
     * 仅对预定义标签有效，自定义标签为null
     * 血糖标签: 0-7 (对应blood_sugar_labels数组)
     * 血压标签: 0-8 (对应blood_pressure_labels数组)
     */
    @ColumnInfo(name = "c05")
    val predefinedIndex: Int? = null,
    
    /**
     * 标签创建时间
     */
    @ColumnInfo(name = "c06")
    val createTime: Date = Date(),
    
    /**
     * 软删除标记
     * true: 已删除（软删除）
     * false: 未删除（正常状态）
     */
    @ColumnInfo(name = "c07")
    val isDeleted: Boolean = false
) {
    
    /**
     * 获取TagType枚举对象
     * @return 对应的TagType枚举
     */
    fun getTagTypeEnum(): TagType? {
        return TagType.fromValue(tagType)
    }
    
    /**
     * 判断是否为预定义标签
     * @return true if this is a predefined tag
     */
    fun isPredefinedTag(): Boolean = isPredefined
    
    /**
     * 判断是否为自定义标签
     * @return true if this is a custom tag
     */
    fun isCustomTag(): Boolean = !isPredefined
    
    companion object {
        
        /**
         * 创建预定义标签
         * @param name 标签名称
         * @param tagType 标签类型
         * @param index 在字符串数组中的索引位置
         * @return HealthTag实例
         */
        fun createPredefined(name: String, tagType: TagType, index: Int? = null): HealthTag {
            return LocalEntity03(
                name = name,
                tagType = tagType.value,
                isPredefined = true,
                predefinedIndex = index,
                createTime = Date()
            )
        }
        
        /**
         * 创建自定义标签
         * @param name 标签名称
         * @param tagType 标签类型
         * @return HealthTag实例
         */
        fun createCustom(name: String, tagType: TagType): HealthTag {
            return LocalEntity03(
                name = name,
                tagType = tagType.value,
                isPredefined = false,
                predefinedIndex = null,
                createTime = Date()
            )
        }
        
        /**
         * 创建HealthTag实例的便捷方法
         * @param id 标签ID
         * @param name 标签名称
         * @param tagType TagType枚举
         * @param isPredefined 是否为预定义标签
         * @param predefinedIndex 预定义索引
         * @param createTime 创建时间
         * @return HealthTag实例
         */
        fun create(
            id: Long = 0,
            name: String,
            tagType: TagType,
            isPredefined: Boolean,
            predefinedIndex: Int? = null,
            createTime: Date = Date()
        ): HealthTag {
            return LocalEntity03(
                id = id,
                name = name,
                tagType = tagType.value,
                isPredefined = isPredefined,
                predefinedIndex = predefinedIndex,
                createTime = createTime
            )
        }

    }
}

typealias HealthTag = LocalEntity03