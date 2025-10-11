package com.healthtracker.blood.suger.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.utils.safeLaunch

/**
 * 一个用于处理相册/媒体权限请求的帮助类。
 * 此类封装了 Android 不同版本上存储权限的复杂性。
 *
 * - **Android 14 (API 34)+**: 请求 `READ_MEDIA_IMAGES` 和 `READ_MEDIA_VISUAL_USER_SELECTED` 以支持用户选择部分照片。
 * - **Android 13 (API 33)**: 请求 `READ_MEDIA_IMAGES`。
 * - **Android 13 以下**: 请求 `READ_EXTERNAL_STORAGE`。
 *
 * 用法:
 * 1. 在你的 Activity/Fragment 中实现 [PhotoPermissionProvider] 接口。
 * 2. 初始化帮助类: `photoPermission().with(this)`
 * 3. 请求权限: `photoPermission().launch { status, showSettingsRedirect, hasPermission -> ... }`
 */
class PhotoPermission {

    companion object {
        /** 用户授予了所有照片的访问权限。 */
        const val ALLOW_ALL = 1
        /** 用户只授予了部分照片的访问权限 (仅适用于 Android 14+)。 */
        const val ALLOW_ONLY = 2
        /** 用户未授予任何权限。 */
        const val NOT_ALLOW = 0

        /**
         * 检查当前应用的图片访问权限状态。
         * @return [ALLOW_ALL], [ALLOW_ONLY], 或 [NOT_ALLOW] 之一。
         */
        fun hasImagePermission(): Int {
            // Android 14+ (API 34+), 检查是否拥有部分图片访问权限。
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (ContextCompat.checkSelfPermission(App.INSTANCE, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    return ALLOW_ALL
                }
                if (ContextCompat.checkSelfPermission(App.INSTANCE, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED) {
                    return ALLOW_ONLY
                }
            }
            // Android 13 (API 33), 检查完整的媒体图片访问权限。
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(App.INSTANCE, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    return ALLOW_ALL
                }
            }
            // Android 13 以下，检查传统的外部存储读取权限。
            else {
                if (ContextCompat.checkSelfPermission(App.INSTANCE, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    return ALLOW_ALL
                }
            }
            return NOT_ALLOW
        }

        /** 检查是否只拥有部分图片访问权限。 */
        fun hasAllowOnlyImagePermission(): Boolean = hasImagePermission() == ALLOW_ONLY

        /** 检查是否拥有完整的图片访问权限。 */
        fun hasAllowImagePermission(): Boolean = hasImagePermission() == ALLOW_ALL

        /** 检查是否未授予任何图片访问权限。 */
        fun unAllowImagePermission(): Boolean = hasImagePermission() == NOT_ALLOW
    }

    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null
    private var permissionRunnable: ((status: Int, showSettingsRedirect: Boolean, hasPermission: Boolean) -> Unit)? = null
    private var activity: FragmentActivity? = null

    /**
     * 注册权限启动器。必须在 Activity 的 `onCreate` 或 Fragment 的 `onCreate` 中调用。
     * @param activity 用于注册启动器的 [FragmentActivity]。
     */
    fun with(activity: FragmentActivity) {
        this.activity = activity
        permissionLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // 检查 shouldShowRequestPermissionRationale 状态，以确定是否应提示用户前往设置。
            val showSettingsRedirect = permissions().none { permission ->
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }

            val currentStatus = hasImagePermission()
            // 如果权限仍未被授予，并且不建议显示解释，则引导用户去设置。
            if (currentStatus == NOT_ALLOW && showSettingsRedirect) {
                permissionRunnable?.invoke(currentStatus, true, false)
            } else {
                permissionRunnable?.invoke(currentStatus, false, false)
            }
        }
    }

    /**
     * 如果需要，发起权限请求。
     * 如果已拥有完整权限，则立即调用回调。
     *
     * @param runnable 权限请求结果的回调。
     * - `status`: 当前的权限状态 ([ALLOW_ALL], [ALLOW_ONLY], [NOT_ALLOW])。
     * - `showSettingsRedirect`: 如果用户永久拒绝了权限，并且应该引导他们到设置页面，则为 true。
     * - `hasPermission`: 如果在请求之前权限就已经被授予，则为 true。
     */
    fun launch(runnable: ((status: Int, showSettingsRedirect: Boolean, hasPermission: Boolean) -> Unit)?) {
        this.permissionRunnable = runnable
        if (!hasAllowImagePermission()) {
            requestPermissions()
        } else {
            runnable?.invoke(ALLOW_ALL, false, true)
        }
    }

    private fun requestPermissions() {
        activity?.safeLaunch { permissionLauncher?.launch(permissions()) }
    }

    /**
     * 根据 Android SDK 版本确定需要请求的权限数组。
     */
    private fun permissions(): Array<String> {
        return when {
            // Android 14+ 请求新权限以支持部分选择。
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            // Android 13 请求新的图片权限。
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )
            // Android 13 以下，只请求读取权限。遵循最小权限原则。
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
}

/**
 * 为需要处理相册权限的 Activity 或 Fragment 提供 [PhotoPermission] 实例的接口。
 */
interface PhotoPermissionProvider {
    /**
     * 提供一个 [PhotoPermission] 的实例。
     */
    fun photoPermission(): PhotoPermission
}
