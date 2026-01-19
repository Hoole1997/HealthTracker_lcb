package com.healthtracker.framework.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LanguageUtils {

    private const val KEY_APP_LANGUAGE = "key_app_language"
    class LangBean(
        val id: String,
        val displayName: String,
        val sysLang: String = "",
        val country: String = ""
    ) {
        /**
         * 检查指定的 Locale 是否匹配此语言配置
         * @param locale 要检查的 Locale
         * @return 如果匹配返回 true
         */
        fun matches(locale: Locale): Boolean {
            val language = locale.language
            // 印度尼西亚语特殊处理：ISO 639-1 使用 "id"，但 Java 使用 "in"
            val lang = if (language == "id") "in" else language
            return lang == sysLang && (country.isEmpty() || locale.country == country)
        }
    }

    val LanguageMap = listOf(
        LangBean("en", "English", "en"),
        LangBean("es", "Español", "es"),  // 西班牙语
        LangBean("pt_BR", "Português (Brasil)", "pt", "BR"),  // 巴西葡萄牙语
//        LangBean("de", "Deutsch", "de"),
//        LangBean("fr", "Français", "fr"),
//        LangBean("id", "Indonesian", "in", "ID"),   // 印度尼西亚语
//        LangBean("it", "Italiano", "it"),   // 	意大利语
        LangBean("ja", "日本語", "ja"),
        LangBean("ko", "한국어", "ko"),
        LangBean("hi", "हिंदी", "hi", "IN"),  // 印地语
        LangBean("tr", "Türkçe", "tr"),  // 土耳其语
//        LangBean("ru", "Русский", "ru"), // 俄语
//        LangBean("th", "ไทย", "th"), // 泰语
//        LangBean("tl", "Filipino", "fil", "PH"), // 菲律宾语
//        LangBean("vn", "Tiếng Việt", "vi"), // 越南语
//        LangBean("bn", "বাংলা", "bn"), // 孟加拉语
//        LangBean("ms", "Melayu", "ms"), // 马来语
//        LangBean("ar", "العربية", "ar"), // 阿拉伯语
//        LangBean("ur", "اردو", "ur"), // 乌尔都语
//        LangBean("uk", "Українська", "uk"),
//        LangBean("zh_TW", "繁體中文", "zh", "TW"), // 繁體中文
//        LangBean("el", "Ελληνικά", "el"), // 希腊语
//        LangBean("cs", "Čeština", "cs"), // 捷克语
//        LangBean("hu", "Magyar", "hu"), // 匈牙利语
//        LangBean("fa", "فارسی", "fa"), // 波斯语
    )

    const val defaultLanguage = "en"


    /**
     * 获取 Context 的当前 Locale
     * @param context 上下文
     * @return 当前 Locale，对于 API 24+ 返回第一个 Locale
     */
    fun getContextLocale(context: Context): Locale? {
        return context.resources.configuration.locales[0]
    }

    /**
     * 获取系统默认 Locale
     * @return 系统 Locale
     */
    fun getSystemLocale(): Locale? {
        return LocaleList.getDefault()[0]
    }

    /**
     * 获取应用当前语言设置
     * @param context 上下文
     * @return 语言 ID，如果未设置则返回系统语言或默认语言
     */
    fun getAppLanguage(context: Context): String {
        val savedLang = getSavedLanguage()
        return if (savedLang.isNullOrEmpty()) {
            getContextLocale(context)?.let { locale ->
                LanguageMap.find { it.matches(locale) }?.id
            } ?: defaultLanguage
        } else {
            savedLang
        }
    }

    fun getSavedLanguage(): String {
        return SpUtils.getString(KEY_APP_LANGUAGE)
    }


    fun setAppLanguage(lang: String) {
        SpUtils.putString(KEY_APP_LANGUAGE, lang)
    }

    /**
     * 为 Context 应用语言设置
     * 在 Activity/Application 的 attachBaseContext() 中调用
     * @param context 基础 Context
     * @return 应用了语言设置的新 Context
     */
    fun attachBaseContext(context: Context?): Context? {
        if (context == null) return null
        
        val settingsLocale = mapAppLocale() ?: return context
        val resources = context.resources
        val config = resources.configuration
        config.setLocale(settingsLocale)
        
        return context.createConfigurationContext(config)
    }



    /**
     * 获取应用的 Locale 对象
     * @param context 上下文
     * @return Locale 对象
     */
    fun getAppLocale(context: Context): Locale = mapAppLocale() ?: getContextLocale(context) ?: Locale.ENGLISH

    /**
     * 将保存的语言 ID 映射为 Locale 对象
     * @return Locale 对象，如果未设置语言则返回 null
     */
    fun mapAppLocale(): Locale? {
        return when (getSavedLanguage()) {
            "ar" -> Locale.Builder().setLanguage("ar").build()  // 阿拉伯语
            "de" -> Locale.GERMANY  // 德语
            "en" -> Locale.ENGLISH  // 英语
            "es" -> Locale.Builder().setLanguage("es").build()  // 西班牙语
            "fr" -> Locale.FRENCH  // 法语
            "hi" -> Locale.Builder().setLanguage("hi").setRegion("IN").build()  // 印地语
            "it" -> Locale.ITALIAN  // 意大利语
            "id" -> Locale.Builder().setLanguage("in").setRegion("ID").build()  // 印度尼西亚语
            "ja" -> Locale.JAPANESE  // 日语
            "ko" -> Locale.KOREAN  // 韩语
            "ms" -> Locale.Builder().setLanguage("ms").build()  // 马来语
            "ru" -> Locale.Builder().setLanguage("ru").build()  // 俄语
            "tr" -> Locale.Builder().setLanguage("tr").build()  // 土耳其语
            "th" -> Locale.Builder().setLanguage("th").build()  // 泰语
            "tl" -> Locale.Builder().setLanguage("fil").setRegion("PH").build()  // 菲律宾语
            "bn" -> Locale.Builder().setLanguage("bn").build()  // 孟加拉语
            "pt_BR" -> Locale.Builder().setLanguage("pt").setRegion("BR").build()  // 巴西葡萄牙语
            "vn" -> Locale.Builder().setLanguage("vi").build()  // 越南语
            "ur" -> Locale.Builder().setLanguage("ur").build()  // 乌尔都语
            "uk" -> Locale.Builder().setLanguage("uk").build()  // 乌克兰语
            "zh_TW" -> Locale.TAIWAN  // 繁体中文
            "el" -> Locale.Builder().setLanguage("el").build()  // 希腊语
            "cs" -> Locale.Builder().setLanguage("cs").build()  // 捷克语
            "hu" -> Locale.Builder().setLanguage("hu").build()  // 匈牙利语
            "fa" -> Locale.Builder().setLanguage("fa").build()  // 波斯语
            else -> null
        }
    }
    /**
     * 获取语言列表，当前选中的语言会排在第一位
     * @param context 上下文
     * @return 语言列表（不可变）
     */
    fun getLanguageList(context: Context): List<LangBean> {
        val language = getAppLanguage(context)
        return if (LanguageMap.first().id == language) {
            LanguageMap
        } else {
            LanguageMap.find { it.id == language }?.let { currentLang ->
                listOf(currentLang) + LanguageMap.filter { it.id != language }
            } ?: LanguageMap
        }
    }

}