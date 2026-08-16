package com.patechltd.salexfypos.security;

import android.content.Context;
import android.content.SharedPreferences;

import com.patechltd.salexfypos.db.entity.User;

public class Session {

    private static final String PREFS = "salexfy_session";
    private static volatile String currentUserId;

    private Session() {
    }

    public static void start(Context context, User user) {
        currentUserId = user.uid;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString("userId", user.uid).apply();
    }

    public static String userId(Context context) {
        if (currentUserId != null) return currentUserId;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        currentUserId = prefs.getString("userId", null);
        return currentUserId;
    }

    public static void end(Context context) {
        currentUserId = null;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }

    public static boolean isLoggedIn(Context context) {
        return userId(context) != null;
    }
}
