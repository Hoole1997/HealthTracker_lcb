package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.HeartRateStatus
import com.healthtracker.blood.suger.databinding.ActivityHeartRateDetailBinding
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.ui.dialog.LevelExplainDialog
import com.healthtracker.blood.suger.ui.viewmodel.HeartRateDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.invisible
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.ArrayList

@AndroidEntryPoint
class HeartRateDetailActivity :
    BaseMVVMActivity<HeartRateDetailViewModel, ActivityHeartRateDetailBinding>() {

    companion object {
        fun start(context: Context, recordId: Long) {
            context.startActivity<HeartRateDetailActivity>(
                HeartRateDetailViewModel.RECORD_ID to recordId
            )
        }
    }

    override fun createViewBinding(): ActivityHeartRateDetailBinding =
        ActivityHeartRateDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = HeartRateDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupActionBar()
        setupStatusView()
        observeViewModel()
    }

    private fun setupActionBar() {
        with(mViewBind){
            btnBack.click { finish() }
            btnDelete.clickWithDuration {

            }

            btnEdit.clickWithDuration {

            }
        }
    }

    private fun setupStatusView() {
        val levels = LeveDataFactory.HeartRate.buildItems(this)
        mViewBind.bpmStatusView.apply {
            setLevels(levels)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            collectLatest(mViewModel.record) { record ->
                record?.let { updateRecord(it) }
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.status) { status ->
                status?.let { updateStatus(it) }
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.isLoading) { loading ->

            }
        }
    }

    private fun updateRecord(record: HeartRateRecord) {
        mViewBind.tvBpmValue.text = record.heartRateBpm.toString()
        mViewBind.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)
        if(record.tagIds.isNullOrEmpty()){
            mViewBind.tvTags.invisible()
        }else{
            mViewBind.tvTags.visible()
        }
        val index = LeveDataFactory.HeartRate.indexFor(record.heartRateBpm)
        mViewBind.bpmStatusView.setCurrentLevel(index)
    }

    private fun updateStatus(status: HeartRateStatus) {
        mViewBind.bpmStatusView.setCurrentLevel(
            LeveDataFactory.HeartRate.indexFor(status)
        )
        mViewBind.tvLeveDes.text = getString(status.descriptionRes)
    }

    override fun getStatusBarColor() = R.color.c5
}
