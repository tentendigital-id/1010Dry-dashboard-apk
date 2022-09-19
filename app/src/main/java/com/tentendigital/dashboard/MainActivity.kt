package com.tentendigital.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.webkit.WebSettings.RenderPriority
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import java.net.CookieHandler
import java.net.CookiePolicy
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mContext: Context
    internal var mLoaded = false
    internal var URL = "https://dashboard.1010dry.id/"

    internal var doubleBackToExitPressedOnce = false
    var filePath: ValueCallback<Array<Uri>>? = null

    private lateinit var btnTryAgain: Button
    private lateinit var mWebView: WebView
    private lateinit var prgs: ProgressBar
    private lateinit var layoutSplash: ConstraintLayout
    private lateinit var layoutWebview: RelativeLayout
    private lateinit var layoutNoInternet: RelativeLayout

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        mLoaded = false
        mContext = this
        mWebView = findViewById<View>(R.id.webview) as WebView
        prgs = findViewById<View>(R.id.progressBar) as ProgressBar
        btnTryAgain = findViewById<View>(R.id.btn_try_again) as Button
        layoutWebview = findViewById<View>(R.id.layout_webview) as RelativeLayout
        layoutNoInternet = findViewById<View>(R.id.layout_no_internet) as RelativeLayout
        layoutSplash = findViewById<View>(R.id.layout_splash) as ConstraintLayout

        val cookieManager = CookieManager.getInstance()
        CookieManager.setAcceptFileSchemeCookies(true)
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(mWebView, true)

        val manager = java.net.CookieManager()
        manager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        CookieHandler.setDefault(manager)

        requestForWebview()
        hasWriteStoragePermission()

        btnTryAgain.setOnClickListener {
            mWebView.visibility = View.GONE
            prgs.visibility = View.VISIBLE
            layoutSplash.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            requestForWebview()
        }
    }

    private fun requestForWebview() {
        if (!mLoaded) {
            requestWebView()
            Handler().postDelayed({
                //prgs.visibility = View.VISIBLE
                mWebView.visibility = View.VISIBLE
            }, 3000)

        } else {
            mWebView.visibility = View.VISIBLE
            prgs.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.GONE
        }

    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun requestWebView() {
        if (internetCheck(mContext)) {
            mWebView.visibility = View.VISIBLE
            layoutNoInternet.visibility = View.GONE
            mWebView.loadUrl(URL)
        } else {
            prgs.visibility = View.GONE
            mWebView.visibility = View.GONE
            layoutSplash.visibility = View.GONE
            layoutNoInternet.visibility = View.VISIBLE
            return
        }
        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.setGeolocationEnabled(true)
        mWebView.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
        mWebView.settings.setRenderPriority(RenderPriority.HIGH)
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        mWebView.settings.domStorageEnabled = true
        mWebView.settings.setAppCacheEnabled(true)
        mWebView.settings.databaseEnabled = true
        mWebView.settings.allowContentAccess = true

        mWebView.settings.setSupportMultipleWindows(false)
        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val cookieString = CookieManager.getInstance().getCookie(url)
            downloadFile(url!!, cookieString)
        }
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {

                Log.d("101010 overide", url!!)
                if (internetCheck(mContext)) {
                    if (url.contains("dashboard.1010dry.id/user/logout")){
                        WebStorage.getInstance().deleteAllData()
                        CookieManager.getInstance().removeAllCookies {}
                    }

                    if (url.contains("1010dry.id")) {
                        return false
                    }else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    }
                } else {
                    prgs.visibility = View.GONE
                    mWebView.visibility = View.GONE
                    layoutSplash.visibility = View.GONE
                    layoutNoInternet.visibility = View.VISIBLE
                }

                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                if (prgs.visibility == View.GONE) {
                    prgs.visibility = View.VISIBLE
                }
                Log.d("101010 started", url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                mLoaded = true
                if (prgs.visibility == View.VISIBLE)
                    prgs.visibility = View.GONE

                Log.d("101010 finished", url!!)
                Handler().postDelayed({
                    layoutSplash.visibility = View.GONE
                }, 2000)
            }
        }

        mWebView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView, filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams): Boolean {

                filePath = filePathCallback
                val contentIntent = Intent(Intent.ACTION_GET_CONTENT)
                contentIntent.type = "image/*"
                contentIntent.addCategory(Intent.CATEGORY_OPENABLE)

                startActivityForResult(contentIntent, 1)
                return true
            }
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_CANCELED) {
            filePath?.onReceiveValue(null)
            return
        } else if (resultCode == Activity.RESULT_OK) {
            if (filePath == null) return
            filePath!!.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            )
            filePath = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            if (!((mWebView.url == "https://dashboard.1010dry.id/") || (mWebView.url == "https://dashboard.1010dry.id/user/login"))) {
                mWebView.goBack()
                return true
            }
        }

        if (doubleBackToExitPressedOnce) {
            return super.onKeyDown(keyCode, event)
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Tekan kembali sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
        return true
    }

    fun downloadFile(url: String, cookieString: String) {
        val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)

        val date: String = SimpleDateFormat("HHmmss_dd_MM_yyyy", Locale.getDefault()).format(Date())
        var filename = ""

        if(uri.lastPathSegment!! == "antrian"){
            if(uri.getQueryParameter("filter") == null){
                filename = String.format("produksi_antrian_%s.xls", date)
            }else if(uri.getQueryParameter("filter") == "13"){
                filename = String.format("produksi_antrian_%s.xls", date)
            }else if(uri.getQueryParameter("filter") == "12"){
                filename = String.format("produksi_proses_%s.xls", date)
            }else if(uri.getQueryParameter("filter") == "14"){
                filename = String.format("produksi_selesai_%s.xls", date)
            }
        }else {
            if (uri.pathSegments.size > 1) {
                filename = String.format("%s_%s_%s.xls", uri.pathSegments[uri.pathSegments.size - 2], uri.lastPathSegment!!, date)
            } else {
                filename = String.format("%s_%s.xls", uri.lastPathSegment!!, date)
            }
        }

        request.addRequestHeader("cookie", cookieString)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)

        val reference: Long = manager.enqueue(request)
    }

    private fun hasWriteStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
                return false
            }
        }
        return true
    }

    companion object {
        fun internetCheck(context: Context): Boolean {
            var available = false
            val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkInfo = connectivity.allNetworkInfo
            for (i in networkInfo.indices) {
                if (networkInfo[i].state == NetworkInfo.State.CONNECTED) {
                    available = true
                    break
                }
            }
            return available
        }
    }
}