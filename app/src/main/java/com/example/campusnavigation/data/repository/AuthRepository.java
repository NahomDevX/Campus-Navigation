package com.example.campusnavigation.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.campusnavigation.R;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * Local, offline account store. Credentials are kept in SharedPreferences with the
 * password stored only as a salted SHA-256 hash. No external auth provider is used.
 */
public class AuthRepository {
    private static final String PREFS_NAME = "campus_auth";
    private static final String KEY_USER_PREFIX = "user_";
    private static final String KEY_CURRENT_USER = "current_user";
    private static final String GUEST_USER = "guest";
    private static final String SALT = "campus_navigation_v1";

    private final Context context;
    private final SharedPreferences prefs;

    public AuthRepository(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** @return null on success, otherwise a user-facing error message. */
    public String register(String email, String password) {
        String key = userKey(email);
        if (prefs.contains(key)) {
            return context.getString(R.string.account_exists);
        }
        prefs.edit()
                .putString(key, hash(password))
                .putString(KEY_CURRENT_USER, normalize(email))
                .apply();
        return null;
    }

    /** @return null on success, otherwise a user-facing error message. */
    public String signIn(String email, String password) {
        String stored = prefs.getString(userKey(email), null);
        if (stored == null || !stored.equals(hash(password))) {
            return context.getString(R.string.invalid_credentials);
        }
        prefs.edit().putString(KEY_CURRENT_USER, normalize(email)).apply();
        return null;
    }

    public void signInAsGuest() {
        prefs.edit().putString(KEY_CURRENT_USER, GUEST_USER).apply();
    }

    public String getCurrentUser() {
        return prefs.getString(KEY_CURRENT_USER, null);
    }

    public boolean isGuest() {
        return GUEST_USER.equals(getCurrentUser());
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public void signOut() {
        prefs.edit().remove(KEY_CURRENT_USER).apply();
    }

    private String userKey(String email) {
        return KEY_USER_PREFIX + normalize(email);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String hash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((SALT + password).getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, bytes));
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}
