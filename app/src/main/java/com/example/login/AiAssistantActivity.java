package com.example.login;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI Assistant using OpenRouter API (OpenAI Compatible)
 */
public class AiAssistantActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ChatAdapter adapter;
    private List<Message> messageList;

    private final OkHttpClient client = new OkHttpClient();

    // --- OPENROUTER CONFIG ---
    private final String OPENROUTER_API_KEY = "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";
    private final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    // Use a :free model for testing, or "meta-llama/llama-3.3-70b-instruct" for high quality

    // Option A: Fast and extremely cheap (Current best for testing)
    private final String MODEL_ID = "google/gemini-2.0-flash-001";

// Option B: Completely Free (if available)
// private final String MODEL_ID = "google/gemini-2.0-pro-exp-02-05:free";

    // Option C: High-quality Llama model
// private final String MODEL_ID = "meta-llama/llama-3.3-70b-instruct";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        recyclerView = findViewById(R.id.chat_recycler_view);
        etMessage = findViewById(R.id.et_message);
        ImageButton btnSend = findViewById(R.id.btn_send);
        ImageButton btnBack = findViewById(R.id.btn_back);

        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendToAi(text);
                etMessage.setText(""); // Clear input
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void sendToAi(String userText) {
        addMessage(userText, Message.SENT_BY_USER);

        // Add "Thinking..." bubble
        Message typingPlaceholder = new Message("Thinking...", Message.SENT_BY_BOT);
        messageList.add(typingPlaceholder);
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);

        // Build JSON Request
        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("model", MODEL_ID);

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", "You are the SkillSpark Career Mentor, an expert AI counselor dedicated to helping students and early-career professionals transform their raw experiences into high-impact resumes. Your goal is to be a supportive, detail-oriented, and strategic collaborator.\n" +
                    "\n" +
                    "Core Objectives:\n" +
                    "\n" +
                    "AI-Guided Writing: Help users phrase their achievements using the STAR method (Situation, Task, Action, Result). If a user provides a weak description like \"I helped with a project,\" prompt them for specific metrics or tools used.\n" +
                    "\n" +
                    "Spelling & Grammar: Silently correct minor errors, but provide \"Smart Feedback\" for significant stylistic improvements to ensure the tone remains professional.\n" +
                    "\n" +
                    "Strategic Gap Filling: Identify \"empty\" sections of a resume and suggest specific types of National or International programs, competitions, or volunteering opportunities that would strengthen that specific user’s profile.\n" +
                    "\n" +
                    "Opportunity Matching: Based on the skills the user enters (e.g., \"Python\" or \"Graphic Design\"), suggest relevant areas for growth, such as Hackathons, NGO volunteering, or specialized certifications.\n" +
                    "\n" +
                    "Tone and Style:\n" +
                    "\n" +
                    "Empathetic but Candid: Validate the user's current skills while being direct about what needs improvement.\n" +
                    "\n" +
                    "Concise & Scannable: Use bullet points and bold text to make your advice easy to digest.\n" +
                    "\n" +
                    "Interactive: Always end your response with a clear next step or a thought-provoking question (e.g., \"What was the most challenging part of this project?\").\n" +
                    "\n" +
                    "Adaptive: Mirror the user's level of experience. If they are a high schooler, use accessible language; if they are a senior tech student, use industry-standard terminology.\n" +
                    "\n" +
                    "Response Constraints:\n" +
                    "\n" +
                    "Do not just rewrite the text; explain why the change makes the resume better.\n" +
                    "\n" +
                    "Focus on \"Action Verbs\" (e.g., Developed, Orchestrated, Optimized).\n" +
                    "\n" +
                    "Avoid corporate jargon that obscures actual meaning."));
            messages.put(new JSONObject().put("role", "user").put("content", userText));

            jsonRequest.put("messages", messages);
        } catch (Exception e) {
            Log.e("AI_DEBUG", "JSON Build Error", e);
        }

        RequestBody body = RequestBody.create(
                jsonRequest.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        // Build HTTP Request
        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + OPENROUTER_API_KEY)
                .header("HTTP-Referer", "https://com.example.login") // Required for some models
                .header("X-Title", "Career Coach Android App")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    removePlaceholder(typingPlaceholder);
                    addMessage("Connection failed: " + e.getMessage(), Message.SENT_BY_BOT);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String rawResponse = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(rawResponse);
                        String aiReply = json.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content");

                        runOnUiThread(() -> {
                            removePlaceholder(typingPlaceholder);
                            addMessage(aiReply, Message.SENT_BY_BOT);
                        });
                    } catch (Exception e) {
                        Log.e("AI_DEBUG", "Parse Error: " + rawResponse);
                        runOnUiThread(() -> {
                            removePlaceholder(typingPlaceholder);
                            addMessage("Error parsing AI response.", Message.SENT_BY_BOT);
                        });
                    }
                } else {
                    Log.e("AI_DEBUG", "Server Error " + response.code() + ": " + rawResponse);
                    runOnUiThread(() -> {
                        removePlaceholder(typingPlaceholder);
                        addMessage("Server Error: " + response.code(), Message.SENT_BY_BOT);
                    });
                }
            }
        });
    }

    private void removePlaceholder(Message placeholder) {
        int index = messageList.indexOf(placeholder);
        if (index != -1) {
            messageList.remove(index);
            adapter.notifyItemRemoved(index);
        }
    }

    private void addMessage(String text, int sentBy) {
        messageList.add(new Message(text, sentBy));
        adapter.notifyItemInserted(messageList.size() - 1);
        recyclerView.scrollToPosition(messageList.size() - 1);
    }
}