package com.healthtracker.framework.log

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomDiskLogStrategy(private val context: Context, private val looper: Looper){
    companion object{
         const val SPACE_SIZE_LIMIT = 2048 // 2MB
         const val LOG = 0x005
         const val LOG_DATA = 0x006
         const val LOG_EXCEPTION = 0x007
    }

    private val handler = WriteHandler(looper)

    fun log(type:Int, message: String) {
        handler.sendMessage(handler.obtainMessage(type,message))
    }

   inner class WriteHandler(private val looper: Looper):
        Handler(looper){
        override fun handleMessage(msg: Message) {
            val content = msg.obj as String
           val filaName = when(msg.what) {
                LOG, LOG_EXCEPTION -> "crash.log"
                LOG_DATA -> "data.log"
               else -> "crash.log"
            }
            writeLog(context,content,filaName)
        }

        @Synchronized
        private fun writeLog(context: Context, content: String, fileName: String) {
            try {
                val myFile = getFile(context, fileName)
                val originContent = "${getCurrentTime()} > $content"
                FileOutputStream(myFile, true).use { fos ->
                    fos.write("\r\n".toByteArray())
                    fos.write(originContent.toByteArray())
                    fos.flush()
                    fos.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getFile(context: Context, name: String): File {
            deleteOldFile(context, name)
            return File(initLogFolder(context), name)
        }
        @Throws(IOException::class)
        fun deleteOldFile(context: Context, name: String = "crash.log") {
            val logFile = File(initLogFolder(context), name)
            if (logFile.exists()) {
                val size = logFile.length() / 1024 // 转换为KB
                if (size > SPACE_SIZE_LIMIT) {
                    logFile.delete()
                    logFile.createNewFile()
                }
            } else {
                logFile.createNewFile()
            }
        }

        private fun initLogFolder(context: Context): String {
            val file = File(context.cacheDir, "log")
            if (!file.exists()) {
                file.mkdirs()
            }
            return file.absolutePath
        }
        private fun getCurrentTime(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return dateFormat.format(Date())
        }
    }
}