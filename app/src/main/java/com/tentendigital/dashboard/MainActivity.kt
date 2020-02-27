package com.tentendigital.dashboard

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.CookieSyncManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView.webViewClient = myWebclient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://dashboard.1010dry.id/")
        progressBar.visibility = View.VISIBLE
    }

    inner class myWebclient : WebViewClient() {

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            progressBar.visibility = View.VISIBLE
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            progressBar.visibility = View.GONE
            CookieSyncManager.getInstance().sync()
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith("whatsapp:")) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                startActivity(intent)
                return true
            }
            view.loadUrl(url)
            return super.shouldOverrideUrlLoading(view, url)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            if ((webView.url=="https://dashboard.1010dry.id/") || (webView.url=="https://dashboard.1010dry.id/user/login")){
                return super.onKeyDown(keyCode, event)
            }
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
