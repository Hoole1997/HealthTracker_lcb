package com.healthtracker.blood.suger.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.R
import java.util.*

/**
 * 血压标签数据实体
 * 对应数据表：blood_pressure_tags
 */
@Entity(tableName = "blood_pressure_tags")
data class BloodPressureTag(
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


    @ColumnInfo(name = "is_delete")
    val isDelete:Int
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
         * 创建预定义血压标签的工厂方法
         * @param name 标签名称
         * @return 预定义血压标签实例
         */
        fun createPredefined(name: String): BloodPressureTag {
            return BloodPressureTag(
                name = name,
                isPreDefined = 1,
                isDelete = 0

            )
        }

        /**
         * 创建自定义血压标签的工厂方法
         * @param name 标签名称
         * @return 自定义血压标签实例
         */
        fun createCustom(name: String): BloodPressureTag {
            return BloodPressureTag(
                name = name,
                isPreDefined = 0,
                isDelete = 0
            )
        }

        /**
         * 获取所有预定义血压标签名称
         * @return 预定义血压标签名称列表
         */
        fun getPredefinedTagNames() = App.INSTANCE.resources.getStringArray(R.array.blood_pressure_labels)

        /**
         * 创建所有预定义血压标签
         * @return 预定义血压标签实例列表
         */
        fun createAllPredefinedTags(): List<BloodPressureTag> {
            return getPredefinedTagNames().map { name ->
                createPredefined(name)
            }
        }
    }
}