package com.example.scpreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.navigation.NavigationView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements SCPAdapter.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private SCPAdapter adapter;
    private Map<String, List<SCPObject>> categoryData;
    private List<SCPObject> allSCPs = new ArrayList<>();
    private List<SCPObject> currentBaseList = new ArrayList<>();
    private EditText searchField;
    private Spinner filterSpinner;
    private DatabaseHelper dbHelper;
    private LinearLayout listLayout;
    private View categoriesLayout;
    private Toolbar toolbar;

    private final String[] categories = {
            "Серия I", "Серия II", "Серия III", "Серия IV", "Серия V",
            "Серия VI", "Серия VII", "Серия VIII", "Серия IX", "Серия X",
            "Шуточные объекты", "Обоснованные объекты",
            "Русский филиал", "Другие филиалы"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DatabaseHelper(this);
        loadJsonData();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        recyclerView = findViewById(R.id.scpList);
        searchField = findViewById(R.id.searchField);
        filterSpinner = findViewById(R.id.filterSpinner);
        listLayout = findViewById(R.id.list_layout);
        categoriesLayout = findViewById(R.id.categories_layout);

        adapter = new SCPAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupCategoryButtons();
        setupFilterSpinner();

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilterSpinner() {
        String[] options = {"Все", "Прочитанные", "Непрочитанные"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void applyFilters() {
        String query = searchField.getText().toString().toLowerCase().trim();
        int filterPos = filterSpinner.getSelectedItemPosition(); // 0: Все, 1: Прочитанные, 2: Непрочитанные

        if (!query.isEmpty() && categoriesLayout.getVisibility() == View.VISIBLE) {
            categoriesLayout.setVisibility(View.GONE);
            listLayout.setVisibility(View.VISIBLE);
            toolbar.setTitle("Результаты поиска");
        } else if (query.isEmpty() && "Результаты поиска".equals(toolbar.getTitle())) {
            showCategories();
        }

        List<SCPObject> baseList;
        if (!query.isEmpty()) {
            // Если есть поиск, ищем во всех категориях
            baseList = allSCPs;
        } else {
            // Если поиска нет, используем текущий список (категория, избранное и т.д.)
            baseList = currentBaseList;
        }

        Set<String> favorites = dbHelper.getAllFavoritesNumbers();
        Set<String> reads = dbHelper.getAllReadNumbers();

        List<SCPObject> filteredList = new ArrayList<>();
        for (SCPObject scp : baseList) {
            boolean isFavorite = favorites.contains(scp.getNumber());
            boolean isRead = reads.contains(scp.getNumber());
            
            scp.setFavorite(isFavorite);
            scp.setRead(isRead);

            // Text search
            boolean matchesQuery = query.isEmpty() || 
                    scp.getNumber().toLowerCase().contains(query) || 
                    scp.getTitle().toLowerCase().contains(query);

            if (!matchesQuery) continue;

            // Read/Unread filter
            boolean matchesFilter = true;
            if (filterPos == 1) { // Прочитанные
                matchesFilter = isRead;
            } else if (filterPos == 2) { // Непрочитанные
                matchesFilter = !isRead;
            }

            if (matchesFilter) {
                filteredList.add(scp);
            }
        }
        adapter.updateList(filteredList);
    }

    private void setupCategoryButtons() {
        GridLayout grid = findViewById(R.id.categories_grid);
        grid.removeAllViews();
        for (String category : categories) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(android.view.Gravity.CENTER);
            itemLayout.setPadding(16, 16, 16, 16);
            itemLayout.setFocusable(true);
            itemLayout.setClickable(true);
            itemLayout.setBackgroundResource(R.drawable.ripple_effect); // We'll create this or use standard

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(12, 12, 12, 12);
            itemLayout.setLayoutParams(params);

            android.widget.ImageView icon = new android.widget.ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(96, 96));
            icon.setImageResource(android.R.drawable.ic_menu_agenda);
            icon.setColorFilter(getResources().getColor(R.color.colorAccent));
            
            android.widget.TextView text = new android.widget.TextView(this);
            text.setText(category);
            text.setGravity(android.view.Gravity.CENTER);
            text.setTextColor(getResources().getColor(R.color.textPrimary));
            text.setTextSize(14);
            text.setLines(2);
            text.setEllipsize(android.text.TextUtils.TruncateAt.END);

            itemLayout.addView(icon);
            itemLayout.addView(text);
            itemLayout.setContentDescription(category);

            itemLayout.setOnClickListener(v -> loadCategory(category));
            grid.addView(itemLayout);
        }
    }

    private void loadCategory(String categoryName) {
        if (categoryName.equals("Другие филиалы")) {
            showOtherBranches();
            return;
        }
        toolbar.setTitle(categoryName);
        categoriesLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        searchField.setText("");

        List<SCPObject> list = categoryData.get(categoryName);
        if (list != null && !list.isEmpty()) {
            currentBaseList = list;
            applyFilters();
        } else {
            Toast.makeText(this, "Список пуст в JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void showOtherBranches() {
        List<String> otherBranches = new ArrayList<>();
        java.util.Set<String> mainCategories = new java.util.HashSet<>(java.util.Arrays.asList(categories));
        for (String key : categoryData.keySet()) {
            if (!mainCategories.contains(key)) {
                otherBranches.add(key);
            }
        }

        if (otherBranches.isEmpty()) {
            Toast.makeText(this, "Нет других филиалов", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otherBranches.size() == 1) {
            loadCategory(otherBranches.get(0));
            return;
        }

        String[] branchArray = otherBranches.toArray(new String[0]);
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Выберите филиал")
                .setItems(branchArray, (dialog, which) -> {
                    toolbar.setTitle(branchArray[which]);
                    categoriesLayout.setVisibility(View.GONE);
                    listLayout.setVisibility(View.VISIBLE);
                    searchField.setText("");
                    currentBaseList = categoryData.get(branchArray[which]);
                    applyFilters();
                })
                .setNegativeButton(R.string.cancel_button, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateStatusesInList(List<SCPObject> list) {
        for (SCPObject scp : list) {
            scp.setFavorite(dbHelper.isFavorite(scp.getNumber()));
            scp.setRead(dbHelper.isRead(scp.getNumber()));
        }
    }

    private void loadJsonData() {
        categoryData = new HashMap<>();
        allSCPs = new ArrayList<>();
        try {
            InputStream is = getAssets().open("database.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            String json = sb.toString();
            JSONObject obj = new JSONObject(json);

            java.util.Iterator<String> keys = obj.keys();
            while (keys.hasNext()) {
                String category = keys.next();
                List<SCPObject> list = new ArrayList<>();
                JSONArray array = obj.getJSONArray(category);
                for (int i = 0; i < array.length(); i++) {
                    String item = array.getString(i);
                    String[] parts = item.split("\\|\\|\\|");
                    if (parts.length >= 2) {
                        SCPObject scp = new SCPObject(parts[0].trim(), parts[1].trim());
                        list.add(scp);
                        allSCPs.add(scp);
                    }
                }
                categoryData.put(category, list);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("MainActivity", "Error loading JSON data", e);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_categories) {
            showCategories();
        } else if (id == R.id.nav_random) {
            openRandomArticle();
        } else if (id == R.id.nav_favorites) {
            showFavorites();
        } else if (id == R.id.nav_saved) {
            showSaved();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showCategories() {
        toolbar.setTitle(R.string.app_name);
        categoriesLayout.setVisibility(View.VISIBLE);
        listLayout.setVisibility(View.GONE);
        searchField.setText("");
    }

    private void showFavorites() {
        toolbar.setTitle("Избранное");
        categoriesLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        searchField.setText("");
        currentBaseList = dbHelper.getFavorites();
        applyFilters();
    }

    private void showSaved() {
        toolbar.setTitle("Сохраненные");
        categoriesLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        searchField.setText("");
        
        List<SCPObject> savedList = new ArrayList<>();
        File dir = getFilesDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".html"));
        if (files != null) {
            for (File f : files) {
                String number = f.getName().replace(".html", "");
                SCPObject scp = dbHelper.getSCP(number);
                if (scp == null) {
                    String title = findTitle(number);
                    scp = new SCPObject(number, title);
                }
                savedList.add(scp);
            }
        }
        currentBaseList = savedList;
        applyFilters();
    }

    private String findTitle(String number) {
        // Check DB first
        SCPObject fromDb = dbHelper.getSCP(number);
        if (fromDb != null && fromDb.getTitle() != null) return fromDb.getTitle();

        for (List<SCPObject> list : categoryData.values()) {
            for (SCPObject scp : list) {
                if (scp.getNumber().equalsIgnoreCase(number)) return scp.getTitle();
            }
        }
        return "Объект " + number;
    }

    private void openRandomArticle() {
        List<SCPObject> all = new ArrayList<>();
        for (List<SCPObject> list : categoryData.values()) {
            all.addAll(list);
        }
        if (!all.isEmpty()) {
            SCPObject randomScp = all.get(new Random().nextInt(all.size()));
            openDetail(randomScp, false);
        } else {
            Toast.makeText(this, "Список пуст", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(SCPObject scp) {
        openDetail(scp, false);
    }

    @Override
    public void onDownloadClick(SCPObject scp) {
        openDetail(scp, true);
    }

    @Override
    public void onFavoriteClick(SCPObject scp) {
        dbHelper.setFavorite(scp.getNumber(), scp.getTitle(), scp.isFavorite());
        Toast.makeText(this, scp.isFavorite() ? "Добавлено в избранное" : "Удалено из избранного", Toast.LENGTH_SHORT).show();
    }

    private void openDetail(SCPObject scp, boolean autoDownload) {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("scp", scp);
        intent.putExtra("auto_download", autoDownload);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (listLayout.getVisibility() == View.VISIBLE) {
            showCategories();
        } else {
            super.onBackPressed();
        }
    }
}

