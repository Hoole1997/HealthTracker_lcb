package com.healthtracker.blood.suger.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.BuildConfig
import com.healthtracker.blood.suger.R
import java.util.Locale
import java.util.TimeZone

/**
 * 反馈工具类
 * 用于发送用户反馈邮件，包含设备信息和附件
 */
object FeedbackUtils {

    /**
     * 发送反馈邮件
     * @param context 上下文
     * @param feedback 用户反馈内容
     * @param fileList 附件列表（图片 URI）
     * @param subAppend 邮件主题附加内容（可选）
     */
    fun sendFeedback(
        context: Context,
        feedback: String,
        fileList: List<Uri>,
        subAppend: String? = null,
        onResult:((Boolean) -> Unit)? = null

    ) {
        // 构建邮件正文
        val emailBody = buildEmailBody(context, feedback)
        
        // 获取反馈邮箱地址
        val email = "jiaoyun76861590@gmail.com"
        
        // 构建邮件主题
        val subject = buildSubject(context, subAppend)
        
        try {
            App.INSTANCE.isFeatureLeave = true
            // 第一次尝试：使用首选邮件应用
            sendEmailWithPreferredApp(context, email, subject, emailBody, fileList)
            onResult?.invoke(true)
        } catch (e: Throwable) {
            try {
                e.printStackTrace()
                // 第二次尝试：使用应用选择器
                sendEmailWithChooser(context, email, subject, emailBody, fileList)
                onResult?.invoke(true)
            } catch (e2: Throwable) {
                e2.printStackTrace()
                // 发送失败，显示提示
                onResult?.invoke(false)
            }
        }
    }

    /**
     * 构建邮件正文，包含设备信息和用户反馈
     */
    private fun buildEmailBody(context: Context, feedback: String): String {
        return buildString {
            // 设备信息部分
            append("Device info:\n")
            append(getAppVersion(context))
            append(", ${Build.MODEL}")
            append(", OS_${Build.VERSION.RELEASE}")
            
            // 屏幕信息
            val metrics = context.resources.displayMetrics
            append(", ${metrics.widthPixels}x${metrics.heightPixels}")
            append(", ${metrics.densityDpi}Dpi")
            
            // 语言和时区
            val locale = context.resources.configuration.locales[0]
            append(", ${locale.language}_${locale.country}")
            append(", ${TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT)}")
            append("\n")
            
            // 用户反馈内容
            if (feedback.isNotBlank()) {
                append(feedback)
                append(":\n\n")
            }
        }
    }

    /**
     * 获取应用版本信息
     */
    private fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.applicationContext.packageManager
                .getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName}(${BuildConfig.VERSION_CODE})"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "Unknown"
        }
    }

    /**
     * 构建邮件主题
     */
    private fun buildSubject(context: Context, subAppend: String?): String {
        val baseSubject = context.getString(
            R.string.feedback_email_title,
            context.getString(R.string.app_name)
        )
        return if (subAppend.isNullOrBlank()) {
            baseSubject
        } else {
            "$baseSubject $subAppend"
        }
    }

    /**
     * 使用首选邮件应用发送邮件
     */
    private fun sendEmailWithPreferredApp(
        context: Context,
        email: String,
        subject: String,
        body: String,
        fileList: List<Uri>
    ) {
        // 转换所有 file:// URI 为 content:// URI
        val convertedUris = fileList.map { convertFileUriToContentUri(context, it) }
        
        val intent = createEmailIntent(email, subject, body, convertedUris)
        
        // 设置首选邮件应用
        when {
            EmailUtils.getInstance().hasGmail(context) -> {
                intent.setPackage(EmailUtils.PACKAGE_GMAIL)
            }
            EmailUtils.getInstance().hasEmailApp(context) -> {
                intent.setPackage(EmailUtils.PACKAGE_EMAIL_APP)
            }
        }
        
        context.startActivity(intent)
    }

    /**
     * 使用应用选择器发送邮件
     */
    private fun sendEmailWithChooser(
        context: Context,
        email: String,
        subject: String,
        body: String,
        fileList: List<Uri>
    ) {
        // 转换所有 file:// URI 为 content:// URI
        val convertedUris = fileList.map { convertFileUriToContentUri(context, it) }
        
        val intent = createEmailIntent(email, subject, body, convertedUris)
        context.startActivity(Intent.createChooser(intent, subject))
    }

    /**
     * 将 file:// URI 转换为 content:// URI（通过 FileProvider）
     * 用于解决 Android N+ 的 FileUriExposedException 问题
     */
    private fun convertFileUriToContentUri(context: Context, uri: Uri): Uri {
        return if (uri.scheme == "file") {
            try {
                // file:// URI 需要通过 FileProvider 转换
                val file = java.io.File(uri.path ?: return uri)
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
                uri // 转换失败，返回原 URI
            }
        } else {
            uri // content:// URI 直接返回
        }
    }

    /**
     * 创建邮件 Intent
     */
    private fun createEmailIntent(
        email: String,
        subject: String,
        body: String,
        fileList: List<Uri>
    ): Intent {
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            
            // 添加附件
            if (fileList.isNotEmpty()) {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(fileList))
            }
            
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            // Android N+ 需要授予读取权限
            // 关键：FLAG_GRANT_READ_URI_PERMISSION 会自动应用到所有 EXTRA_STREAM 中的 URI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
