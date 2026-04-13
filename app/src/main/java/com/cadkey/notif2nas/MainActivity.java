package com.cadkey.notif2nas;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
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

    private static final int BG       = 0xFF121212;
    private static final int BG_CARD  = 0xFF1E1E2E;
    private static final int ACCENT   = 0xFF7B5EA7;
    private static final int ACCENT2  = 0xFF9B7EC8;
    private static final int TEXT     = 0xFFE0E0E0;
    private static final int TEXT_SUB = 0xFF9E9E9E;
    private static final int BTN_TEST = 0xFF2E7D32;
    private static final int BTN_DEL  = 0xFF7B1F1F;
    private static final int FIELD_BG = 0xFF2A2A3E;

    private SharedPreferences prefs;
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("notif2nas", MODE_PRIVATE);
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 56, 48, 48);
        root.setBackgroundColor(BG);

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setPadding(0, 0, 0, 32);

        LinearLayout headerLeft = new LinearLayout(this);
        headerLeft.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        headerLeft.setLayoutParams(llp);

        TextView appName = new TextView(this);
        appName.setText("Notif2Nas");
        appName.setTextSize(24);
        appName.setTextColor(ACCENT2);
        appName.setTypeface(null, Typeface.BOLD);
        headerLeft.addView(appName);

        TextView appSub = new TextView(this);
        appSub.setText("Transfert de notifications vers NAS");
        appSub.setTextSize(12);
        appSub.setTextColor(TEXT_SUB);
        headerLeft.addView(appSub);

        header.addView(headerLeft);

        Button btnHelp = new Button(this);
        btnHelp.setText("?");
        btnHelp.setTextColor(Color.WHITE);
        btnHelp.setTextSize(16);
        btnHelp.setTypeface(null, Typeface.BOLD);
        btnHelp.setBackground(makeRoundBg(ACCENT, 24));
        btnHelp.setPadding(0, 0, 0, 8);
        LinearLayout.LayoutParams helpLp = new LinearLayout.LayoutParams(96, 96);
        helpLp.setMargins(16, 0, 0, 0);
        btnHelp.setLayoutParams(helpLp);
        btnHelp.setOnClickListener(v -> showHelp());
        header.addView(btnHelp);

        root.addView(header);

        // Bouton 1 - Acces notifications
        Button btnAcces = makeBtn("Vérifier accès aux notifications", ACCENT, 0f);
        btnAcces.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnAcces.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(btnAcces);

        addSpace(root, 8);

        // Bouton 2 - Parametres restreints
        Button btnRestreint = makeBtn("Autoriser paramètres restreints", 0xFF37474F, 0f);
        btnRestreint.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        btnRestreint.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(android.net.Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        });
        root.addView(btnRestreint);

        addSpace(root, 32);

        // Regles
        root.addView(makeSection("Règles configurées"));
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);
        refreshList();

        addSpace(root, 28);

        // Ajouter
        root.addView(makeSection("Ajouter une règle"));

        EditText etPkg = makeField("Package (ex: com.hyundai.oneapp.eu)");
        root.addView(etPkg);
        addSpace(root, 10);

        EditText etUrl = makeField("URL du script (ex: https://app.mydomain/notif.php)");
        root.addView(etUrl);
        addSpace(root, 10);

        EditText etToken = makeField("Token (optionnel)");
        root.addView(etToken);
        addSpace(root, 20);

        Button btnAdd = makeBtn("+ Ajouter", ACCENT, 0f);
        btnAdd.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
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
            etPkg.setText(""); etUrl.setText(""); etToken.setText("");
            refreshList();
            Toast.makeText(this, "Règle ajoutée", Toast.LENGTH_SHORT).show();
        });
        root.addView(btnAdd);

        addSpace(root, 48);

        // Footer
        TextView footer = new TextView(this);
        String versionName = "?";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {}
        footer.setText("\u00A9 Cadkey MT  v" + versionName);
        footer.setTextSize(11);
        footer.setTextColor(0xFF444466);
        footer.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        root.addView(footer);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void refreshList() {
        listContainer.removeAllViews();
        int count = prefs.getInt("count", 0);
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("Aucune règle configurée.");
            empty.setTextColor(TEXT_SUB);
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
            card.setBackground(makeRoundBg(BG_CARD, 16));
            card.setPadding(28, 28, 28, 20);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(cp);

            TextView label = new TextView(this);
            label.setText("Règle " + (idx + 1));
            label.setTextColor(ACCENT2);
            label.setTextSize(13);
            label.setTypeface(null, Typeface.BOLD);
            label.setPadding(0, 0, 0, 14);
            card.addView(label);

            EditText ePkg   = makeField("Package");
            ePkg.setText(pkg);
            card.addView(ePkg);
            addSpace(card, 10);

            EditText eUrl   = makeField("URL PHP");
            eUrl.setText(url);
            card.addView(eUrl);
            addSpace(card, 10);

            EditText eToken = makeField("Token (optionnel)");
            eToken.setText(token);
            card.addView(eToken);
            addSpace(card, 18);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button bSave = makeBtn("Sauver", ACCENT, 1f);
            Button bTest = makeBtn("Tester", BTN_TEST, 1f);
            Button bDel  = makeBtn("Supprimer", BTN_DEL, 1f);

            LinearLayout.LayoutParams b1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            b1.setMargins(0, 0, 8, 0);
            bSave.setLayoutParams(b1);

            LinearLayout.LayoutParams b2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            b2.setMargins(0, 0, 8, 0);
            bTest.setLayoutParams(b2);

            LinearLayout.LayoutParams b3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            bDel.setLayoutParams(b3);

            bSave.setOnClickListener(v -> {
                prefs.edit()
                        .putString("pkg_"   + idx, ePkg.getText().toString().trim())
                        .putString("url_"   + idx, eUrl.getText().toString().trim())
                        .putString("token_" + idx, eToken.getText().toString().trim())
                        .apply();
                Toast.makeText(this, "Sauvegarde", Toast.LENGTH_SHORT).show();
            });

            bTest.setOnClickListener(v -> {
                String u = eUrl.getText().toString().trim();
                String t = eToken.getText().toString().trim();
                String p = ePkg.getText().toString().trim();
                if (u.isEmpty()) { Toast.makeText(this, "URL manquante", Toast.LENGTH_SHORT).show(); return; }
                sendTest(u, t, p);
            });

            bDel.setOnClickListener(v -> { deleteRule(idx); refreshList(); });

            btnRow.addView(bSave);
            btnRow.addView(bTest);
            btnRow.addView(bDel);
            card.addView(btnRow);

            listContainer.addView(card);
        }
    }

    private void showHelp() {
        String msg =
            "L'app détecte les notifications Android et les envoie en HTTPS vers un script de votre NAS.\n\n" +
            "ACTIVATION (3 étapes)\n\n" +
            "1. Bouton 'Vérifier accès aux notifications'\n" +
            "   Sélectionner Notif2Nas\n" +
            "   > Autoriser l'accès aux notifications\n\n" +
            "   Si ok, l'activation est terminée\n" + 
            "   Sinon quitter complètement l'app\n" +
            "   et continuer à l'étape 2\n\n" + 
            "2. Bouton 'Autoriser paramètres restreints'\n" +
            "   Appuyer sur '\u22EE' en haut à droite\n" +
            "   > Autoriser les paramètres restreints\n\n" +
            "3. Bouton 'Vérifier accès aux notifications'\n" +
            "   Sélectionner Notif2Nas\n" +
            "   > Autoriser l'accès aux notifications\n" +
            "   l'activation est terminée\n\n" +
            "CONFIGURATION\n" +
            "Package: Identifiant de l'app Android à surveiller\n" +
            "URL: adresse HTTPS de votre script sur le NAS\n" +
            "Token: envoyé dans le header X-Token\n" +
            "  pour authentification\n\n" +
            "JSON ENVOYÉ\n" +
            "{ app, title, text, time }\n\n" +
            "SÉCURITÉ\n" +
            "- L'application installe aucune donnée\n" +
            "- Les données saisies sont stockées localement sur l'appareil\n" +
            "- Désinstaller l'app supprime toutes les données\n" +
            "- Le token valide chaque requête coté serveur\n\n" +
            "Une fois configurée, l'app tourne en arrière-plan, silencieuse.";

        new AlertDialog.Builder(this)
                .setTitle("Notif2Nas — Aide")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void sendTest(String urlStr, String token, String pkg) {
        String json = "{\"app\":\"" + pkg + "\","
                + "\"title\":\"Test Notif2Nas\","
                + "\"text\":\"Ceci est une notification de test réussi\","
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
                        code == 200 ? "OK — HTTP 200" : "Erreur HTTP " + code,
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
            ed.putString("pkg_"   + i, prefs.getString("pkg_"   + (i+1), ""));
            ed.putString("url_"   + i, prefs.getString("url_"   + (i+1), ""));
            ed.putString("token_" + i, prefs.getString("token_" + (i+1), ""));
        }
        ed.remove("pkg_"   + (count-1));
        ed.remove("url_"   + (count-1));
        ed.remove("token_" + (count-1));
        ed.putInt("count", count-1);
        ed.apply();
    }

    private GradientDrawable makeRoundBg(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(radius);
        return gd;
    }

    private EditText makeField(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setHintTextColor(0xFF555577);
        et.setTextColor(TEXT);
        et.setTextSize(14);
        et.setBackground(makeRoundBg(FIELD_BG, 12));
        et.setPadding(24, 20, 24, 20);
        et.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private Button makeBtn(String text, int color, float weight) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setBackground(makeRoundBg(color, 14));
        btn.setTextSize(13);
        btn.setPadding(12, 20, 12, 20);
        if (weight > 0) {
            btn.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        }
        return btn;
    }

    private TextView makeSection(String text) {
        TextView tv = new TextView(this);
        tv.setText(text.toUpperCase());
        tv.setTextColor(0xFF6A5A8A);
        tv.setTextSize(11);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(0, 0, 0, 14);
        tv.setLetterSpacing(0.12f);
        return tv;
    }

    private void addSpace(LinearLayout p, int h) {
        android.view.View v = new android.view.View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        p.addView(v);
    }
}
