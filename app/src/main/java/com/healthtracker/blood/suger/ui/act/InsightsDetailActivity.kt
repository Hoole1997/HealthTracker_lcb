package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.text.style.URLSpan
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.databinding.HtActivityInsightsDetailBinding
import com.healthtracker.blood.suger.utils.InsightAssetPreparer
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ui.NativeAdStyle
import java.io.File

class InsightsDetailActivity :
    BaseInterActivity<BaseViewModel, HtActivityInsightsDetailBinding>() {

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_IMAGE = "extra_image"
        private const val EXTRA_ARTICLE_ID = "extra_article_id"

        fun start(context: Context, article: InsightAssetPreparer.InsightArticle) {
            ReportDataManager.reportData("Insights_item_click",mapOf("article_id" to article.articleId))
            val intent = Intent(context, InsightsDetailActivity::class.java).apply {
                putExtra(EXTRA_TITLE, article.title)
                putExtra(EXTRA_CONTENT, article.content)
                putExtra(EXTRA_IMAGE, article.listImagePath)
                putExtra(EXTRA_ARTICLE_ID, article.articleId)
            }
            context.startActivity(intent)
        }
    }

    private var isWebViewMode = false

    override fun createViewBinding() = HtActivityInsightsDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val imagePath = intent.getStringExtra(EXTRA_IMAGE).orEmpty()

        with(mViewBind) {
            btnBack.clickWithDuration { handleBackPress() }
            tvToolbarTitle.text = getString(R.string.insights)
            tvArticleTitle.text = title
            
            // Set HTML content and intercept link clicks
            // Set HTML content and intercept link clicks
            val spannedContent = HtmlCompat.fromHtml(
                content,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            // Replace default QuoteSpan with CustomQuoteSpan
            val spannableBuilder = android.text.SpannableStringBuilder(spannedContent)
            val quoteSpans = spannableBuilder.getSpans(0, spannableBuilder.length, android.text.style.QuoteSpan::class.java)
            for (quoteSpan in quoteSpans) {
                val start = spannableBuilder.getSpanStart(quoteSpan)
                val end = spannableBuilder.getSpanEnd(quoteSpan)
                val flags = spannableBuilder.getSpanFlags(quoteSpan)
                spannableBuilder.removeSpan(quoteSpan)
                spannableBuilder.setSpan(
                    com.healthtracker.blood.suger.ui.widget.CustomQuoteSpan(
                        ContextCompat.getColor(this@InsightsDetailActivity, R.color.color_3b82f6),
                        stripeWidth = 10,
                        gapWidth = 20
                    ),
                    start,
                    end,
                    flags
                )
            }

            tvArticleContent.text = spannableBuilder
            setupLinkClickInterceptor(tvArticleContent, spannableBuilder)

            if (imagePath.isNotEmpty()) {
                Glide.with(ivCover)
                    .load(File(imagePath))
                    .placeholder(R.drawable.ht_bg_rect_white_12)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.ht_bg_rect_white_12)
            }
            
            // Initialize WebView

            
            loadNative(adContainer, style = NativeAdStyle.STANDARD)
        }
    }

    private fun setupLinkClickInterceptor(textView: android.widget.TextView, spanned: Spanned) {
        // Get all URLSpan instances from the spanned text
        val spans = spanned.getSpans(0, spanned.length, URLSpan::class.java)
        
        // Create a custom ClickableSpan movement method
        textView.movementMethod = object : android.text.method.LinkMovementMethod() {
            override fun onTouchEvent(
                widget: android.widget.TextView,
                buffer: android.text.Spannable,
                event: android.view.MotionEvent
            ): Boolean {
                val action = event.action
                if (action == android.view.MotionEvent.ACTION_UP) {
                    var x = event.x.toInt()
                    var y = event.y.toInt()
                    x -= widget.totalPaddingLeft
                    y -= widget.totalPaddingTop
                    x += widget.scrollX
                    y += widget.scrollY

                    val layout = widget.layout
                    val line = layout.getLineForVertical(y)
                    val off = layout.getOffsetForHorizontal(line, x.toFloat())

                    val links = buffer.getSpans(off, off, URLSpan::class.java)
                    if (links.isNotEmpty()) {
                        val url = links[0].url
                        // Load URL in WebView instead of opening in browser
                        InnerWebActivity.start(this@InsightsDetailActivity,url)
                        return true
                    }
                }
                return super.onTouchEvent(widget, buffer, event)
            }
        }
    }





}
