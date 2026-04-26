package com.example.smartseva;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.List;
import com.example.smartseva.BuildConfig;

public class PredictiveAlertsActivity extends AppCompatActivity {

    // Tabs
    Button btnTabPredictions, btnTabWeekly;
    ScrollView panelPredictions, panelWeekly;

    // Predictions
    LinearLayout layoutPredictions, layoutWeekly;
    TextView tvPredictionCount, tvLocation, tvWeatherInfo;
    Button btnRefresh;

    // Weather data
    String weatherCondition = "";
    double temperature = 30.0;
    boolean weatherLoaded = false;

    SharedPreferences prefs;
    String userLocation = "Raipur, Chhattisgarh";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predictive_alerts);

        prefs        = getSharedPreferences("SmartSeva", MODE_PRIVATE);
        userLocation = prefs.getString("city",
                prefs.getString("orgCity", "Raipur"));

        bindViews();
        setListeners();
        fetchWeatherAndLoad();
    }

    void bindViews() {
        btnTabPredictions = findViewById(R.id.btnTabPredictions);
        btnTabWeekly      = findViewById(R.id.btnTabWeekly);
        tvPredictionCount = findViewById(R.id.tvPredictionCount);
        tvLocation        = findViewById(R.id.tvLocation);
        tvWeatherInfo     = findViewById(R.id.tvWeatherInfo);
        btnRefresh        = findViewById(R.id.btnRefreshPredictions);
        layoutPredictions = findViewById(R.id.layoutPredictions);
        layoutWeekly      = findViewById(R.id.layoutWeekly);

        // ScrollView alag se
        panelPredictions = findViewById(R.id.panelPredictions);
        panelWeekly      = findViewById(R.id.panelWeekly);
    }

    void setListeners() {
        findViewById(R.id.btnBackPredictive)
                .setOnClickListener(v -> finish());

        btnRefresh.setOnClickListener(v -> {
            fetchWeatherAndLoad();
            Toast.makeText(this, "Refreshing... ✅",
                    Toast.LENGTH_SHORT).show();
        });

        btnTabPredictions.setOnClickListener(v ->
                switchTab("predictions"));
        btnTabWeekly.setOnClickListener(v ->
                switchTab("weekly"));
    }

    void switchTab(String tab) {
        boolean isPred = tab.equals("predictions");
        panelPredictions.setVisibility(
                isPred ? View.VISIBLE : View.GONE);
        panelWeekly.setVisibility(
                isPred ? View.GONE : View.VISIBLE);

        btnTabPredictions.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor(
                        isPred ? "#1A1A2E" : "#F3F4F6")));
        btnTabPredictions.setTextColor(Color.parseColor(
                isPred ? "#FFFFFF" : "#6B7280"));

        btnTabWeekly.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor(
                        isPred ? "#F3F4F6" : "#1A1A2E")));
        btnTabWeekly.setTextColor(Color.parseColor(
                isPred ? "#6B7280" : "#FFFFFF"));

        if (!isPred) loadWeeklyForecast();
    }

    // ── WEATHER FETCH ─────────────────────────────────────

    void fetchWeatherAndLoad() {
        tvWeatherInfo.setText("🌤 Loading predictions...");
        tvLocation.setText("📍 " + userLocation);

        new Thread(() -> {
            try {
                String apiKey = BuildConfig.WEATHER_API_KEY;

                // Agar API key empty ya demo hai
                if (apiKey.isEmpty() || apiKey.equals("demo")
                        || apiKey.equals("TUMHARI_API_KEY_YAHAN")) {
                    runOnUiThread(() -> {
                        tvWeatherInfo.setText(
                                "🌤 Seasonal predictions (no weather API)");
                        loadPredictions();
                    });
                    return;
                }

                String url =
                        "https://api.openweathermap.org/data/2.5/weather"
                                + "?q=" + java.net.URLEncoder.encode(
                                userLocation, "UTF-8")
                                + "&appid=" + apiKey
                                + "&units=metric";

                java.net.HttpURLConnection conn =
                        (java.net.HttpURLConnection)
                                new java.net.URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                java.io.BufferedReader reader =
                        new java.io.BufferedReader(
                                new java.io.InputStreamReader(
                                        conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line);

                org.json.JSONObject json =
                        new org.json.JSONObject(sb.toString());
                temperature = json.getJSONObject("main")
                        .getDouble("temp");
                weatherCondition = json.getJSONArray("weather")
                        .getJSONObject(0).getString("main");

                runOnUiThread(() -> {
                    tvWeatherInfo.setText(
                            "🌡️ " + (int)temperature + "°C  •  "
                                    + weatherCondition + "  •  "
                                    + userLocation);
                    loadPredictions();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvWeatherInfo.setText(
                            "🌤 Seasonal predictions active");
                    loadPredictions();
                });
            }
        }).start();
    }

    // ── LOAD PREDICTIONS ──────────────────────────────────

    void loadPredictions() {
        List<PredictiveAlertEngine.Prediction> predictions =
                PredictiveAlertEngine.generatePredictions(
                        userLocation, weatherCondition, temperature);

        tvPredictionCount.setText(
                predictions.size() + " predictions");

        layoutPredictions.removeAllViews();

        // Notify top critical
        for (PredictiveAlertEngine.Prediction p : predictions) {
            if ("Critical".equals(p.urgency)) {
                NotificationHelper.notifyPredictiveAlert(
                        this, p.title, p.description);
                break;
            }
        }

        for (PredictiveAlertEngine.Prediction p : predictions)
            addPredictionCard(layoutPredictions, p, false);
    }

    // ── WEEKLY FORECAST ───────────────────────────────────

    void loadWeeklyForecast() {
        List<PredictiveAlertEngine.Prediction> weekly =
                PredictiveAlertEngine.generateWeeklyForecast(
                        userLocation);

        layoutWeekly.removeAllViews();
        for (PredictiveAlertEngine.Prediction p : weekly)
            addPredictionCard(layoutWeekly, p, true);
    }

    // ── PREDICTION CARD ───────────────────────────────────

    void addPredictionCard(LinearLayout parent,
                           PredictiveAlertEngine.Prediction p,
                           boolean isWeekly) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(36, 28, 36, 28);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cp);

        // Day label for weekly
        if (isWeekly) {
            TextView dayLbl = new TextView(this);
            dayLbl.setText(p.dayLabel);
            dayLbl.setTextSize(11f);
            dayLbl.setTextColor(Color.WHITE);
            dayLbl.setBackgroundColor(
                    Color.parseColor("#1A1A2E"));
            dayLbl.setPadding(20, 6, 20, 6);
            LinearLayout.LayoutParams dlp =
                    new LinearLayout.LayoutParams(-2, -2);
            dlp.setMargins(0, 0, 0, 12);
            dayLbl.setLayoutParams(dlp);
            card.addView(dayLbl);
        }

        // Top row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trp =
                new LinearLayout.LayoutParams(-1, -2);
        trp.setMargins(0, 0, 0, 10);
        topRow.setLayoutParams(trp);

        // Icon
        TextView icon = new TextView(this);
        icon.setText(p.icon);
        icon.setTextSize(30f);
        LinearLayout.LayoutParams ip =
                new LinearLayout.LayoutParams(-2, -2);
        ip.setMargins(0, 0, 14, 0);
        icon.setLayoutParams(ip);
        topRow.addView(icon);

        // Title + category
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

        TextView cat = new TextView(this);
        cat.setText(p.category);
        cat.setTextSize(11f);
        cat.setTextColor(Color.parseColor("#6B7280"));
        titleCol.addView(cat);
        topRow.addView(titleCol);

        // Urgency badge
        int bgColor = "Critical".equals(p.urgency)
                ? Color.parseColor("#FEE2E2")
                : "Moderate".equals(p.urgency)
                ? Color.parseColor("#FEF9C3")
                : Color.parseColor("#DCFCE7");
        int txColor = "Critical".equals(p.urgency)
                ? Color.parseColor("#991B1B")
                : "Moderate".equals(p.urgency)
                ? Color.parseColor("#854D0E")
                : Color.parseColor("#166534");

        TextView urg = new TextView(this);
        urg.setText(p.urgency);
        urg.setTextSize(10f);
        urg.setTextColor(txColor);
        urg.setBackgroundColor(bgColor);
        urg.setPadding(16, 6, 16, 6);
        topRow.addView(urg);
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

        // Confidence
        TextView confLbl = new TextView(this);
        confLbl.setText("Confidence: " + p.confidence + "%");
        confLbl.setTextSize(11f);
        confLbl.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams clp =
                new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, 0, 0, 6);
        confLbl.setLayoutParams(clp);
        card.addView(confLbl);

        // Progress bar
        FrameLayout barBg = new FrameLayout(this);
        barBg.setBackgroundColor(Color.parseColor("#F3F4F6"));
        LinearLayout.LayoutParams bgp =
                new LinearLayout.LayoutParams(-1, 14);
        bgp.setMargins(0, 0, 0, 12);
        barBg.setLayoutParams(bgp);

        View fill = new View(this);
        fill.setBackgroundColor(
                "Critical".equals(p.urgency)
                        ? Color.parseColor("#EF4444")
                        : "Moderate".equals(p.urgency)
                        ? Color.parseColor("#F59E0B")
                        : Color.parseColor("#10B981"));

        int maxW = getResources().getDisplayMetrics()
                .widthPixels - 144;
        FrameLayout.LayoutParams fp =
                new FrameLayout.LayoutParams(
                        (int)(maxW * p.confidence / 100f), -1);
        fill.setLayoutParams(fp);
        barBg.addView(fill);
        card.addView(barBg);

        // Basis
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
        card.addView(div);
        LinearLayout.LayoutParams divp =
                new LinearLayout.LayoutParams(-1, 1);
        divp.setMargins(0, 0, 0, 14);
        div.setLayoutParams(divp);

        // Create Task button
        Button btnCreate = new Button(this);
        btnCreate.setText("➕ Create Task for This Need");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#1A1A2E")));
        btnCreate.setTextSize(13f);
        card.addView(btnCreate);

        btnCreate.setOnClickListener(v -> {
            LocalTaskStore.getInstance().addTask(
                    new LocalTaskStore.LocalTask(
                            p.title, p.description, p.category,
                            p.urgency, "Any Skill", "10",
                            userLocation));

            btnCreate.setText("✅ Task Created!");
            btnCreate.setBackgroundTintList(
                    ColorStateList.valueOf(
                            Color.parseColor("#2E7D32")));
            btnCreate.setEnabled(false);

            NotificationHelper.notifyNewTask(
                    this, p.title);

            Toast.makeText(this,
                    "Task added to dashboard! ✅",
                    Toast.LENGTH_SHORT).show();
        });

        parent.addView(card);
    }
}