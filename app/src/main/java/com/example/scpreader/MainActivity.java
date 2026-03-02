package com.example.scpreader;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SCPAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private SCPAdapter adapter;
    private List<SCPObject> scpList;
    private List<SCPObject> popularScpList;
    private EditText searchField;
    private Spinner categorySpinner;
    private DatabaseHelper dbHelper;
    private WebView hiddenWebView;

    private final String[] categories = {
            "Популярные", "Серия I", "Серия II", "Серия III", "Серия IV", "Серия V",
            "Серия VI", "Серия VII", "Серия VIII", "Серия IX", "Серия X",
            "Филиал RU"
    };

    private final String[] categoryUrls = {
            null,
            "https://scpfoundation.net/scp-series",
            "https://scpfoundation.net/scp-series-2",
            "https://scpfoundation.net/scp-series-3",
            "https://scpfoundation.net/scp-series-4",
            "https://scpfoundation.net/scp-series-5",
            "https://scpfoundation.net/scp-series-6",
            "https://scpfoundation.net/scp-series-7",
            "https://scpfoundation.net/scp-series-8",
            "https://scpfoundation.net/scp-series-9",
            "https://scpfoundation.net/scp-series-10",
            "https://scpfoundation.net/scp-list-ru"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация БД
        dbHelper = new DatabaseHelper(this);

        // Инициализация UI
        recyclerView = findViewById(R.id.scpList);
        searchField = findViewById(R.id.searchField);
        categorySpinner = findViewById(R.id.categorySpinner);

        // Инициализация скрытого WebView для парсинга
        hiddenWebView = findViewById(R.id.hidden_webview);
        hiddenWebView.getSettings().setJavaScriptEnabled(true);
        hiddenWebView.getSettings().setDomStorageEnabled(true);
        hiddenWebView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.127 Mobile Safari/537.36");
        hiddenWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        parseScpList();
                    }
                }, 4000);
            }
        });

        // Настройка Spinner (категории)
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Обработка выбора категории
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String url = categoryUrls[position];
                if (url == null) {
                    // "Популярные"
                    adapter.updateList(popularScpList);
                } else {
                    Toast.makeText(MainActivity.this, "Загрузка списка...", Toast.LENGTH_SHORT).show();
                    hiddenWebView.loadUrl(url);
                }
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Список 50 популярных SCP
        initScpList();

        adapter = new SCPAdapter(new ArrayList<>(popularScpList), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Логика поиска
        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void parseScpList() {
        String js = "(function() { var res=[]; var seen={}; var lis=document.querySelectorAll('li'); for(var i=0;i<lis.length;i++){ var li=lis[i]; var a=li.querySelector('a'); if(a){ var href=a.getAttribute('href')||''; if(href.indexOf('/scp-')==-1){ var num=a.innerText.trim().toUpperCase(); if(num.match(/SCP-\\d+/i)){ var m=li.innerText.match(/SCP-\\d+(?:-[A-Z]+)?/i); if(m) num=m[0].toUpperCase(); } if(num && num.indexOf('SCP-')===0 && !seen[num]){ seen[num]=true; var title=li.innerText.replace(a.innerText, '').replace(/^[\\s\\-\\—\\–\\:\\[\\]]+/, '').trim(); if(title) title='Объект '+num; res.push(num+'|||'+title); } } } } return res.join('###'); })();";

        hiddenWebView.evaluateJavascript(js, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(final String value) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (value == null || value.equals("null") || value.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 2) Распарси полученную строку (сначала убери кавычки)
                        String data = value;
                        if (data.startsWith("\"") && data.endsWith("\"")) {
                            data = data.substring(1, data.length() - 1);
                        }
                        // Убираем экранирование, которое может добавить evaluateJavascript
                        data = data.replace("\\\\", "\\").replace("\\\"", "\"");

                        // split("###")
                        String[] items = data.split("###");

                        // 3) ОЧИСТИ старый список объектов
                        if (scpList == null) {
                            scpList = new ArrayList<>();
                        }
                        scpList.clear();

                        // 4) В цикле добавь новые объекты в список
                        for (String item : items) {
                            // split("\\|\\|\\|")
                            String[] parts = item.split("\\|\\|\\|");
                            if (parts.length >= 2) {
                                String num = parts[0].trim();
                                String title = parts[1].trim();
                                if (!num.isEmpty()) {
                                    scpList.add(new SCPObject(num, title));
                                }
                            }
                        }

                        // 5) ОБНОВИ адаптер
                        adapter.updateList(scpList);

                        // 6) Оставь Toast с количеством найденных объектов
                        Toast.makeText(MainActivity.this, "Найдено объектов: " + scpList.size(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void initScpList() {
        popularScpList = new ArrayList<>();
        // Сначала загружаем из БД (те, что добавил пользователь)
        popularScpList.addAll(dbHelper.getAllSCPs());
        
        // Список 50 популярных объектов
        List<SCPObject> defaultList = new ArrayList<>();
        defaultList.add(new SCPObject("SCP-173", "Скульптура"));
        defaultList.add(new SCPObject("SCP-049", "Чумной доктор"));
        defaultList.add(new SCPObject("SCP-087", "Лестница"));
        defaultList.add(new SCPObject("SCP-096", "Скромник"));
        defaultList.add(new SCPObject("SCP-106", "Старик"));
        defaultList.add(new SCPObject("SCP-682", "Неуязвимая рептилия"));
        defaultList.add(new SCPObject("SCP-914", "Часовой механизм"));
        defaultList.add(new SCPObject("SCP-999", "Щекочущий монстр"));
        defaultList.add(new SCPObject("SCP-079", "Старый ИИ"));
        defaultList.add(new SCPObject("SCP-035", "Маска одержимости"));
        defaultList.add(new SCPObject("SCP-076", "Авель"));
        defaultList.add(new SCPObject("SCP-073", "Каин"));
        defaultList.add(new SCPObject("SCP-2521", "●●|●●●●●|●●|●"));
        defaultList.add(new SCPObject("SCP-3000", "Ананта-Шеша"));
        defaultList.add(new SCPObject("SCP-3008", "Абсолютно нормальная старая добрая Икея"));
        defaultList.add(new SCPObject("SCP-5000", "Почему?"));
        defaultList.add(new SCPObject("SCP-001", "Страж Врат"));
        defaultList.add(new SCPObject("SCP-055", "[ДАННЫЕ УДАЛЕНЫ]"));
        defaultList.add(new SCPObject("SCP-093", "Объект из Красного моря"));
        defaultList.add(new SCPObject("SCP-1762", "Куда делись драконы?"));
        defaultList.add(new SCPObject("SCP-2317", "Пожиратель миров"));
        defaultList.add(new SCPObject("SCP-231", "Семь невест для семи рогов"));
        defaultList.add(new SCPObject("SCP-2935", "О Смерть"));
        defaultList.add(new SCPObject("SCP-3999", "Я есть центр всего, что происходит со мной"));
        defaultList.add(new SCPObject("SCP-4999", "Тот, кто присматривает за нами"));
        defaultList.add(new SCPObject("SCP-2000", "Деус Экс Машина"));
        defaultList.add(new SCPObject("SCP-1471", "MalO ver1.0.0"));
        defaultList.add(new SCPObject("SCP-012", "Скверная композиция"));
        defaultList.add(new SCPObject("SCP-811", "Болотница"));
        defaultList.add(new SCPObject("SCP-1048", "Мишка-строитель"));
        defaultList.add(new SCPObject("SCP-239", "Дитя-ведьма"));
        defaultList.add(new SCPObject("SCP-008", "Зомби-вирус"));
        defaultList.add(new SCPObject("SCP-015", "Кошмарный трубопровод"));
        defaultList.add(new SCPObject("SCP-053", "Девочка"));
        defaultList.add(new SCPObject("SCP-066", "Игрушка Эрика"));
        defaultList.add(new SCPObject("SCP-166", "Суккуб-подросток"));
        defaultList.add(new SCPObject("SCP-2316", "Вы не узнаете тела в воде"));
        defaultList.add(new SCPObject("SCP-3199", "Люди опровергнутые"));
        defaultList.add(new SCPObject("SCP-3812", "Голос позади меня"));
        defaultList.add(new SCPObject("SCP-4666", "Йольский парень"));
        defaultList.add(new SCPObject("SCP-4000", "Табу"));
        defaultList.add(new SCPObject("SCP-408", "Иллюзорные бабочки"));
        defaultList.add(new SCPObject("SCP-420-J", "Самая лучшая [УДАЛЕНО] в мире"));
        defaultList.add(new SCPObject("SCP-426", "Я — тостер"));
        defaultList.add(new SCPObject("SCP-458", "Бесконечная коробка пиццы"));
        defaultList.add(new SCPObject("SCP-500", "Панацея"));
        defaultList.add(new SCPObject("SCP-504", "Критические помидоры"));
        defaultList.add(new SCPObject("SCP-513", "Коровье ботало"));
        defaultList.add(new SCPObject("SCP-610", "Ненавидящая плоть"));
        defaultList.add(new SCPObject("SCP-939", "Со множеством голосов"));
        
        // Добавляем только те, которых еще нет в списке (уникальность по номеру)
        for (SCPObject defaultScp : defaultList) {
            boolean exists = false;
            for (SCPObject existing : popularScpList) {
                if (existing.getNumber().equalsIgnoreCase(defaultScp.getNumber())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                popularScpList.add(defaultScp);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(SCPObject scp) {
        openDetail(scp, false);
    }

    @Override
    public void onDownloadClick(SCPObject scp) {
        openDetail(scp, true);
    }

    private void openDetail(SCPObject scp, boolean autoDownload) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("scp", scp);
        intent.putExtra("auto_download", autoDownload);
        startActivity(intent);
    }
}
