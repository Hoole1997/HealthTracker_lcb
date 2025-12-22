package com.healthtracker.blood.suger.ui.act

import android.content.Context
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContextCompat
import com.healthtracker.blood.suger.R
import com.healthtracker.blood.suger.databinding.HtActivityInnerWebBinding
import com.healthtracker.blood.suger.utils.WebViewUtils
import com.healthtracker.framework.base.BaseMVVMActivity
import com.healthtracker.framework.base.BaseViewModel
import com.healthtracker.framework.ext.clickWithDuration
import com.healthtracker.framework.ext.logd
import com.healthtracker.framework.ext.startActivity

class InnerWebActivity: BaseMVVMActivity<BaseViewModel, HtActivityInnerWebBinding>() {

    companion object{
        private const val EXTRA_RRL = "EXTRA_RRL"
        fun start(context: Context,url:String){
            context.startActivity<InnerWebActivity>(EXTRA_RRL to url)
        }
    }

    override fun createViewBinding() = HtActivityInnerWebBinding.inflate(layoutInflater)

    override fun getVMModelClass() = BaseViewModel::class.java

    override fun initView(savedInstanceState: Bundle?) {
        setupWebView()
        intent.getStringExtra(EXTRA_RRL)?.let {
            loadUrlInWebView(it)
        }

    }


    private fun setupWebView() {
        with(mViewBind) {
            btnBack.clickWithDuration {
                handleBackPress()
            }
            // Initialize WebView with WebViewUtils
            WebViewUtils.initWebViewSetting(webView, true, true)
            progress.setColor(ContextCompat.getColor(this@InnerWebActivity, R.color.c5))
            // Setup WebViewClient for page load events
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

                    view.loadUrl(url)
                    return true
                }

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    view.title?.let {
                        mViewBind.tvTitle.text = it
                    }
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

                    progress.setWebProgress(newProgress)

                    if(newProgress == 100){
                        startTransition(1500)
                        progress.hide()
                    }
                }
            }
        }
    }

    private fun loadUrlInWebView(url: String) {
        with(mViewBind) {
            // Reset and show progress
            startTransition()
            progress.reset()
            progress.show()
            "url: $url".logd("InsightsDetailActivity")
            webView.loadUrl(url)
        }
    }

    override fun handleBackPress(): Boolean{
        if(mViewBind.webView.canGoBack()){
            mViewBind.webView.goBack()
            return true
        }
        finish()
        return true
    }

}