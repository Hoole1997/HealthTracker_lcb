package com.daily.health.manager.face.act

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.data.utils.DateTimeUtils
import com.daily.health.manager.databinding.HtActivityAddReminderBinding
import com.daily.health.manager.face.adapter.ReminderTimeAdapter
import com.daily.health.manager.face.dialog.AlarmTimeSelectDialog
import com.daily.health.manager.face.dialog.DosesTimesDialog
import com.daily.health.manager.face.dialog.ImgGetTypeDialog
import com.daily.health.manager.face.viewmodel.AddReminderUiState
import com.daily.health.manager.face.viewmodel.AddReminderViewModel
import com.daily.health.manager.face.viewmodel.SaveState
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.hideSoftKeyBoard
import net.corekit.core.report.ReportDataManager

class AddReminderScreen : BaseInterActivity<AddReminderViewModel, HtActivityAddReminderBinding>(){


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

                }
            }
        }


    private lateinit var timeAdapter: ReminderTimeAdapter

    override fun createViewBinding() = HtActivityAddReminderBinding.inflate(layoutInflater)

    override fun getVMModelClass() = AddReminderViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // 获取传入的参数
        val remindId = intent.getLongExtra("remindId", -1L).takeIf { it != -1L }
        val startDate = intent.getStringExtra("startDate")

        // 初始化ViewModel
        mViewModel.initPage(remindId, startDate)

        setupViews()
        setupRecyclerView()
        observeViewModel()
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
            val intent = android.content.Intent(context, AddReminderScreen::class.java).apply {
                remindId?.let { putExtra("remindId", it) }
                startDate?.let { putExtra("startDate", it) }
            }
            context.startActivity(intent)
        }
    }

    private fun setupViews() {
        with(mViewBind) {
            // 返回按钮
            btnBack.clickWithDuration {
                onBackPress()
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
                ReportDataManager.reportData("med_click_save",mapOf())
                mViewModel.saveReminder()
            }

            ivImg.clickWithDuration {
                ImgGetTypeDialog.show(supportFragmentManager, {
                    ImagePicker.with(this@AddReminderScreen)
                        .cropSquare()        // 打开裁剪功能，可传入比例 crop(1f, 1f) 做正方形
                        .compress(1024) // 压缩图片至1MB以内
                        .maxResultSize(1080, 1080) // 限制分辨率
                        .cameraOnly()
                        .createIntent {
                            startForProfileImageResult.launch(it)
                            App.INSTANCE.isFeatureLeave = true
                        }
                }) {
                    ImagePicker.with(this@AddReminderScreen)
                        .cropSquare()
                        .compress(1024) // 压缩图片至1MB以内
                        .maxResultSize(1080, 1080) // 限制分辨率
                        .galleryOnly()
                        .createIntent {
                            startForProfileImageResult.launch(it)
                            App.INSTANCE.isFeatureLeave = true
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
            layoutManager = GridLayoutManager(this@AddReminderScreen, 3)
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
                if (state.isEditMode) getString(R.string.ht_save_changes) else getString(R.string.ht_save)

            Glide.with(this@AddReminderScreen)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(R.drawable.ht_ic_camera))
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
                mViewBind.btnSave.text = getString(R.string.ht_saving)
            }

            is SaveState.Success -> {
                onBackPress()
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
        val timePair = DateTimeUtils.parseTimeString(currentTime) ?: run {
            val nowComponents = DateTimeUtils.extractDateComponents(DateTimeUtils.now())
            nowComponents.hour to nowComponents.minute
        }
        AlarmTimeSelectDialog.show(supportFragmentManager, timePair) {
            val timeString = DateTimeUtils.formatTimeComponents(it.first, it.second)
            mViewModel.updateReminderTime(position, timeString)

        }
    }
}