package me.cadkey.hyundainotif;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HyundaiNotificationListener extends NotificationListenerService {

    private static final String TAG = "HyundaiNotif";
    private static final String WEBHOOK_URL = "https://ext.cadkey.synology.me/notificationApk.php";
    private static final String TARGET_PACKAGE = "com.hyundai.oneapp.eu";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!TARGET_PACKAGE.equals(sbn.getPackageName())) return;

        android.app.Notification notif = sbn.getNotification();
        String title = "";
        String text = "";

        if (notif.extras != null) {
            CharSequence t = notif.extras.getCharSequence(android.app.Notification.EXTRA_TITLE);
            CharSequence b = notif.extras.getCharSequence(android.app.Notification.EXTRA_TEXT);
            if (t != null) title = t.toString();
            if (b != null) text = b.toString();
        }

        final String json = "{\"app\":\"" + TARGET_PACKAGE + "\","
                + "\"title\":" + jsonString(title) + ","
                + "\"text\":" + jsonString(text) + ","
                + "\"time\":" + sbn.getPostTime() + "}";

        new Thread(() -> sendPost(json)).start();
    }

    private String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private void sendPost(String json) {
        try {
            URL url = new URL(WEBHOOK_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }
            int code = conn.getResponseCode();
            Log.d(TAG, "POST sent, response: " + code);
            conn.disconnect();
        } catch (Exception e) {
            Log.e(TAG, "POST failed: " + e.getMessage());
        }
    }
}
