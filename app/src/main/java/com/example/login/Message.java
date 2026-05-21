package com.example.login;

import org.json.JSONException;
import org.json.JSONObject;

public class Message {
    public static final int SENT_BY_USER = 0;
    public static final int SENT_BY_BOT = 1;

    private String text;
    private int sentBy;
    private long timestamp;

    public Message(String text, int sentBy) {
        this.text = text;
        this.sentBy = sentBy;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public int getSentBy() { return sentBy; }
    public long getTimestamp() { return timestamp; }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("text", text);
        obj.put("sentBy", sentBy);
        obj.put("timestamp", timestamp);
        return obj;
    }

    public static Message fromJson(JSONObject obj) throws JSONException {
        Message msg = new Message(obj.getString("text"), obj.getInt("sentBy"));
        msg.timestamp = obj.optLong("timestamp", 0);
        return msg;
    }
}
