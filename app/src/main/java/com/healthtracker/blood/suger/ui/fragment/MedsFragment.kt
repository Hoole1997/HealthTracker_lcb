package com.healthtracker.blood.suger.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import com.healthtracker.blood.suger.data.utils.DateTimeUtils
import com.healthtracker.blood.suger.databinding.FragmentMedsBinding
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.base.fragment.BaseMVVMFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MedsFragment: BaseMVVMFragment<BaseViewModel, FragmentMedsBinding>() {
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = FragmentMedsBinding.inflate(layoutInflater,parent,attachToParent)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        // Fragment初始化逻辑
        // TODO: 这里将来会初始化周视图组件
         mViewBind?.run {
             weeklyDateSelector.setDefaultSelectedDate(DateTimeUtils.now())
         }
    }

    
    /**
     * 获取格式化的月份字符串
     * 供MainActivity调用以显示月份信息
     * 统一使用DateTimeUtils工具类进行格式化
     * @return 格式化的月份字符串，如"Sep.2025"
     */
    fun getFormattedMonth(): String {
        return DateTimeUtils.getCurrentMonthYear()
    }
}