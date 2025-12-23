package com.daily.health.manager.ui.act

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.daily.health.manager.R
import com.daily.health.manager.data.enums.BsUnit
import com.daily.health.manager.databinding.HtActivityTargetRangeBinding
import com.daily.health.manager.ui.adapter.TargetRangeAdapter
import com.daily.health.manager.ui.dialog.ConfirmDialog
import com.daily.health.manager.ui.dialog.RangeEditDialog
import com.daily.health.manager.ui.viewmodel.TargetRangeViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.fragment.DialogListener
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.showToast
import kotlinx.coroutines.launch

/**
 * 血糖目标范围设置页面
 */
class TargetRangeActivity : BaseMVVMActivity<TargetRangeViewModel, HtActivityTargetRangeBinding>() {

    private lateinit var adapter: TargetRangeAdapter
    private var hasChanged = false  // 标记是否有修改

    override fun createViewBinding() = HtActivityTargetRangeBinding.inflate(layoutInflater)

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

        mViewBind.tvTitle.text = getString(R.string.ht_target_range_temp, BsUnit.getPreferredUnit().displayName)
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

        // 观察是否有自定义范围，控制 Reset 按钮显示
        lifecycleScope.launch {
            mViewModel.hasAnyCustomRanges.collect { hasCustom ->
                updateResetButtonVisibility(hasCustom)
            }
        }
    }

    /**
     * 显示编辑对话框
     */
    private fun showEditDialog(rangeItem: com.daily.health.manager.ui.viewmodel.RangeItem) {
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
                showToast(getString(R.string.ht_save_success))
            }
        ).show(supportFragmentManager, "RangeEditDialog")
    }

    /**
     * 显示重置确认对话框
     */
    private fun showResetConfirmDialog() {
        ConfirmDialog(
            getString(R.string.ht_reset),
            getString(R.string.ht_reset_all_ranges_confirm),
            leftText = getString(R.string.ht_cancel),
            rightText = getString(R.string.ht_confirm),
            onDialogListener = object :DialogListener{
                override fun onItemClick(dialogFragment: DialogFragment, which: Int) {
                    super.onItemClick(dialogFragment, which)
                    if(which == R.id.btn_ok){
                        mViewModel.resetAllRanges()
                        hasChanged = true
                        showToast(getString(R.string.ht_reset_success))
                    }
                }
            }
        ).show(supportFragmentManager)
    }

    /**
     * 更新 Reset 按钮的可见性
     * @param hasCustomRanges 是否有任何自定义范围
     */
    private fun updateResetButtonVisibility(hasCustomRanges: Boolean) {
        mViewBind.tvReset.visibility = if (hasCustomRanges) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
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