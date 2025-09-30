//package com.healthtracker.framework.util
//
//import android.content.Context
//import android.os.Bundle
//import com.google.firebase.analytics.FirebaseAnalytics
//import com.google.firebase.crashlytics.FirebaseCrashlytics
//import java.util.Calendar
//
//fun logEvent(context: Context,eventName:String,bundle: Bundle? = null){
//   FirebaseAnalytics.getInstance(context).logEvent(eventName,bundle)
//}
//
//fun logException(e:Throwable){
//   FirebaseCrashlytics.getInstance().recordException(e)
//}
//
//
//fun getInstallTime(context: Context) = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
//
//
//fun isInstall24Hour(context: Context) = System.currentTimeMillis() - getInstallTime(context) <= 24 * 3600 * 1000
//
//
//fun Int.isDaySinceInstall(context: Context): Boolean {
//
//   val installCal = Calendar.getInstance().apply {
//      timeInMillis = getInstallTime(context)
//   }
//   val currentCal = Calendar.getInstance()
//
//   var daysSinceInstall = currentCal.get(Calendar.DAY_OF_YEAR) - installCal.get(Calendar.DAY_OF_YEAR)
//
//   // 处理跨年情况
//   val yearDiff = currentCal.get(Calendar.YEAR) -installCal.get(Calendar.YEAR)
//   if (yearDiff > 0) {
//      var daysInYear = 365
//      for (year in installCal.get(Calendar.YEAR) until currentCal.get(Calendar.YEAR)) {
//         if (isLeapYear(year)) {
//            daysInYear = 366
//         }
//         daysSinceInstall += daysInYear
//      }
//   }
//
//   return daysSinceInstall == this
//}
//
//private fun isLeapYear(year: Int): Boolean {
//   return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
//}