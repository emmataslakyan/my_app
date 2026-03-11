package com.example.login;

import android.os.Bundle;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VolunteeringActivity extends BaseActivity {

    private TextInputEditText editOrg, editPos, editLoc, editDate;
    private RecyclerView rvBullets;
    private BulletAdapter adapter;
    private final List<String> bulletList = new ArrayList<>();
    private AppDatabase db;
    private int resumeId;
    private Resume resume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteering);

        db = AppDatabase.getInstance(this);
        resumeId = getIntent().getIntExtra("RESUME_ID", -1);

        // Bind all 4 main inputs
        editOrg = findViewById(R.id.editVolOrg);
        editPos = findViewById(R.id.editVolPos);
        editLoc = findViewById(R.id.editVolLocation);
        editDate = findViewById(R.id.editVolDate);
        rvBullets = findViewById(R.id.rvVolBullets);

        rvBullets.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpgradeHint).setOnClickListener(v -> showVolGuide());
        findViewById(R.id.btnAddVolBullet).setOnClickListener(v -> {
            if (adapter != null) adapter.addBullet();
        });
        findViewById(R.id.btnSaveVol).setOnClickListener(v -> saveVol());

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            resume = db.resumeDao().getResumeById(resumeId);
            if (resume != null) {
                String saved = resume.getVolBullets();
                if (saved != null && !saved.isEmpty()) {
                    bulletList.addAll(Arrays.asList(saved.split("\\|")));
                } else {
                    bulletList.add("");
                }
                runOnUiThread(() -> {
                    editOrg.setText(resume.getVolOrgName());
                    editPos.setText(resume.getVolPosition());
                    editLoc.setText(resume.getVolLocation());
                    editDate.setText(resume.getVolDate());

                    adapter = new BulletAdapter(bulletList);
                    rvBullets.setAdapter(adapter);
                });
            }
        }).start();
    }

    private void saveVol() {
        if (resume == null) return;

        resume.setVolOrgName(editOrg.getText().toString());
        resume.setVolPosition(editPos.getText().toString());
        resume.setVolLocation(editLoc.getText().toString());
        resume.setVolDate(editDate.getText().toString());

        StringBuilder sb = new StringBuilder();
        if (adapter != null) {
            for (String s : adapter.getBullets()) {
                if (!s.trim().isEmpty()) sb.append(s).append("|");
            }
        }
        resume.setVolBullets(sb.toString());

        new Thread(() -> {
            db.resumeDao().update(resume);
            runOnUiThread(() -> {
                Toast.makeText(this, "Volunteering Saved!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private void showVolGuide() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        android.view.View sheetView = getLayoutInflater().inflate(R.layout.hint_bottom_sheet, null);
        android.widget.TextView hintText = sheetView.findViewById(R.id.hintContent);
        hintText.setText("🤝 VOLUNTEERING TIPS:\n\n• Impact: Mention how many people you helped.\n• Consistency: If you've volunteered long-term, highlight that commitment.\n• Transferable Skills: Did you use communication, teaching, or tech skills?");
        bottomSheet.setContentView(sheetView);
        bottomSheet.show();
    }
}