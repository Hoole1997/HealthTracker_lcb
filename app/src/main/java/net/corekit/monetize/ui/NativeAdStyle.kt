package net.corekit.monetize.ui

import com.android.common.bill.ui.NativeAdStyleType

enum class NativeAdStyle(private val remaxStyleType: NativeAdStyleType) {
    STANDARD(NativeAdStyleType.STANDARD),
    CARD(NativeAdStyleType.LARGE),
    CARD_3(NativeAdStyleType.LARGE),
    CARD_4(NativeAdStyleType.LARGE),
    CARD_5(NativeAdStyleType.LARGE),
    CARD_6(NativeAdStyleType.LARGE),
    CARD_7(NativeAdStyleType.LARGE),
    CARD_8(NativeAdStyleType.LARGE);

    fun toRemaxStyleType(): NativeAdStyleType = remaxStyleType
}
