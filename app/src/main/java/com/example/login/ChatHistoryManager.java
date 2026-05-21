package com.example.login;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryManager {
    private static final String PREFS_NAME = "spark_sessions";
    private static final String KEY_IDS = "session_ids";

    private final SharedPreferences prefs;
    private static ChatHistoryManager instance;

    private ChatHistoryManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static ChatHistoryManager get(Context ctx) {
        if (instance == null) instance = new ChatHistoryManager(ctx);
        return instance;
    }

    /** Session IDs ordered most-recent-first. */
    public List<String> getSessionIds() {
        List<String> ids = new ArrayList<>();
        try {
            String json = prefs.getString(KEY_IDS, null);
            if (json == null) return ids;
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) ids.add(arr.getString(i));
        } catch (Exception ignored) {}
        return ids;
    }

    /** Load lightweight metadata (no messages) for displaying the list. */
    public List<ChatSession> loadAllMeta() {
        List<String> ids = getSessionIds();
        List<ChatSession> result = new ArrayList<>();
        for (String id : ids) {
            try {
                String json = prefs.getString("s_" + id, null);
                if (json == null) continue;
                JSONObject obj = new JSONObject(json);
                ChatSession s = new ChatSession();
                s.id = obj.getString("id");
                s.title = obj.getString("title");
                s.createdAt = obj.getLong("createdAt");
                s.updatedAt = obj.getLong("updatedAt");
                // skip messages — only need metadata here
                result.add(s);
            } catch (Exception ignored) {}
        }
        return result;
    }

    public ChatSession loadSession(String id) {
        try {
            String json = prefs.getString("s_" + id, null);
            if (json == null) return null;
            return ChatSession.fromJson(new JSONObject(json));
        } catch (Exception e) {
            return null;
        }
    }

    public void saveSession(ChatSession session) {
        try {
            session.updatedAt = System.currentTimeMillis();
            prefs.edit().putString("s_" + session.id, session.toJson().toString()).apply();
            promoteToFront(session.id);
        } catch (Exception ignored) {}
    }

    public void deleteSession(String id) {
        prefs.edit().remove("s_" + id).apply();
        List<String> ids = getSessionIds();
        ids.remove(id);
        persistIds(ids);
    }

    /** Most recent session id, or null if no history exists. */
    public String latestSessionId() {
        List<String> ids = getSessionIds();
        return ids.isEmpty() ? null : ids.get(0);
    }

    private void promoteToFront(String id) {
        List<String> ids = getSessionIds();
        ids.remove(id);
        ids.add(0, id);
        persistIds(ids);
    }

    private void persistIds(List<String> ids) {
        try {
            JSONArray arr = new JSONArray();
            for (String s : ids) arr.put(s);
            prefs.edit().putString(KEY_IDS, arr.toString()).apply();
        } catch (Exception ignored) {}
    }
}
