package com.daily.health.manager.face.act

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.text.style.URLSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.daily.health.manager.R
import com.daily.health.manager.ad.BaseInterActivity
import com.daily.health.manager.databinding.TrActivityInsightsDetailBinding
import com.daily.health.manager.utils.InsightAssetPreparer
import com.daily.health.manager.utils.loadNative
import com.healthtracker.framework.base.BaseViewModel
import net.corekit.core.report.ReportDataManager
import net.corekit.monetize.ads.AdPosition
import net.corekit.monetize.ui.NativeAdStyle
import java.io.File
import com.healthtracker.framework.R as FrameworkR

class InsightsDetailAct :
    BaseInterActivity<BaseViewModel, TrActivityInsightsDetailBinding>() {

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_CONTENT = "extra_content"
        private const val EXTRA_IMAGE = "extra_image"
        private const val EXTRA_ARTICLE_ID = "extra_article_id"

        fun start(context: Context, article: InsightAssetPreparer.InsightArticle) {
            ReportDataManager.reportData("Insights_item_click",mapOf("article_id" to article.articleId))
            val intent = Intent(context, InsightsDetailAct::class.java).apply {
                putExtra(EXTRA_TITLE, article.title)
                putExtra(EXTRA_CONTENT, article.content)
                putExtra(EXTRA_IMAGE, article.listImagePath)
                putExtra(EXTRA_ARTICLE_ID, article.articleId)
            }
            context.startActivity(intent)
        }
    }

    private var isWebViewMode = false

    override fun createViewBinding() = TrActivityInsightsDetailBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val content = intent.getStringExtra(EXTRA_CONTENT).orEmpty()
        val imagePath = intent.getStringExtra(EXTRA_IMAGE).orEmpty()
        val spannableContent = buildSpannableContent(content)

        with(mViewBind) {
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                InsightsDetailScreen(
                    articleTitle = title,
                    content = spannableContent,
                    imagePath = imagePath,
                    onBack = { handleBackPress() },
                    onBindLinkInterceptor = { tv ->
                        setupLinkClickInterceptor(tv, spannableContent)
                    },
                )
            }

            loadNative(adContainer, AdPosition.NA_INSIGHTS_DETAIL_BOTTOM, style = NativeAdStyle.STANDARD)
        }
    }

    private fun buildSpannableContent(html: String): android.text.SpannableStringBuilder {
        val spannedContent = HtmlCompat.fromHtml(
            html,
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        val spannableBuilder = android.text.SpannableStringBuilder(spannedContent)

        val quoteSpans = spannableBuilder.getSpans(
            0,
            spannableBuilder.length,
            android.text.style.QuoteSpan::class.java
        )
        for (quoteSpan in quoteSpans) {
            val start = spannableBuilder.getSpanStart(quoteSpan)
            val end = spannableBuilder.getSpanEnd(quoteSpan)
            val flags = spannableBuilder.getSpanFlags(quoteSpan)
            spannableBuilder.removeSpan(quoteSpan)
            spannableBuilder.setSpan(
                com.daily.health.manager.face.widget.CustomQuoteSpan(
                    ContextCompat.getColor(this@InsightsDetailAct, R.color.color_3b82f6),
                    stripeWidth = 10,
                    gapWidth = 20
                ),
                start,
                end,
                flags
            )
        }

        return spannableBuilder
    }

    private fun setupLinkClickInterceptor(textView: android.widget.TextView, spanned: Spanned) {
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
                        InnerWebAct.start(this@InsightsDetailAct,url)
                        return true
                    }
                }
                return super.onTouchEvent(widget, buffer, event)
            }
        }
    }


    override fun getBackAdPosition() = AdPosition.IV_INSIGHTS_DETAILS_BACK


}

@Composable
private fun InsightsDetailScreen(
    articleTitle: String,
    content: Spanned,
    imagePath: String,
    onBack: () -> Unit,
    onBindLinkInterceptor: (TextView) -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.c1))
    ) {
        InsightsTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = articleTitle,
                color = colorResource(R.color.t1),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp),
                factory = { ctx ->
                    ImageFilterView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        round = ctx.resources.getDimension(FrameworkR.dimen.dp_12)
                        setBackgroundColor(ContextCompat.getColor(ctx, R.color.c1))
                        contentDescription = ctx.getString(R.string.tr_insights)
                    }
                },
                update = { imageView ->
                    if (imagePath.isNotEmpty()) {
                        Glide.with(imageView)
                            .load(File(imagePath))
                            .placeholder(R.drawable.tr_bg_rect_white_12)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(imageView)
                    } else {
                        imageView.setImageResource(R.drawable.tr_bg_rect_white_12)
                    }
                }
            )

            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(ContextCompat.getColor(ctx, R.color.t1))
                        textSize = 14f
                        setLineSpacing(ctx.resources.getDimension(FrameworkR.dimen.dp_4), 1f)
                    }
                },
                update = { tv ->
                    tv.text = content
                    onBindLinkInterceptor(tv)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InsightsTopBar(
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(colorResource(R.color.c1))
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.tr_ic_back),
                    contentDescription = "back",
                    tint = Color.Unspecified
                )
            }
        }

        Text(
            text = stringResource(R.string.tr_insights),
            color = colorResource(R.color.t1),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}
