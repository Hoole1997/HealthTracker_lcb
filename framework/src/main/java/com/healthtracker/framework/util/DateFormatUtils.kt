package com.healthtracker.framework.util

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日期格式化工具类
 * 提供多语言支持的日期格式获取
 */
object DateFormatUtils {

    /**
     * 返回一个年份+月份+日期的格式
     * 例如：MMM d, yyyy (en)
     */
    fun getLocaleDateFormatYMD(locale: Locale): SimpleDateFormat {
        val lang = locale.language
        val format = when (lang) {
            "fr", "it", "es", "th", "in", "el", "uk", "fa", "nl", "ro", "my", "sq", "hi", "iw", "sv" -> "d MMM yyyy"
            "de", "da", "nb", "fi" -> "d. MMM yyyy"
            "ko" -> "yyyy년 M월 d일"
            "ja", "zh" -> "yyyy年M月d日"
            "ar", "ur" -> "d MMM، yyyy"
            "ru", "mk" -> "d MMM yyyy 'г.'"
            "tr" -> "dd MMM yyyy"
            "pt" -> if (locale.country == "PT") "d/MM/yyyy" else "d 'de' MMM 'de' yyyy"
            "sr", "hr" -> "d. MMM yyyy."
            "bg" -> "d.MM.yyyy 'г.'"
            "pl" -> "d.MM.yyyy"
            "sk", "cs" -> "d. M. yyyy"
            "hu" -> "yyyy. MMM d."
            "vi" -> "dd MMM, yyyy"
            else -> "MMM d, yyyy" // 默认 en
        }
        return SimpleDateFormat(format, locale)
    }

    /**
     * 返回一个年份的日期格式
     * 例如：yyyy (en)
     */
    fun getLocaleDateFormatY(locale: Locale): SimpleDateFormat {
        val lang = locale.language
        val format = when (lang) {
            "ko" -> "yyyy년"
            "ja", "zh" -> "yyyy年"
            else -> "yyyy"
        }
        return SimpleDateFormat(format, locale)
    }

    /**
     * 返回一个年份+月份的日期格式
     * 例如：MMM yyyy (en)
     */
    fun getLocaleDateFormatYM(locale: Locale): SimpleDateFormat {
        val lang = locale.language
        val format = when (lang) {
            "fr", "it", "de", "es", "th", "ar", "in", "tr", "el", "uk", "fa", "nl", "sk", "da", "ro", "my", "sq", "vi", "hi", "iw", "ur", "sv", "nb" -> "MMM yyyy"
            "ko" -> "yyyy년 M월"
            "ja", "zh" -> "yyyy年M月"
            "ru", "pl" -> "MM.yyyy"
            "bg" -> "MM.yyyy 'г.'"
            "pt" -> if (locale.country == "PT") "MM/yyyy" else "MMM 'de' yyyy"
            "sr", "hr" -> "MMM yyyy."
            "hu" -> "yyyy. MMM"
            "mk" -> "MMM yyyy 'г.'"
            "cs", "fi" -> "M. yyyy"
            else -> "MMM yyyy" // 默认 en
        }
        return SimpleDateFormat(format, locale)
    }

    /**
     * 返回一个月份和日期的日期格式
     * 例如：MMM d (en)
     */
    fun getLocaleDateFormatMD(locale: Locale): SimpleDateFormat {
        val lang = locale.language
        val format = when (lang) {
            "fr", "it", "es", "th", "ar", "ru", "in", "tr", "el", "uk", "fa", "nl", "ro", "my", "sq", "vi", "mk", "hi", "iw", "ur", "sv" -> "d MMM"
            "de", "da", "nb", "fi" -> "d. MMM"
            "ko" -> "M월 d일"
            "ja", "zh" -> "M月d日"
            "pt" -> if (locale.country == "PT") "d/MM" else "d 'de' MMM"
            "sr", "hr" -> "d. MMM"
            "bg", "pl" -> "d.MM"
            "sk" -> "d. M"
            "hu" -> "MMM d."
            "cs" -> "d. M."
            else -> "MMM d" // 默认 en
        }
        return SimpleDateFormat(format, locale)
    }

    /**
     * 获取12小时制时间格式（带 AM/PM）
     * 例如：1:36 PM (en), 下午1:36 (zh)
     *
     * @param locale 目标语言环境
     * @param fallbackToEnglish 是否在语言资源缺失时降级到英文格式（默认 true）
     */
    fun getLocaleTimeFormat12H(
        locale: Locale,
        fallbackToEnglish: Boolean = true
    ): SimpleDateFormat {
        val lang = locale.language
        val format = when (lang) {
            "zh" -> "ahh:mm"  // 下午1:36
            "ja" -> "ahh:mm"  // 午後1:36
            "ko" -> "a h:mm"  // 오후 1:36
            else -> "h:mm a"  // 1:36 PM
        }

        return try {
            SimpleDateFormat(format, locale).apply {
                // 验证 locale 是否可用
                this.format(Date())
            }
        } catch (e: Exception) {
            // 降级到英文格式
            if (fallbackToEnglish) {
                SimpleDateFormat("h:mm a", Locale.US)
            } else {
                throw e
            }
        }
    }

    /**
     * 获取24小时制时间格式（不带 AM/PM）
     * 例如：13:36
     */
    fun getLocaleTimeFormat24H(locale: Locale): SimpleDateFormat {
        return SimpleDateFormat("HH:mm", locale)
    }

    /**
     * 根据系统设置自动选择12/24小时制
     */
    fun getLocaleTimeFormatAuto(context: Context, locale: Locale): SimpleDateFormat {
        return if (DateFormat.is24HourFormat(context)) {
            getLocaleTimeFormat24H(locale)
        } else {
            getLocaleTimeFormat12H(locale)
        }
    }
}
