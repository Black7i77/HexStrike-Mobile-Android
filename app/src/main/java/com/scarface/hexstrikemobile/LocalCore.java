package com.scarface.hexstrikemobile;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
import android.provider.Settings;

/** Small, in-process Android-only core; no Flask, IP address, network, or background daemon. */
public final class LocalCore {
    private final Context context;
    private boolean running;
    public LocalCore(Context context) { this.context = context.getApplicationContext(); }
    public void start() { running = true; }
    public void stop() { running = false; }
    public boolean isRunning() { return running; }

    private boolean setting(String key) {
        try { return Settings.Global.getInt(context.getContentResolver(), key, 0) != 0; }
        catch (Exception ignored) { return false; }
    }

    public String status() {
        if (!running) return "CORE STOPPED";
        return "CORE ONLINE • On-device • No network required";
    }

    public String deviceReport() {
        if (!running) return "Core is not running";
        boolean developer = setting(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED);
        boolean adb = setting(Settings.Global.ADB_ENABLED);
        boolean debugger = Debug.isDebuggerConnected();
        String patch = Build.VERSION.SECURITY_PATCH == null || Build.VERSION.SECURITY_PATCH.isEmpty()
                ? "Not reported" : Build.VERSION.SECURITY_PATCH;
        StringBuilder sb = new StringBuilder();
        sb.append("ANDROID DEVICE CHECK\n\n")
          .append("Device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
          .append("Android version: ").append(Build.VERSION.RELEASE).append('\n')
          .append("Security patch: ").append(patch).append('\n')
          .append("Developer options: ").append(developer ? "ON" : "OFF").append('\n')
          .append("ADB debugging: ").append(adb ? "ON — review if not needed" : "OFF").append('\n')
          .append("Debugger attached to this app: ").append(debugger ? "YES" : "NO").append('\n')
          .append("\nThese are informational checks only. This app cannot determine whether your phone is hacked or malware-free.");
        return sb.toString();
    }

    public String reply(String prompt) {
        if (!running) return "The core is stopped. Reopen the app.";
        if (prompt == null || prompt.trim().isEmpty()) return "Ask me a question about this phone or its security.";
        String q = prompt.toLowerCase(java.util.Locale.ROOT);
        if (q.contains("status") || q.contains("online") || q.contains("server")) return status() + "\nAll functions in v0.2 run within the Android app process. No socket server is required.";
        if (q.contains("scan") || q.contains("check") || q.contains("protect") || q.contains("security")) return deviceReport() + "\n\nKeep Android and Play Protect updated. This is not an antivirus scanner.";
        if (q.contains("permission")) return "Check Settings → Apps → [app] → Permissions to review app access. Android restricts inspection of other apps; this version doesn't collect their data.";
        if (q.contains("xss")) return "Cross-site scripting (XSS) occurs when a site treats untrusted content as executable code. Defend with context-aware output escaping, safe DOM APIs, and a restrictive Content Security Policy.";
        if (q.contains("malware") || q.contains("virus")) return "I can't detect malware in this version. Use Google Play Protect, review installed apps, and avoid unknown APKs. A future release could analyze APK files explicitly selected by the user.";
        if (q.contains("hello") || q.contains("hi ") || q.equals("hi")) return "Hey bro! 🐺 HexStrike's offline core is running. Tap Device Check or ask about Android security.";
        return "HexStrike v0.3 has a built-in offline help engine, not a language model yet. I can explain status, device checks, permissions, malware limitations and XSS. An actual on-device AI model is planned for a later version.";
    }
}
