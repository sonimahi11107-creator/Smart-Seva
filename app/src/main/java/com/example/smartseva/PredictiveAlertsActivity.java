package com.example.smartseva;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class PredictiveAlertsActivity extends AppCompatActivity {

    LinearLayout layoutPredictions;
    TextView tvPredictionCount, tvLocation;
    Button btnRefreshPredictions;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predictive_alerts);

        prefs = getSharedPreferences("SmartSeva", MODE_PRIVATE);

        layoutPredictions    = findViewById(R.id.layoutPredictions);
        tvPredictionCount    = findViewById(R.id.tvPredictionCount);
        tvLocation           = findViewById(R.id.tvLocation);
        btnRefreshPredictions= findViewById(R.id.btnRefreshPredictions);

        findViewById(R.id.btnBackPredictive)
                .setOnClickListener(v -> finish());

        btnRefreshPredictions.setOnClickListener(v -> {
            loadPredictions();
            Toast.makeText(this,
                    "Predictions refreshed! ✅",
                    Toast.LENGTH_SHORT).show();
        });

        loadPredictions();
    }

    void loadPredictions() {
        String location = prefs.getString("orgCity",
                prefs.getString("city", "Raipur, Chhattisgarh"));
        tvLocation.setText("📍 " + location);

        List<PredictiveAlertEngine.Prediction> predictions =
                PredictiveAlertEngine.generatePredictions(
                        this, location);

        tvPredictionCount.setText(
                predictions.size() + " predictions");

        layoutPredictions.removeAllViews();

        // Send top critical notification
        for (PredictiveAlertEngine.Prediction p : predictions) {
            if ("Critical".equals(p.urgency)) {
                NotificationHelper.notifyPredictiveAlert(
                        this, p.title, p.description);
                break;
            }
        }

        for (PredictiveAlertEngine.Prediction p : predictions) {
            addPredictionCard(p);
        }
    }

    void addPredictionCard(PredictiveAlertEngine.Prediction p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(36, 28, 36, 28);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cp);

        // Top row — icon + urgency
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trp =
                new LinearLayout.LayoutParams(-1, -2);
        trp.setMargins(0, 0, 0, 10);
        topRow.setLayoutParams(trp);

        TextView icon = new TextView(this);
        icon.setText(p.icon);
        icon.setTextSize(28f);
        LinearLayout.LayoutParams ip =
                new LinearLayout.LayoutParams(-2, -2);
        ip.setMargins(0, 0, 14, 0);
        icon.setLayoutParams(ip);
        topRow.addView(icon);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tcp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        titleCol.setLayoutParams(tcp);

        TextView title = new TextView(this);
        title.setText(p.title);
        title.setTextSize(15f);
        title.setTextColor(Color.parseColor("#111827"));
        title.setTypeface(null, Typeface.BOLD);
        titleCol.addView(title);

        TextView category = new TextView(this);
        category.setText(p.category);
        category.setTextSize(11f);
        category.setTextColor(Color.parseColor("#6B7280"));
        titleCol.addView(category);
        topRow.addView(titleCol);

        // Urgency badge
        int badgeBg = "Critical".equals(p.urgency)
                ? Color.parseColor("#FEE2E2")
                : "Moderate".equals(p.urgency)
                ? Color.parseColor("#FEF9C3")
                : Color.parseColor("#DCFCE7");
        int badgeText = "Critical".equals(p.urgency)
                ? Color.parseColor("#991B1B")
                : "Moderate".equals(p.urgency)
                ? Color.parseColor("#854D0E")
                : Color.parseColor("#166534");

        TextView urgency = new TextView(this);
        urgency.setText(p.urgency);
        urgency.setTextSize(10f);
        urgency.setTextColor(badgeText);
        urgency.setBackgroundColor(badgeBg);
        urgency.setPadding(16, 6, 16, 6);
        topRow.addView(urgency);
        card.addView(topRow);

        // Description
        TextView desc = new TextView(this);
        desc.setText(p.description);
        desc.setTextSize(13f);
        desc.setTextColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams dp =
                new LinearLayout.LayoutParams(-1, -2);
        dp.setMargins(0, 0, 0, 14);
        desc.setLayoutParams(dp);
        card.addView(desc);

        // Confidence bar
        TextView confLabel = new TextView(this);
        confLabel.setText("Confidence: " + p.confidence + "%");
        confLabel.setTextSize(11f);
        confLabel.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams clp =
                new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, 0, 0, 6);
        confLabel.setLayoutParams(clp);
        card.addView(confLabel);

        // Progress bar
        FrameLayout barBg = new FrameLayout(this);
        barBg.setBackgroundColor(Color.parseColor("#F3F4F6"));
        LinearLayout.LayoutParams bgp =
                new LinearLayout.LayoutParams(-1, 12);
        bgp.setMargins(0, 0, 0, 12);
        barBg.setLayoutParams(bgp);

        View fill = new View(this);
        int fillColor = "Critical".equals(p.urgency)
                ? Color.parseColor("#EF4444")
                : "Moderate".equals(p.urgency)
                ? Color.parseColor("#F59E0B")
                : Color.parseColor("#10B981");
        fill.setBackgroundColor(fillColor);
        int maxW = getResources().getDisplayMetrics().widthPixels - 144;
        FrameLayout.LayoutParams fp =
                new FrameLayout.LayoutParams(
                        (int)(maxW * p.confidence / 100f), -1);
        fill.setLayoutParams(fp);
        barBg.addView(fill);
        card.addView(barBg);

        // Basis tag
        TextView basis = new TextView(this);
        basis.setText("📌 " + p.basis);
        basis.setTextSize(11f);
        basis.setTextColor(Color.parseColor("#9CA3AF"));
        LinearLayout.LayoutParams bap =
                new LinearLayout.LayoutParams(-1, -2);
        bap.setMargins(0, 0, 0, 14);
        basis.setLayoutParams(bap);
        card.addView(basis);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#F3F4F6"));
        LinearLayout.LayoutParams divp =
                new LinearLayout.LayoutParams(-1, 1);
        divp.setMargins(0, 0, 0, 14);
        div.setLayoutParams(divp);
        card.addView(div);

        // Create Task button
        Button btnCreate = new Button(this);
        btnCreate.setText("➕ Create Task for This Need");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
        btnCreate.setTextSize(13f);
        card.addView(btnCreate);

        btnCreate.setOnClickListener(v -> {
            // LocalTaskStore mein save karo
            LocalTaskStore.getInstance().addTask(
                    new LocalTaskStore.LocalTask(
                            p.title, p.description, p.category,
                            p.urgency, "Any Skill", "10",
                            prefs.getString("city", "Local Area")));

            btnCreate.setText("✅ Task Created!");
            btnCreate.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#2E7D32")));
            btnCreate.setEnabled(false);

            Toast.makeText(this,
                    "Task dashboard mein add ho gaya! ✅",
                    Toast.LENGTH_SHORT).show();
        });

        layoutPredictions.addView(card);
    }
}