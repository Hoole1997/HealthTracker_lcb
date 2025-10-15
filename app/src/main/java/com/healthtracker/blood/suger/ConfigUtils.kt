package com.healthtracker.blood.suger

import com.healthtracker.blood.suger.constants.KEY_HAS_ADD_PROFILE
import com.healthtracker.blood.suger.constants.KEY_IS_NEW_USER
import com.healthtracker.blood.suger.constants.KEY_USER_AGE
import com.healthtracker.blood.suger.constants.KEY_USER_GENDER
import com.healthtracker.framework.util.SpUtils

fun saveUserAge(age:Int) = SpUtils.putInt(KEY_USER_AGE,age)

fun getUserAge() = SpUtils.getInt(KEY_USER_AGE,45)


fun isMale() = SpUtils.getInt(KEY_USER_GENDER,0) == 0

fun saveUserGender(type:Int) = SpUtils.putInt(KEY_USER_GENDER,type)


fun isNewUser() = SpUtils.getBoolean(KEY_IS_NEW_USER,true)

/**
 * 用户是否设置年龄and性别
 */
fun hasAddProfile() = SpUtils.getBoolean(KEY_HAS_ADD_PROFILE,false)
