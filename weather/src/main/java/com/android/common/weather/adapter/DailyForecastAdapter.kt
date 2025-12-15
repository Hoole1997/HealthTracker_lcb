package com.android.common.weather.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.common.weather.databinding.ItemDailyWeatherBinding
import com.android.common.weather.model.AccuWeatherDailyForecast
import com.android.common.weather.util.TemperaturePreferences
import com.android.common.weather.util.TemperatureUtils
import com.android.common.weather.util.WeatherIconMapper
import com.android.common.weather.util.fahrenheitToCelsius
import com.healthtracker.framework.util.LanguageUtils
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

class DailyForecastAdapter : ListAdapter<AccuWeatherDailyForecast, DailyForecastAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyWeatherBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        
        // 动态设置 item 宽度，使其平均分配 RecyclerView 宽度
        // 假设显示 5 天数据
        val itemWidth = parent.measuredWidth / 5
        binding.root.layoutParams = ViewGroup.LayoutParams(
            itemWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemDailyWeatherBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(forecast: AccuWeatherDailyForecast) {
            with(binding) {
                // 绑定日期
                forecast.date?.let {
                    try {
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                        val dayFormat = SimpleDateFormat("EEE", LanguageUtils.getAppLocale(binding.root.context))
                        val date = isoFormat.parse(it)
                        tvDate.text = date?.let { d -> dayFormat.format(d) } ?: "N/A"
                    } catch (e: Exception) {
                        tvDate.text = "N/A"
                    }
                }

                // 绑定天气图标
                forecast.day?.icon?.let {
                    ivWeather.setImageResource(WeatherIconMapper.getIconResource(it))
                }
                
                // 绑定温度范围（API返回华氏度，转换为当前单位）
                val isCelsius = TemperaturePreferences.isCelsius()
                val highTemp = if (isCelsius) {
                    forecast.temperature?.maximum?.value?.fahrenheitToCelsius() ?: 0
                } else {
                    forecast.temperature?.maximum?.value?.roundToInt() ?: 0
                }
                val lowTemp = if (isCelsius) {
                    forecast.temperature?.minimum?.value?.fahrenheitToCelsius() ?: 0
                } else {
                    forecast.temperature?.minimum?.value?.roundToInt() ?: 0
                }
                tvTemperatureRange.text = "$lowTemp°/$highTemp°"

                
                // 绑定降水概率
                val rainPercent = forecast.day?.precipitationProbability ?: 0
                tvRainPercent.text = "$rainPercent%"
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AccuWeatherDailyForecast>() {
        override fun areItemsTheSame(oldItem: AccuWeatherDailyForecast, newItem: AccuWeatherDailyForecast):
                Boolean = oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: AccuWeatherDailyForecast, newItem: AccuWeatherDailyForecast):
                Boolean = oldItem == newItem
    }
}