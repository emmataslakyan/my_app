package com.example.login; // <--- ADD THIS

import java.util.List;

public class OpenRouterModels {
    public static class ChatRequest {
        String model;
        List<Message> messages;

        public ChatRequest(String model, List<Message> messages) {
            this.model = model;
            this.messages = messages;
        }
    }

    public static class Message {
        String role;
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ChatResponse {
        public List<Choice> choices;
    }

    public static class Choice {
        public Message message;
    }
}