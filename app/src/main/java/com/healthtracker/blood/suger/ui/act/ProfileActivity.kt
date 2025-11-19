package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.constants.KEY_HAS_ADD_PROFILE
import com.healthtracker.blood.suger.databinding.ActivityProfileBinding
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.isMale
import com.healthtracker.blood.suger.saveUserAge
import com.healthtracker.blood.suger.saveUserGender
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.startActivity
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoRegular

class ProfileActivity: BaseInterActivity<BaseViewModel, ActivityProfileBinding>() {
    companion object{
        private const val TAG = "ProfileActivity"

        private const val EXTRA_LAUNCH_MODE = "extra_launch_mode"
        const val MODE_SETTINGS = 0
        const val MODE_GUIDE = 1

        fun createGuideIntent(context: Context): Intent {
            return Intent(context, ProfileActivity::class.java).apply {
                putExtra(EXTRA_LAUNCH_MODE, MODE_GUIDE)
            }
        }
    }

    private var age = getUserAge()
    private var gender = if(isMale()) 0 else 1

    private var launchMode = MODE_SETTINGS




    override fun createViewBinding() = ActivityProfileBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        launchMode = resolveLaunchMode(intent)

        with(mViewBind){
            btnContinue.clickWithDuration {
                handleSaveAndFinish()
            }

            tvSkip.clickWithDuration {
                handleSaveAndFinish()
            }

            btnBack.clickWithDuration {
                onBackPress()
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

            val selectFont = getRobotoBold(this@ProfileActivity)
            val normalFont = getRobotoRegular(this@ProfileActivity)
            numberPicker.setContentSelectedTextTypeface(selectFont)
            numberPicker.setContentNormalTextTypeface(normalFont)
            val ages = (1..110).map { it.toString() }.toTypedArray()
            numberPicker.displayedValues = ages
            numberPicker.minValue = 0
            numberPicker.maxValue = ages.lastIndex
            val normalizedAge = age.coerceIn(1, 110)
            val currentIndex = ages.indexOf(normalizedAge.toString())
            numberPicker.value = if (currentIndex >= 0) currentIndex else 0
            age = if (currentIndex >= 0) normalizedAge else ages.first().toInt()
            numberPicker.setOnValueChangedListener { _,_, newVal ->
                age = ages[newVal].toInt()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchMode = resolveLaunchMode(intent)
    }

    private fun resolveLaunchMode(sourceIntent: Intent?): Int {
        return sourceIntent?.getIntExtra(EXTRA_LAUNCH_MODE, MODE_SETTINGS) ?: MODE_SETTINGS
    }

    private fun handleSaveAndFinish() {
        saveUserAge(age)
        saveUserGender(gender)
        SpUtils.putBoolean(KEY_HAS_ADD_PROFILE, true)

        when (launchMode) {
            MODE_GUIDE -> {
                setResult(RESULT_OK)
                finish()
            }
            else -> {
                startActivity<MainActivity>(isFinishSelf = true)
            }
        }
    }
}
