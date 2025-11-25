package com.healthtracker.blood.suger.ui.act

import android.content.Intent
import androidx.viewbinding.ViewBinding
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.ui.dialog.FSIPermissionDialog
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.logd
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
abstract class BaseFSiRequestActivity: BaseMVVMActivity<BaseViewModel, ViewBinding>() {
    companion object{
        private const val TAG = "BaseFSiRequestActivity"
    }

    // ==================== FSI权限管理 ====================
    @Inject
    lateinit var permissionManager: PermissionManager
    /**
     * 检查全屏通知权限
     */
    private fun checkFullScreenIntentPermission() {
        if (permissionManager.shouldRequestFSIPermission()) {
            "Should request FSI permission for medication reminders".logd(TAG)
            showFSIPermissionExplanationDialog()
        } else {
            "FSI permission check: no need to request".logd(TAG)
        }
    }

    /**
     * 显示FSI权限说明对话框
     */
    private fun showFSIPermissionExplanationDialog() {
        FSIPermissionDialog.show(
            supportFragmentManager,
            onAllowPermission = {
                "User agreed to FSI permission".logd(TAG)
                permissionManager.requestFSIPermission(this)
            },
            onDenyPermission = {
                "User declined FSI permission".logd(TAG)
                permissionManager.recordFSIPermissionRequest(false)
            }
        )
    }

    /**
     * 处理Activity返回结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (permissionManager.handleActivityResult(requestCode, resultCode)) {
            // FSI权限请求处理完成
            "FSI permission activity result handled".logd(TAG)
        }
    }

    /**
     * 处理权限申请结果
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionManager.handlePermissionResult(requestCode, permissions, grantResults)
    }

}