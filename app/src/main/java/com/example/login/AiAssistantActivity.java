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

public class AiAssistantActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private EditText etMessage;
    private ChatAdapter adapter;
    private List<Message> messageList;

    // Groq Connection Essentials
    private final OkHttpClient client = new OkHttpClient();
    // Get your free key at https://console.groq.com/keys
    private final String GROQ_API_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_assistant);

        // 1. Setup UI Components
        recyclerView = findViewById(R.id.chat_recycler_view);
        etMessage = findViewById(R.id.et_message);
        ImageButton btnSend = findViewById(R.id.btn_send);
        ImageButton btnBack = findViewById(R.id.btn_back);

        // 2. Setup RecyclerView
        messageList = new ArrayList<>();
        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 3. Listeners
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendToAi(text);
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void sendToAi(String userText) {
        // 1. Add User Message and Placeholder
        addMessage(userText, Message.SENT_BY_USER);
        Message typingPlaceholder = new Message("Thinking...", Message.SENT_BY_BOT);
        messageList.add(typingPlaceholder);
        adapter.notifyItemInserted(messageList.size() - 1);

        // 2. Build JSON safely
        JSONObject jsonRequest = new JSONObject();
        try {
            jsonRequest.put("model", "llama3-8b-8192");
            JSONArray messages = new JSONArray();

            messages.put(new JSONObject().put("role", "system").put("content", "Professional Career Coach."));
            messages.put(new JSONObject().put("role", "user").put("content", userText));

            jsonRequest.put("messages", messages);
        } catch (Exception e) {
            Log.e("GROQ_DEBUG", "JSON Error", e);
        }

        RequestBody body = RequestBody.create(
                jsonRequest.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        // 3. The Request
        Request request = new Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + GROQ_API_KEY) // Ensure space after Bearer
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    removePlaceholder(typingPlaceholder);
                    addMessage("Connection Failed.", Message.SENT_BY_BOT);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String raw = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(raw);
                        String reply = json.getJSONArray("choices").getJSONObject(0)
                                .getJSONObject("message").getString("content");
                        runOnUiThread(() -> {
                            removePlaceholder(typingPlaceholder);
                            addMessage(reply, Message.SENT_BY_BOT);
                        });
                    } catch (Exception e) {
                        Log.e("GROQ_DEBUG", "Parse Error: " + raw);
                    }
                } else {
                    // If you get Error 400, this line will show the EXACT reason in Logcat
                    Log.e("GROQ_DEBUG", "Error " + response.code() + ": " + raw);
                    runOnUiThread(() -> {
                        removePlaceholder(typingPlaceholder);
                        addMessage("Error " + response.code(), Message.SENT_BY_BOT);
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