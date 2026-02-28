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
import java.net.HttpURLConnection;
import java.net.URL;

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
            loadArticle();

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
        new Thread(() -> {
            try {
                String number = scp.getNumber().toLowerCase().replace("scp-", "");
                String urlString = "https://scpfoundation.net/scp-" + number;
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    reader.close();

                    File file = new File(getFilesDir(), scp.getNumber() + ".html");
                    FileWriter writer = new FileWriter(file);
                    writer.write(stringBuilder.toString());
                    writer.close();

                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Статья сохранена", Toast.LENGTH_SHORT).show());
                } else {
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Ошибка при скачивании", Toast.LENGTH_SHORT).show());
                }
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
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
        File file = new File(getFilesDir(), scp.getNumber() + ".html");
        if (file.exists()) {
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
                String number = scp.getNumber().toLowerCase().replace("scp-", "");
                String url = "https://scpfoundation.net/scp-" + number;
                webView.loadUrl(url);
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
