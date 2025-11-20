package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.text.style.URLSpan
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.ActivityInsightsDetailBinding
import com.healthtracker.blood.suger.utils.InsightAssetPreparer
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.blood.suger.ad.BaseInterActivity
import com.healthtracker.blood.suger.utils.loadNative
import com.healthtracker.blood.suger.utils.WebViewUtils
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

    private var isWebViewMode = false

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
            
            // Set HTML content and intercept link clicks
            val spannedContent = HtmlCompat.fromHtml(
                content,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            tvArticleContent.text = spannedContent
            setupLinkClickInterceptor(tvArticleContent, spannedContent)

            if (imagePath.isNotEmpty()) {
                Glide.with(ivCover)
                    .load(File(imagePath))
                    .placeholder(R.drawable.bg_rect_white_12)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(ivCover)
            } else {
                ivCover.setImageResource(R.drawable.bg_rect_white_12)
            }
            
            // Initialize WebView
            setupWebView()
            
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
                        loadUrlInWebView(url)
                        return true
                    }
                }
                return super.onTouchEvent(widget, buffer, event)
            }
        }
    }

    private fun setupWebView() {
        with(mViewBind) {
            // Initialize WebView with WebViewUtils
            WebViewUtils.initWebViewSetting(webView, true, true)
            progress.setColor(ContextCompat.getColor(this@InsightsDetailActivity, R.color.c5))
            // Setup WebViewClient for page load events
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    // Hide progress bar when page finishes loading
//                    progress.hide()
                }

                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String,
                    failingUrl: String
                ) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    // Hide progress bar on error
                    progress.hide()
                }
            }

            // Setup WebChromeClient for progress tracking
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    // Only update progress if WebView is visible

                    if (isWebViewMode) {
                        progress.setWebProgress(newProgress)
                    }

                    if(newProgress == 100){
                        progress.hide()
                    }
                }
            }
        }
    }

    private fun loadUrlInWebView(url: String) {
        with(mViewBind) {
            // Show WebView mode
            showWebViewMode()
            
            // Reset and show progress
            progress.reset()
            progress.show()
            webView.loadUrl(url)
        }
    }

    private fun showWebViewMode() {
        isWebViewMode = true
        with(mViewBind) {
            scrollContainer.visibility = View.GONE
            webView.visibility = View.VISIBLE
        }
    }

    private fun showArticleMode() {
        isWebViewMode = false
        with(mViewBind) {
            webView.visibility = View.GONE
            scrollContainer.visibility = View.VISIBLE
            progress.hide()
        }
    }

    override fun handleBackPress(): Boolean {
        if (isWebViewMode) {
            // If WebView can go back, navigate back in WebView
            if (mViewBind.webView.canGoBack()) {
                mViewBind.webView.goBack()
                return true
            } else {
                // Otherwise, return to article mode
                showArticleMode()
                return true
            }
        } else {
            // In article mode, finish activity
            return super.handleBackPress()
        }
    }
}
