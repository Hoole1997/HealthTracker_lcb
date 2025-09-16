package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 * 健康标签数据实体
 * 对应数据表：health_tags
 */
@Entity(tableName = "health_tags")
data class HealthTag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /**
     * 标签名称
     */
    @ColumnInfo(name = "name")
    val name: String,

    /**
     * 是否为预定义标签
     * 0 = 自定义标签，1 = 预定义标签
     */
    @ColumnInfo(name = "is_predefined")
    val isPreDefined: Int,

    /**
     * 创建时间
     */
    @ColumnInfo(name = "create_time")
    val createTime: Date
) {
    /**
     * 判断是否为预定义标签
     * @return true表示预定义标签
     */
    fun isPredefinedTag(): Boolean {
        return isPreDefined == 1
    }

    /**
     * 判断是否为自定义标签
     * @return true表示自定义标签
     */
    fun isCustomTag(): Boolean {
        return isPreDefined == 0
    }

    companion object {
        /**
         * 创建预定义标签的工厂方法
         * @param name 标签名称
         * @return 预定义标签实例
         */
        fun createPredefined(name: String): HealthTag {
            return HealthTag(
                name = name,
                isPreDefined = 1,
                createTime = Date()
            )
        }

        /**
         * 创建自定义标签的工厂方法
         * @param name 标签名称
         * @return 自定义标签实例
         */
        fun createCustom(name: String): HealthTag {
            return HealthTag(
                name = name,
                isPreDefined = 0,
                createTime = Date()
            )
        }

        /**
         * 获取所有预定义标签名称
         * @return 预定义标签名称列表
         */
        fun getPredefinedTagNames(): List<String> {
            return listOf(
                "兴奋状态",
                "沮丧",
                "平静的",
                "剧烈运动后",
                "有氧运动后",
                "无氧运动后",
                "节食",
                "酒后"
            )
        }

        /**
         * 创建所有预定义标签
         * @return 预定义标签实例列表
         */
        fun createAllPredefinedTags(): List<HealthTag> {
            return getPredefinedTagNames().map { name ->
                createPredefined(name)
            }
        }
    }
}