package com.example.login;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class OpportunitiesActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private OpportunityAdapter adapter;
    private List<Opportunity> opportunityList;
    private ProgressBar progressBar;

    private ChipGroup formatChipGroup, categoryChipGroup, costChipGroup;
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
        formatChipGroup = findViewById(R.id.format_chip_group);
        categoryChipGroup = findViewById(R.id.category_chip_group);
        costChipGroup = findViewById(R.id.cost_chip_group);

        setupChipGroupListener(formatChipGroup, "format");
        setupChipGroupListener(categoryChipGroup, "category");
        setupChipGroupListener(costChipGroup, "cost");
    }

    private void setupChipGroupListener(ChipGroup chipGroup, String type) {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }

            int checkedId = checkedIds.get(0);
            Chip chip = findViewById(checkedId);
            if (chip != null) {
                String text = chip.getText().toString();

                switch (type) {
                    case "format":
                        selectedFormat = text;
                        break;
                    case "category":
                        selectedCategory = text;
                        break;
                    case "cost":
                        selectedCost = text;
                        break;
                }

                applyFilters();
            }
        });
    }

    private void applyFilters() {
        adapter.filter(selectedFormat, selectedCategory, selectedCost);
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
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(OpportunitiesActivity.this,
                            "Failed to load opportunities", Toast.LENGTH_SHORT).show();
                    Log.e("OpportunitiesActivity", "Error loading", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONArray jsonArray = new JSONArray(responseBody);
                        List<Opportunity> newOpportunities = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            Opportunity opp = new Opportunity();
                            opp.setId(obj.optString("id"));
                            opp.setTitle(obj.optString("title"));
                            opp.setDescription(obj.optString("description"));
                            opp.setUrl(obj.optString("url"));
                            opp.setSource(obj.optString("source"));
                            opp.setFormat(obj.optString("format"));
                            opp.setCategory(obj.optString("category"));
                            opp.setCost(obj.optString("cost"));
                            opp.setLocation(obj.optString("location"));
                            opp.setImageUrl(obj.optString("image_url"));
                            newOpportunities.add(opp);
                        }

                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            opportunityList.clear();
                            opportunityList.addAll(newOpportunities);
                            adapter.updateData(newOpportunities);

                            if (newOpportunities.isEmpty()) {
                                fetchAndPopulateOpportunities();
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(OpportunitiesActivity.this,
                                    "Error parsing data", Toast.LENGTH_SHORT).show();
                            Log.e("OpportunitiesActivity", "Parse error", e);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Log.e("OpportunitiesActivity", "Response error: " + responseBody);
                    });
                }
            }
        });
    }

    private void fetchAndPopulateOpportunities() {
        String supabaseUrl = BuildConfig.SUPABASE_URL;
        String supabaseKey = BuildConfig.SUPABASE_ANON_KEY;

        Request request = new Request.Builder()
                .url(supabaseUrl + "/functions/v1/fetch-opportunities")
                .header("apikey", supabaseKey)
                .header("Authorization", "Bearer " + supabaseKey)
                .post(okhttp3.RequestBody.create("", null))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("OpportunitiesActivity", "Failed to populate", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> loadOpportunities());
                }
            }
        });
    }
}
