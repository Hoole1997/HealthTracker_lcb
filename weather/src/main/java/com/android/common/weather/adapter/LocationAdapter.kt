package com.android.common.weather.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.android.common.weather.R
import com.android.common.weather.databinding.ItemLocationBinding
import com.android.common.weather.model.CityInfo

/**
 * 位置列表 Adapter
 * 第一项为当前城市（加粗、绿色图标、显示刷新按钮）
 * 其余为搜索结果（普通、灰色图标、隐藏刷新按钮）
 */
class LocationAdapter(
    private val onItemClick: (CityInfo, Int) -> Unit,
    private val onRefreshClick: () -> Unit
) : ListAdapter<LocationAdapter.LocationItem, LocationAdapter.LocationViewHolder>(LocationDiffCallback()) {

    /**
     * 位置项数据封装
     */
    data class LocationItem(
        val cityInfo: CityInfo,
        val isCurrentLocation: Boolean
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val binding = ItemLocationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LocationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LocationViewHolder(
        private val binding: ItemLocationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LocationItem) {
            val context = binding.root.context
            val cityInfo = item.cityInfo

            if (item.isCurrentLocation) {
                // 当前城市样式
                binding.tvCurrentLocation.text = cityInfo.localizedName ?: cityInfo.englishName ?: ""
                binding.tvCurrentLocation.setTypeface(null, Typeface.BOLD)
                binding.ivLocation.setColorFilter(ContextCompat.getColor(context, R.color.color_02BC77))
                binding.ivRefresh.visibility = View.VISIBLE
                binding.ivRefresh.setOnClickListener { onRefreshClick() }
            } else {
                // 搜索结果样式：格式 "行政区, 城市名, 国家"
                val locationName = buildLocationName(cityInfo)
                binding.tvCurrentLocation.text = locationName
                binding.tvCurrentLocation.setTypeface(null, Typeface.NORMAL)
                binding.ivLocation.setColorFilter(ContextCompat.getColor(context, R.color.color_999))
                binding.ivRefresh.visibility = View.GONE
            }

            // 点击事件
            binding.root.setOnClickListener {
                onItemClick(cityInfo, bindingAdapterPosition)
            }
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
    }

    /**
     * DiffUtil 回调
     */
    class LocationDiffCallback : DiffUtil.ItemCallback<LocationItem>() {
        override fun areItemsTheSame(oldItem: LocationItem, newItem: LocationItem): Boolean {
            return oldItem.cityInfo.key == newItem.cityInfo.key &&
                   oldItem.isCurrentLocation == newItem.isCurrentLocation
        }

        override fun areContentsTheSame(oldItem: LocationItem, newItem: LocationItem): Boolean {
            return oldItem == newItem
        }
    }
}
