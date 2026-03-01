package com.example.login;
public class Message {
    public static final int SENT_BY_USER = 0;
    public static final int SENT_BY_BOT = 1;

    private String text;
    private int sentBy;

    public Message(String text, int sentBy) {
        this.text = text;
        this.sentBy = sentBy;
    }

    public String getText() { return text; }
    public int getSentBy() { return sentBy; }
}