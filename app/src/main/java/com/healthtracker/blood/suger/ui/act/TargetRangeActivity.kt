package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityTargetRangeBinding
import com.healthtracker.blood.suger.ui.adapter.TargetRangeAdapter
import com.healthtracker.blood.suger.ui.dialog.RangeEditDialog
import com.healthtracker.blood.suger.ui.viewmodel.TargetRangeViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import android.app.AlertDialog
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * 血糖目标范围设置页面
 */
@AndroidEntryPoint
class TargetRangeActivity : BaseMVVMActivity<TargetRangeViewModel, ActivityTargetRangeBinding>() {

    private lateinit var adapter: TargetRangeAdapter
    private var hasChanged = false  // 标记是否有修改

    override fun createViewBinding() = ActivityTargetRangeBinding.inflate(layoutInflater)

    override fun getVMModelClass() = TargetRangeViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupToolbar()
        setupRecyclerView()
        observeData()
    }

    /**
     * 设置标题栏
     */
    private fun setupToolbar() {
        mViewBind.btnBack.click {
            finishWithResult()
        }

        mViewBind.tvReset.click {
            showResetConfirmDialog()
        }
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = TargetRangeAdapter(
            currentUnit = mViewModel.currentUnit.value,
            onItemClick = { rangeItem ->
                showEditDialog(rangeItem)
            }
        )

        mViewBind.rvRange.apply {
            layoutManager = LinearLayoutManager(this@TargetRangeActivity)
            adapter = this@TargetRangeActivity.adapter
        }
    }

    /**
     * 观察数据变化
     */
    private fun observeData() {
        lifecycleScope.launch {
            mViewModel.rangeItems.collect { items ->
                adapter.submitList(items)
            }
        }

        lifecycleScope.launch {
            mViewModel.currentUnit.collect { unit ->
                // 单位变化时更新adapter
                adapter = TargetRangeAdapter(
                    currentUnit = unit,
                    onItemClick = { rangeItem ->
                        showEditDialog(rangeItem)
                    }
                )
                mViewBind.rvRange.adapter = adapter
                adapter.submitList(mViewModel.rangeItems.value)
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    private fun showEditDialog(rangeItem: com.healthtracker.blood.suger.ui.viewmodel.RangeItem) {
        RangeEditDialog.newInstance(
            status = rangeItem.status,
            unit = mViewModel.currentUnit.value,
            onSave = { status, unit, ranges ->
                // 1. 如果单位发生变化，先切换单位
                if (unit != mViewModel.currentUnit.value) {
                    mViewModel.switchUnit(unit)
                }
                // 2. 保存范围值
                mViewModel.saveRanges(status, unit, ranges)
                hasChanged = true
                showToast(getString(R.string.save_success))
            }
        ).show(supportFragmentManager, "RangeEditDialog")
    }

    /**
     * 显示重置确认对话框
     */
    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset)
            .setMessage(R.string.reset_all_ranges_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                mViewModel.resetAllRanges()
                hasChanged = true
                showToast(getString(R.string.reset_success))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * 完成并返回结果
     */
    private fun finishWithResult() {
        if (hasChanged) {
            setResult(RESULT_OK)
        }
        finish()
    }

    override fun onBackPressed() {
        finishWithResult()
        super.onBackPressed()
    }
}