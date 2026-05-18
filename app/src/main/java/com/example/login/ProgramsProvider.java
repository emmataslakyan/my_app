package com.example.login;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches the public list of approved programs that powers https://greenwich.am/programs.
 *
 * The Greenwich web app is a React SPA that itself calls this REST endpoint at runtime —
 * so we skip the page entirely and hit the same API directly. Returned data is cached
 * statically for the lifetime of the process; call {@link #clearCache()} to force a refresh.
 */
public class ProgramsProvider {

    private static final String API_URL =
            "https://backend-production-40c02.up.railway.app/api/programs/approved";

    private static final String TAG = "ProgramsProvider";

    private static final OkHttpClient client = new OkHttpClient();

    /** In-memory cache shared across activity instances within the same process. */
    private static List<Program> cache = null;

    public interface Callback {
        void onSuccess(List<Program> programs);
        void onError(String message);
    }

    public ProgramsProvider(Context context) {
        // Context unused now that we no longer instantiate a WebView, but kept
        // in the constructor to avoid touching callers.
    }

    public static List<Program> getCache() {
        return cache;
    }

    public static void clearCache() {
        cache = null;
    }

    public void fetch(Callback callback) {
        Request req = new Request.Builder()
                .url(API_URL)
                .header("Accept", "application/json")
                .build();

        client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "Network failure", e);
                callback.onError("Couldn't reach Greenwich.");
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        callback.onError("Server returned " + response.code());
                        return;
                    }
                    List<Program> result = parseResponse(body.string());
                    cache = result;
                    callback.onSuccess(result);
                } catch (JSONException e) {
                    Log.w(TAG, "Bad JSON", e);
                    callback.onError("Couldn't read programs.");
                } catch (Exception e) {
                    Log.w(TAG, "Unexpected error", e);
                    callback.onError("Couldn't load programs.");
                }
            }
        });
    }

    static List<Program> parseResponse(String json) throws JSONException {
        List<Program> out = new ArrayList<>();
        if (json == null || json.isEmpty()) return out;

        JSONObject root = new JSONObject(json);
        JSONArray arr = root.optJSONArray("programs");
        if (arr == null) return out;

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            String title = o.optString("title", "").trim();
            if (title.isEmpty()) continue;

            String imageUrl = o.optString("image_url", "").trim();
            String programUrl = o.optString("url", "").trim();
            String meta = buildMeta(o);
            String categoriesText = joinCategories(o);
            String description = o.optString("description", "");
            long deadlineMillis = Deadlines.extract(description);

            out.add(new Program(title, imageUrl, programUrl, meta, categoriesText,
                    description, deadlineMillis));
        }
        return out;
    }

    /** All category labels joined by spaces. Used by ProgramFilter for matching. */
    private static String joinCategories(JSONObject o) {
        JSONArray cats = o.optJSONArray("categories");
        if (cats == null || cats.length() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cats.length(); i++) {
            String c = cats.optString(i, "").trim();
            if (c.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }

    /** First few categories joined with " · ", falling back to source, falling back to start_date. */
    private static String buildMeta(JSONObject o) {
        JSONArray cats = o.optJSONArray("categories");
        if (cats != null && cats.length() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cats.length() && i < 3; i++) {
                String c = cats.optString(i, "").trim();
                if (c.isEmpty()) continue;
                if (sb.length() > 0) sb.append(" · ");
                sb.append(c);
            }
            if (sb.length() > 0) return sb.toString();
        }
        String source = o.optString("source", "").trim();
        if (!source.isEmpty()) return source;
        return o.optString("start_date", "").trim();
    }
}
