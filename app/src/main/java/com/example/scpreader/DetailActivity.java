package com.example.scpreader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;

public class DetailActivity extends AppCompatActivity {
    private TextView articleTitle, articleContent;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SCPObject scp;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> chain.proceed(chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .build()))
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        articleTitle = findViewById(R.id.articleTitle);
        articleContent = findViewById(R.id.articleContent);
        progressBar = findViewById(R.id.progressBar);
        dbHelper = new DatabaseHelper(this);

        scp = (SCPObject) getIntent().getSerializableExtra("scp");
        if (scp != null) {
            articleTitle.setText(scp.getNumber());
            loadArticle();
        }
    }

    private void loadArticle() {
        String savedText = dbHelper.getArticleContent(scp.getNumber());
        if (savedText != null) {
            articleContent.setText(savedText);
            // TalkBack озвучит текст статьи при фокусировке на ScrollView
        } else {
            downloadArticle();
        }
    }

    private void downloadArticle() {
        progressBar.setVisibility(View.VISIBLE);
        executor.execute(() -> {
            try {
                String number = scp.getNumber().toLowerCase().replace("scp-", "");
                String url = "https://scpfoundation.net/scp-" + number;
                
                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    String html = response.body().string();
                    
                    // Очистка от HTML с помощью Jsoup
                    String cleanText = Jsoup.parse(html).select("#page-content").text();
                    
                    // Если #page-content не найден, попробуем взять весь текст
                    if (cleanText.isEmpty()) {
                        cleanText = Jsoup.parse(html).text();
                    }

                    final String finalCleanText = cleanText;
                    dbHelper.saveArticle(scp.getNumber(), scp.getTitle(), finalCleanText);

                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.GONE);
                        articleContent.setText(finalCleanText);
                        articleContent.announceForAccessibility("Статья загружена и сохранена.");
                    });
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    // Метод для очистки HTML от мусора
    private String cleanHtml(String html) {
        if (html == null) return "";
        // Убираем теги <script> и <style>
        html = html.replaceAll("<script[\\s\\S]*?</script>", "");
        html = html.replaceAll("<style[\\s\\S]*?</style>", "");
        // Убираем все остальные теги
        html = html.replaceAll("<[^>]*>", "");
        // Декодируем некоторые сущности
        html = html.replace("&nbsp;", " ")
                   .replace("&quot;", "\"")
                   .replace("&apos;", "'")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&");
        // Убираем лишние пустые строки
        return html.replaceAll("(?m)^[ \t]*?\r?\n", "").trim();
    }
}
