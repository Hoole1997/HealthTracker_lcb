package com.daily.health.manager.feature

import org.junit.Assert.assertFalse
import org.junit.Test

class NotificationFeatureSwitchTest {

    @Test
    fun notifications_are_globally_disabled() {
        assertFalse(NotificationFeatureSwitch.notificationsEnabled)
        assertFalse(NotificationFeatureSwitch.reminderEntryEnabled)
        assertFalse(NotificationFeatureSwitch.hotResumeSplashAdEnabled)
        assertFalse(NotificationFeatureSwitch.notificationPermissionPromptEnabled)
    }
}
