package com.healthtracker.blood.suger.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.healthtracker.blood.suger.databinding.DialogAlarmTimeSelectBinding
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.click
import com.healthtracker.framework.util.FontUtils
import java.util.Calendar
import java.util.Locale
import com.healthtracker.blood.suger.data.utils.DateTimeUtils

class AlarmTimeSelectDialog(private val def: Pair<Int,Int>? = null,private val callBack:((Pair<Int, Int>)-> Unit)? = null) : BaseBottomSheetDialogFragment<DialogAlarmTimeSelectBinding>() {

    companion object{
        fun show(manager: FragmentManager, def: Pair<Int,Int>? = null, callBack:((Pair<Int, Int>)-> Unit)? = null){
            AlarmTimeSelectDialog(def,callBack).show(manager)
        }
    }
    constructor():this(null,null)

    private val current by lazy {

        if (def == null) {
            //获取当前时间
            val components = DateTimeUtils.extractDateComponents(DateTimeUtils.now())
            components.hour to components.minute
        } else {
            def
        }

    }
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogAlarmTimeSelectBinding.inflate(inflater, parent, attachToParent)

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.run {
            val font = FontUtils.getInstance().robotoBold
            hourPicker.setContentSelectedTextTypeface(font)
            minutePicker.setContentSelectedTextTypeface(font)
            setupHourPicker(current.first)
            setupMinutePicker(current.second)

            btnSave.click {
                val hour = hourPicker.contentByCurrValue.toInt()
                val minute = minutePicker.contentByCurrValue.toInt()
                callBack?.invoke(hour to minute)
                dismissAllowingStateLoss()
            }
        }
    }


    private fun setupHourPicker(currentHour: Int) {
        mViewBind?.run {
            val hours = Array(24) { i -> DateTimeUtils.formatTwoDigit(i) }
            hourPicker.displayedValues = hours
            // 先设置min/max值，再设置displayedValues
            hourPicker.minValue = 0
            hourPicker.maxValue = 23

            hourPicker.value = currentHour

        }
    }

    private fun setupMinutePicker(currentMinute: Int) {
       mViewBind?.run {
           val minutes = Array(60) { i -> DateTimeUtils.formatTwoDigit(i) }
           minutePicker.displayedValues = minutes
           // 先设置min/max值，再设置displayedValues
           minutePicker.minValue = 0
           minutePicker.maxValue = 59

           minutePicker.value = currentMinute
       }
    }
}