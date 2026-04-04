package com.cadkey.notif2nas;

import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NotifListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        SharedPreferences prefs = getSharedPreferences("notif2nas", MODE_PRIVATE);
        int count = prefs.getInt("count", 0);

        for (int i = 0; i < count; i++) {
            String pkg   = prefs.getString("pkg_"   + i, "");
            String url   = prefs.getString("url_"   + i, "");
            String token = prefs.getString("token_" + i, "");

            if (pkg.isEmpty() || url.isEmpty()) continue;
            if (!sbn.getPackageName().equals(pkg)) continue;

            String title = "";
            String text  = "";
            if (sbn.getNotification().extras != null) {
                CharSequence t = sbn.getNotification().extras.getCharSequence("android.title");
                CharSequence b = sbn.getNotification().extras.getCharSequence("android.text");
                if (t != null) title = t.toString();
                if (b != null) text  = b.toString();
            }

            final String json = "{\"app\":\"" + escapeJson(pkg) + "\","
                    + "\"title\":\"" + escapeJson(title) + "\","
                    + "\"text\":\"" + escapeJson(text) + "\","
                    + "\"time\":" + sbn.getPostTime() + "}";
            final String fUrl   = url;
            final String fToken = token;

            new Thread(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(fUrl).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    if (!fToken.isEmpty()) conn.setRequestProperty("X-Token", fToken);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                    }
                    conn.getResponseCode();
                    conn.disconnect();
                } catch (Exception ignored) {}
            }).start();
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }
}
