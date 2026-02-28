package com.example.scpreader;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import java.io.Serializable;

public class DetailActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private SCPObject scp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scp")) {
            scp = (SCPObject) intent.getSerializableExtra("scp");
        }
        
        if (scp != null) {
            setTitle(scp.getNumber());
            setupWebView();
            loadArticle();
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        // Настройки кэширования для офлайн-режима
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        settings.setAllowFileAccess(true);
        
        // Современный способ кэширования через DOM Storage и стандартный кэш браузера
        // LOAD_CACHE_ELSE_NETWORK заставит WebView брать данные из кэша, если сети нет.

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                // Инъекция JS для скрытия визуального мусора
                view.evaluateJavascript("try { " +
                        "document.getElementById('header').style.display='none'; " +
                        "document.getElementById('top-bar').style.display='none'; " +
                        "document.getElementById('side-bar').style.display='none'; " +
                        "document.getElementById('footer').style.display='none'; " +
                        "document.getElementById('page-options-container').style.display='none'; " +
                        "} catch(e) {}", null);
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Если ошибка связана с отсутствием сети, но кэш может сработать, 
                // мы просто полагаемся на LOAD_CACHE_ELSE_NETWORK.
            }
        });
    }

    private void loadArticle() {
        String number = scp.getNumber().toLowerCase().replace("scp-", "");
        String url = "https://scpfoundation.net/scp-" + number;
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
