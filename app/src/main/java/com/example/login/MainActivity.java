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
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.CredentialManagerCallback;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
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

        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginBtn = findViewById(R.id.login_btn);
        registerLink = findViewById(R.id.register_link);
        googleBtn = findViewById(R.id.google_sign_in_button);
        forgotPassword = findViewById(R.id.forgot_password);
        View langBtn = findViewById(R.id.btn_language_menu);

        if (langBtn != null) langBtn.setOnClickListener(v -> showModernLanguageSheet());

        if (googleBtn != null) googleBtn.setOnClickListener(v -> startGoogleLogin());

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
        // Pre-flight: Google Sign-In requires Google Play Services. If it's missing or
        // outdated on the device, Credential Manager would fail with a confusing
        // "No credentials available" — show Google's own update/recovery dialog instead.
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int status = api.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (api.isUserResolvableError(status)) {
                api.getErrorDialog(this, status, /*requestCode*/ 9001).show();
            } else {
                Toast.makeText(this,
                        "Google Sign-In isn't supported on this device.",
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        CredentialManager credentialManager = CredentialManager.create(this);

        // GetSignInWithGoogleOption is the right option when the user explicitly taps a
        // "Sign in with Google" button: it triggers the full account picker (and lets the
        // user add a Google account if none is signed in on the device). GetGoogleIdOption
        // is for silent / bottom-sheet flows and returns "No credentials available" when
        // there's no previously-authorized account for this client ID.
        GetSignInWithGoogleOption googleOption =
                new GetSignInWithGoogleOption.Builder(getString(R.string.client_id)).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption).build();

        credentialManager.getCredentialAsync(this, request, null, Runnable::run,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        try {
                            GoogleIdTokenCredential googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(result.getCredential().getData());
                            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                        } catch (Exception e) {
                            Log.e("Auth", "Token parse failed", e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                    "Google Sign-In failed", Toast.LENGTH_SHORT).show());
                        }
                    }
                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e("Auth", "GetCredential failed", e);
                        // User dismissed the picker — silent.
                        if (e instanceof GetCredentialCancellationException) return;
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "Google Sign-In unavailable: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
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
                        return;
                    }
                    Exception ex = task.getException();
                    Log.e("Auth", "Firebase signInWithCredential failed", ex);
                    String msg = ex != null && ex.getMessage() != null
                            ? ex.getMessage()
                            : "Unknown error";
                    // The most common cause here is a missing/incorrect SHA-1 fingerprint
                    // in the Firebase project for the current signing key.
                    Toast.makeText(this, "Google Sign-In Failed: " + msg,
                            Toast.LENGTH_LONG).show();
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