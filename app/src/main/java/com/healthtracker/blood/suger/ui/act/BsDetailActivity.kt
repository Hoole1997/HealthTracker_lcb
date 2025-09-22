package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import com.healthtracker.blood.suger.databinding.ActivityBsDetailBinding
import com.healthtracker.blood.suger.ui.viewmodel.BsDetailViewModel
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.ext.startActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BsDetailActivity: BaseMVVMActivity<BsDetailViewModel, ActivityBsDetailBinding>() {


    companion object{
        private const val RECORD_ID = "record_id"
        // 启动编辑模式
        fun start(context: Context, recordId: Long? = null) {
            context.startActivity<BsRecordActivity>(RECORD_ID to recordId)
        }
    }

    override fun createViewBinding() =  ActivityBsDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BsDetailViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
       with(mViewBind){
           btnBack.click {
               finish()
           }

       }
    }
}