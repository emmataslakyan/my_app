package com.example.login;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// IMPORTANT: This import connects the code to the generated Gradle constants
import com.example.login.BuildConfig;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OpportunitiesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private OpportunityAdapter adapter;
    private List<Opportunity> opportunityList;
    private ProgressBar progressBar;

    private String selectedFormat = "All Formats";
    private String selectedCategory = "All Categories";
    private String selectedCost = "All";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opportunities);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        recyclerView = findViewById(R.id.opportunities_recycler_view);
        progressBar = findViewById(R.id.progress_bar);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        opportunityList = new ArrayList<>();
        adapter = new OpportunityAdapter(opportunityList);
        recyclerView.setAdapter(adapter);

        setupFilters();
        loadOpportunities();
    }

    private void setupFilters() {
        // Converted to local variables to satisfy linting
        ChipGroup formatChipGroup = findViewById(R.id.format_chip_group);
        ChipGroup categoryChipGroup = findViewById(R.id.category_chip_group);
        ChipGroup costChipGroup = findViewById(R.id.cost_chip_group);

        setupChipGroupListener(formatChipGroup, "format");
        setupChipGroupListener(categoryChipGroup, "category");
        setupChipGroupListener(costChipGroup, "cost");
    }

    private void setupChipGroupListener(ChipGroup chipGroup, String type) {
        if (chipGroup == null) return;
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;

            int checkedId = checkedIds.get(0);
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                String text = chip.getText().toString();
                switch (type) {
                    case "format": selectedFormat = text; break;
                    case "category": selectedCategory = text; break;
                    case "cost": selectedCost = text; break;
                }
                adapter.filter(selectedFormat, selectedCategory, selectedCost);
            }
        });
    }

    private void loadOpportunities() {
        progressBar.setVisibility(View.VISIBLE);

        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;

        Request request = new Request.Builder()
                .url(supabaseUrl + "/rest/v1/opportunities?select=*")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OpportunitiesActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // IOException is handled inside the try-with-resources if string() fails
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                        return;
                    }

                    String rawJson = body.string();
                    List<Opportunity> results = parseJson(rawJson);

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        opportunityList.clear();
                        opportunityList.addAll(results);
                        adapter.updateData(results);

                        if (results.isEmpty()) {
                            fetchAndPopulateOpportunities();
                        }
                    });
                } catch (Exception e) {
                    Log.e("API_ERROR", "Parse failure", e);
                }
            }
        });
    }

    private List<Opportunity> parseJson(String json) throws Exception {
        JSONArray array = new JSONArray(json);
        List<Opportunity> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            Opportunity opp = new Opportunity();
            opp.setId(obj.optString("id"));
            opp.setTitle(obj.optString("title"));
            opp.setDescription(obj.optString("description"));
            opp.setUrl(obj.optString("url"));
            opp.setFormat(obj.optString("format"));
            opp.setCategory(obj.optString("category"));
            opp.setCost(obj.optString("cost"));
            opp.setLocation(obj.optString("location"));
            list.add(opp);
        }
        return list;
    }

    private void fetchAndPopulateOpportunities() {
        String url = BuildConfig.SUPABASE_URL;
        String key = BuildConfig.SUPABASE_ANON_KEY;

        RequestBody empty = RequestBody.create("", MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url + "/functions/v1/fetch-opportunities")
                .header("apikey", key)
                .header("Authorization", "Bearer " + key)
                .post(empty)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("Populate", "Failed", e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> loadOpportunities());
                }
                response.close();
            }
        });
    }
}