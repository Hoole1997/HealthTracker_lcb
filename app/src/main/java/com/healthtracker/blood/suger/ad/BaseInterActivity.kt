package com.healthtracker.blood.suger.ad

import androidx.viewbinding.ViewBinding
import com.healthtracker.blood.suger.utils.showInter
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel

abstract class BaseInterActivity<VM : BaseViewModel, VB : ViewBinding>: BaseMVVMActivity<VM,VB>() {

    override fun onBackPress() {
        showInter {
            finish()
        }
    }


}