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
import android.widget.ImageButton;
import android.widget.TextView;
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
    private ImageButton btnDownload;
    private ImageButton btnBack;
    private TextView toolbarTitle;
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
        btnBack = findViewById(R.id.btnBack);
        toolbarTitle = findViewById(R.id.toolbar_title);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scp")) {
            scp = (SCPObject) intent.getSerializableExtra("scp");
        }
        
        if (scp == null) {
            finish();
            return;
        }

        toolbarTitle.setText(scp.getNumber());
        setupWebView();
            
        if (savedInstanceState == null) {
            loadArticle();
            dbHelper.setRead(scp.getNumber(), scp.getTitle(), true);
        }

        btnBack.setOnClickListener(v -> {
            finish();
        });

        btnDownload.setOnClickListener(v -> {
            saveOffline();
        });

        btnDownload.setOnLongClickListener(v -> {
                File file = new File(getFilesDir(), scp.getNumber() + ".html");
                if (file.exists()) {
                    if (file.delete()) {
                        Toast.makeText(this, "Файл удален", Toast.LENGTH_SHORT).show();
                        String url = getUrlForSCP(scp);
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

    private String getUrlForSCP(SCPObject scp) {
        String rawNumber = scp.getNumber().toLowerCase().trim();
        
        // Особые случаи из вашего списка
        if (rawNumber.contains("daveyoufool")) return "https://scpfoundation.net/daveyoufool-proposal";
        if (rawNumber.contains("директор болд-j")) return "https://scpfoundation.net/director-bold-s-proposal-j";
        if (rawNumber.contains("доктор паланез")) return "https://scpfoundation.net/weryllium-proposal";
        if (rawNumber.contains("общее предложение")) return "https://scpfoundation.net/everyone-s-proposal";
        if (rawNumber.contains("очередное предложение djkaktus")) return "https://scpfoundation.net/djkaktus-s-proposal-j";
        if (rawNumber.equals("scp-(-1)-j")) return "https://scpfoundation.net/scp-minus-1-j";
        if (rawNumber.equals("scp-:3-j")) return "https://scpfoundation.net/scp-003-j";
        if (rawNumber.equals("scp-006-ня-ex")) return "https://scpfoundation.net/scp-006-cu-ex";
        if (rawNumber.equals("scp-3jio-j")) return "https://scpfoundation.net/scp-3v1l-j";
        if (rawNumber.equals("scp-666½-j")) return "https://scpfoundation.net/scp-666-and-a-half-j";
        if (rawNumber.equals("scp-682-ня")) return "https://scpfoundation.net/scp-682-cu";
        if (rawNumber.equals("scp-____-j")) return "https://scpfoundation.net/scp-j";
        if (rawNumber.equals("scp-????-j")) return "https://scpfoundation.net/scp-in-a-box-j";
        if (rawNumber.equals("scp-варуйубевай-j")) return "https://scpfoundation.net/scp-damej-j";
        if (rawNumber.equals("scp-жутко-j")) return "https://scpfoundation.net/scp-spooky-j";
        if (rawNumber.equals("scp-тчту-j")) return "https://scpfoundation.net/scp-ttku-j";
        if (rawNumber.equals("scp-мяу-j")) return "https://scpfoundation.net/scp-meow-j";
        if (rawNumber.equals("scp-загадка-j")) return "https://scpfoundation.net/scp-mystery-j";
        if (rawNumber.equals("scp-wtf-j")) return "https://scpfoundation.net/scp-wtf-j#footnoteref-8";
        if (rawNumber.contains("\\̅") || rawNumber.contains("botnik")) return "https://scpfoundation.net/scp-botnik-j";
        if (rawNumber.equals("сцааааааааааааааааааааа-jp-j")) return "https://scpfoundation.net/scpaaaaaaaaaaaaaaaaaa-jp-j";

        String urlPart;
        if (rawNumber.startsWith("scp-")) {
            urlPart = rawNumber;
        } else if (rawNumber.startsWith("spc-") || rawNumber.startsWith("skp-")) {
            urlPart = rawNumber;
        } else if (rawNumber.contains("кодовое имя:")) {
            urlPart = rawNumber.replace("кодовое имя:", "").trim().replace(" ", "-").replace("/", "-");
        } else if (rawNumber.startsWith("файл №")) {
            urlPart = rawNumber.replace("файл №", "").trim().toLowerCase();
            if (urlPart.startsWith("cn-")) {
                urlPart = "scp-cn-" + urlPart.substring(3);
            }
        } else {
            urlPart = "scp-" + rawNumber;
        }
        
        urlPart = urlPart.replace("--", "-").replace(":", "");
        return "https://scpfoundation.net/" + urlPart;
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
                webView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String js = "javascript:(function() { " +
                        "var style = document.createElement('style'); " +
                        "style.innerHTML = 'html, body { background-color: #121212 !important; color: #E0E0E0 !important; } " +
                        "#header, #top-bar, nav, #side-bar, #login-status, #search-top-box, #navi-bar, #navi-bar-shadow, #breadcrumbs, " +
                        "[class*=\"rate\"], [id*=\"rate\"], .creditRate, .page-rate-widget-box, .rate-box-with-margin, .rate-box-inline-with-margin, .credit-rate, .rate-box, .wd-rate-widget, .credit-rating-box, " +
                        "[class*=\"author\"], [class*=\"credit\"], .author-links-box, .author-info, " +
                        "#page-info, .page-info, [class*=\"version\"], .page-version, " +
                        ".buttons, .page-options-bottom, #page-options-container, #action-area, #discuss-button, " +
                        ".page-tags, #page-info-section, #page-info-break, #footer, #license-area, .bottom-group, .license-area, " +
                        ".printuser, #odialog-container, .wd-editor-mobile-indicator, #header-extra-div-1, #header-extra-div-2, #header-extra-div-3, .error-block, #content-panel { display: none !important; } " +
                        "#main-content { margin: 0 !important; padding: 15px !important; width: 100% !important; border: none !important; } " +
                        "#page-content { font-size: 1.1em !important; line-height: 1.6 !important; }'; " +
                        "document.head.appendChild(style); " +
                        "return true; " +
                        "})()";
                view.evaluateJavascript(js, value -> {
                    loadingOverlay.postDelayed(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        webView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                    }, 150);
                });
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
    }

    private void loadArticle() {
        File file = new File(getFilesDir(), scp.getNumber() + ".html");
        String url = getUrlForSCP(scp);

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
