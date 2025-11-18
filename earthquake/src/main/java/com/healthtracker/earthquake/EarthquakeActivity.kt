package com.healthtracker.earthquake

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.constraintlayout.widget.ConstraintLayout
import android.widget.LinearLayout
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.corekit.monetize.ads.NativeAds
import net.corekit.monetize.ui.NativeAdStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt

class EarthquakeActivity : Activity() {
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_earthquake)

        findViewById<ImageView>(R.id.ivBack).setOnClickListener { finish() }

        val mag = intent.getDoubleExtra("eq_extra_mag", -1.0)
        val place = intent.getStringExtra("eq_extra_place")
        val time = intent.getLongExtra("eq_extra_time", -1L)
        val magType = intent.getStringExtra("eq_extra_mag_type")
        val status = intent.getStringExtra("eq_extra_status")
        val tsunami = intent.getIntExtra("eq_extra_tsunami", 0)
        val alert = intent.getStringExtra("eq_extra_alert")
        val depth = intent.getDoubleExtra("eq_extra_depth", Double.NaN)

        findViewById<TextView>(R.id.tvMagnitude)?.text = if (mag >= 0) String.format(Locale.getDefault(), "%.1f", mag) else "-"
        findViewById<TextView>(R.id.tvMagnitudeUnit)?.text = "M"
        findViewById<TextView>(R.id.tvLocation)?.text = place ?: "-"
        findViewById<TextView>(R.id.tvDepth)?.text = if (!depth.isNaN()) String.format(Locale.getDefault(), "%.2f km", depth) else "-"
        findViewById<TextView>(R.id.tvTsunami)?.text = if (tsunami == 1) "Yes" else "No"
        findViewById<TextView>(R.id.tvWarning)?.text = alert?.uppercase(Locale.getDefault()) ?: "-"
        findViewById<TextView>(R.id.tvMagType)?.text = if (!magType.isNullOrBlank()) magType else getString(R.string.mww)
        findViewById<TextView>(R.id.tvDataStatus)?.text = if (!status.isNullOrBlank()) status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else getString(R.string.reviewed)

        if (time > 0) {
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            findViewById<TextView>(R.id.tvOriginTime)?.text = sdf.format(Date(time))
        }

        val adContainer = findViewById<ViewGroup>(R.id.adContainer)
        scope.launch {
            val success = NativeAds.getInstance().displayAdInView(
                context = this@EarthquakeActivity,
                container = adContainer,
                style = NativeAdStyle.CARD_3
            )
            adContainer.visibility = if (success) View.VISIBLE else View.GONE
        }

        applyMagnitudeColors(mag)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun applyMagnitudeColors(mag: Double) {
        val (cardColor, barColor) = when {
            mag >= 7.0 -> Pair("#f75846", "#f86555")
            mag >= 5.0 -> Pair("#f79e46", "#f8aa5c")
            mag >= 3.0 -> Pair("#f3c700", "#f4ce1f")
            else -> Pair("#aaaaaa", "#bcbcbc")
        }

        val card = findViewById<ConstraintLayout>(R.id.earthquake_card)
        val bar = findViewById<LinearLayout>(R.id.earthquake_info_bar)

        (card.background as? GradientDrawable)?.setColor(cardColor.toColorInt())
        (bar.background as? GradientDrawable)?.setColor(barColor.toColorInt())
    }
}