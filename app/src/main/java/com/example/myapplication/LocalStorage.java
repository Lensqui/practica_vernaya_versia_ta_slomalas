package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalStorage {
    private static LocalStorage instance;
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_PROFILE_ID = "profile_id";

    private final SharedPreferences prefs;

    private LocalStorage(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized LocalStorage getInstance() {
        return instance;
    }

    public static void initialize(Context context) {
        if (instance == null) {
            instance = new LocalStorage(context.getApplicationContext());
        }
    }

    public void saveCredentials(String email, String password) {
        prefs.edit().putString(KEY_EMAIL, email).putString(KEY_PASSWORD, password).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void saveProfileId(String profileId) {
        prefs.edit().putString(KEY_PROFILE_ID, profileId).apply();
    }

    public String getProfileId() {
        return prefs.getString(KEY_PROFILE_ID, null);
    }

    public void clearCredentials() {
        prefs.edit().remove(KEY_EMAIL).remove(KEY_PASSWORD).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
    public void setEmail(String email) {
        prefs.edit().putString("email", email).apply();
    }
}