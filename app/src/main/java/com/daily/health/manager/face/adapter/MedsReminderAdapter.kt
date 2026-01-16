package com.daily.health.manager.face.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.ethanhua.skeleton.ViewSkeletonScreen
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtItemMedsRemindBinding
import com.daily.health.manager.databinding.HtLayoutAdItemBinding
import com.daily.health.manager.face.model.MedsReminderItem
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.gone
import com.healthtracker.framework.ext.visible
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle

/**
 * 药物提醒列表适配器
 */
class MedsReminderAdapter(
    private val activity: FragmentActivity,
    private val onItemClick: (View, MedsReminderItem) -> Unit,
) : ListAdapter<MedsReminderItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var isFragmentVisible = false
    private var adLogged = false

    var needLoadAd = false

    override fun getItemViewType(position: Int): Int {
        return if (position == AD_POSITION) VIEW_TYPE_AD else VIEW_TYPE_REMINDER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_AD) {
            AdViewHolder(HtLayoutAdItemBinding.inflate(LayoutInflater.from(parent.context),parent,false))
        } else {
            val binding = HtItemMedsRemindBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            ReminderViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ReminderViewHolder -> holder.bind(getItem(position))
            is AdViewHolder -> holder.bind()
        }
    }

    fun setFragmentVisible(visible: Boolean) {
        val becameVisible = !isFragmentVisible && visible
        isFragmentVisible = visible
        if (becameVisible && !adLogged && currentList.size > AD_POSITION) {
            notifyItemChanged(AD_POSITION)
        }
    }

    inner class ReminderViewHolder(
        private val binding: HtItemMedsRemindBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // 设置点击监听
            binding.root.clickWithDuration {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(binding.ivMore, getItem(position))
                }
            }
        }

        fun bind(item: MedsReminderItem) {
            with(binding) {
                // 设置时间
                tvTime.text = item.time

                if (!item.medicineCover.isEmpty()) {
                    Glide.with(ivStatu)
                        .applyDefaultRequestOptions(RequestOptions.placeholderOf(R.drawable.ht_ic_camera))
                        .load(item.medicineCover.toUri())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(ivStatu)
                }
                // 设置药物名称
                tvName.text = item.medicineName

                // 设置备注
                if (item.notes.isNotBlank()) {
                    tvNotes.text = item.notes
                    tvNotes.visibility = android.view.View.VISIBLE
                } else {
                    tvNotes.visibility = android.view.View.GONE
                }
                // 根据状态设置图标和背景
                if (item.isTaken()) {
                    ivTake.visible()
                } else {
                    ivTake.gone()
                }
            }
        }
    }

    inner class AdViewHolder(private val binding: HtLayoutAdItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var skeleton: ViewSkeletonScreen
        fun bind() {
            if (needLoadAd) {
                binding.root.visible()
                skeleton = ViewSkeletonScreen.Builder(binding.adContainer)
                    .load(R.layout.ht_layout_skeleton_banner_ads)
                    .shimmer(true)
                    .angle(30)
                    .duration(1200)
                    .color(net.corekit.monetize.R.color.white)
                    .show()
                binding.adContainer.removeAllViews()
                activity.loadNative(binding.adContainer, AdPosition.NA_MEDS_REMINDER_LIST, style = NativeAdStyle.CARD_8){
                    if(it){
                        android.util.Log.d("MedsReminderAdapter", "Ad placeholder visible at position=$bindingAdapterPosition")

                    }else{
                        binding.root.gone()
                    }

                    skeleton.hide()
                }
            }
        }
    }

    /**
     * DiffUtil回调，用于高效更新列表
     */
    private class DiffCallback : DiffUtil.ItemCallback<MedsReminderItem>() {
        override fun areItemsTheSame(
            oldItem: MedsReminderItem,
            newItem: MedsReminderItem
        ): Boolean {
            return oldItem.reminderId == newItem.reminderId &&
                    oldItem.reminderDateTime == newItem.reminderDateTime
        }

        override fun areContentsTheSame(
            oldItem: MedsReminderItem,
            newItem: MedsReminderItem
        ): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_REMINDER = 0
        private const val VIEW_TYPE_AD = 1
        private const val AD_POSITION = 1
    }
}
