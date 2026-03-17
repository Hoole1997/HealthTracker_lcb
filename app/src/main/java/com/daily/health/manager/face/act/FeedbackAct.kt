package com.daily.health.manager.face.act

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.github.dhaval2404.imagepicker.ImagePicker
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.databinding.FcActivityFeedbackBinding
import com.daily.health.manager.face.adapter.ChoosePhotoRCVAdapter
import com.daily.health.manager.face.dialog.ImgGetTypeDialog
import com.daily.health.manager.utils.FeedbackUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.hjq.toast.Toaster
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedbackAct: BaseMVVMActivity<BaseViewModel, FcActivityFeedbackBinding>() {
    
    companion object {
        private const val MAX_PHOTO_COUNT = 6
    }
    
    // 图片路径列表（字符串，给适配器使用）
    private val photoList = mutableListOf<String>()
    
    // 图片 Uri 列表（保留权限，给邮件发送使用）
    private val photoUris = mutableListOf<Uri>()
    
    // 图片适配器
    private lateinit var photoAdapter: ChoosePhotoRCVAdapter
    
    // 图片选择启动器
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data
        
        when (resultCode) {
            RESULT_OK -> {
                // 获取图片 URI
                val fileUri = data?.data
                fileUri?.let { uri ->
                    // 尝试获取持久化权限（对于 content:// URI）
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        // 某些 URI 不支持持久化权限，忽略异常
                        e.printStackTrace()
                    }
                    
                    // 同时添加到两个列表
                    photoList.add(uri.toString())  // 字符串列表给适配器
                    photoUris.add(uri)              // Uri 列表给邮件发送
                    photoAdapter.notifyDataSetChanged()
                    updatePhotoAdapter()
                    
                    // 自动滚动到最后，显示新添加的图片和添加按钮
                    mViewBind.rvPhoto.smoothScrollToPosition(photoList.size)
                }
            }
            ImagePicker.RESULT_ERROR -> {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun createViewBinding() = FcActivityFeedbackBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupViews()
        setupRecyclerView()
    }
    
    private fun setupViews() {
        with(mViewBind) {
            // 返回按钮
            btnBack.clickWithDuration {
                finish()
            }
            
            // 文本输入监听
            etContent.addTextChangedListener { text ->
                // 更新字符计数
                val length = text?.length ?: 0
                tvInputLimit.text = "$length/500"
                
                // 根据输入内容启用/禁用提交按钮
                btnSubmit.isEnabled = text?.isNotBlank() == true
            }
            
            // 提交按钮（空实现）
            btnSubmit.clickWithDuration {
                handleSubmit()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        if(goFeedback){
            lifecycleScope.launch {
                delay(100L)
                Toaster.show(getString(R.string.fc_feedback_submitted))
                finish()
            }
        }

    }
    
    private fun setupRecyclerView() {
        // 初始化适配器
        photoAdapter = ChoosePhotoRCVAdapter(
            photoList,
            object : ChoosePhotoRCVAdapter.ChoosePhotoRCVListener {
                override fun onClickAddPhoto() {
                    handleAddPhoto()
                }
                
                override fun onClickDelPhoto(position: Int) {
                    handleDeletePhoto(position)
                }
            }
        )
        
        // 设置 RecyclerView
        with(mViewBind.rvPhoto) {
            layoutManager = LinearLayoutManager(this@FeedbackAct, HORIZONTAL, false)
            adapter = photoAdapter
        }
    }
    
    /**
     * 处理添加图片
     */
    private fun handleAddPhoto() {
        // 检查图片数量限制
        if (photoList.size >= MAX_PHOTO_COUNT) {
            Toaster.show(getString(R.string.fc_max_upload_photos, MAX_PHOTO_COUNT))
            return
        }
        
        // 弹出选择对话框
        ImgGetTypeDialog.show(
            supportFragmentManager,
            onTakePhoto = {
                // 拍照
                ImagePicker.with(this)
                    .cameraOnly()
                    .createIntent {
                        imagePickerLauncher.launch(it)
                        App.INSTANCE.isFeatureLeave = true
                    }
            },
            onChoosePhoto = {
                // 从相册选择
                ImagePicker.with(this)
                    .galleryOnly()
                    .createIntent {
                        imagePickerLauncher.launch(it)
                        App.INSTANCE.isFeatureLeave = true
                    }
            }
        )
    }
    
    /**
     * 处理删除图片
     */
    private fun handleDeletePhoto(position: Int) {
        if (position < photoList.size) {
            photoList.removeAt(position)
            photoUris.removeAt(position)  // 同时删除 Uri
            photoAdapter.notifyDataSetChanged()
            updatePhotoAdapter()
        }
    }
    
    /**
     * 更新图片适配器显示状态
     */
    private fun updatePhotoAdapter() {
        // 如果已达到最大数量，隐藏添加按钮
        photoAdapter.showAddPhoto = photoList.size < MAX_PHOTO_COUNT
        photoAdapter.notifyDataSetChanged()
    }

    private var goFeedback = false
    /**
     * 处理提交反馈
     */
    private fun handleSubmit() {
        // 获取用户输入的反馈内容
        val feedbackContent = mViewBind.etContent.text?.toString() ?: ""
        
        FeedbackUtils.sendFeedback(
            context = this,
            feedback = feedbackContent,
            fileList = photoUris  // 使用 Uri 列表
        ){
            if(it){
              goFeedback = true
            }else{
                Toast.makeText(
                    this
                    ,
                    getString(R.string.fc_feedback_sending_failed),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        

    }
}