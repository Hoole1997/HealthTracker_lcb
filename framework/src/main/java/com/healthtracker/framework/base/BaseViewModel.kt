package com.healthtracker.framework.base

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
open class BaseViewModel @Inject constructor() : ViewModel(){
    val toastStr = MutableLiveData<Int>()

    fun showToast(@StringRes resId: Int) {
        toastStr.postValue(resId)
    }
}