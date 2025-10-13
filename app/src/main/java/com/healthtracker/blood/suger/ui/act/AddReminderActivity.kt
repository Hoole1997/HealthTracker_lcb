package com.healthtracker.blood.suger.ui.act

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.input.key.Key.Companion.G
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.alarm.PermissionManager
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityAddReminderBinding
import com.healthtracker.blood.suger.permission.CameraPermission
import com.healthtracker.blood.suger.permission.CameraPermissionProvider
import com.healthtracker.blood.suger.permission.PhotoPermission
import com.healthtracker.blood.suger.permission.PhotoPermissionProvider
import com.healthtracker.blood.suger.ui.adapter.ReminderTimeAdapter
import com.healthtracker.blood.suger.ui.dialog.AlarmTimeSelectDialog
import com.healthtracker.blood.suger.ui.dialog.DosesTimesDialog
import com.healthtracker.blood.suger.ui.dialog.FSIPermissionDialog
import com.healthtracker.blood.suger.ui.dialog.ImgGetTypeDialog
import com.healthtracker.blood.suger.ui.viewmodel.AddReminderUiState
import com.healthtracker.blood.suger.ui.viewmodel.AddReminderViewModel
import com.healthtracker.blood.suger.ui.viewmodel.SaveState
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.hideSoftKeyBoard
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddReminderActivity : BaseMVVMActivity<AddReminderViewModel, ActivityAddReminderBinding>(),
    PhotoPermissionProvider, CameraPermissionProvider {

    @Inject
    lateinit var permissionManager: PermissionManager

    private val photoPermission = PhotoPermission()
    private val cameraPermission = CameraPermission()


    private val startForProfileImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            val resultCode = result.resultCode
            val data = result.data

            when (resultCode) {
                RESULT_OK -> {
                    //Image Uri will not be null for RESULT_OK
                    val fileUri = data?.data
                    fileUri?.let {
                        mViewModel.setCoverUri(it)
                    }

                }

                ImagePicker.RESULT_ERROR -> {
                    Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
                }

                else -> {
                    Toast.makeText(this, "Task Cancelled", Toast.LENGTH_SHORT).show()
                }
            }
        }


    private lateinit var timeAdapter: ReminderTimeAdapter

    override fun createViewBinding() = ActivityAddReminderBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AddReminderViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 获取传入的参数
        val remindId = intent.getLongExtra("remindId", -1L).takeIf { it != -1L }
        val startDate = intent.getStringExtra("startDate")

        cameraPermission.with(this)
        photoPermission.with(this)

        // 初始化ViewModel
        mViewModel.initPage(remindId, startDate)

        setupViews()
        setupRecyclerView()
        observeViewModel()

        // 检查FSI权限
        checkFullScreenIntentPermission()
    }

    companion object {

        private const val TAG = "AddReminderActivity"

        /**
         * 启动Activity的便利方法
         * @param context 上下文
         * @param remindId 提醒ID，null表示新建模式
         * @param startDate 新建模式的起始日期
         */
        @JvmStatic
        fun start(
            context: android.content.Context,
            remindId: Long? = null,
            startDate: String? = null
        ) {
            val intent = android.content.Intent(context, AddReminderActivity::class.java).apply {
                remindId?.let { putExtra("remindId", it) }
                startDate?.let { putExtra("startDate", it) }
            }
            context.startActivity(intent)
        }
    }

    private fun setupViews() {
        with(mViewBind) {
            // 返回按钮
            btnBack.click {
                finish()
            }

            // 药物名称输入监听
            etMedicationName.addTextChangedListener { text ->
                mViewModel.setMedicineName(text?.toString() ?: "")
            }

            // 备注输入监听
            etNotes.addTextChangedListener { text ->
                mViewModel.setNotes(text?.toString() ?: "")
            }

            // 日历同步选择
            cbSyncCalendar.setOnCheckedChangeListener { _, isChecked ->
                mViewModel.setSyncCalendar(isChecked)
            }

            // 每日服药次数点击
            tvDoseCount.click {
                showDoseCountDialog()
            }

            // 保存按钮
            btnSave.click {
                mViewModel.saveReminder()
            }

            ivImg.clickWithDuration {
                ImgGetTypeDialog.show(supportFragmentManager, {
                    ImagePicker.with(this@AddReminderActivity)
                        .cropSquare()        // 打开裁剪功能，可传入比例 crop(1f, 1f) 做正方形
                        .compress(1024) // 压缩图片至1MB以内
                        .maxResultSize(1080, 1080) // 限制分辨率
                        .galleryOnly()
                        .createIntent {
                            startForProfileImageResult.launch(it)
                        }
                }) {
                    ImagePicker.with(this@AddReminderActivity)
                        .cropSquare()
                        .compress(1024) // 压缩图片至1MB以内
                        .maxResultSize(1080, 1080) // 限制分辨率
                        .cameraOnly()
                        .createIntent {
                            startForProfileImageResult.launch(it)
                        }

                }

            }
        }
    }

    private fun setupRecyclerView() {
        timeAdapter = ReminderTimeAdapter { position ->
            showTimePickerDialog(position)
        }

        with(mViewBind.rvDailyRemind) {
            layoutManager = GridLayoutManager(this@AddReminderActivity, 3)
            adapter = timeAdapter
        }
    }

    private fun observeViewModel() {
        this.collectLatest(mViewModel.uiState) { state ->
            updateUI(state)
        }

        this.collectLatest(mViewModel.saveState) { saveState ->
            handleSaveState(saveState)
        }
    }

    private fun updateUI(state: AddReminderUiState) {
        with(mViewBind) {
            // 更新表单内容
            if (etMedicationName.text.toString() != state.medicineName) {
                etMedicationName.setText(state.medicineName)
                etMedicationName.setSelection(state.medicineName.length)
            }

            if (etNotes.text.toString() != state.notes) {
                etNotes.setText(state.notes)
            }

            cbSyncCalendar.isChecked = state.syncCalendar

            // 更新每日服药次数显示
            tvDoseCount.text = state.dailyDoses.toString()

            // 更新时间列表
            timeAdapter.updateTimes(state.reminderTimes)

            // 更新保存按钮状态和文字
            btnSave.isEnabled = state.isFormValid
            btnSave.text =
                if (state.isEditMode) getString(R.string.save_changes) else getString(R.string.save)

            Glide.with(this@AddReminderActivity)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(R.drawable.ic_camera))
                .load(state.coverUri)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(ivImg)
        }
    }

    private fun handleSaveState(saveState: SaveState) {
        when (saveState) {
            is SaveState.Idle -> {
                mViewBind.btnSave.isEnabled = saveState.isAlbe
                mViewBind.btnSave.alpha = if (saveState.isAlbe) 1.0f else 0.3f
            }

            is SaveState.Loading -> {
                mViewBind.btnSave.isEnabled = false
                mViewBind.btnSave.text = getString(R.string.saving)
            }

            is SaveState.Success -> {
                finish()
            }

            is SaveState.Error -> {
                ToastUtils.showShort("Save failed")
            }
        }
    }

    private fun showDoseCountDialog() {
        DosesTimesDialog(mViewModel.uiState.value.dailyDoses) {
            mViewModel.setDailyDoses(it)
        }.show(supportFragmentManager)
    }

    private fun showTimePickerDialog(position: Int) {
        hideSoftKeyBoard()
        val currentTime = mViewModel.uiState.value.reminderTimes[position]
        val timeParts = currentTime.split(":")
        val hour = timeParts[0].toInt()
        val minute = timeParts[1].toInt()
        AlarmTimeSelectDialog.show(supportFragmentManager, hour to minute) {
            val timeString = DateTimeUtils.formatTimeComponents(it.first, it.second)
            mViewModel.updateReminderTime(position, timeString)

        }
    }

    // ==================== FSI权限管理 ====================

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

    override fun photoPermission() = photoPermission

    override fun permission() = cameraPermission
}