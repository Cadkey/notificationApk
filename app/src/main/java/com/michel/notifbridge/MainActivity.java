package com.michel.notifbridge;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);

        // Titre
        TextView title = new TextView(this);
        title.setText("NotifBridge");
        title.setTextSize(22);
        title.setPadding(0, 0, 0, 24);
        root.addView(title);

        // Bouton accès notifications
        Button btnAccess = new Button(this);
        btnAccess.setText("Accès aux notifications");
        btnAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(btnAccess);

        // Séparateur
        TextView sep = new TextView(this);
        sep.setText("\nRègles configurées :");
        sep.setTextSize(16);
        root.addView(sep);

        // Liste des règles existantes
        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);
        refreshList();

        // Séparateur
        TextView sep2 = new TextView(this);
        sep2.setText("\nAjouter une règle :");
        sep2.setTextSize(16);
        root.addView(sep2);

        // Champs saisie
        EditText etPkg = new EditText(this);
        etPkg.setHint("Package (ex: com.hyundai.oneapp.eu)");
        root.addView(etPkg);

        EditText etUrl = new EditText(this);
        etUrl.setHint("URL PHP (https://...)");
        root.addView(etUrl);

        EditText etToken = new EditText(this);
        etToken.setHint("Token (optionnel)");
        root.addView(etToken);

        Button btnAdd = new Button(this);
        btnAdd.setText("Ajouter");
        btnAdd.setOnClickListener(v -> {
            String pkg = etPkg.getText().toString().trim();
            String url = etUrl.getText().toString().trim();
            String token = etToken.getText().toString().trim();
            if (pkg.isEmpty() || url.isEmpty()) {
                Toast.makeText(this, "Package et URL obligatoires", Toast.LENGTH_SHORT).show();
                return;
            }
            int count = prefs.getInt("count", 0);
            prefs.edit()
                    .putString("pkg_" + count, pkg)
                    .putString("url_" + count, url)
                    .putString("token_" + count, token)
                    .putInt("count", count + 1)
                    .apply();
            etPkg.setText("");
            etUrl.setText("");
            etToken.setText("");
            refreshList();
            Toast.makeText(this, "Règle ajoutée", Toast.LENGTH_SHORT).show();
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
            empty.setText("Aucune règle.");
            listContainer.addView(empty);
            return;
        }
        for (int i = 0; i < count; i++) {
            final int idx = i;
            String pkg = prefs.getString("pkg_" + i, "");
            String url = prefs.getString("url_" + i, "");
            String token = prefs.getString("token_" + i, "");

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, 16, 0, 8);

            EditText ePkg = new EditText(this);
            ePkg.setText(pkg);
            ePkg.setHint("Package");
            row.addView(ePkg);

            EditText eUrl = new EditText(this);
            eUrl.setText(url);
            eUrl.setHint("URL PHP");
            row.addView(eUrl);

            EditText eToken = new EditText(this);
            eToken.setText(token);
            eToken.setHint("Token");
            row.addView(eToken);

            LinearLayout btnRow = new LinearLayout(this);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);

            Button btnSave = new Button(this);
            btnSave.setText("Sauver");
            btnSave.setOnClickListener(v -> {
                prefs.edit()
                        .putString("pkg_" + idx, ePkg.getText().toString().trim())
                        .putString("url_" + idx, eUrl.getText().toString().trim())
                        .putString("token_" + idx, eToken.getText().toString().trim())
                        .apply();
                Toast.makeText(this, "Sauvegardé", Toast.LENGTH_SHORT).show();
            });
            btnRow.addView(btnSave);

            Button btnDel = new Button(this);
            btnDel.setText("Supprimer");
            btnDel.setOnClickListener(v -> {
                deleteRule(idx);
                refreshList();
            });
            btnRow.addView(btnDel);
            row.addView(btnRow);

            TextView divider = new TextView(this);
            divider.setText("───────────────────");
            row.addView(divider);

            listContainer.addView(row);
        }
    }

    private void deleteRule(int idx) {
        int count = prefs.getInt("count", 0);
        SharedPreferences.Editor ed = prefs.edit();
        for (int i = idx; i < count - 1; i++) {
            ed.putString("pkg_" + i, prefs.getString("pkg_" + (i + 1), ""));
            ed.putString("url_" + i, prefs.getString("url_" + (i + 1), ""));
            ed.putString("token_" + i, prefs.getString("token_" + (i + 1), ""));
        }
        ed.remove("pkg_" + (count - 1));
        ed.remove("url_" + (count - 1));
        ed.remove("token_" + (count - 1));
        ed.putInt("count", count - 1);
        ed.apply();
    }
}
