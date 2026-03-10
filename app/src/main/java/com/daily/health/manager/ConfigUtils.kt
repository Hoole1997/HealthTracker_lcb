package com.daily.health.manager

import com.daily.health.manager.constants.KEY_HAS_ADD_PROFILE
import com.daily.health.manager.constants.KEY_HAS_GUIDE_NEW
import com.daily.health.manager.constants.KEY_HAS_SHOW_GUIDE_BP
import com.daily.health.manager.constants.KEY_HAS_SHOW_GUIDE_BS
import com.daily.health.manager.constants.KEY_HAS_SHOW_GUIDE_HR
import com.daily.health.manager.constants.KEY_HAS_SHOW_HOME_ENTRY_GUIDE_V2
import com.daily.health.manager.constants.KEY_IS_NEW_USER
import com.daily.health.manager.constants.KEY_USER_AGE
import com.daily.health.manager.constants.KEY_USER_GENDER
import com.healthtracker.framework.util.SpUtils

fun saveUserAge(age:Int) = SpUtils.putInt(KEY_USER_AGE,age)

fun getUserAge() = SpUtils.getInt(KEY_USER_AGE,40)


fun isMale() = SpUtils.getInt(KEY_USER_GENDER,0) == 0

fun saveUserGender(type:Int) = SpUtils.putInt(KEY_USER_GENDER,type)


fun isNewUser() = SpUtils.getBoolean(KEY_IS_NEW_USER,true)

/**
 * 用户是否设置年龄and性别
 */
fun hasAddProfile() = SpUtils.getBoolean(KEY_HAS_ADD_PROFILE,false)

fun saveHasNewGuide() = SpUtils.putBoolean(KEY_HAS_GUIDE_NEW,true)

fun hasNewGuide() = SpUtils.getBoolean(KEY_HAS_GUIDE_NEW,false)


fun saveShowGuideBp() = SpUtils.putBoolean(KEY_HAS_SHOW_GUIDE_BP,true)

fun hasShowGuideBp() = SpUtils.getBoolean(KEY_HAS_SHOW_GUIDE_BP,false)

fun saveShowGuideBs() = SpUtils.putBoolean(KEY_HAS_SHOW_GUIDE_BS,true)

fun hasShowGuideBs() = SpUtils.getBoolean(KEY_HAS_SHOW_GUIDE_BS,false)

fun saveShowGuideHr() = SpUtils.putBoolean(KEY_HAS_SHOW_GUIDE_HR,true)

fun hasShowGuideHr() = SpUtils.getBoolean(KEY_HAS_SHOW_GUIDE_HR,false)



fun saveShowHomeEntryGuideV2() = SpUtils.putBoolean(KEY_HAS_SHOW_HOME_ENTRY_GUIDE_V2,true)

fun hasShowHomeEntryGuideV2() = SpUtils.getBoolean(KEY_HAS_SHOW_HOME_ENTRY_GUIDE_V2,false)

fun hasShowAllGuide() = hasShowGuideBs()


