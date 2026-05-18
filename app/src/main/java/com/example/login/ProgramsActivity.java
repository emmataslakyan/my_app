package com.example.login;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProgramsActivity extends BaseActivity {

    // Age filter: each entry is the filter's age window [lo, hi]. A program matches when
    // any age range parsed from its description overlaps this window. null = no filter.
    private static final String[] AGE_LABELS = {"Any Age", "18-25 years", "25-35 years", "35+ years"};
    private static final int[][] AGE_RANGES = {
            null,
            {18, 25},
            {25, 35},
            {35, Integer.MAX_VALUE}
    };

    // Range patterns inside a program description.
    // "16-25", "16–25" (en-dash), "16−25" (minus), with optional spaces.
    private static final Pattern AGE_RANGE_RE = Pattern.compile(
            "(?<![0-9])(\\d{1,2})\\s*[-–−]\\s*(\\d{1,3})(?![0-9])");
    // "16+" → [16, ∞)
    private static final Pattern AGE_PLUS_RE = Pattern.compile(
            "(?<![0-9])(\\d{1,2})\\s*\\+");


    // Deadline filter: label → max days from now. -1 = no filter.
    private static final String[] DEADLINE_LABELS = {
            "Any deadline", "Last 24 hours", "In 3 days", "In 7 days", "In 30 days"
    };
    private static final int[] DEADLINE_DAYS = {-1, 1, 3, 7, 30};

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rv;
    private LinearLayout loadingView;
    private LinearLayout errorView;
    private TextView errorMessage;
    private ChipGroup filterChips;
    private Spinner spinnerAge;
    private Spinner spinnerDeadline;

    private final List<Program> allPrograms = new ArrayList<>();
    private final List<Program> programs = new ArrayList<>();
    private ProgramAdapter adapter;
    private ProgramsProvider provider;
    private final Set<ProgramFilter> activeFilters = new LinkedHashSet<>();
    private int ageIdx = 0;
    private int deadlineIdx = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_programs);

        provider = new ProgramsProvider(this);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        rv = findViewById(R.id.rvPrograms);
        loadingView = findViewById(R.id.loadingView);
        errorView = findViewById(R.id.errorView);
        errorMessage = findViewById(R.id.errorMessage);
        filterChips = findViewById(R.id.filterChips);
        spinnerAge = findViewById(R.id.spinnerAge);
        spinnerDeadline = findViewById(R.id.spinnerDeadline);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            ProgramsProvider.clearCache();
            fetch();
        });

        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProgramAdapter(programs);
        rv.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            ProgramsProvider.clearCache();
            fetch();
        });

        setupFilterChips();
        setupDropdowns();

        List<Program> cached = ProgramsProvider.getCache();
        if (cached != null && !cached.isEmpty()) {
            showResults(cached);
        } else {
            fetch();
        }
    }

    private void setupFilterChips() {
        filterChips.removeAllViews();
        activeFilters.clear();
        activeFilters.add(ProgramFilter.ALL);
        for (ProgramFilter f : ProgramFilter.ALL_FILTERS) {
            Chip chip = new Chip(this);
            chip.setText(f.label);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setTag(f);
            chip.setTextSize(15f);
            chip.setChipMinHeight(getResources().getDisplayMetrics().density * 40f);
            if (activeFilters.contains(f)) chip.setChecked(true);
            chip.setOnClickListener(v -> onChipToggled((Chip) v));
            filterChips.addView(chip);
        }
    }

    /**
     * Multi-select with an "All Programs" reset chip:
     * - Tapping "All" deselects every other chip.
     * - Tapping any other chip deselects "All" and toggles that filter.
     * - Deselecting the last chip falls back to "All" (selection can't be empty).
     */
    private void onChipToggled(Chip chip) {
        ProgramFilter f = (ProgramFilter) chip.getTag();
        boolean nowChecked = chip.isChecked();

        if (f == ProgramFilter.ALL) {
            if (!nowChecked) {
                // User tried to deselect "All" by tapping it — keep it on.
                chip.setChecked(true);
                return;
            }
            activeFilters.clear();
            activeFilters.add(ProgramFilter.ALL);
            for (int i = 0; i < filterChips.getChildCount(); i++) {
                Chip c = (Chip) filterChips.getChildAt(i);
                if (c != chip) c.setChecked(false);
            }
        } else {
            if (nowChecked) {
                activeFilters.remove(ProgramFilter.ALL);
                activeFilters.add(f);
                Chip allChip = findChipForFilter(ProgramFilter.ALL);
                if (allChip != null) allChip.setChecked(false);
            } else {
                activeFilters.remove(f);
                if (activeFilters.isEmpty()) {
                    activeFilters.add(ProgramFilter.ALL);
                    Chip allChip = findChipForFilter(ProgramFilter.ALL);
                    if (allChip != null) allChip.setChecked(true);
                }
            }
        }
        applyFilter();
    }

    private Chip findChipForFilter(ProgramFilter f) {
        for (int i = 0; i < filterChips.getChildCount(); i++) {
            Chip c = (Chip) filterChips.getChildAt(i);
            if (c.getTag() == f) return c;
        }
        return null;
    }

    private void setupDropdowns() {
        spinnerAge.setAdapter(makeSpinnerAdapter(AGE_LABELS));
        spinnerDeadline.setAdapter(makeSpinnerAdapter(DEADLINE_LABELS));

        spinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                ageIdx = pos; applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        spinnerDeadline.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                deadlineIdx = pos; applyFilter();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private ArrayAdapter<String> makeSpinnerAdapter(String[] items) {
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, Arrays.asList(items));
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void applyFilter() {
        programs.clear();
        for (Program p : allPrograms) {
            if (!matchesAnyChip(p)) continue;
            if (!matchesAge(p)) continue;
            if (!matchesDeadline(p)) continue;
            programs.add(p);
        }
        adapter.notifyDataSetChanged();
        // Empty-after-filter falls into the same "no results" UX as a fresh empty load.
        if (!allPrograms.isEmpty() && programs.isEmpty()) {
            rv.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
            errorMessage.setText("No programs match your filters.");
        } else if (!programs.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            errorView.setVisibility(View.GONE);
        }
    }

    private boolean matchesAnyChip(Program p) {
        if (activeFilters.isEmpty() || activeFilters.contains(ProgramFilter.ALL)) return true;
        for (ProgramFilter f : activeFilters) {
            if (f.matches(p)) return true;
        }
        return false;
    }

    private boolean matchesAge(Program p) {
        int[] window = AGE_RANGES[ageIdx];
        if (window == null) return true;
        String desc = p.description == null ? "" : p.description;
        if (desc.isEmpty()) return false;

        int filterLo = window[0], filterHi = window[1];

        Matcher m = AGE_RANGE_RE.matcher(desc);
        while (m.find()) {
            int lo = parseIntSafe(m.group(1));
            int hi = parseIntSafe(m.group(2));
            if (lo < 6 || hi < lo || hi > 120) continue; // skip dates/years/typos
            if (lo <= filterHi && hi >= filterLo) return true;
        }

        m = AGE_PLUS_RE.matcher(desc);
        while (m.find()) {
            int lo = parseIntSafe(m.group(1));
            if (lo < 6 || lo > 120) continue;
            // [lo, ∞) overlaps [filterLo, filterHi] iff lo ≤ filterHi
            if (lo <= filterHi) return true;
        }
        return false;
    }

    private boolean matchesDeadline(Program p) {
        int days = DEADLINE_DAYS[deadlineIdx];
        if (days < 0) return true;
        if (p.deadlineMillis <= 0L) return false;
        long delta = p.deadlineMillis - System.currentTimeMillis();
        return delta >= 0L && delta <= days * 86_400_000L;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    private void fetch() {
        showLoading();
        provider.fetch(new ProgramsProvider.Callback() {
            @Override
            public void onSuccess(List<Program> result) {
                runOnUiThread(() -> {
                    if (result == null || result.isEmpty()) {
                        showError("No programs found. Pull down to retry.");
                    } else {
                        showResults(result);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showError(message));
            }
        });
    }

    private void showLoading() {
        // Only show the centered spinner if we're not already showing data behind a swipe gesture.
        if (!swipeRefresh.isRefreshing() && programs.isEmpty()) {
            loadingView.setVisibility(View.VISIBLE);
        }
        errorView.setVisibility(View.GONE);
        rv.setVisibility(programs.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showResults(List<Program> result) {
        allPrograms.clear();
        allPrograms.addAll(result);
        loadingView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        swipeRefresh.setRefreshing(false);
        applyFilter();
    }

    private void showError(String message) {
        loadingView.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);
        if (programs.isEmpty()) {
            rv.setVisibility(View.GONE);
            errorView.setVisibility(View.VISIBLE);
            errorMessage.setText(message == null || message.isEmpty()
                    ? "Couldn't load programs." : message);
        } else {
            // Keep stale data on screen; user can pull-to-refresh again.
            errorView.setVisibility(View.GONE);
        }
    }
}
