package com.example.scpreader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.widget.Button;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

public class DetailActivity extends AppCompatActivity {
    private WebView webView;
    private RelativeLayout loadingOverlay;
    private Button btnDownload;
    private Button btnUpdate;
    private Button btnBack;
    private SCPObject scp;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        dbHelper = new DatabaseHelper(this);
        webView = findViewById(R.id.webview);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        btnDownload = findViewById(R.id.btnDownload);
        btnUpdate = findViewById(R.id.btnUpdate);
        btnBack = findViewById(R.id.btnBack);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scp")) {
            scp = (SCPObject) intent.getSerializableExtra("scp");
        }
        
        if (scp == null) {
            finish();
            return;
        }

        setTitle(scp.getNumber());
        setupWebView();
            
        if (savedInstanceState == null) {
            loadArticle();
        }

        btnBack.setOnClickListener(v -> {
            onBackPressed();
        });

            btnDownload.setOnClickListener(v -> {
                saveOffline();
            });

            btnUpdate.setOnClickListener(v -> {
                loadingOverlay.setVisibility(View.VISIBLE);
                webView.reload();
            });

            btnDownload.setOnLongClickListener(v -> {
                File file = new File(getFilesDir(), scp.getNumber() + ".html");
                if (file.exists()) {
                    if (file.delete()) {
                        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
                        String number = scp.getNumber().toLowerCase().replace("scp-", "");
                        String url = "https://scpfoundation.net/scp-" + number;
                        webView.getSettings().setJavaScriptEnabled(true);
                        webView.getSettings().setBlockNetworkLoads(false);
                        webView.loadUrl(url);
                    }
                }
                return true;
            });

            if (intent.getBooleanExtra("auto_download", false)) {
                // Будет вызвано после загрузки страницы в onPageFinished для надежности
            }
    }

    private void saveOffline() {
        webView.evaluateJavascript("javascript:window.HTMLOUT.save('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');", null);
    }

    private void setupWebView() {
        webView.setBackgroundColor(android.graphics.Color.parseColor("#121212"));
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

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void save(String html) {
                try {
                    File f = new File(getFilesDir(), scp.getNumber() + ".html");
                    FileOutputStream out = new FileOutputStream(f);
                    out.write(html.getBytes("UTF-8"));
                    out.close();
                    if (scp != null) {
                        dbHelper.addOrUpdateSCP(scp);
                    }
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Статья сохранена", Toast.LENGTH_SHORT).show());
                } catch(Exception e){}
            }
        }, "HTMLOUT");

        // Настройки кэширования для офлайн-режима
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                loadingOverlay.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = "javascript:(function() { " +
                        "var style = document.createElement('style'); " +
                        "style.innerHTML = 'html, body { background-color: #121212 !important; color: #E0E0E0 !important; } " +
                        "#header, #top-bar, nav, #side-bar, #login-status, #search-top-box, #navi-bar, #navi-bar-shadow, #breadcrumbs, " +
                        ".page-rate-widget-box, .rate-box-with-margin, .rate-box-inline-with-margin, .credit-rate, .rate-box, " +
                        ".wd-rate-widget, .credit-rating-box, .rate-box-with-margin, .rate-box-inline-with-margin, " +
                        ".page-tags, #page-info-section, #page-info-break, #footer, #license-area, #page-options-bottom, " +
                        "#page-options-container, #action-area, #discuss-button, .bottom-group, .license-area, " +
                        ".printuser, #odialog-container, .wd-editor-mobile-indicator, #header-extra-div-1, #header-extra-div-2, " +
                        "#header-extra-div-3, .error-block, #content-panel { display: none !important; } " +
                        "#main-content { margin: 0 !important; padding: 15px !important; width: 100% !important; border: none !important; } " +
                        "#page-content { font-size: 1.1em !important; line-height: 1.6 !important; }'; " +
                        "document.head.appendChild(style); " +
                        "return true; " +
                        "})()";
                view.evaluateJavascript(js, value -> {
                    loadingOverlay.postDelayed(() -> loadingOverlay.setVisibility(View.GONE), 150);
                });
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadArticle() {
        File file = new File(getFilesDir(), scp.getNumber() + ".html");
        String number = scp.getNumber().toLowerCase().replace("scp-", "");
        String url = "https://scpfoundation.net/scp-" + number;

        if (file.exists()) {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setBlockNetworkLoads(true);
            try {
                StringBuilder content = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                webView.loadDataWithBaseURL("https://scpfoundation.net/", content.toString(), "text/html", "UTF-8", null);
            } catch (Exception e) {
                e.printStackTrace();
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setBlockNetworkLoads(false);
                webView.loadUrl(url);
            }
        } else {
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setBlockNetworkLoads(false);
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
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
