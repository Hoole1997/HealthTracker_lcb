package com.daily.health.manager.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.compose.runtime.Applier
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.daily.health.manager.App
import com.daily.health.manager.R
import com.daily.health.manager.databinding.HtItemInsightsBinding
import com.daily.health.manager.utils.InsightAssetPreparer
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.util.isLeast9
import java.io.File

class InsightsArticleAdapter(
    private val onItemClick: (InsightAssetPreparer.InsightArticle) -> Unit
) : ListAdapter<InsightAssetPreparer.InsightArticle, InsightsArticleAdapter.ArticleViewHolder>(
    DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = HtItemInsightsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArticleViewHolder(
        private val binding: HtItemInsightsBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentArticle: InsightAssetPreparer.InsightArticle? = null

        init {
            binding.root.clickWithDuration {
                currentArticle?.let(onItemClick)
            }
        }

        fun bind(article: InsightAssetPreparer.InsightArticle) {
            currentArticle = article
            binding.tvTitle.text = article.title
            if(isLeast9()){
                val shadowColor = ContextCompat.getColor(App.INSTANCE,
                    R.color.color_DFDFDF)
                binding.rootCard.outlineSpotShadowColor = shadowColor
                binding.rootCard.outlineAmbientShadowColor = shadowColor
            }
            val imagePath = article.listImagePath
            if (imagePath.isNullOrEmpty()) {
                binding.ivImg.setImageResource(R.drawable.ht_bg_rect_white_12)
            } else {
                Glide.with(binding.ivImg)
                    .load(File(imagePath))
                    .placeholder(R.drawable.ht_bg_rect_white_12)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.ivImg)
            }
        }
    }

    private object DiffCallback :
        DiffUtil.ItemCallback<InsightAssetPreparer.InsightArticle>() {
        override fun areItemsTheSame(
            oldItem: InsightAssetPreparer.InsightArticle,
            newItem: InsightAssetPreparer.InsightArticle
        ): Boolean = oldItem.articleId == newItem.articleId

        override fun areContentsTheSame(
            oldItem: InsightAssetPreparer.InsightArticle,
            newItem: InsightAssetPreparer.InsightArticle
        ): Boolean = oldItem == newItem
    }
}
