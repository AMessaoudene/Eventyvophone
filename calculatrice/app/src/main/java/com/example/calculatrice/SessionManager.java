package com.example.calculatrice;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility wrapper around SharedPreferences for persisting the logged in user.
 */
public final class SessionManager {

    private static final String PREF_NAME = "auth";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USERNAME = "username";

    private SessionManager() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static void saveUser(Context context, User user) {
        if (user == null) return;
        prefs(context).edit()
                .putLong(KEY_USER_ID, user.id)
                .putString(KEY_USERNAME, user.username)
                .apply();
    }

    public static long getUserId(Context context) {
        return prefs(context).getLong(KEY_USER_ID, -1L);
    }

    public static String getUsername(Context context) {
        return prefs(context).getString(KEY_USERNAME, null);
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }
}

