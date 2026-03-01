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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Serializable;

public class DetailActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;
    private Button btnDownload;
    private Button btnUpdate;
    private SCPObject scp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progressBar);
        btnDownload = findViewById(R.id.btnDownload);
        btnUpdate = findViewById(R.id.btnUpdate);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scp")) {
            scp = (SCPObject) intent.getSerializableExtra("scp");
        }
        
        if (scp != null) {
            setTitle(scp.getNumber());
            setupWebView();
            
            if (savedInstanceState == null) {
                loadArticle();
            }

            btnDownload.setOnClickListener(v -> {
                saveOffline();
            });

            btnUpdate.setOnClickListener(v -> {
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
    }

    private void saveOffline() {
        webView.evaluateJavascript("javascript:window.HTMLOUT.save('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');", null);
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

        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void save(String html) {
                try {
                    java.io.File f = new java.io.File(getFilesDir(), scp.getNumber() + ".html");
                    java.io.FileOutputStream out = new java.io.FileOutputStream(f);
                    out.write(html.getBytes("UTF-8"));
                    out.close();
                    runOnUiThread(() -> android.widget.Toast.makeText(DetailActivity.this, "Статья сохранена", android.widget.Toast.LENGTH_SHORT).show());
                } catch(Exception e){}
            }
        }, "HTMLOUT");

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
            }

            @Override
            public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
            }

            @Override
            public void onReceivedHttpError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceResponse errorResponse) {
            }
        });
    }

    private void loadArticle() {
        File file = new File(getFilesDir(), scp.getNumber() + ".html");
        String number = scp.getNumber().toLowerCase().replace("scp-", "");
        String url = "https://scpfoundation.net/scp-" + number;

        if (file.exists()) {
            webView.getSettings().setJavaScriptEnabled(false);
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
