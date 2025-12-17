package com.app.raise.listeners

import android.app.Dialog


interface EvaluateListener {

    fun evaluateUs(evaluateScore: Int)

    fun feedback(evaluateScore: Int)

    fun cancelDialog(dialog:Dialog)

    fun dismissDialog(dialog:Dialog)

    fun sendEvent(var1: String?, var2: String?, var3: String?)

    fun sendException(throwable: Throwable?)
}

