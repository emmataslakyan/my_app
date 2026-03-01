package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.CredentialManagerCallback;

import com.google.android.gms.common.SignInButton;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button loginBtn;
    private SignInButton googleBtn;
    private TextView registerLink, forgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // 1. Initialize Views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginBtn = findViewById(R.id.login_btn);
        registerLink = findViewById(R.id.register_link);
        googleBtn = findViewById(R.id.google_sign_in_button);
        forgotPassword = findViewById(R.id.forgot_password);
        View langBtn = findViewById(R.id.btn_language_menu);

        // 2. Button Listeners
        if (langBtn != null) langBtn.setOnClickListener(v -> showModernLanguageSheet());

        if (googleBtn != null) googleBtn.setOnClickListener(v -> startGoogleLogin());

        // FIXED: Forgot Password Listener
        if (forgotPassword != null) {
            forgotPassword.setOnClickListener(v -> {
                startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class));
            });
        }

        if (loginBtn != null) {
            loginBtn.setOnClickListener(v -> {
                String email = emailInput.getText().toString().trim();
                String pass = passwordInput.getText().toString().trim();
                if (!email.isEmpty() && !pass.isEmpty()) {
                    performEmailLogin(email, pass);
                }
            });
        }

        if (registerLink != null) {
            registerLink.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        }
    }

    // FIXED: Automatic Session Login
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d("Auth", "User session found, skipping login.");
            navigateToHome();
        }
    }

    private void performEmailLogin(String email, String pass) {
        mAuth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        navigateToHome();
                    } else {
                        Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void startGoogleLogin() {
        CredentialManager credentialManager = CredentialManager.create(this);
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption).build();

        credentialManager.getCredentialAsync(this, request, null, Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                        } catch (Exception e) {
                            Log.e("Auth", "Error: " + e.getMessage());
                        }
                    }
                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e("Auth", "Failed: " + e.getMessage());
                    }
                });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveUserToFirestoreBackground(mAuth.getCurrentUser());
                        navigateToHome();
                    } else {
                        Toast.makeText(this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestoreBackground(FirebaseUser user) {
        if (user == null) return;
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", user.getDisplayName());
        userMap.put("email", user.getEmail());
        userMap.put("uid", user.getUid());
        userMap.put("lastLogin", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("users")
                .document(user.getUid())
                .set(userMap, SetOptions.merge())
                .addOnFailureListener(e -> Log.e("Firestore", "Background update failed", e));
    }

    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showModernLanguageSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.layout_language_sheet, null);
        bottomSheetDialog.setContentView(sheetView);
        sheetView.findViewById(R.id.btn_select_en).setOnClickListener(v -> setAppLocale("en"));
        sheetView.findViewById(R.id.btn_select_ru).setOnClickListener(v -> setAppLocale("ru"));
        bottomSheetDialog.show();
    }

    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }
}