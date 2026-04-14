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

        // 3 boutons sur une ligne : Vérifier | Autoriser | Accès
        LinearLayout btnRow3 = new LinearLayout(this);
        btnRow3.setOrientation(LinearLayout.HORIZONTAL);

        Button btnVerif1 = makeBtn("Vérifier", ACCENT, 1f);
        Button btnAutoriser = makeBtn("Autoriser", 0xFF37474F, 1f);
        Button btnAcces2 = makeBtn("Accès", ACCENT, 1f);

        LinearLayout.LayoutParams bra1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bra1.setMargins(0, 0, 8, 0);
        btnVerif1.setLayoutParams(bra1);

        LinearLayout.LayoutParams bra2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        bra2.setMargins(0, 0, 8, 0);
        btnAutoriser.setLayoutParams(bra2);

        LinearLayout.LayoutParams bra3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        btnAcces2.setLayoutParams(bra3);

        btnVerif1.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        btnAutoriser.setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.setData(android.net.Uri.fromParts("package", getPackageName(), null));
            startActivity(i);
        });

        btnAcces2.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        btnRow3.addView(btnVerif1);
        btnRow3.addView(btnAutoriser);
        btnRow3.addView(btnAcces2);
        root.addView(btnRow3);

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

        EditText etUrl = makeField("URL (ex: https://app.mydomain/notif.php)");
        root.addView(etUrl);
        addSpace(root, 10);

        EditText etToken = makeField("Token (optionnel) a-z A-Z 0-9 - _");
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

            EditText ePkg   = makeField("Package (ex: com.hyundai.oneapp.eu)");
            ePkg.setText(pkg);
            card.addView(ePkg);
            addSpace(card, 10);

            EditText eUrl   = makeField("URL (ex: https://app.mydomain/notif.php)");
            eUrl.setText(url);
            card.addView(eUrl);
            addSpace(card, 10);

            EditText eToken = makeField("Token (optionnel) a-z A-Z 0-9 - _");
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
    ScrollView scroll = new ScrollView(this);
    scroll.setBackgroundColor(BG);

    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(48, 48, 48, 32);
    root.setBackgroundColor(BG);

    TextView intro = new TextView(this);
    intro.setText("Notif2Nas détecte les notifications Android et les envoie en HTTPS vers un script de votre NAS.");
    intro.setTextColor(TEXT);
    intro.setTextSize(14);
    intro.setPadding(0, 0, 0, 28);
    root.addView(intro);

    TextView t1 = new TextView(this);
    t1.setText("ACTIVATION (3 étapes)");
    t1.setTextColor(ACCENT2);
    t1.setTextSize(16);
    t1.setTypeface(null, Typeface.BOLD);
    t1.setPadding(0, 24, 0, 0);
    root.addView(t1);

    TextView t1b = new TextView(this);
    t1b.setText(
            "1. Bouton 'Vérifier'\n" +
            "Sélectionner Notif2Nas\n" +
            "> Autoriser l'accès aux notifications\n" +
            "Ça ne fonctionne pas, c'est normal,\n" +
            "mais l'étape est obligatoire.\n" +
            "Quitter complètement l'application\n" +
            "et reprendre à l'étape 2.\n\n" +
            "2. Bouton 'Autoriser'\n" +
            "Appuyer sur '\u22EE' en haut à droite\n" +
            "> Autoriser les paramètres restreints\n\n" +
            "3. Bouton 'Accès'\n" +
            "Sélectionner Notif2Nas\n" +
            "> Autoriser l'accès aux notifications\n" +
            "C'est ok, l'activation est terminée."
    );
    t1b.setTextColor(TEXT);
    t1b.setTextSize(14);
    t1b.setLineSpacing(0, 1.15f);
    t1b.setPadding(0, 14, 0, 28);
    root.addView(t1b);

    TextView t2 = new TextView(this);
    t2.setText("CONFIGURATION");
    t2.setTextColor(ACCENT2);
    t2.setTextSize(16);
    t2.setTypeface(null, Typeface.BOLD);
    t2.setPadding(0, 24, 0, 0);
    root.addView(t2);

    TextView t2b = new TextView(this);
    t2b.setText(
            "Package : identifiant de l'app Android à surveiller.\n\n" +
            "URL : adresse HTTPS de votre script sur le NAS.\n\n" +
            "Token : envoyé dans le header X-Token\n" +
            "pour authentification."
    );
    t2b.setTextColor(TEXT);
    t2b.setTextSize(14);
    t2b.setLineSpacing(0, 1.15f);
    t2b.setPadding(0, 14, 0, 28);
    root.addView(t2b);

    TextView t3 = new TextView(this);
    t3.setText("JSON ENVOYÉ");
    t3.setTextColor(ACCENT2);
    t3.setTextSize(16);
    t3.setTypeface(null, Typeface.BOLD);
    t3.setPadding(0, 24, 0, 0);
    root.addView(t3);

    TextView t3b = new TextView(this);
    t3b.setText(
        "{\n" +
        "  \"app\": \"com.hyundai.oneapp.eu\",\n" +
        "  \"title\": \"Commande à distance\",\n" +
        "  \"text\": \"Portes verrouillées.\",\n" +
        "  \"time\": 1776139361\n" +
        "}"
    );
    t3b.setTextColor(0xFFD6D6F0);
    t3b.setTextSize(13);
    t3b.setTypeface(Typeface.MONOSPACE);
    t3b.setBackground(makeRoundBg(FIELD_BG, 12));
    t3b.setPadding(22, 18, 22, 18);
    t3b.setPadding(22, 18, 22, 18);
    LinearLayout.LayoutParams lp3 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp3.setMargins(0, 14, 0, 28);
    t3b.setLayoutParams(lp3);
    root.addView(t3b);

    TextView t4 = new TextView(this);
    t4.setText("SÉCURITÉ");
    t4.setTextColor(ACCENT2);
    t4.setTextSize(16);
    t4.setTypeface(null, Typeface.BOLD);
    t4.setPadding(0, 24, 0, 0);
    root.addView(t4);

    TextView t4b = new TextView(this);
    t4b.setText(
            "- L'application installe aucune donnée.\n\n" +
            "- Les données saisies sont stockées localement sur l'appareil.\n\n" +
            "- Désinstaller l'application supprime toutes les données.\n\n" +
            "- Le token permet d'authentifier et de sécuriser chaque requête vers le serveur."
    );
    t4b.setTextColor(TEXT);
    t4b.setTextSize(14);
    t4b.setLineSpacing(0, 1.15f);
    t4b.setPadding(0, 14, 0, 28);
    root.addView(t4b);

    TextView t5 = new TextView(this);
    t5.setText("INFOS");
    t5.setTextColor(ACCENT2);
    t5.setTextSize(16);
    t5.setTypeface(null, Typeface.BOLD);
    t5.setPadding(0, 24, 0, 0);
    root.addView(t5);

    TextView t5b = new TextView(this);
    t5b.setText(
        "Par souci de compatibilité entre les différents systèmes, utilisez" +
        " de préférence les caractères a-z A-Z 0-9 - _ . dans le token.\n\n" +
        "Une fois configurée, l'app tourne silencieusement en arrière-plan."
    );
    t5b.setTextColor(TEXT);
    t5b.setTextSize(14);
    t5b.setLineSpacing(0, 1.15f);
    t5b.setPadding(0, 14, 0, 28);
    root.addView(t5b);

    TextView t6 = new TextView(this);
    t6.setText("EXEMPLE PHP");
    t6.setTextColor(ACCENT2);
    t6.setTextSize(16);
    t6.setTypeface(null, Typeface.BOLD);
    t6.setPadding(0, 24, 0, 0);
    root.addView(t6);

    TextView t6b = new TextView(this);
    t6b.setText(
            "$token = 'votre_token_fort';\n" +
            "$xtoken = $_SERVER['HTTP_X_TOKEN'] ?? '';\n" +
            "if ($token != $xtoken) {\n" +
            "    http_response_code(403);\n" +
            "    exit;\n" +
            "}\n" +
            "$raw = file_get_contents('php://input');\n" +
            "$data = json_decode($raw, true);\n" +
            "if (!$data) {\n" +
            "    http_response_code(400);\n" +
            "    exit;\n" +
            "}\n" +
            "[$app, $title, $text, $time] = [$data['app'], $data['title'], $data['text'], $data['time']];"
    );
    t6b.setTextColor(0xFFD6D6F0);
    t6b.setTextSize(12);
    t6b.setTypeface(Typeface.MONOSPACE);
    t6b.setBackground(makeRoundBg(FIELD_BG, 12));
    t6b.setPadding(22, 18, 22, 18);
    t6b.setLineSpacing(0, 1.1f);
    t6b.setTextIsSelectable(true);
    LinearLayout.LayoutParams lp6 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp6.setMargins(0, 14, 0, 0);
    t6b.setLayoutParams(lp6);
    root.addView(t6b);
    
    TextView t7 = new TextView(this);
    t7.setText("EXEMPLE SH");
    t7.setTextColor(ACCENT2);
    t7.setTextSize(16);
    t7.setTypeface(null, Typeface.BOLD);
    t7.setPadding(0, 52, 0, 0);
    root.addView(t7);

    TextView t7b = new TextView(this);
    t7b.setText(
            "token='votre_token_fort'\n" +
            "xtoken=\"${HTTP_X_TOKEN:-}\"\n" +
            "if [ \"$token\" != \"$xtoken\" ]; then\n" +
            "    printf 'Status: 403 Forbidden\\r\\n\\r\\n'\n" +
            "    exit\n" +
            "fi\n" +
            "\n" +
            "raw=$(cat)\n" +
            "data=$(printf '%s' \"$raw\" | jq -c . 2>/dev/null)\n" +
            "if [ -z \"$data\" ] || [ \"$data\" = \"null\" ]; then\n" +
            "    printf 'Status: 400 Bad Request\\r\\n\\r\\n'\n" +
            "    exit\n" +
            "fi\n" +
            "\n" +
            "app=$(printf '%s' \"$data\" | jq -r '.app')\n" +
            "title=$(printf '%s' \"$data\" | jq -r '.title')\n" +
            "text=$(printf '%s' \"$data\" | jq -r '.text')\n" +
            "time=$(printf '%s' \"$data\" | jq -r '.time')"
    );
    t7b.setTextColor(0xFFD6D6F0);
    t7b.setTextSize(12);
    t7b.setTypeface(Typeface.MONOSPACE);
    t7b.setBackground(makeRoundBg(FIELD_BG, 12));
    t7b.setPadding(22, 18, 22, 18);
    t7b.setLineSpacing(0, 1.1f);
    t7b.setTextIsSelectable(true);
    LinearLayout.LayoutParams lp7 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp7.setMargins(0, 14, 0, 0);
    t7b.setLayoutParams(lp7);
    root.addView(t7b);
    
    TextView t8 = new TextView(this);
    t8.setText("EXEMPLE PYTHON");
    t8.setTextColor(ACCENT2);
    t8.setTextSize(16);
    t8.setTypeface(null, Typeface.BOLD);
    t8.setPadding(0, 52, 0, 0);
    root.addView(t8);

    TextView t8b = new TextView(this);
    t8b.setText(
            "token = 'votre_token_fort'\n" +
            "xtoken = os.environ.get('HTTP_X_TOKEN', '')\n" +
            "if token != xtoken:\n" +
            "    print('Status: 403 Forbidden')\n" +
            "    print()\n" +
            "    raise SystemExit\n" +
            "\n" +
            "raw = sys.stdin.read()\n" +
            "data = json.loads(raw) if raw else None\n" +
            "if not data:\n" +
            "    print('Status: 400 Bad Request')\n" +
            "    print()\n" +
            "    raise SystemExit\n" +
            "\n" +
            "app, title, text, time = data['app'], data['title'], data['text'], data['time']"
    );
    t8b.setTextColor(0xFFD6D6F0);
    t8b.setTextSize(12);
    t8b.setTypeface(Typeface.MONOSPACE);
    t8b.setBackground(makeRoundBg(FIELD_BG, 12));
    t8b.setPadding(22, 18, 22, 18);
    t8b.setLineSpacing(0, 1.1f);
    t8b.setTextIsSelectable(true);
    LinearLayout.LayoutParams lp8 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    lp8.setMargins(0, 14, 0, 0);
    t8b.setLayoutParams(lp8);
    root.addView(t8b);
    
    scroll.addView(root);

    new AlertDialog.Builder(this)
            .setTitle("Notif2Nas — Aide")
            .setView(scroll)
            .setPositiveButton("OK", null)
            .show();
    }

    private void sendTest(String urlStr, String token, String pkg) {
        String json = "{\"app\":\"" + pkg + "\","
                + "\"title\":\"Test Notif2Nas\","
                + "\"text\":\"Ceci est une notification de test réussi\","
                + "\"time\":" + (System.currentTimeMillis() / 1000) + "}";
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
