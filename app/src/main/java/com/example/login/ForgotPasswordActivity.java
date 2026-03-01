package com.example.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button resetBtn;
    private TextView backBtn;
    private ProgressBar progressBar; // Declare it here for class-wide access
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // 1. Initialize Firebase and UI elements
        mAuth = FirebaseAuth.getInstance();
        emailInput = findViewById(R.id.reset_email_input);
        resetBtn = findViewById(R.id.reset_password_btn);
        backBtn = findViewById(R.id.back_to_login);
        progressBar = findViewById(R.id.reset_progress);

        // 2. Set Click Listener for the Reset Button
        resetBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            // Validation
            if (TextUtils.isEmpty(email)) {
                emailInput.setError("Email is required");
                return;
            }

            // Show progress bar and hide button to prevent multiple requests
            progressBar.setVisibility(View.VISIBLE);
            resetBtn.setVisibility(View.GONE);

            // 3. Send the Reset Email
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        // Always hide progress bar when the task finishes
                        progressBar.setVisibility(View.GONE);
                        resetBtn.setVisibility(View.VISIBLE);

                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPasswordActivity.this,
                                    "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                            finish(); // Close this activity and go back
                        } else {
                            Exception e = task.getException();
                            if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                                Toast.makeText(this, "This email is not registered!", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Network Error: Check your connection", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        // 4. Back button logic
        backBtn.setOnClickListener(v -> finish());
    }
}