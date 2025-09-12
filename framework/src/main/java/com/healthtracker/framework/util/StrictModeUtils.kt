package com.healthtracker.framework.util

import android.os.StrictMode

fun openStrictMode() {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectCustomSlowCalls() //配合StrictMode.noteSlowCall使用
            .detectDiskReads()//是否在主线程中进行磁盘读取
            .detectDiskWrites()//是否在主线程中进行磁盘写入
            .detectNetwork() // 是否在主线程中进行网络请求
            .penaltyDialog() //弹出违规提示对话
            .penaltyLog() //在Logcat 中打印违规异常信息
            .penaltyDropBox()//将违规信息记录到 dropbox 系统日志目录中
            .build()
    )
    StrictMode.setVmPolicy(
        StrictMode.VmPolicy.Builder()
            .detectActivityLeaks()//Activity是否内存泄漏
            .detectLeakedSqlLiteObjects()//数据库是否未关闭
            .detectLeakedClosableObjects()//文件是否未关闭
            .detectLeakedRegistrationObjects()//对象是否被正确关闭
            .penaltyLog()//打印日志
            .build()
    )
}