package com.example.scpreader;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;

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

        scp = (SCPObject) getIntent().getSerializableExtra("scp");
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
