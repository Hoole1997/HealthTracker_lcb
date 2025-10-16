package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.data.entity.HealthTag
import com.healthtracker.blood.suger.data.entity.HeartRateRecord
import com.healthtracker.blood.suger.data.enums.HeartRateStatus
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.ActivityHeartRateDetailBinding
import com.healthtracker.blood.suger.ui.dialog.LevelExplainDialog
import com.healthtracker.blood.suger.ui.act.HeartRateRecordActivity
import com.healthtracker.blood.suger.ui.dialog.ConfirmDialog
import com.healthtracker.blood.suger.ui.viewmodel.HeartRateDetailViewModel
import com.healthtracker.blood.suger.ui.weight.LeveDataFactory
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.collectLatest
import com.healthtracker.framework.ext.showToast
import com.healthtracker.framework.ext.startActivity
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
        with(mViewBind) {
            btnBack.click { finish() }
            btnDelete.click {
                showDeleteConfirm()
            }

            btnEdit.click {
                mViewModel.currentRecordId()?.let { id ->
                    HeartRateRecordActivity.start(this@HeartRateDetailActivity, id)
                } ?: showToast(getString(R.string.record_not_ready))
            }
        }
    }

    private fun setupStatusView() {
        val levels = LeveDataFactory.HeartRate.buildItems(this)
        mViewBind.bpmStatusView.apply {
            setLevels(levels)
            setExplainClickable(true)
            setOnExplainClick {
                LevelExplainDialog.show(
                    supportFragmentManager,
                    items = ArrayList(LeveDataFactory.HeartRate.buildExplainItems(this@HeartRateDetailActivity))
                )
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            collectLatest(mViewModel.record) { record ->
                if (record != null) {
                    updateRecord(record)
                }
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

        lifecycleScope.launch {
            collectLatest(mViewModel.tags) { tags ->
                updateTags(tags.take(2))
            }
        }

        lifecycleScope.launch {
            collectLatest(mViewModel.error) { error ->
                error?.let {
                    showToast(it)
                    mViewModel.clearError()
                }
            }
        }
    }

    private fun updateRecord(record: HeartRateRecord) {
        mViewBind.tvBpmValue.text = record.heartRateBpm.toString()
        mViewBind.tvTime.text = DateTimeUtils.formatDateTime(record.recordTime)
        val index = LeveDataFactory.HeartRate.indexFor(record.heartRateBpm)
        mViewBind.bpmStatusView.setCurrentLevel(index)
    }

    private fun updateStatus(status: HeartRateStatus) {
        mViewBind.bpmStatusView.setCurrentLevel(
            LeveDataFactory.HeartRate.indexFor(status)
        )
        mViewBind.tvLeveDes.text = getString(status.descriptionRes)
    }

    private fun updateTags(tags: List<HealthTag>) {
        mViewBind.tvTags.text = if (tags.isEmpty()) {
            getString(R.string.heart_rate_no_tags)
        } else {
            tags.joinToString(" · ") { it.name }
        }
    }
    override fun getStatusBarColor() = R.color.c5
    private fun showDeleteConfirm() {
        ConfirmDialog(
            title = getString(R.string.delete_record_remind_title),
            message = getString(R.string.delete_record_remind),
            leftText = getString(R.string.cancel),
            rightText = getString(R.string.confirm),
            onDialogListener = object : DialogListener {
                override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                    super.onItemClick(dialogFragment, which)
                    if (which == R.id.btn_ok) {
                        lifecycleScope.launch {
                            if (mViewModel.deleteRecord()) {
                                finish()
                            } else {
                                showToast(getString(R.string.delete_record_failed))
                            }
                        }
                    }
                }
            }
        ).show(supportFragmentManager)
    }

}
