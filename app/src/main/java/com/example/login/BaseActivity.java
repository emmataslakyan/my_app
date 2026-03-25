package com.example.login;

import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.os.LocaleListCompat;

public class BaseActivity extends AppCompatActivity {

    protected void setupCommonToolbar() {
        View backBtn = findViewById(R.id.backBtn);
        View langBtn = findViewById(R.id.btn_language_menu);

        if (backBtn != null) {
            backBtn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        }

        if (langBtn != null) {
            langBtn.setOnClickListener(v -> {
                // local variable to fix "field can be converted to local" warning
                PopupMenu languagePopup = new PopupMenu(this, v);
                languagePopup.getMenu().add(0, 1, 0, "English 🇬🇧");
                languagePopup.getMenu().add(0, 2, 1, "Русский 🇷🇺");

                languagePopup.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == 1) {
                        setAppLocale("en");
                        return true;
                    } else if (item.getItemId() == 2) {
                        setAppLocale("ru");
                        return true;
                    }
                    return false;
                });
                languagePopup.show();
            });
        }
    }

    private void setAppLocale(String languageCode) {
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(languageCode);
        AppCompatDelegate.setApplicationLocales(appLocale);
    }
}