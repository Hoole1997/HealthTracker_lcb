package com.healthtracker.framework.base

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel(){
    val toastStr = MutableLiveData<Int>()

    fun showToast(@StringRes resId: Int) {
        toastStr.postValue(resId)
    }
}