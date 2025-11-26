//package com.healthtracker.blood.suger.permission
//
//import android.content.Intent
//import androidx.activity.result.ActivityResultLauncher
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.fragment.app.FragmentActivity
//import com.healthtracker.blood.suger.alarm.PermissionManager
//import com.healthtracker.blood.suger.ui.dialog.FSIPermissionDialog
//import com.healthtracker.framework.ext.logd
//import net.corekit.core.report.ReportDataManager
//
///**
// * Helper class for handling Full-Screen Intent (FSI) permission requests.
// * This class encapsulates the logic for requesting FSI permission,
// * showing an explanation dialog, and handling the result, using the modern ActivityResultLauncher API.
// *
// * Usage:
// * 1. In your Activity, create an instance: `private val fsiRequester = FSIPermissionRequester()`
// * 2. Inject PermissionManager: `@Inject lateinit var permissionManager: PermissionManager`
// * 3. In `initView` or `onCreate`, register the requester: `fsiRequester.with(this, permissionManager)`
// * 4. To request permission, call: `fsiRequester.launch("your_position") { ... on complete ... }`
// */
//class FSIPermissionRequester {
//
//    private var fsiLauncher: ActivityResultLauncher<Intent>? = null
//    private var onFlowComplete: (() -> Unit)? = null
//    private var activity: FragmentActivity? = null
//    private var requestPosition: String = ""
//    private var permissionManager: PermissionManager? = null
//
//    companion object {
//        private const val TAG = "FSIPermissionRequester"
//    }
//
//    /**
//     * Registers the activity result launcher. Must be called in Activity's onCreate/initView.
//     * @param activity The [FragmentActivity] to register with.
//     * @param manager The [PermissionManager] instance from Hilt.
//     */
//    fun with(activity: FragmentActivity, manager: PermissionManager) {
//        this.activity = activity
//        this.permissionManager = manager
//        this.fsiLauncher = activity.registerForActivityResult(
//            ActivityResultContracts.StartActivityForResult()
//        ) {
//            // This block is called when the user returns from the system settings screen.
//            val granted = permissionManager?.isFSIPermissionGranted() ?: false
//            permissionManager?.recordFSIPermissionResult(granted)
//
//            if (granted) {
//                "FSI permission granted after settings".logd(TAG)
//            } else {
//                "FSI permission still denied after settings".logd(TAG)
//            }
//
//            // The flow is complete.
//            onFlowComplete?.invoke()
//        }
//    }
//
//    /**
//     * Initiates the FSI permission request flow if needed.
//     *
//     * @param position A string indicating where the request is initiated (e.g., "splash", "main").
//     * @param onComplete A callback that is invoked when the entire flow is finished.
//     */
//    fun launch(position: String, onComplete: () -> Unit) {
//        val currentActivity = activity
//        val currentManager = permissionManager
//        if (currentActivity == null || currentManager == null) {
//            "FSIPermissionRequester.with() must be called first.".logd(TAG)
//            onComplete()
//            return
//        }
//
//        this.onFlowComplete = onComplete
//        this.requestPosition = position
//
//        if (currentManager.shouldRequestFSIPermission()) {
//            "Should request FSI permission at $position".logd(TAG)
//            showFSIPermissionExplanationDialog(currentActivity, currentManager)
//        } else {
//            "FSI permission check: no need to request at $position".logd(TAG)
//            onComplete()
//        }
//    }
//
//    private fun showFSIPermissionExplanationDialog(activity: FragmentActivity, manager: PermissionManager) {
//        // Record impression
//        manager.recordFSIDialogImpression()
//        ReportDataManager.reportData("permission_full_screen_show", mapOf("position" to requestPosition))
//
//        FSIPermissionDialog.show(
//            activity.supportFragmentManager,
//            onAllowPermission = {
//                "User agreed to FSI permission".logd(TAG)
//                ReportDataManager.reportData("permission_full_screen_allow", mapOf("position" to requestPosition))
//
//                // Launch the system settings screen
//                val intent = manager.createFSIPermissionIntent()
//                if (intent != null) {
//                    fsiLauncher?.launch(intent)
//                } else {
//                    // If intent is null (e.g., on older Android versions where it's not needed),
//                    // the flow is effectively complete.
//                    onFlowComplete?.invoke()
//                }
//            },
//            onDenyPermission = {
//                "User declined FSI permission".logd(TAG)
//                ReportDataManager.reportData("permission_full_screen_deny", mapOf("position" to requestPosition))
//                manager.recordFSIPermissionResult(false)
//
//                // Flow is complete
//                onFlowComplete?.invoke()
//            }
//        )
//    }
//}
