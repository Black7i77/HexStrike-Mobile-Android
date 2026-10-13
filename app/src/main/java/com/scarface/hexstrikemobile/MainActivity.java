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
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import android.content.Intent;
import android.net.Uri;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private LocalCore core;
    private ModelBridge modelBridge;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private volatile boolean modelReady = false;
    private TextView statusView;
    private Button sendButton;
    private static final int PICK_GGUF = 1407;
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
        modelBridge = new ModelBridge();

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
        headings.addView(text("UNOFFICIAL  •  OFFLINE AI  •  v0.8", 9, Color.LTGRAY));
        header.addView(headings, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(40)));

        TextView status = text("● CORE ONLINE  •  ON-DEVICE  •  NO NETWORK", 10, Color.rgb(108, 228, 161));
        status.setGravity(Gravity.CENTER);
        status.setBackground(shape(Color.rgb(19, 34, 29), 8, Color.rgb(46, 121, 77)));
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(26));
        statusParams.topMargin = dp(2);
        root.addView(status, statusParams);
        statusView = status;

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams toolParams = new LinearLayout.LayoutParams(-1, dp(36));
        toolParams.topMargin = dp(5);
        Button health = button("DEVICE CHECK");
        Button clear = button("CHANGE MODEL");
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
        transcript = text("HEXSTRIKE SECURITY CONSOLE\n[OK] Local Android core started\n[AI] Preparing bundled offline model...\n\nDevice Check works independently.\n", 12, WHITE);
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
        sendButton = send;
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
        clear.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("*/*");
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pick, PICK_GGUF);
        });
        Runnable submit = () -> {
            String question = prompt.getText().toString().trim();
            if (question.isEmpty()) return;
            prompt.setText("");
            addEntry("YOU", question);
            if (!modelReady) {
                addEntry("HEXSTRIKE", "AI is loading. If this build has no bundled model, use CHANGE MODEL to import a GGUF file.");
                return;
            }
            sendButton.setEnabled(false);
            addEntry("HEXSTRIKE", "Generating offline response…");
            worker.execute(() -> {
                String result;
                try { result = modelBridge.generate(question, 240); }
                catch (Exception e) { result = "ERROR: " + e.getMessage(); }
                final String reply = result;
                runOnUiThread(() -> {
                    addEntry("AI", reply);
                    sendButton.setEnabled(true);
                });
            });
        };
        send.setOnClickListener(v -> submit.run());
        autoLoadBundledModel();
        prompt.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                submit.run(); return true;
            }
            return false;
        });
    }


    // This happens automatically on first launch; subsequent launches reuse the
    // app-private copy. The model is never downloaded or accessed over the network.
    private void autoLoadBundledModel() {
        worker.execute(() -> {
            String message;
            try {
                File target = new File(getFilesDir(), "hexstrike-model.gguf");
                if (!target.isFile() || target.length() < 100_000_000L) {
                    try (InputStream in = getAssets().open("qwen2.5-1.5b-instruct-q4_k_m.gguf");
                         OutputStream out = new FileOutputStream(new File(getFilesDir(), "model-import.tmp"))) {
                        byte[] buf = new byte[1024 * 1024];
                        int count;
                        while ((count = in.read(buf)) != -1) out.write(buf, 0, count);
                    }
                    File temporary = new File(getFilesDir(), "model-import.tmp");
                    if (temporary.length() < 100_000_000L || !temporary.renameTo(target)) {
                        throw new java.io.IOException("Model copy incomplete; check phone storage");
                    }
                }
                boolean loaded = modelBridge.load(target.getAbsolutePath());
                modelReady = loaded;
                message = loaded ? "Bundled Qwen2.5 AI loaded. Ask me a question!" :
                        "Model could not load. Check available RAM or use CHANGE MODEL.";
            } catch (java.io.FileNotFoundException missing) {
                message = "Bundled GGUF missing from APK. Run scripts/prepare-model.sh, rebuild, or tap CHANGE MODEL.";
            } catch (Exception error) {
                message = "Automatic model setup failed: " + error.getMessage();
            }
            final String result = message;
            runOnUiThread(() -> {
                addEntry("AI", result);
                statusView.setText(modelReady ? "● CORE ONLINE  •  AI READY  •  OFFLINE" :
                        "● CORE ONLINE  •  AI MODEL NOT LOADED");
            });
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_GGUF || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        modelReady = false;
        addEntry("SYSTEM", "Importing model into private storage; this can take a while…");
        worker.execute(() -> {
            String msg;
            try {
                File target = new File(getFilesDir(), "hexstrike-model.gguf");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     OutputStream out = new FileOutputStream(target)) {
                    if (in == null) throw new java.io.IOException("Cannot open selected model");
                    byte[] buf = new byte[1024 * 1024];
                    int len;
                    while ((len = in.read(buf)) >= 0) out.write(buf, 0, len);
                }
                modelReady = modelBridge.load(target.getAbsolutePath());
                msg = modelReady ? "Real on-device model loaded. Ask a question!" : "Model failed to load. Check GGUF compatibility and available RAM.";
            } catch (Exception e) { msg = "Model import failed: " + e.getMessage(); }
            final String result = msg;
            runOnUiThread(() -> {
                addEntry("MODEL", result);
                statusView.setText(modelReady ? "● CORE ONLINE  •  AI READY  •  OFFLINE" : "● CORE ONLINE  •  NO MODEL LOADED");
            });
        });
    }

    @Override protected void onDestroy() {
        if (core != null) core.stop();
        worker.execute(() -> modelBridge.unload());
        worker.shutdown();
        super.onDestroy();
    }
}
