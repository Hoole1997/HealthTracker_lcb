package com.healthtracker.framework.util
import android.content.Context
import android.os.Environment
import android.os.HandlerThread
import com.healthtracker.framework.log.CustomDiskLogStrategy
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

object LogUtils{
    private var strategy:CustomDiskLogStrategy? = null
    private val handlerThread = HandlerThread("log")
    fun init(context: Context){
        handlerThread.start()
        strategy = CustomDiskLogStrategy(context,handlerThread.looper)

    }




    fun log(content: String?) {
        if (content.isNullOrEmpty()) return
        strategy?.log(CustomDiskLogStrategy.LOG, content)

    }

    fun logData( content: String?) {
        if (content.isNullOrEmpty()) return
        strategy?.log(CustomDiskLogStrategy.LOG_DATA, content)
    }

    fun getExternalLogFolder(): String {
        return Environment.getExternalStorageDirectory().toString() + "/Music/crash/"
    }


    fun logException( e: Throwable, uncatched: Boolean) {
        val stringWriter = StringWriter()
        PrintWriter(stringWriter).use { writer ->
            e.printStackTrace(writer)
        }
        val buffer = stringWriter.toString()
        val exceptionType = if (uncatched) "uncatched" else "catched"
        strategy?.log(CustomDiskLogStrategy.LOG_EXCEPTION, "\r\nException: $exceptionType\r\n$buffer")
    }

    fun initLogFolder(context: Context): String {
        val file = File(context.cacheDir, "log")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file.absolutePath
    }


}