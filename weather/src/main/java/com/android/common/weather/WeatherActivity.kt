package com.android.common.weather

import android.graphics.Color
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.common.weather.adapter.DailyForecastAdapter
import com.android.common.weather.databinding.ActivityWeatherBinding
import com.android.common.weather.model.AccuWeatherDailyForecast
import com.android.common.weather.model.CurrentConditionResponse
import com.android.common.weather.model.Headline
import com.android.common.weather.util.TemperaturePreferences
import com.android.common.weather.util.WeatherIconMapper
import com.android.common.weather.util.celsiusToFahrenheit
import com.android.common.weather.util.fahrenheitToCelsius
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.loge
import com.healthtracker.framework.util.LanguageUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class WeatherActivity : BaseMVVMActivity<WeatherViewModel, ActivityWeatherBinding>(),
    SearchDialogCallback {
    
    companion object {
        private const val TAG = "WeatherActivity"
    }
    
    private val adapter by lazy {
        DailyForecastAdapter()
    }

    private var lastWeatherState: WeatherUiState.Success? = null
    
    override fun createViewBinding() = ActivityWeatherBinding.inflate(layoutInflater)

    override fun getVMModelClass() = WeatherViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupActionBar()
        setupRecyclerView()
        setupSwipeRefresh()
        observeWeatherData()
        observeSearchResult()
    }
    
    /**
     * 设置下拉刷新
     */
    private fun setupSwipeRefresh() {
        mViewBind.srlRefresh.setOnRefreshListener {
            "Swipe refresh triggered".logd(TAG)
            mViewModel.requestWeather(forceRefresh = true)
        }
        
        // 设置刷新指示器颜色
        mViewBind.srlRefresh.setColorSchemeResources(
             R.color.color_30BAFF,
             R.color.color_02BC77
        )
    }

    /**
     * 设置顶部操作栏
     */
    private fun setupActionBar() {
        with(mViewBind) {
            btnBack.clickWithDuration {
                finish()
            }
            
            // 点击温度单位切换
            tvTemperatureUnit.text = if (TemperaturePreferences.isCelsius()) "°C" else "°F"
            tvTemperatureUnit.clickWithDuration {
                UnitMenu(this@WeatherActivity) { unitValue ->
                    TemperaturePreferences.saveUnit(unitValue)
                    tvTemperatureUnit.text = if (TemperaturePreferences.isCelsius()) "°C" else "°F"
                    lastWeatherState?.let { bindWeatherData(it) }
                }.apply {
                    isFocusable = true
                    isOutsideTouchable = true
                    setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                }.show(tvTemperatureUnit)
            }
            tvCityName.clickWithDuration {
                showSearchDialog()
            }
        }
    }
    
    /**
     * 设置 RecyclerView
     */
    private fun setupRecyclerView() {
        with(mViewBind.rv5DayWeather) {
            layoutManager = LinearLayoutManager(
                this@WeatherActivity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = this@WeatherActivity.adapter
        }
    }
    
    /**
     * 观察天气数据变化
     */
    private fun observeWeatherData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.uiState.collect { state ->
                    when (state) {
                        is WeatherUiState.Loading -> {
                            "Loading weather data...".logd(TAG)
                            // 统一使用下拉刷新动画显示加载状态
                            mViewBind.srlRefresh.isRefreshing = true
                        }
                        is WeatherUiState.Success -> {
                            "Weather data received".logd(TAG)
                            mViewBind.srlRefresh.isRefreshing = false
                            bindWeatherData(state)
                            // 如果 SearchDialog 正在显示，更新其当前城市
                            SearchDialog.getInstance()?.updateCurrentCity(state.cityInfo)
                        }
                        is WeatherUiState.Error -> {
                            "Weather error: ${state.message}".loge(TAG)
                            mViewBind.srlRefresh.isRefreshing = false
                            showError(state.message)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 观察搜索结果
     */
    private fun observeSearchResult() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mViewModel.searchResult.collect { response ->
                    response?.let {
                        "Search result received in Activity".logd(TAG)
                        SearchDialog.getInstance()?.updateSearchResult(it)
                    }
                }
            }
        }
    }
    
    /**
     * 显示搜索对话框
     */
    private fun showSearchDialog() {
        val currentCityInfo = lastWeatherState?.cityInfo
        SearchDialog.show(supportFragmentManager, currentCityInfo).setCallback(this)
    }
    
    // ==================== SearchDialogCallback 实现 ====================
    
    override fun onSearchAddress(address: String) {
        "Activity: onSearchAddress called with: $address".logd(TAG)
        mViewModel.searchWeatherByAddress(address)
    }
    
    override fun onRefreshByIP() {
        "Activity: onRefreshByIP called".logd(TAG)
        mViewModel.refreshWeatherByIP()
    }
    
    override fun onCitySelected(cityInfo: com.android.common.weather.model.CityInfo) {
        "Activity: onCitySelected called with: ${cityInfo.localizedName}".logd(TAG)
        // 通过 locationKey 获取该城市的完整天气信息，传入 cityInfo 作为备用
        cityInfo.key?.let { key ->
            mViewModel.getWeatherByLocationKey(key, cityInfo)
        }
    }
    
    /**
     * 绑定天气数据到 UI
     */
    private fun bindWeatherData(state: WeatherUiState.Success) {
        lastWeatherState = state
        bindCurrentDate()
        bindCityName(state)
        val todayForecast = state.dailyForecasts?.firstOrNull()
        state.currentWeather?.let { bindCurrentWeather(it, todayForecast) }
        state.dailyForecasts?.let { bindDailyForecasts(it) }
        state.headline?.let { bindHeadline(it) }
    }
    
    /**
     * 绑定城市名称
     */
    private fun bindCityName(state: WeatherUiState.Success) {
        val cityName = state.cityInfo?.localizedName
            ?: state.cityInfo?.englishName
            ?: ""
        mViewBind.tvCityName.text = cityName
    }
    
    /**
     * 绑定当前日期
     */
    private fun bindCurrentDate() {
        val dateFormat = SimpleDateFormat("EEE, MMM dd", LanguageUtils.getAppLocale(this))
        mViewBind.tvDate.text = dateFormat.format(Date())
    }
    
    /**
     * 绑定当前天气信息
     * @param weather 当前天气数据
     * @param todayForecast 今天的预报数据，用于获取降水概率
     */
    private fun bindCurrentWeather(
        weather: CurrentConditionResponse,
        todayForecast: AccuWeatherDailyForecast?
    ) {
        with(mViewBind) {
            // 天气图标
            weather.weatherIcon?.let { iconId ->
                val iconRes = WeatherIconMapper.getIconResource(iconId)
                ivWeather.setImageResource(iconRes)
            }
            
            weather.temperature?.metric?.value?.let { temp ->
                val isCelsius = TemperaturePreferences.isCelsius()
                val displayTemp = if (isCelsius) {
                    temp.roundToInt()
                } else {
                    temp.celsiusToFahrenheit()
                }
                tvTemperatureValue.text = displayTemp.toString()
            }

            // 天气状况文本
            val weatherDesc = weather.weatherIcon?.let { 
                getString(WeatherIconMapper.getIconDescriptionRes(it)) 
            } ?: weather.weatherText ?: "N/A"
            tvWeather.text = weatherDesc
            
            weather.wind?.speed?.metric?.value?.let { speed ->
                tvWindSpeedValue.text = "${speed.roundToInt()} km/h"
            }
            
            
            // 降水概率（从今天的预报数据中获取）
            val precipProb = if (weather.isDayTime == true) {
                todayForecast?.day?.precipitationProbability
            } else {
                todayForecast?.night?.precipitationProbability
            }
            tvPrecipitationPercent.text = "${precipProb ?: 0}%"
            
            // 湿度
            weather.relativeHumidity?.let { humidity ->
                tvHumidityValue.text = humidity.toString()
            }
            
            // UV 指数
            weather.uvIndex?.let { uv ->
                tvUvValue.text = uv.toString()
                tvUvTag.text = weather.uvIndexText ?: "N/A"
            }
            
            weather.pressure?.metric?.value?.let { pressure ->
                tvPressureValue.text = pressure.roundToInt().toString()
            }
            
            weather.visibility?.metric?.value?.let { visibility ->
                tvVisibilityValue.text = visibility.roundToInt().toString()
            }
            
//            weather.realFeelTemperature?.metric?.value?.let { realFeel ->
//                val feel = realFeel.roundToInt()
//                tvHighTemperature.text = "Feels: $feel°"
//            }
        }
    }
    
    /**
     * 绑定未来 5 天预报
     */
    private fun bindDailyForecasts(forecasts: List<AccuWeatherDailyForecast>) {
        "Binding ${forecasts.size} daily forecasts".logd(TAG)
        
        // 过滤掉过去的预报（例如缓存了其它的数据）
        val todayCalendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayMillis = todayCalendar.timeInMillis
        
        val validForecasts = forecasts.filter { forecast ->
            // 解析日期比较，或者简单地使用 epochDate
            // AccuWeather 的 Date 字段是 ISO8601 包含时区，使用它来判断日期更准确
            val forecastDateStr = forecast.date
            if (forecastDateStr != null) {
                try {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                    val date = isoFormat.parse(forecastDateStr)
                    
                    if (date != null) {
                        val forecastCalendar = java.util.Calendar.getInstance()
                        forecastCalendar.time = date
                        forecastCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        forecastCalendar.set(java.util.Calendar.MINUTE, 0)
                        forecastCalendar.set(java.util.Calendar.SECOND, 0)
                        forecastCalendar.set(java.util.Calendar.MILLISECOND, 0)
                        
                        // 保留 >= 今天的预报
                        !forecastCalendar.before(todayCalendar)
                    } else {
                        true // 解析失败保留
                    }
                } catch (e: Exception) {
                    true // 异常保留
                }
            } else {
                true // 无日期保留
            }
        }
        
        // 如果过滤后为空（比如所有数据都过期了），则还是显示原始数据以免空白
        val finalForecasts = if (validForecasts.isNotEmpty()) validForecasts else forecasts
        
        // 获取今天的最高最低温（API返回华氏度，转换为当前单位）
        // 注意：这里取过滤后的第一天作为"今天"的显示数据
        val today = finalForecasts.firstOrNull()
        today?.let { 
            val isCelsius = TemperaturePreferences.isCelsius()
            val highTemp = if (isCelsius) {
                it.temperature?.maximum?.value?.fahrenheitToCelsius() ?: 0
            } else {
                it.temperature?.maximum?.value?.roundToInt() ?: 0
            }
            val lowTemp = if (isCelsius) {
                it.temperature?.minimum?.value?.fahrenheitToCelsius() ?: 0
            } else {
                it.temperature?.minimum?.value?.roundToInt() ?: 0
            }

            with(mViewBind) {
                tvHighTemperature.text = getString(R.string.temperature_high, highTemp)
                tvLowTemperature.text = getString(R.string.temperature_low, lowTemp)
            }
            
            // 绑定日出日落时间
            it.sun?.let { sun ->
                sun.rise?.let { rise ->
                    try {
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val date = isoFormat.parse(rise)
                        mViewBind.tvSunriseValue.text = date?.let { timeFormat.format(it) } ?: "N/A"
                    } catch (e: Exception) {
                        mViewBind.tvSunriseValue.text = "N/A"
                    }
                }
                
                sun.set?.let { set ->
                    try {
                        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val date = isoFormat.parse(set)
                        mViewBind.tvSunsetValue.text = date?.let { timeFormat.format(it) } ?: "N/A"
                    } catch (e: Exception) {
                        mViewBind.tvSunsetValue.text = "N/A"
                    }
                }
            }
        }
        
        // 提交数据到 Adapter（先提交 null 再提交数据以强制刷新）
        adapter.submitList(null)
        adapter.submitList(forecasts)
    }

    
    /**
     * 绑定天气总览信息
     */
    private fun bindHeadline(headline: Headline) {
        "Headline: ${headline.text}".logd(TAG)
        // 可以在未来扩展显示 headline 信息
    }
    
    /**
     * 显示加载状态
     */
    private fun showLoading() {
        // 可以添加一个 ProgressBar 或 Loading 指示器
        "Showing loading...".logd(TAG)
    }
    
    /**
     * 隐藏加载状态
     */
    private fun hideLoading() {
        "Hiding loading...".logd(TAG)
    }
    
    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        "Showing error: $message".loge(TAG)
        // 可以显示 Toast 或 Snackbar
    }

    override fun getStatusBarColor() = com.healthtracker.framework.R.color.transparent
}