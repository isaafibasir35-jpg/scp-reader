package com.example.scpreader;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
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
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SCPAdapter.OnItemClickListener, NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView recyclerView;
    private SCPAdapter adapter;
    private Map<String, List<SCPObject>> categoryData;
    private EditText searchField;
    private DatabaseHelper dbHelper;
    private WebView hiddenWebView;
    private LinearLayout listLayout;
    private View categoriesLayout;
    private Toolbar toolbar;

    private final String[] categories = {
            "Серия I", "Серия II", "Серия III", "Серия IV", "Серия V",
            "Серия VI", "Серия VII", "Серия VIII", "Серия IX", "Серия X",
            "Филиал RU"
    };

    private final String[] categoryUrls = {
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
        listLayout = findViewById(R.id.list_layout);
        categoriesLayout = findViewById(R.id.categories_layout);

        adapter = new SCPAdapter(new ArrayList<>(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        setupCategoryButtons();

        hiddenWebView = findViewById(R.id.hidden_webview);
        hiddenWebView.getSettings().setJavaScriptEnabled(true);
        hiddenWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                new android.os.Handler().postDelayed(() -> parseScpList(), 4000);
            }
        });

        searchField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupCategoryButtons() {
        GridLayout grid = findViewById(R.id.categories_grid);
        grid.removeAllViews();
        for (String category : categories) {
            Button btn = new Button(this);
            btn.setText(category);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);
            btn.setOnClickListener(v -> loadCategory(category));
            grid.addView(btn);
        }
    }

    private void loadCategory(String categoryName) {
        toolbar.setTitle(categoryName);
        categoriesLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);

        List<SCPObject> list = categoryData.get(categoryName);
        if (list != null && !list.isEmpty()) {
            updateFavoritesInList(list);
            adapter.updateList(list);
        } else {
            // Find URL
            int index = -1;
            for(int i=0; i<categories.length; i++) if(categories[i].equals(categoryName)) index = i;
            if (index != -1) {
                Toast.makeText(this, "Загрузка списка из сети...", Toast.LENGTH_SHORT).show();
                hiddenWebView.loadUrl(categoryUrls[index]);
            }
        }
    }

    private void updateFavoritesInList(List<SCPObject> list) {
        for (SCPObject scp : list) {
            scp.setFavorite(dbHelper.isFavorite(scp.getNumber()));
        }
    }

    private void loadJsonData() {
        categoryData = new HashMap<>();
        try {
            InputStream is = getAssets().open("database.json");
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            is.close();
            String json = sb.toString();
            JSONObject obj = new JSONObject(json);

            for (String category : categories) {
                if (obj.has(category)) {
                    List<SCPObject> list = new ArrayList<>();
                    JSONArray array = obj.getJSONArray(category);
                    for (int i = 0; i < array.length(); i++) {
                        String item = array.getString(i);
                        String[] parts = item.split("\\|\\|\\|");
                        if (parts.length >= 2) {
                            list.add(new SCPObject(parts[0].trim(), parts[1].trim()));
                        }
                    }
                    categoryData.put(category, list);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("MainActivity", "Error loading JSON data", e);
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
    }

    private void showFavorites() {
        toolbar.setTitle("Избранное");
        categoriesLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        List<SCPObject> favorites = dbHelper.getFavorites();
        adapter.updateList(favorites);
    }

    private void showSaved() {
        toolbar.setTitle("Сохраненные");
        categoriesLayout.setVisibility(View.GONE);
        listLayout.setVisibility(View.VISIBLE);
        
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
                    scp.setFavorite(dbHelper.isFavorite(number));
                }
                savedList.add(scp);
            }
        }
        adapter.updateList(savedList);
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

    private void parseScpList() {
        String js = "(function() { var res=[]; var seen={}; var lis=document.querySelectorAll('li'); for(var i=0;i<lis.length;i++){ var li=lis[i]; var aTags=li.querySelectorAll('a'); for(var j=0;j<aTags.length;j++){ var a=aTags[j]; var href=a.getAttribute('href')||''; var match=href.match(/\\/(scp-\\d+(?:-[a-z]+)*)/i); if(match){ var id=match[1].toUpperCase(); if(!seen[id]){ seen[id]=true; var title=li.textContent; title=title.replace(new RegExp(id, 'i'), '').replace(a.textContent, '').replace(/^[\\s\\-\\—\\–\\:\\[\\]]+/, '').replace(/\\n/g, ' ').replace(/\\s+/g, ' ').trim(); if(!title) title='Объект '+id; res.push(id+'|||'+title); } break; } } } return res.join('###'); })();";

        hiddenWebView.evaluateJavascript(js, value -> {
            if (value == null || value.equals("null") || value.isEmpty()) return;
            String data = value;
            if (data.startsWith("\"") && data.endsWith("\"")) data = data.substring(1, data.length() - 1);
            data = data.replace("\\\\", "\\").replace("\\\"", "\"");
            String[] items = data.split("###");
            List<SCPObject> newList = new ArrayList<>();
            for (String item : items) {
                String[] parts = item.split("\\|\\|\\|");
                if (parts.length >= 2) {
                    newList.add(new SCPObject(parts[0].trim(), parts[1].trim()));
                }
            }
            updateFavoritesInList(newList);
            adapter.updateList(newList);
            Toast.makeText(MainActivity.this, "Найдено: " + newList.size(), Toast.LENGTH_SHORT).show();
        });
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
