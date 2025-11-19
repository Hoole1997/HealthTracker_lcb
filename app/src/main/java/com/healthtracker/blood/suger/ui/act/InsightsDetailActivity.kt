package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityInsightsDetailBinding
import com.healthtracker.blood.suger.utils.InsightAssetPreparer
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import android.text.method.LinkMovementMethod
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.utils.loadNative
import net.corekit.monetize.ui.NativeAdStyle
import java.io.File

class InsightsDetailActivity :
    BaseInterActivity<BaseViewModel, ActivityInsightsDetailBinding>() {

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_IMAGE = "extra_image"
        private const val EXTRA_ARTICLE_ID = "extra_article_id"

        fun start(context: Context, article: InsightAssetPreparer.InsightArticle) {
            val intent = Intent(context, InsightsDetailActivity::class.java).apply {
                putExtra(EXTRA_TITLE, article.title)
                putExtra(EXTRA_CONTENT, article.content)
                putExtra(EXTRA_IMAGE, article.listImagePath)
                putExtra(EXTRA_ARTICLE_ID, article.articleId)
            }
            context.startActivity(intent)
        }
    }

    override fun createViewBinding() = ActivityInsightsDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val imagePath = intent.getStringExtra(EXTRA_IMAGE).orEmpty()

        with(mViewBind) {
            btnBack.clickWithDuration { handleBackPress() }
            tvToolbarTitle.text = getString(R.string.insights)
            tvArticleTitle.text = title
            tvArticleContent.text = HtmlCompat.fromHtml(
                content,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            tvArticleContent.movementMethod = LinkMovementMethod.getInstance()

            if (imagePath.isNotEmpty()) {
                Glide.with(ivCover)
                    .load(File(imagePath))
                    .placeholder(R.drawable.bg_rect_white_12)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.bg_rect_white_12)
            }
            loadNative(adContainer, style = NativeAdStyle.STANDARD)
        }
    }
}
