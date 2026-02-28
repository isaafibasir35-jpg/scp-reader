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
import android.widget.Button;
import android.widget.Toast;
import java.io.File;
import java.io.Serializable;

public class DetailActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private Button btnDownload;
    private SCPObject scp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        btnDownload = findViewById(R.id.btnDownload);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scp")) {
            scp = (SCPObject) intent.getSerializableExtra("scp");
        }
        
        if (scp != null) {
            setTitle(scp.getNumber());
            setupWebView();
            loadArticle();

            btnDownload.setOnClickListener(v -> {
                saveOffline();
            });

            btnDownload.setOnLongClickListener(v -> {
                File file = new File(getFilesDir(), scp.getNumber() + ".xml.webarchive");
                if (file.exists()) {
                    if (file.delete()) {
                        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
                        String number = scp.getNumber().toLowerCase().replace("scp-", "");
                        String url = "https://scpfoundation.net/scp-" + number;
                        webView.loadUrl(url);
                    }
                }
                return true;
            });

            if (intent.getBooleanExtra("auto_download", false)) {
                // Будет вызвано после загрузки страницы в onPageFinished для надежности
                // или сразу здесь, если мы уверены в WebView.
                // Но лучше дождаться окончания загрузки, если это новый файл.
            }
        }
    }

    private void saveOffline() {
        File file = new File(getFilesDir(), scp.getNumber() + ".xml.webarchive");
        webView.saveWebArchive(file.getAbsolutePath());
        Toast.makeText(this, "Статья сохранена", Toast.LENGTH_SHORT).show();
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");

        // Настройки кэширования для офлайн-режима
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                // Инъекция JS для скрытия визуального мусора
                view.evaluateJavascript("try { document.getElementById('header').style.display='none'; document.getElementById('top-bar').style.display='none'; document.getElementById('side-bar').style.display='none'; document.getElementById('footer').style.display='none'; document.getElementById('page-options-container').style.display='none'; } catch(e) {}", null);
                
                if (getIntent().getBooleanExtra("auto_download", false)) {
                    getIntent().removeExtra("auto_download"); // Чтобы не качать повторно при пересоздании
                    saveOffline();
                }
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Ошибка
            }
        });
    }

    private void loadArticle() {
        File file = new File(getFilesDir(), scp.getNumber() + ".xml.webarchive");
        if (file.exists()) {
            if (file.length() == 0) {
                file.delete();
                String number = scp.getNumber().toLowerCase().replace("scp-", "");
                String url = "https://scpfoundation.net/scp-" + number;
                webView.loadUrl(url);
            } else {
                webView.loadUrl("file://" + file.getAbsolutePath());
            }
        } else {
            String number = scp.getNumber().toLowerCase().replace("scp-", "");
            String url = "https://scpfoundation.net/scp-" + number;
            webView.loadUrl(url);
        }
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
