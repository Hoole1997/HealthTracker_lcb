package com.healthtracker.framework.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.core.os.ConfigurationCompat
import com.healthtracker.framework.ext.logd
import java.util.Locale

// http://doc.web.inter.appsinnova.com/web/#/4?page_id=32
object LanguageUtils {
    private const val DEFAULT_LANGUAGE = "en"
    private const val KEY_APP_LANGUAGE = "key_app_language"

    class LangBean(
        val id: String,
        val displayName: String,
        private val sysLang: String = "",
        private val country: String = "",
    ) {
        var select: Boolean = false

        fun equals(locale: Locale) =
            locale.language == sysLang && (country.isEmpty() || locale.country == country)
    }

    fun attachBaseContext(context: Context?): Context? {
        if (context == null) {
            return null
        }

        val settingsLocale = mapAppLocale() ?: LocaleList.getDefault()[0]
        val resources = context.resources
        val config = resources.configuration

        setLocal(config, settingsLocale)

        return context.createConfigurationContext(config)
    }

    private val LanguageMap = listOf(
        LangBean("en", "English", "en"),
        LangBean("es", "Español", "es"),
        LangBean("pt_BR", "Português", "pt", "BR"),
        LangBean("de", "Deutsch", "de"),
        LangBean("fr", "Français", "fr"),
        LangBean("id", "Indonesian", "in", "ID"),
        LangBean("fil", "Filipino", "fil", "PH"),
//        LangBean("zh_CN", "简体中文", "zh", "CN"),
//        LangBean("zh_TW", "繁體中文", "zh", "TW"),
        LangBean("ja", "日本語", "jp"),
        LangBean("ko", "한국어", "ko"),

        LangBean("tr", "Türkçe", "tr"),
        LangBean("hi", "हिंदी", "hi", "IN"),
        LangBean("vi", "Việt", "vi"),
        LangBean("th", "ภาษาไทย", "th"),
        LangBean("ar", "العربية", "ar"),
    )

    private fun setLocal(configuration: Configuration, locale: Locale) =
        configuration.setLocale(locale)

    private fun getSaveLanguage() = SpUtils.getString(KEY_APP_LANGUAGE)

    private fun getSystemLocale(): Locale? {

        LocaleList.getDefault()[0]?.run {
            return this
        }
        try {
            ConfigurationCompat.getLocales(Resources.getSystem().configuration).run {
                if (size() > 0) {
                    return this[0]
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getContextLocale(context: Context) = context.resources.configuration.locales[0]

    fun getAppLanguage(): String {
        var savedLang = getSaveLanguage()
        "getAppLanguage.savedLang1:$savedLang".logd("LanguageUtils")
        if (savedLang.isEmpty()) {
            savedLang = getSystemLocale()?.let { locale ->
                "getAppLanguage.locale:${locale.language}".logd("LanguageUtils")
                LanguageMap.find {
                    it.equals(locale) || (it.id == "pt_BR" && locale.language.equals("pt"))
                }?.id
            } ?: DEFAULT_LANGUAGE
            "getAppLanguage.savedLang2:$savedLang".logd("LanguageUtils")
        }
        return savedLang
    }

    fun setAppLanguage(lang: String) {
        SpUtils.putString(KEY_APP_LANGUAGE, lang)
    }

    fun getLanguageLocal() = mapAppLocale() ?: Locale.ENGLISH

    private fun mapAppLocale(): Locale? {
        return when (getSaveLanguage()) {
            "ar" -> Locale("ar")
            "de" -> Locale.GERMANY
            "en" -> Locale.ENGLISH
            "es" -> Locale("es")
            "fr" -> Locale.FRENCH
//            "it" -> Locale("it")
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "vi" -> Locale("vi")
            "th" -> Locale("th")
            "tr" -> Locale("tr")
            "id" -> Locale("in", "ID")
            "fil" -> Locale("fil", "PH")
            "hi" -> Locale("hi", "IN")
            "pt_BR" -> Locale("pt", "BR")
            "zh_CN" -> Locale.SIMPLIFIED_CHINESE
            "zh_TW" -> Locale.TAIWAN
            else -> null
        }
    }

    fun getLanguageList(): MutableList<LangBean> {
        val displayList = LanguageMap.toMutableList()
        val language = getAppLanguage()
        if (displayList.first().id != language) {
            displayList.find { it.id == language }?.run {
                displayList.remove(this)
                displayList.add(0, this)
            }
        }
        return displayList
    }

    /**
     * 语言代码是否是 右对齐语言
     *
     * @param context
     * @return
     */
    fun isRtlLanguage(context: Context): Boolean {
        val locale =  context.resources.configuration.locales.get(0)
        val languageCode = locale.language.lowercase(locale)
        return when (languageCode) {
            "ar", "fa", "iw", "ur" -> true // "he"
            else -> false
        }
    }
}