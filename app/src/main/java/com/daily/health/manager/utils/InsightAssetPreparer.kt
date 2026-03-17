package com.daily.health.manager.utils

import android.app.Application
import com.daily.health.manager.App
import com.daily.health.manager.constants.KEY_INSIGHTS_LIST_READY
import com.daily.health.manager.constants.KEY_NEWS_READY
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.LanguageUtils
import com.healthtracker.framework.util.SpUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import org.json.JSONArray

/**
 * 负责在首次启动时将 assets 中的压缩资源解压到私有目录
 */
object InsightAssetPreparer {

    private const val TAG = "InsightAssetPreparer"
    private const val DIR_INSIGHTS_LIST = "insights_list"
    private const val DIR_NEWS = "insights_content"
    private const val INSIGHTS_LIST_ASSET_VERSION = 2
    private const val INSIGHTS_CONTENT_ASSET_VERSION = 1

    private data class AssetConfig(
        val assetName: String,
        val readyKey: String,
        val version: Int
    ) {
        val versionKey: String = "${readyKey}_version"
    }

    data class InsightArticle(
        val title: String,
        val content: String,
        val articleId: String,
        val listImagePath: String,
    )

    private val assetConfigs = listOf(
        // Bump the asset version when bundled files change so installed apps re-extract them.
        AssetConfig(DIR_INSIGHTS_LIST, KEY_INSIGHTS_LIST_READY, INSIGHTS_LIST_ASSET_VERSION),
        AssetConfig(DIR_NEWS, KEY_NEWS_READY, INSIGHTS_CONTENT_ASSET_VERSION)
    )

    private val articleCache = mutableMapOf<String, List<InsightArticle>>()
    private var applicationContext  = App.INSTANCE

    fun prepare(application: Application) {
        assetConfigs.forEach { config ->
            runCatching {
                if (shouldExtract(application, config)) {
                    extractAsset(application, config)
                }
            }.onFailure {
                if (BuildState.debug) {
                    "prepare ${config.assetName} failed: ${it.message}".loge(TAG)
                }
            }
        }
    }

    /**
     * 获取指定分类的图文配置
     */
    fun getArticles(category: String): List<InsightArticle> {
        val savedLang = LanguageUtils.getSavedLanguage()
        val appLocale = LanguageUtils.getAppLocale(applicationContext)
        val language = appLocale?.language ?: "en"
        
        if (BuildState.debug) {
            "getArticles: category=$category, savedLang=$savedLang, appLocale=$appLocale, language=$language".loge(TAG)
        }

        val cacheKey = "${category}_$language"
        articleCache[cacheKey]?.let { return it }
        val file = File(applicationContext.filesDir, "$DIR_NEWS/insights_${category}_data.json")
        if (!file.exists()) {
            return emptyList()
        }
        return runCatching {
            parseArticles(file, language)
        }.onSuccess {
            articleCache[cacheKey] = it
        }.getOrElse {
            if (BuildState.debug) {
                "Parse ${file.name} failed: ${it.message}".loge(TAG)
            }
            emptyList()
        }
    }


    private fun shouldExtract(
        application: Application,
        config: AssetConfig
    ): Boolean {
        val targetDir = File(application.filesDir, config.assetName)
        val ready = SpUtils.getBoolean(config.readyKey, false)
        val currentVersion = SpUtils.getInt(config.versionKey, 0)
        return !ready ||
            currentVersion != config.version ||
            !targetDir.exists() ||
            targetDir.list()?.isEmpty() == true
    }

    private fun extractAsset(
        application: Application,
        config: AssetConfig
    ) {
        val tempZipFile = File(application.filesDir, "${config.assetName}.zip")
        val tempExtractDir = File(application.filesDir, "tmp_${config.assetName}")
        val targetDir = File(application.filesDir, config.assetName)
        try {
            if (tempZipFile.exists()) {
                tempZipFile.delete()
            }
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
            application.assets.open(config.assetName).use { input ->
                FileOutputStream(tempZipFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileUtil.createOrExistsDir(tempExtractDir)
            FileUtil.decompressFile(tempExtractDir.absolutePath, tempZipFile.absolutePath)
            moveExtractedContent(tempExtractDir, targetDir, config.assetName)
            if (config.assetName == DIR_NEWS) {
                articleCache.clear()
            }
            SpUtils.putBoolean(config.readyKey, true)
            SpUtils.putInt(config.versionKey, config.version)
        } catch (e: Exception) {
            SpUtils.putBoolean(config.readyKey, false)
            SpUtils.putInt(config.versionKey, 0)
            throw e
        } finally {
            tempZipFile.delete()
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
        }
    }

    private fun parseArticles(file: File, language: String): List<InsightArticle> {
        val jsonArray = JSONArray(file.readText())
        val result = mutableListOf<InsightArticle>()
        for (index in 0 until jsonArray.length()) {
            val obj = jsonArray.optJSONObject(index) ?: continue
            val contentObj = obj.optJSONObject(language) ?: obj.optJSONObject("en")
            result.add(
                InsightArticle(
                    title = contentObj?.optString("title") ?: "",
                    content = contentObj?.optString("content") ?: "",
                    articleId = obj.optString("article_id"),
                    listImagePath = resolveAssetPath(obj.optString("list_image")),
                )
            )
        }
        return result
    }

    private fun resolveAssetPath(relative: String?): String {
        val application = applicationContext ?: return ""
        if (relative.isNullOrBlank()) return ""
        var normalized = relative.trim()
        if (normalized.startsWith("insights/")) {
            normalized = normalized.removePrefix("insights/")
        }
        normalized = normalized.removePrefix("/")
        val primary = File(application.filesDir, normalized)
        if (primary.exists()) {
            return primary.absolutePath
        }
        if (normalized.endsWith(".png", ignoreCase = true)) {
            val webpCandidate = File(
                application.filesDir,
                normalized.dropLast(4) + ".webp"
            )
            if (webpCandidate.exists()) {
                return webpCandidate.absolutePath
            }
        }
        return primary.absolutePath
    }

    private fun resolveCategoryAssets(dirName: String, category: String): List<String> {
        val application = applicationContext ?: return emptyList()
        val dir = File(application.filesDir, "$dirName/$category")
        if (!dir.exists()) {
            return emptyList()
        }
        return dir.listFiles { file -> file.isFile }
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    private fun moveExtractedContent(tempDir: File, targetDir: File, assetName: String) {
        if (!tempDir.exists()) return
        val nestedDir = File(tempDir, assetName)
        if (nestedDir.exists()) {
            replaceDirectory(targetDir, nestedDir)
        } else {
            replaceDirectory(targetDir, tempDir)
        }
    }

    private fun replaceDirectory(targetDir: File, sourceDir: File) {
        if (targetDir.exists()) {
            targetDir.deleteRecursively()
        }
        if (!sourceDir.renameTo(targetDir)) {
            sourceDir.copyRecursively(targetDir, overwrite = true)
            sourceDir.deleteRecursively()
        }
    }
}
