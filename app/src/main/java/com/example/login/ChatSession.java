package com.example.login;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatSession {
    public String id;
    public String title;
    public long createdAt;
    public long updatedAt;
    public List<Message> messages;

    public ChatSession() {
        this.id = UUID.randomUUID().toString();
        this.title = "New conversation";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
        this.messages = new ArrayList<>();
    }

    /** Set title from the first user message text. */
    public void refreshTitle() {
        for (Message m : messages) {
            if (m.getSentBy() == Message.SENT_BY_USER) {
                String t = m.getText().trim();
                title = t.length() > 50 ? t.substring(0, 50) + "…" : t;
                return;
            }
        }
    }

    public JSONObject toJson() throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("createdAt", createdAt);
        obj.put("updatedAt", updatedAt);
        JSONArray arr = new JSONArray();
        for (Message m : messages) arr.put(m.toJson());
        obj.put("messages", arr);
        return obj;
    }

    public static ChatSession fromJson(JSONObject obj) throws Exception {
        ChatSession s = new ChatSession();
        s.id = obj.getString("id");
        s.title = obj.getString("title");
        s.createdAt = obj.getLong("createdAt");
        s.updatedAt = obj.getLong("updatedAt");
        s.messages = new ArrayList<>();
        JSONArray arr = obj.optJSONArray("messages");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                s.messages.add(Message.fromJson(arr.getJSONObject(i)));
            }
        }
        return s;
    }
}
