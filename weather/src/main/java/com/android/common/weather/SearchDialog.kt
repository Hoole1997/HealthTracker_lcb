package com.android.common.weather

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.common.weather.adapter.LocationAdapter
import com.android.common.weather.databinding.DialogLocationSearchBinding
import com.android.common.weather.model.CityInfo
import com.android.common.weather.model.WeatherResponse
import com.healthtracker.framework.base.fragment.BaseBottomSheetDialogFragment
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 搜索对话框回调接口
 */
interface SearchDialogCallback {
    /**
     * 搜索地址
     * @param address 用户输入的地址
     */
    fun onSearchAddress(address: String)
    
    /**
     * 按 IP 刷新位置
     */
    fun onRefreshByIP()
    
    /**
     * 选择城市
     * @param cityInfo 选中的城市信息
     */
    fun onCitySelected(cityInfo: CityInfo)
}

class SearchDialog : BaseBottomSheetDialogFragment<DialogLocationSearchBinding>() {
    
    companion object {
        private const val TAG = "SearchDialog"
        private const val DEBOUNCE_DELAY_MS = 300L
        
        private var instance: SearchDialog? = null
        
        fun show(fragmentManager: FragmentManager, currentCityInfo: CityInfo? = null): SearchDialog {
            val dialog = SearchDialog().apply {
                this.currentCityInfo = currentCityInfo
            }
            dialog.show(fragmentManager, "SearchDialog")
            instance = dialog
            return dialog
        }
        
        /**
         * 获取当前显示的 Dialog 实例
         */
        fun getInstance(): SearchDialog? = instance
    }
    
    private var callback: SearchDialogCallback? = null
    private var currentCityInfo: CityInfo? = null
    private var searchResults: List<CityInfo> = emptyList()
    private var debounceJob: Job? = null
    private val scope = MainScope()
    
    private val locationAdapter by lazy {
        LocationAdapter(
            onItemClick = { cityInfo, position ->
                "Clicked item at position $position: ${cityInfo.localizedName}".logd(TAG)
                callback?.onCitySelected(cityInfo)
                dismissAllowingStateLoss()
            },
            onRefreshClick = {
                "Refresh clicked".logd(TAG)
                callback?.onRefreshByIP()
                dismissAllowingStateLoss()
            }
        )
    }
    
    override fun createViewBinding(
        inflater: LayoutInflater,
        parent: ViewGroup?,
        attachToParent: Boolean
    ) = DialogLocationSearchBinding.inflate(inflater, parent, attachToParent)
    
    /**
     * 设置回调
     */
    fun setCallback(callback: SearchDialogCallback) {
        this.callback = callback
    }
    
    /**
     * 更新当前城市信息
     */
    fun updateCurrentCity(cityInfo: CityInfo?) {
        currentCityInfo = cityInfo
        updateAdapterData()
    }
    
    /**
     * 更新搜索结果
     */
    fun updateSearchResult(response: WeatherResponse?) {
        if (response == null) {
            "SearchDialog: Search result is null".logd(TAG)
            searchResults = emptyList()
        } else {
            val locations = response.locations ?: emptyList()
            "SearchDialog: Got ${locations.size} search results".logd(TAG)
            locations.forEach { city ->
                val name = buildLocationName(city)
                "  - $name (key=${city.key})".logd(TAG)
            }
            searchResults = locations
        }
        updateAdapterData()
    }

    override fun initView(view: View, savedInstanceState: Bundle?) {
        mViewBind?.let { binding ->
            // 设置 RecyclerView
            binding.rvLocations.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = locationAdapter
            }
            
            // 初始显示当前城市
            updateAdapterData()
            
            // 关闭按钮
            binding.ivClose.clickWithDuration {
                dismissAllowingStateLoss()
            }
            
            // 输入框监听（带防抖）
            binding.etInput.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    val query = s?.toString()?.trim() ?: ""
                    
                    // 取消之前的延迟任务
                    debounceJob?.cancel()
                    
                    if (query.length >= 2) {
                        // 防抖延迟搜索
                        debounceJob = scope.launch {
                            delay(DEBOUNCE_DELAY_MS)
                            // 首字母大写
                            val capitalizedQuery = query.replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                            }
                            "SearchDialog: Searching for '$capitalizedQuery'".logd(TAG)
                            callback?.onSearchAddress(capitalizedQuery)
                        }
                    } else {
                        // 清空搜索结果
                        searchResults = emptyList()
                        updateAdapterData()
                    }
                }
            })
        }
    }
    
    /**
     * 更新 Adapter 数据
     * 列表结构：[当前城市] + [搜索结果]
     */
    private fun updateAdapterData() {
        val items = mutableListOf<LocationAdapter.LocationItem>()
        
        // 添加当前城市（第一项）
        currentCityInfo?.let {
            items.add(LocationAdapter.LocationItem(it, isCurrentLocation = true))
        }
        
        // 添加搜索结果
        searchResults.forEach { city ->
            items.add(LocationAdapter.LocationItem(city, isCurrentLocation = false))
        }
        
        locationAdapter.submitList(items)
    }
    
    /**
     * 构建位置名称：格式 "行政区, 城市名, 国家"
     */
    private fun buildLocationName(cityInfo: CityInfo): String {
        val parts = mutableListOf<String>()
        
        // 行政区
        cityInfo.administrativeArea?.localizedName?.let { parts.add(it) }
            ?: cityInfo.administrativeArea?.englishName?.let { parts.add(it) }
        
        // 城市名（如果和行政区不同）
        val cityName = cityInfo.localizedName ?: cityInfo.englishName
        if (cityName != null && !parts.contains(cityName)) {
            parts.add(0, cityName)
        }
        
        // 国家
        cityInfo.country?.localizedName?.let { parts.add(it) }
            ?: cityInfo.country?.englishName?.let { parts.add(it) }
        
        return parts.joinToString(", ")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        debounceJob?.cancel()
        if (instance == this) {
            instance = null
        }
    }
}