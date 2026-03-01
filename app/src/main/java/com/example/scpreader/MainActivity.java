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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SCPAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private SCPAdapter adapter;
    private List<SCPObject> scpList;
    private EditText searchField;
    private Spinner categorySpinner;
    private DatabaseHelper dbHelper;

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

        // Настройка Spinner (категории)
        String[] categories = {
                "Популярные", "Серия I", "Серия II", "Серия III", "Серия IV", "Серия V",
                "Серия VI", "Серия VII", "Серия VIII", "Серия IX", "Серия X",
                "Шуточные (J)", "Обоснованные (EX)", "Филиал RU", "Филиал FR",
                "Филиал JP", "Филиал ES", "Филиал UA"
        };
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        // Обработка выбора категории
        categorySpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selected = categories[position];
                if (selected.equals("Популярные")) {
                    adapter.updateList(scpList);
                } else if (selected.startsWith("Серия")) {
                    int seriesNum = position; // Позиция совпадает с номером серии (Серия I = 1, и т.д.)
                    filterSeries((seriesNum - 1) * 1000, seriesNum * 1000 - 1);
                } else {
                    // Другие категории пока без фильтрации или со специальной логикой
                    // Временно выводим все (или можно расширить логику)
                    adapter.updateList(scpList);
                }
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Список 50 популярных SCP
        initScpList();

        adapter = new SCPAdapter(scpList, this);
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

    private void refreshList() {
        initScpList();
        adapter.updateList(scpList);
    }

    private void initScpList() {
        scpList = new ArrayList<>();
        // Сначала загружаем из БД (те, что добавил пользователь)
        scpList.addAll(dbHelper.getAllSCPs());
        
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
            for (SCPObject existing : scpList) {
                if (existing.getNumber().equalsIgnoreCase(defaultScp.getNumber())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                scpList.add(defaultScp);
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
        int id = item.getItemId();
        // Меню категорий теперь в Spinner, здесь можно оставить только общие действия
        return super.onOptionsItemSelected(item);
    }

    private void filterSeries(int min, int max) {
        List<SCPObject> filtered = new ArrayList<>();
        // Фильтруем объекты по номеру из scpList (если они там есть)
        for (SCPObject scp : scpList) {
            try {
                String numStr = scp.getNumber().replaceAll("[^0-9]", "");
                if (!numStr.isEmpty()) {
                    int num = Integer.parseInt(numStr);
                    if (num >= min && num <= max) {
                        filtered.add(scp);
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
        
        // Если ничего не нашли в текущем списке (например, для новых серий), 
        // добавляем заполнители для примера (как в старой логике, но более чисто)
        if (filtered.isEmpty()) {
            for (int i = min; i < min + 5; i++) {
                String num = String.format("SCP-%03d", i);
                filtered.add(new SCPObject(num, "Объект серии " + num));
            }
        }
        
        adapter.updateList(filtered);
        recyclerView.announceForAccessibility("Отображается список объектов с " + min + " по " + max);
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
