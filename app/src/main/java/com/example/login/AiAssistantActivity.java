package com.example.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * AI Assistant using OpenRouter API (OpenAI Compatible)
 * API key is stored in local.properties and injected via BuildConfig — safe for GitHub.
 *
 * Session flow:
 *  - No extras → resume most recent session (or create new if none)
 *  - EXTRA_SESSION_ID → load that specific session
 *  - EXTRA_NEW_CHAT=true → always create a fresh session
 */
public class AiAssistantActivity extends BaseActivity {

    private static final int MAX_CONTEXT_MESSAGES = 20;

    private RecyclerView recyclerView;
    private EditText etMessage;
    private TextView tvTitle;
    private ChatAdapter adapter;

    private ChatSession session;
    private ChatHistoryManager mgr;

    private final OkHttpClient client = new OkHttpClient();
    private final String OPENROUTER_API_KEY = BuildConfig.OPENROUTER_API_KEY;
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL_ID = "google/gemini-2.0-flash-001";

    private static final String SYSTEM_PROMPT =
            "You are the SkillSpark Career Mentor, an expert AI counselor dedicated to helping students and early-career professionals transform their raw experiences into high-impact resumes. Your goal is to be a supportive, detail-oriented, and strategic collaborator.\n\n" +
            "Core Objectives:\n\n" +
            "AI-Guided Writing: Help users phrase their achievements using the STAR method (Situation, Task, Action, Result). If a user provides a weak description like \"I helped with a project,\" prompt them for specific metrics or tools used.\n\n" +
            "Spelling & Grammar: Silently correct minor errors, but provide \"Smart Feedback\" for significant stylistic improvements to ensure the tone remains professional.\n\n" +
            "Strategic Gap Filling: Identify \"empty\" sections of a resume and suggest specific types of National or International programs, competitions, or volunteering opportunities that would strengthen that specific user's profile.\n\n" +
            "Opportunity Matching: Based on the skills the user enters (e.g., \"Python\" or \"Graphic Design\"), suggest relevant areas for growth, such as Hackathons, NGO volunteering, or specialized certifications.\n\n" +
            "Tone and Style:\n\n" +
            "Empathetic but Candid: Validate the user's current skills while being direct about what needs improvement.\n\n" +
            "Concise & Scannable: Use plain-text bullet points (with a dash or number) to make your advice easy to digest. Never use markdown — no asterisks, no pound signs, no underscores for formatting.\n\n" +
            "Interactive: Always end your response with a clear next step or a thought-provoking question (e.g., \"What was the most challenging part of this project?\").\n\n" +
            "Adaptive: Mirror the user's level of experience. If they are a high schooler, use accessible language; if they are a senior tech student, use industry-standard terminology.\n\n" +
            "Response Constraints:\n\n" +
            "Do not just rewrite the text; explain why the change makes the resume better.\n\n" +
            "Focus on \"Action Verbs\" (e.g., Developed, Orchestrated, Optimized).\n\n" +
            "Avoid corporate jargon that obscures actual meaning.\n\n" +
            "Formatting rule: always respond in plain text. Never use *, **, #, ##, _, __ or any other markdown syntax.";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        mgr = ChatHistoryManager.get(this);

