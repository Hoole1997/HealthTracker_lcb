package com.healthtracker.blood.suger.utils

import android.app.Application
import com.healthtracker.blood.suger.constants.KEY_INSIGHTS_DETAIL_READY
import com.healthtracker.blood.suger.constants.KEY_INSIGHTS_LIST_READY
import com.healthtracker.blood.suger.constants.KEY_NEWS_READY
import com.healthtracker.framework.BuildState
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.SpUtils
import java.io.File
import java.io.FileOutputStream

/**
 * 负责在首次启动时将 assets 中的压缩资源解压到私有目录
 */
object InsightAssetPreparer {

    private const val TAG = "InsightAssetPreparer"

    private data class AssetConfig(
        val assetName: String,
        val readyKey: String
    )

    private val assetConfigs = listOf(
        AssetConfig("insights_detail", KEY_INSIGHTS_DETAIL_READY),
        AssetConfig("insights_list", KEY_INSIGHTS_LIST_READY),
        AssetConfig("news", KEY_NEWS_READY)
    )

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

    private fun shouldExtract(
        application: Application,
        config: AssetConfig
    ): Boolean {
        val targetDir = File(application.filesDir, config.assetName)
        val ready = SpUtils.getBoolean(config.readyKey, false)
        return !ready || !targetDir.exists() || targetDir.list()?.isEmpty() == true
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
            SpUtils.putBoolean(config.readyKey, true)
        } catch (e: Exception) {
            SpUtils.putBoolean(config.readyKey, false)
            throw e
        } finally {
            tempZipFile.delete()
            if (tempExtractDir.exists()) {
                tempExtractDir.deleteRecursively()
            }
        }
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
