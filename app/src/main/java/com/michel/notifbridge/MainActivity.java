package com.michel.notifbridge;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int BG        = 0xFF1A1A2E;
    private static final int BG_CARD   = 0xFF16213E;
    private static final int ACCENT    = 0xFF7B5EA7;
    private static final int TEXT      = 0xFFEEEEEE;
    private static final int TEXT_HINT = 0xFF888888;
    private static final int BTN_TEST  = 0xFF2E7D32;
    private static final int BTN_DEL   = 0xFF8B0000;

    private SharedPreferences prefs;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("notifbridge", MODE_PRIVATE);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 48, 40, 40);
        root.setBackgroundColor(BG);

        TextView subtitle = new TextView(this);
        subtitle.setText("Transfert de notifications vers NAS");
        subtitle.setTextSize(13);
        subtitle.setTextColor(TEXT_HINT);
        subtitle.setPadding(0, 0, 0, 32);
        root.addView(subtitle);

        Button btnAccess = makeButton("Acces aux notifications", ACCENT);
        btnAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(btnAccess);

        addSpace(root, 32);

        root.addView(makeSection("Regles configurees"));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);
        refreshList();

        addSpace(root, 24);

        root.addView(makeSection("Ajouter une regle"));

        EditText etPkg = makeEdit("Package (ex: com.hyundai.oneapp.eu)");
        root.addView(etPkg);
        addSpace(root, 8);

        EditText etUrl = makeEdit("URL PHP (https://...)");
        root.addView(etUrl);
        addSpace(root, 8);

        EditText etToken = makeEdit("Token (optionnel)");
        root.addView(etToken);
        addSpace(root, 16);

        Button btnAdd = makeButton("+ Ajouter", ACCENT);
        btnAdd.setOnClickListener(v -> {
            String pkg   = etPkg.getText().toString().trim();
            String url   = etUrl.getText().toString().trim();
            String token = etToken.getText().toString().trim();
            if (pkg.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Package et URL obligatoires", Toast.LENGTH_SHORT).show();
                return;
            }
            int count = prefs.getInt("count", 0);
            prefs.edit()
                    .putString("pkg_"   + count, pkg)
                    .putString("url_"   + count, url)
                    .putString("token_" + count, token)
                    .putInt("count", count + 1)
                    .apply();
            etPkg.setText("");
            etUrl.setText("");
            etToken.setText("");
            refreshList();
            Toast.makeText(this, "Regle ajoutee", Toast.LENGTH_SHORT).show();
        });
        root.addView(btnAdd);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void refreshList() {
        listContainer.removeAllViews();
        int count = prefs.getInt("count", 0);
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("Aucune regle configuree.");
            empty.setTextColor(TEXT_HINT);
            empty.setTextSize(14);
            empty.setPadding(0, 16, 0, 16);
            listContainer.addView(empty);
            return;
        }
        for (int i = 0; i < count; i++) {
            final int idx = i;
            String pkg   = prefs.getString("pkg_"   + i, "");
            String url   = prefs.getString("url_"   + i, "");
            String token = prefs.getString("token_" + i, "");

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundColor(BG_CARD);
            card.setPadding(24, 24, 24, 16);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cardParams);

            TextView label = new TextView(this);
            label.setText("Regle " + (idx + 1));
            label.setTextColor(ACCENT);
            label.setTextSize(13);
            label.setTypeface(null, Typeface.BOLD);
            label.setPadding(0, 0, 0, 12);
            card.addView(label);

            EditText ePkg = makeEdit("Package");
            ePkg.setText(pkg);
            card.addView(ePkg);
            addSpace(card, 8);

            EditText eUrl = makeEdit("URL PHP");
            eUrl.setText(url);
            card.addView(eUrl);
            addSpace(card, 8);

            EditText eToken = makeEdit("Token");
            eToken.setText(token);
            card.addView(eToken);
            addSpace(card, 16);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnSave = makeButtonSmall("Sauver", ACCENT);
            btnSave.setOnClickListener(v -> {
                prefs.edit()
                        .putString("pkg_"   + idx, ePkg.getText().toString().trim())
                        .putString("url_"   + idx, eUrl.getText().toString().trim())
                        .putString("token_" + idx, eToken.getText().toString().trim())
                        .apply();
                Toast.makeText(this, "Sauvegarde", Toast.LENGTH_SHORT).show();
            });

            Button btnTest = makeButtonSmall("Tester", BTN_TEST);
            btnTest.setOnClickListener(v -> {
                String u = eUrl.getText().toString().trim();
                String t = eToken.getText().toString().trim();
                String p = ePkg.getText().toString().trim();
                if (u.isEmpty()) {
                    Toast.makeText(this, "URL manquante", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendTestJson(u, t, p);
            });

            Button btnDel = makeButtonSmall("Supprimer", BTN_DEL);
            btnDel.setOnClickListener(v -> {
                deleteRule(idx);
                refreshList();
            });

            LinearLayout.LayoutParams bp1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            bp1.setMargins(0, 0, 8, 0);
            btnSave.setLayoutParams(bp1);

            LinearLayout.LayoutParams bp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            bp2.setMargins(0, 0, 8, 0);
            btnTest.setLayoutParams(bp2);

            LinearLayout.LayoutParams bp3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            btnDel.setLayoutParams(bp3);

            btnRow.addView(btnSave);
            btnRow.addView(btnTest);
            btnRow.addView(btnDel);
            card.addView(btnRow);

            listContainer.addView(card);
        }
    }

    private void sendTestJson(String urlStr, String token, String pkg) {
        String json = "{\"app\":\"" + pkg + "\","
                + "\"title\":\"Test NotifBridge\","
                + "\"text\":\"Ceci est une notification de test\","
                + "\"time\":" + System.currentTimeMillis() + "}";

        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                if (!token.isEmpty()) conn.setRequestProperty("X-Token", token);
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                int code = conn.getResponseCode();
                conn.disconnect();
                runOnUiThread(() -> Toast.makeText(this,
                        code == 200 ? "OK - HTTP 200" : "Erreur HTTP " + code,
                        Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this,
                        "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void deleteRule(int idx) {
        int count = prefs.getInt("count", 0);
        SharedPreferences.Editor ed = prefs.edit();
        for (int i = idx; i < count - 1; i++) {
            ed.putString("pkg_"   + i, prefs.getString("pkg_"   + (i + 1), ""));
            ed.putString("url_"   + i, prefs.getString("url_"   + (i + 1), ""));
            ed.putString("token_" + i, prefs.getString("token_" + (i + 1), ""));
        }
        ed.remove("pkg_"   + (count - 1));
        ed.remove("url_"   + (count - 1));
        ed.remove("token_" + (count - 1));
        ed.putInt("count", count - 1);
        ed.apply();
    }

    private EditText makeEdit(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(TEXT_HINT);
        et.setTextColor(TEXT);
        et.setTextSize(13);
        et.setBackgroundColor(0xFF0D1117);
        et.setPadding(16, 12, 16, 12);
        et.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private Button makeButton(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(color);
        btn.setTextSize(14);
        btn.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return btn;
    }

    private Button makeButtonSmall(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundColor(color);
        btn.setTextSize(12);
        return btn;
    }

    private TextView makeSection(String text) {
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextColor(TEXT_HINT);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, 12);
        return tv;
    }

    private void addSpace(LinearLayout parent, int dp) {
        android.view.View space = new android.view.View(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp));
        parent.addView(space);
    }
}
