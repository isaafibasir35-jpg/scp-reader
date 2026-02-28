package com.example.scpreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SCPAdapter.OnItemClickListener {
    private RecyclerView recyclerView;
    private SCPAdapter adapter;
    private List<SCPObject> scpList;
    private EditText searchField, manualInput;
    private Button addButton;
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
        manualInput = findViewById(R.id.manualInput);
        addButton = findViewById(R.id.addButton);

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

        // Ручной ввод
        addButton.setOnClickListener(v -> {
            String number = manualInput.getText().toString().trim();
            if (!number.isEmpty()) {
                if (!number.toLowerCase().startsWith("scp-")) {
                    number = "SCP-" + number;
                }
                SCPObject newScp = new SCPObject(number, "Добавленный объект");
                dbHelper.addSCP(newScp);
                refreshList();
                openDetail(newScp);
            }
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
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.cat_popular) {
            adapter.updateList(scpList);
            return true;
        } else if (id == R.id.cat_series_1) {
            filterSeries(1, 999);
            return true;
        } else if (id == R.id.cat_series_2) {
            filterSeries(1000, 1999);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void filterSeries(int min, int max) {
        List<SCPObject> filtered = new ArrayList<>();
        for (int i = min; i <= max; i += 20) { // Генерация списка для примера (чтобы не было пусто)
            String num = String.format("SCP-%03d", i);
            filtered.add(new SCPObject(num, "Объект из серии " + num));
        }
        adapter.updateList(filtered);
        recyclerView.announceForAccessibility("Отображается список объектов с " + min + " по " + max);
    }

    @Override
    public void onItemClick(SCPObject scp) {
        openDetail(scp);
    }

    private void openDetail(SCPObject scp) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("scp", scp);
        startActivity(intent);
    }
}
