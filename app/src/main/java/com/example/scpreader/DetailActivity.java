package com.example.scpreader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jsoup.Jsoup;

public class DetailActivity extends AppCompatActivity {
    private TextView articleTitle, articleContent;
    private ProgressBar progressBar;
    private DatabaseHelper dbHelper;
    private SCPObject scp;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
                URL url = new URL("https://scpfoundation.net/scp-" + number);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                boolean isContentStarted = false;

                while ((line = reader.readLine()) != null) {
                    // Очень простая логика для поиска основного контента
                    if (line.contains("page-content")) isContentStarted = true;
                    if (isContentStarted) {
                        sb.append(line).append("\n");
                    }
                    if (line.contains("page-info-container")) break;
                }
                reader.close();

                // Очистка от HTML с помощью Jsoup
                String cleanText = Jsoup.parse(sb.toString()).text();
                
                dbHelper.saveArticle(scp.getNumber(), scp.getTitle(), cleanText);

                mainHandler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    articleContent.setText(cleanText);
                    articleContent.announceForAccessibility("Статья загружена и сохранена.");
                });

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
        // Убираем теги <script> и <style>
        html = html.replaceAll("<script[\s\S]*?</script>", "");
        html = html.replaceAll("<style[\s\S]*?</style>", "");
        // Убираем все остальные теги
        html = html.replaceAll("<[^>]*>", "");
        // Декодируем некоторые сущности
        html = html.replace("&nbsp;", " ")
                   .replace("&quot;", """)
                   .replace("&apos;", "'")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&");
        // Убираем лишние пустые строки
        return html.replaceAll("(?m)^[ 	]*?
", "").trim();
    }
}
