package com.n8booksapp.tpo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var topAdWebView: WebView
    private lateinit var bottomAdWebView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar

    private val MAIN_URL = "https://tinyurl.com/n8bookapp"
    private val BANNER_AD_URL = "https://nightbooksapp.blogspot.com/p/app-banner-ads.html"
    private val INTERSTITIAL_AD_URL = "https://nightbooksapp.blogspot.com/p/app-interstitial.html"

    private var interstitialTimer: Timer? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        topAdWebView = findViewById(R.id.topAdWebView)
        bottomAdWebView = findViewById(R.id.bottomAdWebView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)

        setupWebView()
        setupAds()
        setupSwipeRefresh()

        if (isOnline()) {
            webView.loadUrl(MAIN_URL)
        } else {
            showNoInternetDialog()
        }

        startInterstitialTimer()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.loadWithOverviewMode = true
        s.useWideViewPort = true
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setSupportZoom(true)
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                CookieManager.getInstance().flush()
                super.onPageFinished(view, url)
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true) {
                    showNoInternetDialog()
                }
                super.onReceivedError(view, request, error)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupAds() {
        // Top and Bottom banners - both load the same Blogger URL
        listOf(topAdWebView, bottomAdWebView).forEach { wv ->
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.loadsImagesAutomatically = true
            wv.settings.blockNetworkImage = false
            wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
            wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            wv.settings.mediaPlaybackRequiresUserGesture = false
            wv.settings.userAgentString = wv.settings.userAgentString + " BooksLabApp/1.0"
            wv.setBackgroundColor(Color.BLACK)
            
            // Accept third party cookies for ads
            CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
            
            wv.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    view?.loadUrl(request?.url.toString())
                    return true
                }
            }
            
            // Load from Blogger URL (uses referer automatically)
            wv.loadUrl(BANNER_AD_URL)
        }
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(Color.WHITE)
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.BLACK)
        swipeRefresh.setOnRefreshListener {
            if (isOnline()) {
                webView.reload()
            } else {
                swipeRefresh.isRefreshing = false
                showNoInternetDialog()
            }
        }
    }

    private fun startInterstitialTimer() {
        interstitialTimer = Timer()
        interstitialTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread { showInterstitialAd() }
            }
        }, 180000, 180000) // 3 minutes
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showInterstitialAd() {
        val dialog = AlertDialog.Builder(this)
            .setCancelable(false)
            .create()

        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true
        wv.settings.loadsImagesAutomatically = true
        wv.settings.blockNetworkImage = false
        wv.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        wv.settings.mediaPlaybackRequiresUserGesture = false
        wv.settings.userAgentString = wv.settings.userAgentString + " BooksLabApp/1.0"
        wv.setBackgroundColor(Color.BLACK)
        
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
        
        wv.loadUrl(INTERSTITIAL_AD_URL)

        dialog.setView(wv)
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            if (dialog.isShowing) dialog.dismiss()
        }, 6000)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("Check your internet connection and try again or Pull to Refresh.")
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ ->
                if (isOnline()) webView.loadUrl(MAIN_URL) else showNoInternetDialog()
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Do you want to exit Books Lab?")
                .setPositiveButton("Yes") { _, _ -> super.onBackPressed() }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        interstitialTimer?.cancel()
        super.onDestroy()
    }
}
