package com.healthtracker.blood.suger.utils

import android.app.Application
import android.content.Context
import android.webkit.WebView
import com.healthtracker.framework.util.isLeast9
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

object WebViewZygote {


    private const val DEFAULT_PROCESS_NAME = "app_main:webview"
    private const val DEFAULT_PROCESS_FILE_NAME = "app_webview/webview_data.lock"

    fun webViewCompact(context: Context,applicationScope: CoroutineScope) {
        try {
            if (isLeast9()) {
                try {
                    val packageName: String = context.packageName
                    val processName = Application.getProcessName()
                    if (packageName != processName) {
                        WebView.setDataDirectorySuffix(if (processName.isNullOrEmpty()) DEFAULT_PROCESS_NAME else processName)
                    } else {
                        applicationScope.launch { tryLockOrRecreateFile(context) }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * 多进程webView 公用一个资源目录
     *  上锁
     *  @param context Context
     */
    private fun tryLockOrRecreateFile(context: Context) {
        try {
            val file = File(context.dataDir, DEFAULT_PROCESS_FILE_NAME)
            if (file.exists()) {
                try {
                    RandomAccessFile(file, "rw").channel.tryLock()?.close() ?: FileUtil.createFileByDeleteOldFile(file)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    try {
                        FileUtil.createFileByDeleteOldFile(file)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}
