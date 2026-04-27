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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PredictiveAlertsActivity extends AppCompatActivity {

    Button btnTabPredictions, btnTabWeekly, btnTabAiInsight;
    ScrollView panelPredictions, panelWeekly, panelAiInsight;
    LinearLayout layoutPredictions, layoutWeekly;
    TextView tvPredictionCount, tvLocation, tvWeatherInfo;
    Button btnRefresh;
    TextView tvAiInsightText;
    ProgressBar progressAiInsight;
    Button btnGenerateAiInsight;

    String weatherCondition = "";
    double temperature = 30.0;
    SharedPreferences prefs;
    String userLocation = "Raipur, Chhattisgarh";

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=" + BuildConfig.GEMINI_API_KEY;

    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_predictive_alerts);
        prefs = getSharedPreferences("SmartSeva", MODE_PRIVATE);
        userLocation = prefs.getString("city", prefs.getString("orgCity", "Raipur"));
        bindViews();
        setListeners();
        fetchWeatherAndLoad();
    }

    void bindViews() {
        btnTabPredictions    = findViewById(R.id.btnTabPredictions);
        btnTabWeekly         = findViewById(R.id.btnTabWeekly);
        btnTabAiInsight      = findViewById(R.id.btnTabAiInsight);
        tvPredictionCount    = findViewById(R.id.tvPredictionCount);
        tvLocation           = findViewById(R.id.tvLocation);
        tvWeatherInfo        = findViewById(R.id.tvWeatherInfo);
        btnRefresh           = findViewById(R.id.btnRefreshPredictions);
        layoutPredictions    = findViewById(R.id.layoutPredictions);
        layoutWeekly         = findViewById(R.id.layoutWeekly);
        panelPredictions     = findViewById(R.id.panelPredictions);
        panelWeekly          = findViewById(R.id.panelWeekly);
        panelAiInsight       = findViewById(R.id.panelAiInsight);
        tvAiInsightText      = findViewById(R.id.tvAiInsightText);
        progressAiInsight    = findViewById(R.id.progressAiInsight);
        btnGenerateAiInsight = findViewById(R.id.btnGenerateAiInsight);
    }

    void setListeners() {
        findViewById(R.id.btnBackPredictive).setOnClickListener(v -> finish());
        btnRefresh.setOnClickListener(v -> {
            fetchWeatherAndLoad();
            Toast.makeText(this, "Refreshing... ", Toast.LENGTH_SHORT).show();
        });
        btnTabPredictions.setOnClickListener(v -> switchTab("predictions"));
        btnTabWeekly.setOnClickListener(v -> switchTab("weekly"));
        btnTabAiInsight.setOnClickListener(v -> switchTab("ai"));
        btnGenerateAiInsight.setOnClickListener(v -> generateAiInsight());
    }

    void switchTab(String tab) {
        panelPredictions.setVisibility(tab.equals("predictions") ? View.VISIBLE : View.GONE);
        panelWeekly.setVisibility(tab.equals("weekly") ? View.VISIBLE : View.GONE);
        panelAiInsight.setVisibility(tab.equals("ai") ? View.VISIBLE : View.GONE);
        setTabActive(btnTabPredictions, tab.equals("predictions"));
        setTabActive(btnTabWeekly, tab.equals("weekly"));
        setTabActive(btnTabAiInsight, tab.equals("ai"));
        if (tab.equals("weekly")) loadWeeklyForecast();
    }

    void setTabActive(Button btn, boolean active) {
        btn.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor(active ? "#1A1A2E" : "#F3F4F6")));
        btn.setTextColor(Color.parseColor(active ? "#FFFFFF" : "#6B7280"));
    }

    void fetchWeatherAndLoad() {
        tvWeatherInfo.setText("Loading predictions...");
        tvLocation.setText("  " + userLocation);
        new Thread(() -> {
            try {
                String apiKey = BuildConfig.WEATHER_API_KEY;
                if (apiKey.isEmpty() || apiKey.equals("demo")) {
                    runOnUiThread(() -> {
                        tvWeatherInfo.setText("Seasonal predictions active");
                        loadPredictions();
                    });
                    return;
                }
                String url = "https://api.openweathermap.org/data/2.5/weather"
                        + "?q=" + URLEncoder.encode(userLocation, "UTF-8")
                        + "&appid=" + apiKey + "&units=metric";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                JSONObject json = new JSONObject(sb.toString());
                temperature = json.getJSONObject("main").getDouble("temp");
                weatherCondition = json.getJSONArray("weather").getJSONObject(0).getString("main");
                runOnUiThread(() -> {
                    tvWeatherInfo.setText((int) temperature + "C  " + weatherCondition + "  " + userLocation);
                    loadPredictions();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    tvWeatherInfo.setText("Seasonal predictions active");
                    loadPredictions();
                });
            }
        }).start();
    }

    void loadPredictions() {
        List<PredictiveAlertEngine.Prediction> predictions =
                PredictiveAlertEngine.generatePredictions(userLocation, weatherCondition, temperature);
        tvPredictionCount.setText(predictions.size() + " predictions");
        layoutPredictions.removeAllViews();
        for (PredictiveAlertEngine.Prediction p : predictions) {
            if ("Critical".equals(p.urgency)) {
                NotificationHelper.notifyPredictiveAlert(this, p.title, p.description);
                break;
            }
        }
        for (PredictiveAlertEngine.Prediction p : predictions)
            addPredictionCard(layoutPredictions, p, false);
    }

    void loadWeeklyForecast() {
        List<PredictiveAlertEngine.Prediction> weekly =
                PredictiveAlertEngine.generateWeeklyForecast(userLocation);
        layoutWeekly.removeAllViews();
        for (PredictiveAlertEngine.Prediction p : weekly)
            addPredictionCard(layoutWeekly, p, true);
    }

    void generateAiInsight() {
        btnGenerateAiInsight.setEnabled(false);
        btnGenerateAiInsight.setText("Generating...");
        progressAiInsight.setVisibility(View.VISIBLE);
        tvAiInsightText.setText("AI analyzing your area...");

        List<PredictiveAlertEngine.Prediction> predictions =
                PredictiveAlertEngine.generatePredictions(userLocation, weatherCondition, temperature);

        StringBuilder predContext = new StringBuilder();
        for (int i = 0; i < Math.min(predictions.size(), 5); i++) {
            PredictiveAlertEngine.Prediction p = predictions.get(i);
            predContext.append("- ").append(p.title)
                    .append(" (").append(p.urgency).append(", ")
                    .append(p.confidence).append("% confidence)\n");
        }

        List<LocalTaskStore.LocalTask> tasks = LocalTaskStore.getInstance().getTasks();
        StringBuilder taskContext = new StringBuilder();
        for (LocalTaskStore.LocalTask t : tasks)
            taskContext.append("- ").append(t.title).append(" [").append(t.category).append("]\n");

        int month = Calendar.getInstance().get(Calendar.MONTH);
        String[] months = {"January","February","March","April","May","June",
                "July","August","September","October","November","December"};

        String prompt = "You are an AI assistant for SmartSeva, an Indian NGO volunteer platform.\n\n"
                + "Location: " + userLocation + "\n"
                + "Current Month: " + months[month] + "\n"
                + "Weather: " + (int) temperature + "C, " + weatherCondition + "\n\n"
                + "Current Predictions:\n" + predContext + "\n"
                + "Past Tasks:\n" + (taskContext.length() > 0 ? taskContext : "No past tasks yet\n") + "\n"
                + "Please provide:\n"
                + "1. TOP 3 most critical needs for this location right now (with reasons)\n"
                + "2. One unique prediction the rule-based system might have missed\n"
                + "3. Best time of week to organize volunteer activities\n"
                + "4. One actionable recommendation for the NGO\n\n"
                + "Keep response concise, practical and specific to India. Use emojis. Max 250 words.";

        executor.execute(() -> {
            try {
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                JSONArray parts = new JSONArray();
                parts.put(textPart);
                JSONObject content = new JSONObject();
                content.put("parts", parts);
                JSONArray contents = new JSONArray();
                contents.put(content);
                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contents);

                URL url = new URL(GEMINI_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                }

                Scanner scanner = conn.getResponseCode() == 200
                        ? new Scanner(conn.getInputStream())
                        : new Scanner(conn.getErrorStream());
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNextLine()) sb.append(scanner.nextLine());
                scanner.close();

                JSONObject response = new JSONObject(sb.toString());
                if (!response.has("candidates")) {
                    String err = response.has("error")
                            ? response.getJSONObject("error").optString("message", "API Error")
                            : "No response from AI";
                    throw new Exception(err);
                }

                String aiText = response.getJSONArray("candidates")
                        .getJSONObject(0).getJSONObject("content")
                        .getJSONArray("parts").getJSONObject(0)
                        .getString("text").trim();

                mainHandler.post(() -> {
                    tvAiInsightText.setText(aiText);
                    btnGenerateAiInsight.setEnabled(true);
                    btnGenerateAiInsight.setText("Regenerate AI Insight");
                    progressAiInsight.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvAiInsightText.setText("Error: " + e.getMessage());
                    btnGenerateAiInsight.setEnabled(true);
                    btnGenerateAiInsight.setText("Generate AI Insight");
                    progressAiInsight.setVisibility(View.GONE);
                });
            }
        });
    }

    void addPredictionCard(LinearLayout parent, PredictiveAlertEngine.Prediction p, boolean isWeekly) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(36, 28, 36, 28);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cp);

        if (isWeekly) {
            TextView dayLbl = new TextView(this);
            dayLbl.setText(p.dayLabel);
            dayLbl.setTextSize(11f);
            dayLbl.setTextColor(Color.WHITE);
            dayLbl.setBackgroundColor(Color.parseColor("#1A1A2E"));
            dayLbl.setPadding(20, 6, 20, 6);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(-2, -2);
            dlp.setMargins(0, 0, 0, 12);
            dayLbl.setLayoutParams(dlp);
            card.addView(dayLbl);
        }

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trp = new LinearLayout.LayoutParams(-1, -2);
        trp.setMargins(0, 0, 0, 10);
        topRow.setLayoutParams(trp);

        TextView icon = new TextView(this);
        icon.setText(p.icon);
        icon.setTextSize(30f);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-2, -2);
        ip.setMargins(0, 0, 14, 0);
        icon.setLayoutParams(ip);
        topRow.addView(icon);

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

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

        int bgColor = "Critical".equals(p.urgency) ? Color.parseColor("#FEE2E2")
                : "Moderate".equals(p.urgency) ? Color.parseColor("#FEF9C3")
                : Color.parseColor("#DCFCE7");
        int txColor = "Critical".equals(p.urgency) ? Color.parseColor("#991B1B")
                : "Moderate".equals(p.urgency) ? Color.parseColor("#854D0E")
                : Color.parseColor("#166534");

        TextView urg = new TextView(this);
        urg.setText(p.urgency);
        urg.setTextSize(10f);
        urg.setTextColor(txColor);
        urg.setBackgroundColor(bgColor);
        urg.setPadding(16, 6, 16, 6);
        topRow.addView(urg);
        card.addView(topRow);

        TextView desc = new TextView(this);
        desc.setText(p.description);
        desc.setTextSize(13f);
        desc.setTextColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.setMargins(0, 0, 0, 14);
        desc.setLayoutParams(dp);
        card.addView(desc);

        TextView confLbl = new TextView(this);
        confLbl.setText("Confidence: " + p.confidence + "%");
        confLbl.setTextSize(11f);
        confLbl.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-1, -2);
        clp.setMargins(0, 0, 0, 6);
        confLbl.setLayoutParams(clp);
        card.addView(confLbl);

        FrameLayout barBg = new FrameLayout(this);
        barBg.setBackgroundColor(Color.parseColor("#F3F4F6"));
        LinearLayout.LayoutParams bgp = new LinearLayout.LayoutParams(-1, 14);
        bgp.setMargins(0, 0, 0, 12);
        barBg.setLayoutParams(bgp);

        View fill = new View(this);
        fill.setBackgroundColor("Critical".equals(p.urgency) ? Color.parseColor("#EF4444")
                : "Moderate".equals(p.urgency) ? Color.parseColor("#F59E0B")
                : Color.parseColor("#10B981"));
        int maxW = getResources().getDisplayMetrics().widthPixels - 144;
        fill.setLayoutParams(new FrameLayout.LayoutParams((int)(maxW * p.confidence / 100f), -1));
        barBg.addView(fill);
        card.addView(barBg);

        TextView basis = new TextView(this);
        basis.setText("  " + p.basis);
        basis.setTextSize(11f);
        basis.setTextColor(Color.parseColor("#9CA3AF"));
        LinearLayout.LayoutParams bap = new LinearLayout.LayoutParams(-1, -2);
        bap.setMargins(0, 0, 0, 14);
        basis.setLayoutParams(bap);
        card.addView(basis);

        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#F3F4F6"));
        LinearLayout.LayoutParams divp = new LinearLayout.LayoutParams(-1, 1);
        divp.setMargins(0, 0, 0, 14);
        div.setLayoutParams(divp);
        card.addView(div);

        Button btnCreate = new Button(this);
        btnCreate.setText("Create Task for This Need");
        btnCreate.setTextColor(Color.WHITE);
        btnCreate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A2E")));
        btnCreate.setTextSize(13f);
        card.addView(btnCreate);

        btnCreate.setOnClickListener(v -> {
            LocalTaskStore.getInstance().addTask(new LocalTaskStore.LocalTask(
                    p.title, p.description, p.category, p.urgency, "Any Skill", "10", userLocation));
            btnCreate.setText("Task Created!");
            btnCreate.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#2E7D32")));
            btnCreate.setEnabled(false);
            NotificationHelper.notifyNewTask(this, p.title);
            Toast.makeText(this, "Task added to dashboard!", Toast.LENGTH_SHORT).show();
        });

        parent.addView(card);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}