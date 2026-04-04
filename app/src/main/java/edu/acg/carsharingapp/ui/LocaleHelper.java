package edu.acg.carsharingapp.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import java.util.Locale;

public class LocaleHelper {

    private static final String PREF_NAME = "settings";
    private static final String KEY_LANGUAGE = "app_language";
    private static final String DEFAULT_LANGUAGE = "en";

    // 🔹 Apply locale
    public static Context setLocale(Context context, String language) {
        persistLanguage(context, language);
        return updateResources(context, language);
    }

    // 🔹 Load saved language
    public static Context loadLocale(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        String language = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE);
        return updateResources(context, language);
    }

    // 🔹 Save language
    private static void persistLanguage(Context context, String language) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        prefs.edit().putString(KEY_LANGUAGE, language).apply();
    }

    // 🔹 Update configuration
    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}