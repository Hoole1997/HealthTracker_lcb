package com.healthtracker.blood.suger.ui.act

import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.constants.KEY_HAS_ADD_PROFILE
import com.healthtracker.blood.suger.databinding.ActivityProfileBinding
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.isMale
import com.healthtracker.blood.suger.saveUserAge
import com.healthtracker.blood.suger.saveUserGender
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
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
                SpUtils.putBoolean(KEY_HAS_ADD_PROFILE,true)
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
            val ages = (2..110).map { it.toString() }.toTypedArray()
            numberPicker.displayedValues = ages
            numberPicker.minValue = 0
            numberPicker.maxValue = ages.lastIndex
            val normalizedAge = age.coerceIn(2, 110)
            val currentIndex = ages.indexOf(normalizedAge.toString())
            numberPicker.value = if (currentIndex >= 0) currentIndex else 0
            age = if (currentIndex >= 0) normalizedAge else ages.first().toInt()
            numberPicker.setOnValueChangedListener { _,_, newVal ->
                age = newVal
            }
        }
    }
}