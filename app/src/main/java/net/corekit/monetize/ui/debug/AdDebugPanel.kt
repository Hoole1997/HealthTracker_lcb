package net.corekit.monetize.ui.debug

import android.app.Activity
import com.android.common.bill.ads.bidding.AdSourceController

object AdDebugPanel {
    fun showDebugDialog(activity: Activity) {
        AdSourceController.showAdSourceSelection(activity)
    }
}
