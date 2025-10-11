package com.healthtracker.blood.suger.utils

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle


fun FragmentActivity.safeLaunch(afterInvoke: () -> Unit) {
    if (this.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        afterInvoke.invoke()
    }
}