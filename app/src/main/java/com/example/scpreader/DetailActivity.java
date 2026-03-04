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

import android.view.Menu;
import android.view.MenuItem;
import android.media.AudioAttributes;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import java.util.Locale;
import java.util.Set;

public class DetailActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private WebView webView;
    private RelativeLayout loadingOverlay;
    private Button btnDownload;
    private Button btnUpdate;
    private Button btnBack;
    private SCPObject scp;
    private DatabaseHelper dbHelper;
    
    private TextToSpeech tts;
    private boolean isSpeaking = false;
    private boolean isTtsInitialized = false;

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

        tts = new TextToSpeech(this, this);
        
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("scp")) {
            scp = (SCPObject) intent.getSerializableExtra("scp");
        }
        
        if (scp == null) {
            finish();
            return;
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(scp.getNumber());
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem ttsItem = menu.findItem(R.id.action_tts);
        if (ttsItem != null) {
            ttsItem.setIcon(isSpeaking ? R.drawable.ic_stop : R.drawable.ic_play_arrow);
            ttsItem.setTitle(isSpeaking ? "Остановить" : "Озвучить");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_tts) {
            if (isSpeaking) {
                stopTTS();
            } else {
                startTTS();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(new Locale("ru", "RU"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Русский язык не поддерживается", Toast.LENGTH_SHORT).show();
            } else {
                isTtsInitialized = true;
                
                // Настройка AudioAttributes
                tts.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
                
                // Выбор качественного сетевого женского голоса
                try {
                    Set<Voice> voices = tts.getVoices();
                    if (voices != null) {
                        Voice selectedVoice = null;
                        for (Voice voice : voices) {
                            String name = voice.getName().toLowerCase();
                            Locale locale = voice.getLocale();
                            
                            if (locale.getLanguage().equals("ru") && name.contains("network")) {
                                // Ищем женский голос (обычно не содержит "male")
                                if (!name.contains("male")) {
                                    selectedVoice = voice;
                                    break;
                                }
                            }
                        }
                        if (selectedVoice != null) {
                            tts.setVoice(selectedVoice);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            Toast.makeText(this, "Ошибка инициализации TTS", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTTS() {
        if (!isTtsInitialized) {
            Toast.makeText(this, "TTS не готов", Toast.LENGTH_SHORT).show();
            return;
        }
        // Вызываем JS для получения текста
        webView.evaluateJavascript("javascript:window.TEXTOUT.extract(document.body.innerText);", null);
    }

    private void stopTTS() {
        isSpeaking = false;
        invalidateOptionsMenu();
        if (tts != null) {
            tts.stop();
        }
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

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void extract(String text) {
                if (text == null || text.trim().isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(DetailActivity.this, "Текст не найден", Toast.LENGTH_SHORT).show());
                    return;
                }
                
                // Очистка текста
                final String cleanText = text.replaceAll("\\s+", " ").trim();
                
                runOnUiThread(() -> {
                    isSpeaking = true;
                    invalidateOptionsMenu();
                    Toast.makeText(DetailActivity.this, "Запуск озвучки...", Toast.LENGTH_SHORT).show();
                    
                    tts.setOnUtteranceProgressListener(new android.speech.tts.UtteranceProgressListener() {
                        @Override
                        public void onStart(String utteranceId) {}

                        @Override
                        public void onDone(String utteranceId) {
                            // Проверяем, был ли это последний кусок
                            if (utteranceId.endsWith("_last")) {
                                runOnUiThread(() -> {
                                    isSpeaking = false;
                                    invalidateOptionsMenu();
                                });
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {
                            runOnUiThread(() -> {
                                isSpeaking = false;
                                invalidateOptionsMenu();
                            });
                        }
                    });

                    // Разбиение на куски
                    int maxLen = tts.getMaxSpeechInputLength();
                    if (maxLen > 4000) maxLen = 4000; // На некоторых устройствах лимит слишком велик
                    
                    if (cleanText.length() <= maxLen) {
                        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "utterance_last");
                    } else {
                        int start = 0;
                        while (start < cleanText.length()) {
                            int end = Math.min(start + maxLen, cleanText.length());
                            String chunk = cleanText.substring(start, end);
                            boolean isLast = (end == cleanText.length());
                            
                            tts.speak(chunk, start == 0 ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD, 
                                    null, isLast ? "utterance_last" : "utterance_" + start);
                            start = end;
                        }
                    }
                });
            }
        }, "TEXTOUT");

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
        stopTTS();
        if (tts != null) {
            tts.shutdown();
        }
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
