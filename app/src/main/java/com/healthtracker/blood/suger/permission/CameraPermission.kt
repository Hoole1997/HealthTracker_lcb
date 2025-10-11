package com.healthtracker.blood.suger.permission

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.healthtracker.blood.suger.App
import com.healthtracker.blood.suger.utils.safeLaunch

/**
 * 一个用于处理相机权限请求的帮助类。
 * 此类简化了请求 [Manifest.permission.CAMERA] 和使用现代 Activity Result API 处理结果的过程。
 *
 * 用法:
 * 1. 在你的 Activity/Fragment 中实现 [CameraPermissionProvider] 接口。
 * 2. 初始化帮助类: `permission().with(this)`
 * 3. 请求权限: `permission().launch { isSuccess, showSettingsRedirect, hasPermission -> ... }`
 */
class CameraPermission {

    private var cameraLauncher: ActivityResultLauncher<String>? = null
    private var permissionRunnable: ((isSuccess: Boolean, showSettingsRedirect: Boolean, hasPermission: Boolean) -> Unit)? = null
    private var activity: FragmentActivity? = null

    /**
     * 注册权限启动器。必须在 Activity 的 `onCreate` 或 Fragment 的 `onCreate` 中调用。
     * @param activity 用于注册启动器的 [FragmentActivity]。
     */
    fun with(activity: FragmentActivity) {
        this.activity = activity
        // 对于单个权限请求，使用 RequestPermission 更具体、更高效。
        cameraLauncher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 权限被授予
                permissionRunnable?.invoke(true, false, false)
            } else {
                // 权限被拒绝，检查是否用户选择了“不再询问”。
                // 如果 shouldShowRequestPermissionRationale 返回 false，意味着用户已永久拒绝权限。
                val showSettingsRedirect = !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                permissionRunnable?.invoke(false, showSettingsRedirect, false)
            }
        }
    }

    /**
     * 如果应用尚未拥有相机权限，则发起权限请求。
     * 如果权限已被授予，则立即调用回调。
     *
     * @param runnable 权限请求结果的回调。
     * - `isSuccess`: 如果权限被授予，则为 true。
     * - `showSettingsRedirect`: 如果用户永久拒绝了权限，并且应该引导他们到设置页面，则为 true。
     * - `hasPermission`: 如果在请求之前权限就已经被授予，则为 true。
     */
    fun launch(runnable: ((isSuccess: Boolean, showSettingsRedirect: Boolean, hasPermission: Boolean) -> Unit)?) {
        this.permissionRunnable = runnable
        if (!hasPermission()) {
            // 启动单个 CAMERA 权限的请求。
            activity?.safeLaunch { cameraLauncher?.launch(Manifest.permission.CAMERA) }
        } else {
            // 如果权限已被授予，立即调用回调。
            runnable?.invoke(true, false, true)
        }
    }

    /**
     * 检查应用当前是否拥有相机权限。
     * @return 如果权限被授予，则为 true，否则为 false。
     */
    fun hasPermission(): Boolean {
        // 简化了检查逻辑，可读性更好。
        return ContextCompat.checkSelfPermission(App.INSTANCE, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 为需要处理相机权限的 Activity 或 Fragment 提供 [CameraPermission] 实例的接口。
 */
interface CameraPermissionProvider {
    /**
     * 提供一个 [CameraPermission] 的实例。
     */
    fun permission(): CameraPermission
}
