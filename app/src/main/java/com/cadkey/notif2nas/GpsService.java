package com.cadkey.notif2nas;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import android.os.IBinder;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GpsService extends Service {

    private static final String CHANNEL_ID = "gps_channel";
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notif = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Notif2Nas")
                .setContentText("Localisation active")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
        startForeground(1, notif);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5 * 60 * 1000L)
                .setMinUpdateDistanceMeters(50f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                onLocationChanged(result.getLastLocation());
            }
        };

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, getMainLooper());
            fusedClient.getLastLocation().addOnSuccessListener(loc -> {
                if (loc != null) onLocationChanged(loc);
            });
        } catch (SecurityException ignored) {}

        return START_STICKY;
    }

    @Override
    public void onLocationChanged(Location loc) {
        SharedPreferences prefs = getSharedPreferences("notif2nas", MODE_PRIVATE);
        String url   = prefs.getString("gps_url", "");
        String token = prefs.getString("gps_token", "");
        if (url.isEmpty()) return;

        String json = "{\"lat\":" + loc.getLatitude()
                + ",\"lon\":" + loc.getLongitude()
                + ",\"acc\":" + (int) loc.getAccuracy()
                + ",\"time\":" + (loc.getTime() / 1000L) + "}";

        final String fUrl   = url;
        final String fToken = token;
        final String fJson  = json;

        new Thread(() -> {
            try {
                byte[] body = fJson.getBytes(StandardCharsets.UTF_8);
                HttpURLConnection conn = (HttpURLConnection) new URL(fUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                if (!fToken.isEmpty()) conn.setRequestProperty("X-Token", fToken);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body);
                }
                conn.getResponseCode();
                conn.disconnect();
            } catch (Exception ignored) {}
        }).start();
    }

    private void createNotificationChannel() {
        NotificationChannel ch = new NotificationChannel(
                CHANNEL_ID, "GPS Notif2Nas", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedClient != null && locationCallback != null)
            fusedClient.removeLocationUpdates(locationCallback);
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
