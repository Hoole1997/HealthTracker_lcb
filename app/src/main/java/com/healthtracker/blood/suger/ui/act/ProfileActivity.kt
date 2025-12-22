package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.constants.KEY_HAS_ADD_PROFILE
import com.healthtracker.blood.suger.databinding.HtActivityProfileBinding
import com.healthtracker.blood.suger.getUserAge
import com.healthtracker.blood.suger.isMale
import com.healthtracker.blood.suger.saveUserAge
import com.healthtracker.blood.suger.saveUserGender
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible
import com.healthtracker.framework.util.SpUtils
import com.healthtracker.framework.util.getRobotoBold
import com.healthtracker.framework.util.getRobotoRegular
import com.hjq.toast.Toaster
import net.corekit.monetize.ui.NativeAdStyle

class ProfileActivity: BaseInterActivity<BaseViewModel, HtActivityProfileBinding>() {
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

        fun creteEditIntent(context: Context) = Intent(context, ProfileActivity::class.java).apply {
            putExtra(EXTRA_LAUNCH_MODE, MODE_SETTINGS)
        }
    }

    private var age = getUserAge()
    private var gender = if(isMale()) 0 else 1

    private val hasGuide = SpUtils.getBoolean(KEY_HAS_ADD_PROFILE,false)
    
    // 保存初始值，用于比较是否有变化
    private val initialAge = age
    private val initialGender = gender

    private var launchMode = MODE_SETTINGS




    override fun createViewBinding() = HtActivityProfileBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        launchMode = resolveLaunchMode(intent)

        with(mViewBind){
            if(launchMode != MODE_GUIDE){
                groupGuide.gone()
                btnSave.visible()
                
                // 设置 btnSave 初始状态
                updateSaveButtonState()
                
                // 添加 btnSave 点击事件
                btnSave.clickWithDuration {
                    handleSaveAndFinish()
                }
            }else{
                reportGuide(9)
            }
            btnContinue.clickWithDuration {
                reportGuide(10)
                handleSaveAndFinish()
            }

            tvSkip.clickWithDuration {
                reportGuide(10)
                handleSaveAndFinish()
            }

            btnBack.clickWithDuration {
                onBackPress()
            }

            // 先设置初始状态
            rbMale.isChecked = isMale()
            tvMale.isChecked = isMale()
            tvFemale.isChecked = !isMale()
            
            // 再注册监听器，避免初始化时触发导致 gender 值被修改
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
                
                // 性别变化时更新按钮状态
                updateSaveButtonState()
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
                
                // 年龄变化时更新按钮状态
                updateSaveButtonState()
            }
            loadNative(adContainer, style = NativeAdStyle.CARD_7)
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
    
    /**
     * 更新保存按钮的启用状态
     * - 如果未保存过配置（hasGuide == false），始终启用
     * - 如果已保存过配置（hasGuide == true），仅在值发生变化时启用
     */
    private fun updateSaveButtonState() {
        if (launchMode != MODE_GUIDE) {
            val hasChanges = age != initialAge || gender != initialGender
            mViewBind.btnSave.isEnabled = !hasGuide || hasChanges
        }
    }

    private fun handleSaveAndFinish() {
        saveUserAge(age)
        saveUserGender(gender)
        SpUtils.putBoolean(KEY_HAS_ADD_PROFILE, true)
        Toaster.show(getString(R.string.save_success))

        when (launchMode) {
            MODE_GUIDE -> {
                setResult(RESULT_OK)
                finish()
            }
            else -> {
                finish()
            }

        }
    }
}
