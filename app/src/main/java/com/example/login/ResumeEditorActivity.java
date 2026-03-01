package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

public class ResumeEditorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume_editor);

        setupCommonToolbar();

        // Find the "View Resume" bar at the bottom
        LinearLayout btnViewResume = findViewById(R.id.bottomBar);

        btnViewResume.setOnClickListener(v -> {
            // We just navigate to the template selection.
            // We don't save anything yet to avoid duplicates.
            Intent intent = new Intent(ResumeEditorActivity.this, TemplateSelectionActivity.class);
            startActivity(intent);
        });
    }
}