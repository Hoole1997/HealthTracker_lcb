package com.healthtracker.framework.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * 数字格式化工具类
 * 提供智能的地区格式检测和格式化功能
 * 
 * 设计原则：
 * - 数据存储和传输使用英文格式（Locale.US）
 * - UI 显示可选择本地化格式
 * - 提供智能的格式检测辅助功能
 */
object NumberFormatter {
    
    /**
     * 检测字符串中使用的数字格式类型
     * 
     * @param input 输入字符串，如 "1,234.56" 或 "1.234,56"
     * @return 检测到的 Locale，默认返回 Locale.US
     */
   private fun detectLocale(input: String): Locale {
        if (input.isBlank()) return Locale.US
        
        val hasComma = input.contains(',')
        val hasDot = input.contains('.')
        
        return when {
            // 同时包含逗号和点：根据位置判断
            hasComma && hasDot -> {
                val commaIndex = input.lastIndexOf(',')
                val dotIndex = input.lastIndexOf('.')
                if (dotIndex > commaIndex) {
                    // "1,234.56" - 美式：逗号千分位，点小数
                    Locale.US
                } else {
                    // "1.234,56" - 欧式：点千分位，逗号小数
                    Locale.GERMANY
                }
            }
            
            // 仅包含逗号：根据位置判断
            hasComma -> {
                val commaIndex = input.lastIndexOf(',')
                val afterComma = input.length - commaIndex - 1
                if (afterComma <= 2) {
                    // "4,2" 或 "100,50" - 欧式小数
                    Locale.GERMANY
                } else {
                    // "1,234" - 美式千分位
                    Locale.US
                }
            }
            
            // 仅包含点：根据位置判断
            hasDot -> {
                val dotIndex = input.lastIndexOf('.')
                val afterDot = input.length - dotIndex - 1
                if (afterDot <= 2) {
                    // "4.2" 或 "100.50" - 美式小数
                    Locale.US
                } else {
                    // "1.234" - 欧式千分位
                    Locale.GERMANY
                }
            }
            
            // 默认美式
            else -> Locale.US
        }
    }
    
    /**
     * 根据输入格式自动匹配相应的 Locale 进行格式化
     * 
     * @param input 用户输入的字符串示例，用于检测格式
     * @param number 要格式化的数字
     * @param decimalPlaces 小数位数，默认 2 位
     * @return 格式化后的字符串
     */
    fun formatNumberBasedOnInput(
        input: String, 
        number: Double, 
        decimalPlaces: Int = 2
    ): String {
        return try {
            val locale = detectLocale(input)
            formatNumber(number, locale, decimalPlaces)
        } catch (e: Exception) {
            // 降级到英文格式
            formatNumber(number, Locale.US, decimalPlaces)
        }
    }
    
    /**
     * 使用指定 Locale 格式化数字
     * 
     * @param number 要格式化的数字
     * @param locale 地区设置
     * @param decimalPlaces 小数位数
     * @return 格式化后的字符串
     */
    fun formatNumber(
        number: Double, 
        locale: Locale = Locale.US, 
        decimalPlaces: Int = 2
    ): String {
        val pattern = buildString {
            append("#,##0")
            if (decimalPlaces > 0) {
                append(".")
                repeat(decimalPlaces) { append("0") }
            }
        }
        
        val symbols = DecimalFormatSymbols(locale)
        val format = DecimalFormat(pattern, symbols)
        return format.format(number)
    }
}