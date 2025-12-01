//package com.healthtracker.blood.suger.utils;
//
//import android.content.Context;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.net.Uri;
//import android.os.Build;
//import android.text.TextUtils;
//import android.widget.Toast;
//
//import com.healthtracker.blood.suger.BuildConfig;
//import com.healthtracker.blood.suger.R;
//
//import java.util.List;
//import java.util.Locale;
//import java.util.TimeZone;
//
//public class FeedbackUtils {
//
//    public static void sendFeedback(Context context, String feedback, List<Uri> fileList, String subAppend ){
//        StringBuilder sb = new StringBuilder();
//        String email = BuildConfig.FEEDBACK_EMAIL;
//        String app_version = "";
//        try {
//            app_version = context.getApplicationContext().getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName + "(" + BuildConfig.VERSION_CODE + ")";
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
//        sb.append("Device info:");
//        sb.append("\n");
//        sb.append(app_version);
//        sb.append(", ").append(Build.MODEL);
//        sb.append(", OS_").append(Build.VERSION.RELEASE);
//        sb.append(", ");
//        sb.append(context.getResources().getDisplayMetrics().widthPixels).append("x").append(context.getResources().getDisplayMetrics().heightPixels);
//        sb.append(", ");
//        sb.append(context.getResources().getDisplayMetrics().densityDpi).append("Dpi");
//        sb.append(", ");
//        Locale locale = context.getResources().getConfiguration().locale;
//        sb.append(locale.getLanguage()).append(" _ ").append(locale.getCountry());
//        sb.append(", ");
//        sb.append(TimeZone.getDefault().getDisplayName(false, TimeZone.SHORT));
//        sb.append(", ");
//        sb.append("\n");
//        if (!TextUtils.isEmpty(title)) {
//            sb.append(title);
//            sb.append("\n");
//        }
//        if (!TextUtils.isEmpty(content)) {
//            sb.append(content);
//            sb.append("\n");
//        }
//
//        try {
//            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
//            intent.setType("application/octet-stream");
//            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{BuildConfig.FEEDBACK_EMAIL});
//            String subject = context.getString(R.string.feedback_email_title, context.getString(R.string.app_name));
////            if (!TextUtils.isEmpty(subAppend)) {
////                subject = subject + " " + subAppend;
////            }
//            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
//            intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
//            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, );
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            if (EmailUtils.getInstance().hasGmail(context)) {
//                intent.setPackage(EmailUtils.PACKAGE_GMAIL);
//            } else if (EmailUtils.getInstance().hasEmailApp(context)) {
//                intent.setPackage(EmailUtils.PACKAGE_EMAIL_APP);
//            }
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//
//            } else {
//                intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
//            }
//
//            context.startActivity(intent);
//        } catch (Throwable e) {
//            try {
//                e.printStackTrace();
//                Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
//                intent.setType("application/octet-stream");
//                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
//                String subject = context.getString(R.string.feedback_email_title, context.getString(R.string.app_name));
//                intent.putExtra(Intent.EXTRA_SUBJECT, subject);
//                intent.putExtra(Intent.EXTRA_TEXT, sb.toString());
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                context.startActivity(Intent.createChooser(intent, subject));
//            } catch (Throwable e2) {
//                Toast.makeText(context.getApplicationContext(), context.getString(R.string.feedback_sending_failed), Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
////    public static void uploadLogFile(Context context,int type) {
////        try {
//////            context = MusicFileProvider.Companion.getDPContext(context);
////            String src = LogUtils.INSTANCE.initLogFolder(context);
////            String dest = context.getCacheDir().getPath() + "/log.zip";
////            FileUtil.zip(src,dest);
////            File zipFile = new File(dest);
////            if (!zipFile.exists()) {
////                return;
////            }
////            Server.INSTANCE.uploadFile(zipFile,type);
////        }catch (Exception e){
////            e.printStackTrace();
////        }
////    }
//}
