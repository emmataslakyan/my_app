package com.example.login;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Collections;
import okhttp3.*;

public class OpenRouterClient {
    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();
    private final String apiKey;

    public OpenRouterClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public void getCompletion(String model, String prompt, Callback callback) {
        String url = "https://openrouter.ai/api/v1/chat/completions";

        // Create request object
        OpenRouterModels.Message userMessage = new OpenRouterModels.Message("user", prompt);
        OpenRouterModels.ChatRequest requestBodyObj = new OpenRouterModels.ChatRequest(
                model,
                Collections.singletonList(userMessage)
        );

        String json = gson.toJson(requestBodyObj);
        RequestBody body = RequestBody.create(
                json,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("HTTP-Referer", "https://your-app-site.com") // Optional
                .post(body)
                .build();

        // OkHttp handles the background thread automatically when using .enqueue()
        client.newCall(request).enqueue(callback);
    }
}