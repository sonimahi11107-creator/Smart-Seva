package com.example.smartseva;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CreateTaskActivity extends AppCompatActivity {

    EditText etTitle, etDesc, etLocation, etSkills, etVolunteers;
    Spinner spinnerCategory, spinnerUrgency;
    Button btnSubmit, btnAiGenerate;
    TextView tvAiStatus;
    ProgressBar progressAi;

    // Gemini API key — BuildConfig se aayegi (safe)
    private static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY;
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=" + GEMINI_API_KEY;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler mainHandler     = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // ── Views ──
        etTitle         = findViewById(R.id.etTaskTitle);
        etDesc          = findViewById(R.id.etDescription);
        etLocation      = findViewById(R.id.etLocation);
        etSkills        = findViewById(R.id.etSkills);
        etVolunteers    = findViewById(R.id.etVolunteers);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUrgency  = findViewById(R.id.spinnerUrgency);
        btnSubmit       = findViewById(R.id.btnSubmitTask);
        btnAiGenerate   = findViewById(R.id.btnAiGenerate);
        tvAiStatus      = findViewById(R.id.tvAiStatus);
        progressAi      = findViewById(R.id.progressAi);

        // ── Spinners ──
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Education", "Health", "Food", "Environment",
                        "Disaster Relief", "Animal Welfare", "General"});
        catAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> urgAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Low", "Moderate", "Critical"});
        urgAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinnerUrgency.setAdapter(urgAdapter);

        // ── AI Generate Button ──
        btnAiGenerate.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) {
                Toast.makeText(this,
                        "Pehle task title likho!",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            generateWithGemini(title);
        });

        // ── Submit Button ──
        btnSubmit.setOnClickListener(v -> {
            String title    = etTitle.getText().toString().trim();
            String desc     = etDesc.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String skills   = etSkills.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this,
                        "Fill all fields",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            LocalTaskStore.LocalTask task = new LocalTaskStore.LocalTask(
                    title,
                    desc,
                    spinnerCategory.getSelectedItem().toString(),
                    spinnerUrgency.getSelectedItem().toString(),
                    skills,
                    etVolunteers.getText().toString().isEmpty()
                            ? "1" : etVolunteers.getText().toString(),
                    location
            );
            LocalTaskStore.getInstance().addTask(task);

            Toast.makeText(this, "Task created! ✅", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(
                    CreateTaskActivity.this, DashboardActivity.class);
            intent.putExtra("showTasks", true);
            startActivity(intent);
            finish();
        });
    }

    // ═══════════════════════════════════════════
    // GEMINI AI INTEGRATION
    // ═══════════════════════════════════════════

    void generateWithGemini(String title) {
        // UI — loading state
        btnAiGenerate.setEnabled(false);
        btnAiGenerate.setText("Generating...");
        progressAi.setVisibility(View.VISIBLE);
        tvAiStatus.setVisibility(View.VISIBLE);
        tvAiStatus.setText("🤖 AI soch raha hai...");

        String prompt =
                "You are an NGO task assistant for SmartSeva, an Indian volunteer platform.\n\n" +
                        "Task Title: \"" + title + "\"\n\n" +
                        "Generate a JSON response with these exact fields:\n" +
                        "{\n" +
                        "  \"description\": \"2-3 line task description in simple English\",\n" +
                        "  \"category\": \"one of: Education, Health, Food, Environment, Disaster Relief, Animal Welfare, General\",\n" +
                        "  \"urgency\": \"one of: Low, Moderate, Critical\",\n" +
                        "  \"skills\": \"comma separated skills needed e.g. Medical Help, Teaching\",\n" +
                        "  \"volunteers\": \"number between 1-20\"\n" +
                        "}\n\n" +
                        "Return ONLY the JSON, no extra text.";

        executor.execute(() -> {
            try {
                // Build request body
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

                // HTTP call
                URL url = new URL(GEMINI_URL);
                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody.toString()
                            .getBytes(StandardCharsets.UTF_8));
                }

                // Read response
                Scanner scanner;
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    scanner = new Scanner(conn.getInputStream());
                } else {
                    scanner = new Scanner(conn.getErrorStream());
                }

                StringBuilder sb = new StringBuilder();
                while (scanner.hasNextLine()) {
                    sb.append(scanner.nextLine());
                }
                scanner.close();

                // FIX: response PEHLE declare karo — phir check karo
                JSONObject response = new JSONObject(sb.toString());

                // FIX: Sirf EK candidates check — duplicates hata diye
                if (!response.has("candidates")) {
                    String errMsg = response.has("error")
                            ? response.getJSONObject("error")
                            .optString("message", "API Error")
                            : "Invalid API Key ya quota exceed. Response: " + sb;
                    throw new Exception(errMsg);
                }

                // Parse Gemini response
                String rawText = response
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim();

                // Clean JSON (remove markdown if any)
                if (rawText.startsWith("```")) {
                    rawText = rawText
                            .replaceAll("```json", "")
                            .replaceAll("```", "")
                            .trim();
                }

                JSONObject aiResult = new JSONObject(rawText);

                final String desc       = aiResult.optString("description", "");
                final String category   = aiResult.optString("category",    "General");
                final String urgency    = aiResult.optString("urgency",     "Low");
                final String skills     = aiResult.optString("skills",      "");
                final String volunteers = aiResult.optString("volunteers",  "5");

                // Update UI on main thread
                mainHandler.post(() -> {
                    etDesc.setText(desc);
                    etSkills.setText(skills);
                    etVolunteers.setText(volunteers);

                    setSpinnerValue(spinnerCategory, category);
                    setSpinnerValue(spinnerUrgency, urgency);

                    btnAiGenerate.setEnabled(true);
                    btnAiGenerate.setText("🤖 Generate with AI");
                    progressAi.setVisibility(View.GONE);
                    tvAiStatus.setText("✅ AI ne form fill kar diya! Review karo.");
                    tvAiStatus.setTextColor(
                            android.graphics.Color.parseColor("#00C853"));

                    Toast.makeText(CreateTaskActivity.this,
                            "AI ne task details generate kar di! ✨",
                            Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    btnAiGenerate.setEnabled(true);
                    btnAiGenerate.setText("🤖 Generate with AI");
                    progressAi.setVisibility(View.GONE);
                    tvAiStatus.setText("❌ Error: " + e.getMessage());
                    tvAiStatus.setTextColor(
                            android.graphics.Color.parseColor("#FF1744"));
                });
            }
        });
    }

    // Spinner mein value set karo by text
    void setSpinnerValue(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).toString()
                    .equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}