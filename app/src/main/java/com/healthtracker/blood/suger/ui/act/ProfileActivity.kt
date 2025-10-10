package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.constants.KEY_IS_NEW_USER
import com.healthtracker.blood.suger.databinding.ActivityProfileBinding
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.isMale
import com.healthtracker.blood.suger.saveUserAge
import com.healthtracker.blood.suger.saveUserGender
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.FontUtils
import com.healthtracker.framework.util.SpUtils

class ProfileActivity: BaseMVVMActivity<BaseViewModel, ActivityProfileBinding>() {
    companion object{
        private const val TAG = "ProfileActivity"
    }

    private var age = getUserAge()
    private var gender = if(isMale()) 0 else 1




    override fun createViewBinding() = ActivityProfileBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind){
            btnContinue.clickWithDuration {
                saveUserAge(age)
                saveUserGender(gender)
                SpUtils.putBoolean(KEY_IS_NEW_USER,false)
                startActivity<MainActivity>(isFinishSelf = true)
            }

            rbMale.isChecked = isMale()
            rgGender.setOnCheckedChangeListener { _,checkedId ->
                tvMale.isChecked = false
                tvFemale.isChecked = false
                when(checkedId){
                    R.id.rb_male -> {
                        tvMale.isChecked = true
                        gender = 0
                    }
                    else -> {
                        tvFemale.isChecked = true
                        gender = 1
                    }
                }


            }

            val selectFont = FontUtils.getInstance().robotoBold
            val normalFont = FontUtils.getInstance().robotoLight
            numberPicker.setContentSelectedTextTypeface(selectFont)
            numberPicker.setContentNormalTextTypeface(normalFont)
            numberPicker.displayedValues = Array(95) { i -> (i + 6).toString() }
            numberPicker.minValue = 6
            numberPicker.maxValue = 100
            numberPicker.value = numberPicker.displayedValues.indexOf((age + 6).toString())
            numberPicker.setOnValueChangedListener { _,_, newVal ->
                age = newVal
            }
        }
    }
}