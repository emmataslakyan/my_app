package com.example.login;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryProvider {
    private static final String TAG = "SCRAPER_DEBUG";
    private final Context context;
    private final List<Opportunity> allResults = new ArrayList<>();

    public interface DiscoveryCallback {
        void onSuccess(List<Opportunity> opportunities);
        void onError(String error);
    }

    public DiscoveryProvider(Context context) {
        this.context = context;
    }

    public void fetchAllOpportunities(DiscoveryCallback callback) {
        // Step 1: Load Greenwich
        loadAndScrape("https://greenwich.am/programs", html1 -> {
            parseHtml(html1, "Greenwich AM");

            // Step 2: Small delay then load Borderless (bypasses 429 errors)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                loadAndScrape("https://borderless.so/extracurricular-programs", html2 -> {
                    parseHtml(html2, "Borderless");
                    callback.onSuccess(allResults);
                }, 4000); // Borderless is heavier, needs more time
            }, 1500);
        }, 3000);
    }

    private void loadAndScrape(String url, ValueCallback<String> onHtmlReady, int delay) {
        new Handler(Looper.getMainLooper()).post(() -> {
            WebView webView = new WebView(context);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 13)");

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        view.evaluateJavascript(
                                "(function() { return ('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>'); })();",
                                onHtmlReady
                        );
                    }, delay);
                }
            });
            webView.loadUrl(url);
        });
    }

    private void parseHtml(String jsonHtml, String source) {
        // Clean the string returned by evaluateJavascript
        String html = jsonHtml.replace("\\u003C", "<")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("^\"|\"$", "");

        Document doc = Jsoup.parse(html);
        Elements links = doc.select("a[href*='/programs/']");

        for (Element link : links) {
            String title = link.text().trim();
            String url = link.attr("abs:href");
            if (title.length() > 4) {
                allResults.add(new Opportunity(title, url, source));
            }
        }
        Log.d(TAG, source + " items found: " + links.size());
    }
}