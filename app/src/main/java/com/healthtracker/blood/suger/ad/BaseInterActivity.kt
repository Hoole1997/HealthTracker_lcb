package com.healthtracker.blood.suger.ad

import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.healthtracker.blood.suger.utils.loadReword
import com.healthtracker.blood.suger.utils.showInter
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager

abstract class BaseInterActivity<VM : BaseViewModel, VB : ViewBinding>: BaseMVVMActivity<VM,VB>() {

    override fun handleBackPress(): Boolean {
        showInter {
            // 显示插屏广告后关闭 Activity
            finish()
        }
        // 返回 true 表示已处理返回键事件
        return true
    }


    protected fun showReword(){
       lifecycleScope.launch {
           loadReword {
               if(it){
                   hideMask()
               }
           }
       }
    }


    protected open fun hideMask(){

    }


}

fun reportAddRecord(type:String) = ReportDataManager.reportData("add_new_record",mapOf("page_name" to type))