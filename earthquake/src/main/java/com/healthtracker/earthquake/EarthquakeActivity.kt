package com.healthtracker.earthquake

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import com.healthtracker.earthquake.databinding.ActivityEarthquakeBinding
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EarthquakeActivity : BaseMVVMActivity<BaseViewModel, ActivityEarthquakeBinding>() {
    private val scope = MainScope()
    override fun createViewBinding() = ActivityEarthquakeBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java
    override fun initView(savedInstanceState: Bundle?) {
        with(mViewBind){
            ivBack.clickWithDuration {
                finish()
            }
            val mag = intent.getDoubleExtra("eq_extra_mag", -1.0)
            val place = intent.getStringExtra("eq_extra_place")
            val time = intent.getLongExtra("eq_extra_time", -1L)
            val magType = intent.getStringExtra("eq_extra_mag_type")
            val status = intent.getStringExtra("eq_extra_status")
            val tsunami = intent.getIntExtra("eq_extra_tsunami", 0)
            val alert = intent.getStringExtra("eq_extra_alert")
            val depth = intent.getDoubleExtra("eq_extra_depth", Double.NaN)

            tvMagnitude.text = if (mag >= 0) String.format(Locale.getDefault(), "%.1f", mag) else "-"
            tvMagnitudeUnit.text = "M"
            tvLocation.text = place ?: "-"
            tvDepth.text = if (!depth.isNaN()) String.format(Locale.getDefault(), "%.2f km", depth) else "-"
            tvTsunami.text = if (tsunami == 1) "Yes" else "No"
            tvWarning.text = alert?.uppercase(Locale.getDefault()) ?: "-"
            tvMagType.text = if (!magType.isNullOrBlank()) magType else getString(R.string.ht_mww)
            tvDataStatus.text = if (!status.isNullOrBlank()) status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } else getString(R.string.ht_reviewed)

            if (time > 0) {
                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                tvOriginTime.text = sdf.format(Date(time))
            }
            lifecycleScope.launch {
                val success = EarthquakeAdBridge.showNativeAd(this@EarthquakeActivity, adContainer)
                if(success){
                    adContainer.visible()
                }else{
                    adContainer.gone()
                }
            }

            applyMagnitudeColors(mag)
        }
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


       with(mViewBind){
           (earthquakeCard.background as? GradientDrawable)?.setColor(cardColor.toColorInt())
           (earthquakeInfoBar.background as? GradientDrawable)?.setColor(barColor.toColorInt())
       }
    }
}
