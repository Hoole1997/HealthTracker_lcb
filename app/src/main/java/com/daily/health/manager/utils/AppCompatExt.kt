package com.daily.health.manager.utils

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle

/** 权限返回时仅在页面仍可交互的情况下继续；与广告加载无关。 */
fun FragmentActivity.safeLaunch(afterInvoke: () -> Unit) {
    if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
        afterInvoke()
    }
}
