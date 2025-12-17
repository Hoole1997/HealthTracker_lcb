package com.app.raise.config

import com.app.raise.AppraiseManager
import com.app.raise.R

class EvaluateConfig {
    var isRtl: Boolean = false
    var isSupportRTL: Boolean = false
    var evaluateStringRes: Int = R.string.btn_feedback
    var star5GoMarket: Boolean = false
    var marketUrl: String = ""
    var marketPackage: String = AppraiseManager.MARKET_GOOGLE
    var canceledOnTouchOutside: Boolean = false
    var allowIndonesia: Boolean = false
}
