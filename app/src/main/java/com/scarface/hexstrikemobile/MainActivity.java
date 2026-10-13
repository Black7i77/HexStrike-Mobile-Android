package com.scarface.hexstrikemobile;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.os.Build;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;

public final class MainActivity extends Activity {
    private LocalCore core;
    private TextView transcript;
    private EditText prompt;
    private ScrollView scroll;
    private static final int BG = Color.rgb(9, 14, 23);
    private static final int PANEL = Color.rgb(23, 34, 49);
    private static final int RED = Color.rgb(248, 84, 95);
    private static final int WHITE = Color.rgb(236, 241, 247);

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }

    private TextView text(String value, float size, int color) {
        TextView tv = new TextView(this);
        tv.setText(value); tv.setTextSize(size); tv.setTextColor(color);
        tv.setIncludeFontPadding(false);
        return tv;
    }

    private GradientDrawable shape(int background, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(background); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title); b.setTextSize(11); b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setMinHeight(0); b.setMinimumHeight(0);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setBackgroundTintList(ColorStateList.valueOf(Color.rgb(165, 43, 58)));
        return b;
    }

    private void addEntry(String speaker, String message) {
        transcript.append("\n" + speaker + "\n" + message + "\n");
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        core = new LocalCore(this); core.start();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(8), dp(8), dp(8), dp(8));
        // Android 15 draws apps edge-to-edge; keep our controls away from
        // the status icons and gesture/navigation area.
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsets(WindowInsets.Type.systemBars());
                top = safe.top;
                bottom = safe.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            root.setPadding(dp(8), dp(6) + top, dp(8), dp(6) + bottom);
            return insets;
        });
        root.setBackgroundColor(BG);
        setContentView(root);
        root.setClipToPadding(true);
        root.requestApplyInsets();

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        try (InputStream in = getAssets().open("hexstrike-logo.png")) {
            ImageView bear = new ImageView(this);
            bear.setImageBitmap(BitmapFactory.decodeStream(in));
            bear.setScaleType(ImageView.ScaleType.FIT_CENTER);
            header.addView(bear, new LinearLayout.LayoutParams(dp(34), dp(34)));
        } catch (Exception ignored) { }
        LinearLayout headings = new LinearLayout(this);
        headings.setOrientation(LinearLayout.VERTICAL);
        headings.setPadding(dp(6), 0, 0, 0);
        TextView title = text("HEXSTRIKE MOBILE", 15, RED);
        title.setTypeface(null, Typeface.BOLD);
        headings.addView(title);
        headings.addView(text("UNOFFICIAL  •  OFFLINE CORE  •  v0.6", 9, Color.LTGRAY));
        header.addView(headings, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(40)));

        TextView status = text("● CORE ONLINE  •  ON-DEVICE  •  NO NETWORK", 10, Color.rgb(108, 228, 161));
        status.setGravity(Gravity.CENTER);
        status.setBackground(shape(Color.rgb(19, 34, 29), 8, Color.rgb(46, 121, 77)));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(26));
        statusParams.topMargin = dp(2);
        root.addView(status, statusParams);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams toolParams = new LinearLayout.LayoutParams(-1, dp(36));
        toolParams.topMargin = dp(5);
        Button health = button("DEVICE CHECK");
        Button clear = button("CLEAR CHAT");
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0, -1, 1);
        left.rightMargin = dp(4);
        toolbar.addView(health, left);
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0, -1, 1);
        right.leftMargin = dp(4);
        toolbar.addView(clear, right);
        root.addView(toolbar, toolParams);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(shape(PANEL, 8, 0));
        transcript = text("HEXSTRIKE SECURITY CONSOLE\n[OK] Local Android core started\n[OK] No IP or Wi-Fi setup needed\n[INFO] Rule-based help (no AI model yet)\n\nAsk a question or tap DEVICE CHECK.\n", 12, WHITE);
        transcript.setTextIsSelectable(true);
        transcript.setPadding(dp(9), dp(9), dp(9), dp(9));
        scroll.addView(transcript, new ScrollView.LayoutParams(-1, -2));
        // Do not use weight=1: that made the console absurdly tall.
        // A bounded scroll area is usable on normal and smaller screens.
        LinearLayout.LayoutParams consoleParams = new LinearLayout.LayoutParams(-1, dp(260));
        consoleParams.topMargin = dp(5);
        root.addView(scroll, consoleParams);
        // Shrink the console if the keyboard opens or the display is short.
        // This keeps the message field and send button on screen.
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, oright, ob) -> {
            int available = b - t - root.getPaddingTop() - root.getPaddingBottom();
            int desired = Math.min(dp(260), Math.max(dp(96), available - dp(206)));
            if (consoleParams.height != desired) {
                consoleParams.height = desired;
                scroll.setLayoutParams(consoleParams);
            }
        });

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams inputRowParams = new LinearLayout.LayoutParams(-1, dp(40));
        inputRowParams.topMargin = dp(4);
        prompt = new EditText(this);
        prompt.setTextSize(12); prompt.setTextColor(WHITE);
        prompt.setHintTextColor(Color.rgb(156, 165, 177));
        prompt.setHint("Ask HexStrike...");
        prompt.setBackgroundColor(Color.TRANSPARENT);
        prompt.setPadding(dp(6), 0, dp(6), 0);
        prompt.setSingleLine(true);
        prompt.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        prompt.setBackground(shape(Color.rgb(16, 23, 34), 6, Color.rgb(52, 64, 82)));
        inputRow.addView(prompt, new LinearLayout.LayoutParams(0, -1, 1f));
        Button send = button("SEND");
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(dp(62), dp(36));
        sendParams.leftMargin = dp(4);
        inputRow.addView(send, sendParams);
        root.addView(inputRow, inputRowParams);

        TextView footer = text("PRIVATE • OFFLINE • DEVICE CHECKS ONLY", 8, Color.rgb(148, 156, 168));
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footParams = new LinearLayout.LayoutParams(-1, dp(14));
        footParams.topMargin = dp(2);
        root.addView(footer, footParams);

        health.setOnClickListener(v -> addEntry("DEVICE REPORT", core.deviceReport()));
        clear.setOnClickListener(v -> transcript.setText("HEXSTRIKE SECURITY CONSOLE\nNew local chat started.\n"));
        Runnable submit = () -> {
            String question = prompt.getText().toString().trim();
            if (question.isEmpty()) return;
            prompt.setText("");
            addEntry("YOU", question);
            addEntry("HEXSTRIKE", core.reply(question));
        };
        send.setOnClickListener(v -> submit.run());
        prompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                submit.run(); return true;
            }
            return false;
        });
    }

    @Override protected void onDestroy() {
        if (core != null) core.stop();
        super.onDestroy();
    }
}