        // Push the whole layout up by exactly the keyboard height whenever the IME opens.
        // adjustResize doesn't work reliably on Android 11+ with edge-to-edge themes.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root_layout), (v, windowInsets) -> {
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            Insets nav = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            v.setPadding(0, 0, 0, Math.max(ime.bottom, nav.bottom));
            return windowInsets;
        });

        recyclerView = findViewById(R.id.chat_recycler_view);
        etMessage = findViewById(R.id.et_message);
        tvTitle = findViewById(R.id.ai_title);
        ImageButton btnSend = findViewById(R.id.btn_send);
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnHistory = findViewById(R.id.btn_history);
        ImageButton btnNewChat = findViewById(R.id.btn_new_chat);

        // Resolve which session to load
        session = resolveSession();

        adapter = new ChatAdapter(session.messages);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        updateTitle();

        if (!session.messages.isEmpty()) {
            recyclerView.scrollToPosition(session.messages.size() - 1);
        }

        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendToAi(text);
                etMessage.setText("");
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, ChatHistoryActivity.class));
            finish();
        });

        btnNewChat.setOnClickListener(v -> {
            Intent intent = new Intent(this, AiAssistantActivity.class);
            intent.putExtra(ChatHistoryActivity.EXTRA_NEW_CHAT, true);
            startActivity(intent);
            finish();
        });
    }

    private ChatSession resolveSession() {
        boolean newChat = getIntent().getBooleanExtra(ChatHistoryActivity.EXTRA_NEW_CHAT, false);
        if (newChat) return new ChatSession();

        String sessionId = getIntent().getStringExtra(ChatHistoryActivity.EXTRA_SESSION_ID);
        if (sessionId != null) {
            ChatSession loaded = mgr.loadSession(sessionId);
            if (loaded != null) return loaded;
        }

        // No specific session → resume the most recent one
        String latestId = mgr.latestSessionId();
        if (latestId != null) {
            ChatSession loaded = mgr.loadSession(latestId);
            if (loaded != null) return loaded;
        }

        return new ChatSession();
    }

    private void updateTitle() {
        tvTitle.setText(session.title);
    }

    private void sendToAi(String userText) {
        addMessage(userText, Message.SENT_BY_USER);

        // Set a placeholder title from the first user message while the AI title loads
        if (session.title.equals("New conversation")) {
            session.refreshTitle();
            updateTitle();
        }
        mgr.saveSession(session);

        Message typing = new Message("Thinking...", Message.SENT_BY_BOT);
        session.messages.add(typing);
        adapter.notifyItemInserted(session.messages.size() - 1);
        recyclerView.scrollToPosition(session.messages.size() - 1);

        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL_ID);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role", "system").put("content", SYSTEM_PROMPT));

            int start = Math.max(0, session.messages.size() - MAX_CONTEXT_MESSAGES);
            for (int i = start; i < session.messages.size(); i++) {
                Message m = session.messages.get(i);
                if (m == typing) continue;
                String role = m.getSentBy() == Message.SENT_BY_USER ? "user" : "assistant";
                messages.put(new JSONObject().put("role", role).put("content", m.getText()));
            }
            body.put("messages", messages);
        } catch (Exception e) {
            Log.e("AI_DEBUG", "JSON Build Error", e);
        }

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + OPENROUTER_API_KEY)
                .header("HTTP-Referer", "https://com.example.login")
                .header("X-Title", "Career Coach Android App")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    removePlaceholder(typing);
                    addMessage("Connection failed: " + e.getMessage(), Message.SENT_BY_BOT);
                    mgr.saveSession(session);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                ResponseBody rb = response.body();
                if (rb == null) {
                    runOnUiThread(() -> {
                        removePlaceholder(typing);
                        addMessage("Received an empty response from the server.", Message.SENT_BY_BOT);
                        mgr.saveSession(session);
                    });
                    return;
                }

                String raw = rb.string();
                if (response.isSuccessful()) {
                    try {
                        String reply = stripMarkdown(new JSONObject(raw)
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content"));
                        runOnUiThread(() -> {
                            removePlaceholder(typing);
                            addMessage(reply, Message.SENT_BY_BOT);
                            mgr.saveSession(session);
                            if (countBotMessages() == 1) generateTitleAsync();
                        });
                    } catch (Exception e) {
                        Log.e("AI_DEBUG", "Parse Error: " + raw);
                        runOnUiThread(() -> {
                            removePlaceholder(typing);
                            addMessage("Error parsing AI response.", Message.SENT_BY_BOT);
                            mgr.saveSession(session);
                        });
                    }
                } else {
                    Log.e("AI_DEBUG", "Server Error " + response.code() + ": " + raw);
                    runOnUiThread(() -> {
                        removePlaceholder(typing);
                        addMessage("Server Error: " + response.code(), Message.SENT_BY_BOT);
                        mgr.saveSession(session);
                    });
                }
            }
        });
    }

    private int countBotMessages() {
        int count = 0;
        for (Message m : session.messages) {
            if (m.getSentBy() == Message.SENT_BY_BOT) count++;
        }
        return count;
    }

    private void generateTitleAsync() {
        String userMsg = "";
        String botMsg = "";
        for (Message m : session.messages) {
            if (m.getSentBy() == Message.SENT_BY_USER && userMsg.isEmpty()) userMsg = m.getText();
            else if (m.getSentBy() == Message.SENT_BY_BOT && botMsg.isEmpty()) botMsg = m.getText();
            if (!userMsg.isEmpty() && !botMsg.isEmpty()) break;
        }
        if (userMsg.isEmpty()) return;

        String snippet = botMsg.length() > 300 ? botMsg.substring(0, 300) : botMsg;
        String prompt = "Give this conversation a short title: 3-5 words, plain text only, no punctuation, no quotes.\n\nUser: " + userMsg + "\nAssistant: " + snippet + "\n\nTitle:";

        JSONObject body = new JSONObject();
        try {
            body.put("model", MODEL_ID);
            JSONArray msgs = new JSONArray();
            msgs.put(new JSONObject().put("role", "user").put("content", prompt));
            body.put("messages", msgs);
        } catch (Exception e) { return; }

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + OPENROUTER_API_KEY)
                .header("HTTP-Referer", "https://com.example.login")
                .header("X-Title", "Career Coach Android App")
                .post(RequestBody.create(body.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) { /* keep fallback title */ }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) return;
                ResponseBody rb = response.body();
                if (rb == null) return;
                try {
                    String title = new JSONObject(rb.string())
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                            .replaceAll("[*#_\"`]", "")
                            .trim();
                    if (!title.isEmpty()) {
                        session.title = title;
                        mgr.saveSession(session);
                        runOnUiThread(() -> tvTitle.setText(title));
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private String stripMarkdown(String text) {
        return text
                .replaceAll("\\*{1,3}([^*]+)\\*{1,3}", "$1")  // *italic*, **bold**, ***both***
                .replaceAll("_{1,2}([^_]+)_{1,2}", "$1")       // _italic_, __bold__
                .replaceAll("#{1,6}\\s*", "")                  // ## headings
                .replaceAll("`([^`]*)`", "$1")                 // `inline code`
                .trim();
    }

    private void removePlaceholder(Message placeholder) {
        int i = session.messages.indexOf(placeholder);
        if (i != -1) {
            session.messages.remove(i);
            adapter.notifyItemRemoved(i);
        }
    }

    private void addMessage(String text, int sentBy) {
        session.messages.add(new Message(text, sentBy));
        adapter.notifyItemInserted(session.messages.size() - 1);
        recyclerView.scrollToPosition(session.messages.size() - 1);
    }
}
