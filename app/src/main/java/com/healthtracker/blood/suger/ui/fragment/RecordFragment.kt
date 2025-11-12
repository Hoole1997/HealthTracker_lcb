package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.healthtracker.blood.suger.databinding.FragmentRecordBinding
import com.healthtracker.blood.suger.ui.act.HistoryRecordActivity
import com.healthtracker.blood.suger.ui.history.HistoryRecordItem
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import com.healthtracker.framework.ext.clickWithDuration
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecordFragment: BaseMVVMFragment<BaseViewModel, FragmentRecordBinding>() {
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentRecordBinding.inflate(inflater, parent, attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
       mViewBind?.run {
           llBsHistory.clickWithDuration {
               HistoryRecordActivity.start(requireActivity())
           }

           llBpHistory.clickWithDuration {
               HistoryRecordActivity.start(requireActivity(),HistoryRecordItem.RecordType.BLOOD_PRESSURE)
           }
       }

    }
}