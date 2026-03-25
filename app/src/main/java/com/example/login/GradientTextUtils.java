package com.example.login;

import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class GradientTextUtils {

    private static final String TAG = "GradientTextUtils";

    public static void applyGradient(TextView textView, int... colors) {
        if (textView == null) {
            Log.e(TAG, "applyGradient: textView is NULL — check your findViewById ID");
            return;
        }

        // Use ViewTreeObserver so we are 100% sure layout is done
        textView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Remove listener immediately so it only fires once
                        textView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        float width = textView.getWidth();
                        float textSize = textView.getTextSize();

                        Log.d(TAG, "applyGradient: id=" + textView.getId()
                                + "  text=\"" + textView.getText() + "\""
                                + "  width=" + width
                                + "  textSize=" + textSize);

                        if (width <= 0f) {
                            Log.e(TAG, "applyGradient: width is 0 — view may be GONE or not measured");
                            return;
                        }

                        LinearGradient shader = new LinearGradient(
                                0f, 0f,
                                width, textSize,
                                colors,
                                null,
                                Shader.TileMode.CLAMP
                        );
                        textView.getPaint().setShader(shader);
                        textView.invalidate();

                        Log.d(TAG, "applyGradient: SUCCESS on \"" + textView.getText() + "\"");
                    }
                });
    }
}