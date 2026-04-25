package com.example.smartseva;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class DataCollectionActivity extends AppCompatActivity {

    // Tabs
    Button btnTabImage, btnTabCamera, btnTabPDF, btnTabText;
    LinearLayout panelImage, panelCamera, panelPDF, panelText;
    LinearLayout panelProcessing, panelResults;

    // Image
    ImageView imgSurveyPreview;
    Button btnPickGallery, btnAnalyzeImage;

    // Camera
    PreviewView cameraPreview;
    Button btnCaptureScan;
    TextView tvCameraStatus;
    ImageCapture imageCapture;

    // PDF
    LinearLayout layoutPDFPreview;
    TextView tvPDFName, tvPDFSize;
    Button btnPickPDF, btnAnalyzePDF;

    // Text
    EditText etManualText;
    Button btnAnalyzeText;

    // Results
    LinearLayout layoutNeedsList, layoutSuggestedTasks;
    TextView tvProcessingMsg;

    String currentTab = "image";
    Bitmap selectedBitmap = null;
    String extractedText = "";

    static final int REQ_GALLERY  = 302;
    static final int REQ_PDF      = 303;
    static final int REQ_CAMERA_PERM = 304;

    TextRecognizer latinRecognizer, hindiRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        // ML Kit — dono recognizers
        latinRecognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);
        hindiRecognizer = TextRecognition.getClient(
                new DevanagariTextRecognizerOptions.Builder().build());

        bindViews();
        setListeners();
    }

    void bindViews() {
        btnTabImage    = findViewById(R.id.btnTabImage);
        btnTabCamera   = findViewById(R.id.btnTabCamera);
        btnTabPDF      = findViewById(R.id.btnTabPDF);
        btnTabText     = findViewById(R.id.btnTabText);
        panelImage     = findViewById(R.id.panelImage);
        panelCamera    = findViewById(R.id.panelCamera);
        panelPDF       = findViewById(R.id.panelPDF);
        panelText      = findViewById(R.id.panelText);
        panelProcessing    = findViewById(R.id.panelProcessing);
        panelResults       = findViewById(R.id.panelResults);
        imgSurveyPreview   = findViewById(R.id.imgSurveyPreview);
        btnPickGallery     = findViewById(R.id.btnPickGallery);
        btnAnalyzeImage    = findViewById(R.id.btnAnalyzeImage);
        cameraPreview      = findViewById(R.id.cameraPreview);
        btnCaptureScan     = findViewById(R.id.btnCaptureScan);
        tvCameraStatus     = findViewById(R.id.tvCameraStatus);
        layoutPDFPreview   = findViewById(R.id.layoutPDFPreview);
        tvPDFName          = findViewById(R.id.tvPDFName);
        tvPDFSize          = findViewById(R.id.tvPDFSize);
        btnPickPDF         = findViewById(R.id.btnPickPDF);
        btnAnalyzePDF      = findViewById(R.id.btnAnalyzePDF);
        etManualText       = findViewById(R.id.etManualText);
        btnAnalyzeText     = findViewById(R.id.btnAnalyzeText);
        layoutNeedsList    = findViewById(R.id.layoutNeedsList);
        layoutSuggestedTasks = findViewById(R.id.layoutSuggestedTasks);
        tvProcessingMsg    = findViewById(R.id.tvProcessingMsg);
    }

    void setListeners() {
        findViewById(R.id.btnBackDC).setOnClickListener(v -> finish());

        btnTabImage.setOnClickListener(v -> switchTab("image"));
        btnTabCamera.setOnClickListener(v -> switchTab("camera"));
        btnTabPDF.setOnClickListener(v -> switchTab("pdf"));
        btnTabText.setOnClickListener(v -> switchTab("text"));

        btnPickGallery.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i, REQ_GALLERY);
        });

        btnAnalyzeImage.setOnClickListener(v -> {
            if (selectedBitmap == null) {
                Toast.makeText(this, "Pehle image select karo!", Toast.LENGTH_SHORT).show();
                return;
            }
            extractTextFromImage(selectedBitmap);
        });

        btnCaptureScan.setOnClickListener(v -> captureAndScan());

        btnPickPDF.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("application/pdf");
            startActivityForResult(i, REQ_PDF);
        });

        btnAnalyzePDF.setOnClickListener(v -> {
            if (extractedText.isEmpty()) {
                Toast.makeText(this, "Pehle PDF select karo!", Toast.LENGTH_SHORT).show();
                return;
            }
            analyzeExtractedText(extractedText);
        });

        btnAnalyzeText.setOnClickListener(v -> {
            String t = etManualText.getText().toString().trim();
            if (t.isEmpty()) {
                Toast.makeText(this, "Kuch text likho pehle!", Toast.LENGTH_SHORT).show();
                return;
            }
            analyzeExtractedText(t);
        });
    }

    // ── TAB SWITCHING ────────────────────────────────────

    void switchTab(String tab) {
        currentTab = tab;
        panelImage.setVisibility(tab.equals("image")   ? View.VISIBLE : View.GONE);
        panelCamera.setVisibility(tab.equals("camera") ? View.VISIBLE : View.GONE);
        panelPDF.setVisibility(tab.equals("pdf")       ? View.VISIBLE : View.GONE);
        panelText.setVisibility(tab.equals("text")     ? View.VISIBLE : View.GONE);
        panelResults.setVisibility(View.GONE);
        panelProcessing.setVisibility(View.GONE);

        Button[] tabs  = {btnTabImage, btnTabCamera, btnTabPDF, btnTabText};
        String[] names = {"image", "camera", "pdf", "text"};
        for (int i = 0; i < tabs.length; i++) {
            boolean active = names[i].equals(tab);
            tabs[i].setBackgroundTintList(ColorStateList.valueOf(
                    Color.parseColor(active ? "#1A1A1A" : "#FFFFFF")));
            tabs[i].setTextColor(Color.parseColor(
                    active ? "#FFFFFF" : "#888888"));
        }

        if (tab.equals("camera")) startCamera();
    }

    // ── CAMERA ───────────────────────────────────────────

    void startCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
            return;
        }

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                provider.unbindAll();
                provider.bindToLifecycle(this,
                        new androidx.camera.core.CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build(),
                        preview, imageCapture);

                tvCameraStatus.setText("✅ Camera ready — document pe point karo");
            } catch (Exception e) {
                tvCameraStatus.setText("Camera error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void captureAndScan() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera ready nahi hai!", Toast.LENGTH_SHORT).show();
            return;
        }
        tvCameraStatus.setText("📸 Capturing...");
        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap bmp = imageProxyToBitmap(image);
                        image.close();
                        if (bmp != null) {
                            tvCameraStatus.setText("✅ Captured! Analyzing...");
                            extractTextFromImage(bmp);
                        }
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        tvCameraStatus.setText("Capture failed: " + e.getMessage());
                    }
                });
    }

    Bitmap imageProxyToBitmap(ImageProxy image) {
        try {
            ImageProxy.PlaneProxy plane = image.getPlanes()[0];
            java.nio.ByteBuffer buffer = plane.getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) { return null; }
    }

    @Override
    public void onRequestPermissionsResult(int req, @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_CAMERA_PERM && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    // ── ACTIVITY RESULT ──────────────────────────────────

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;

        if (req == REQ_GALLERY) {
            try {
                Uri uri = data.getData();
                InputStream is = getContentResolver().openInputStream(uri);
                selectedBitmap = BitmapFactory.decodeStream(is);
                imgSurveyPreview.setImageBitmap(selectedBitmap);
                Toast.makeText(this, "✅ Image select ho gayi! Analyze dabao.", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { e.printStackTrace(); }

        } else if (req == REQ_PDF) {
            Uri uri = data.getData();
            layoutPDFPreview.setVisibility(View.VISIBLE);
            tvPDFName.setText("📄 " + getFileName(uri));
            extractedText = extractTextFromPDF(uri);
            tvPDFSize.setText(extractedText.length() + " characters extracted");
            Toast.makeText(this, "✅ PDF load ho gaya! Analyze dabao.", Toast.LENGTH_SHORT).show();
        }
    }

    // ── ML KIT OCR ───────────────────────────────────────

    void extractTextFromImage(Bitmap bitmap) {
        panelProcessing.setVisibility(View.VISIBLE);
        panelResults.setVisibility(View.GONE);
        tvProcessingMsg.setText("📖 Text extract ho raha hai...");

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // Latin pehle try karo
        latinRecognizer.process(image)
                .addOnSuccessListener(latinResult -> {
                    String latinText = latinResult.getText().trim();

                    // Hindi bhi try karo
                    hindiRecognizer.process(image)
                            .addOnSuccessListener(hindiResult -> {
                                String hindiText = hindiResult.getText().trim();
                                String combined = latinText + "\n" + hindiText;

                                if (combined.trim().isEmpty()) {
                                    tvProcessingMsg.setText("⚠️ Text nahi mila!");
                                    analyzeExtractedText("Image uploaded but text unclear.");
                                } else {
                                    tvProcessingMsg.setText("✅ Text mila! Analyzing...");
                                    analyzeExtractedText(combined);
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Hindi fail — sirf Latin use karo
                                if (latinText.isEmpty()) {
                                    panelProcessing.setVisibility(View.GONE);
                                    Toast.makeText(this, "OCR fail: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                } else {
                                    analyzeExtractedText(latinText);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    panelProcessing.setVisibility(View.GONE);
                    Toast.makeText(this, "OCR error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // ── PDF TEXT ─────────────────────────────────────────

    String extractTextFromPDF(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String cleaned = line
                        .replaceAll("[^\\x20-\\x7E\\u0900-\\u097F\n]", " ")
                        .trim();
                if (cleaned.length() > 2) sb.append(cleaned).append("\n");
            }
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    String getFileName(Uri uri) {
        try {
            android.database.Cursor c = getContentResolver()
                    .query(uri, null, null, null, null);
            int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
            c.moveToFirst();
            String name = c.getString(idx);
            c.close();
            return name != null ? name : "document.pdf";
        } catch (Exception e) { return "document.pdf"; }
    }

    // ── LOCAL AI ANALYSIS ────────────────────────────────

    void analyzeExtractedText(String text) {
        tvProcessingMsg.setText("🧠 Community needs analyze ho rahi hain...");
        panelProcessing.setVisibility(View.VISIBLE);

        new Thread(() -> {
            List<CommunityNeed> needs = extractNeeds(text);
            List<SuggestedTask>  tasks = generateTasks(needs, text);
            runOnUiThread(() -> {
                panelProcessing.setVisibility(View.GONE);
                showResults(needs, tasks);
            });
        }).start();
    }

    // ── NEED EXTRACTION ──────────────────────────────────

    List<CommunityNeed> extractNeeds(String text) {
        List<CommunityNeed> needs = new ArrayList<>();
        String l = text.toLowerCase();

        if (has(l,"food","hunger","meal","ration","distribute","bhookh",
                "khana","anaaj","rashan","bhojan","grocery","annapoorna"))
            needs.add(new CommunityNeed("Food & Nutrition","Critical",
                    num(text,"food|meal|families|log"),"🍽️"));

        if (has(l,"medical","sick","hospital","doctor","health","disease",
                "medicine","bimar","dawai","ilaj","swasth","nurse","clinic"))
            needs.add(new CommunityNeed("Medical Assistance","Critical",
                    num(text,"sick|patient|bimar|medical"),"🏥"));

        if (has(l,"school","education","children","student","bachche",
                "padhai","siksha","class","teacher","tuition","dropout","books"))
            needs.add(new CommunityNeed("Education Support","Moderate",
                    num(text,"children|student|bachche|kids"),"📚"));

        if (has(l,"water","pani","clean water","drinking","flood","sewage",
                "sanitation","toilet","hygiene","nalka","borewell"))
            needs.add(new CommunityNeed("Water & Sanitation","Critical",
                    "Entire community","💧"));

        if (has(l,"shelter","house","homeless","flood","disaster","relief",
                "ghar","barbaad","toot gaya","camp","tent","natural disaster"))
            needs.add(new CommunityNeed("Emergency Shelter","Critical",
                    num(text,"families|ghar|house|homeless"),"🏠"));

        if (has(l,"elderly","old","senior","budhape","buzurg","aged",
                "alone","akele","widowed","widow","pensioner"))
            needs.add(new CommunityNeed("Elderly Care","Moderate",
                    num(text,"elderly|old|buzurg|senior"),"👴"));

        if (has(l,"garbage","pollution","waste","clean","safai","kachra",
                "environment","tree","plantation","biodiversity","drainage"))
            needs.add(new CommunityNeed("Environmental Cleanup","Normal",
                    "Community area","🌱"));

        if (has(l,"women","mahila","girl","female","empowerment",
                "skill","sewing","stitching","self help","bachat group","shg"))
            needs.add(new CommunityNeed("Women Empowerment","Moderate",
                    "Women in area","👩"));

        if (has(l,"disabled","divyang","handicap","wheelchair","blind",
                "deaf","specially abled","viklang"))
            needs.add(new CommunityNeed("Disability Support","Moderate",
                    num(text,"disabled|divyang|viklang"),"♿"));

        if (has(l,"mental","depression","stress","anxiety","suicide",
                "manasik","counseling","therapy","psychological"))
            needs.add(new CommunityNeed("Mental Health Support","Critical",
                    "Affected individuals","🧠"));

        if (has(l,"unemployed","job","rozgar","berozgar","livelihood",
                "skill training","vocational","self employed"))
            needs.add(new CommunityNeed("Employment & Livelihood","Moderate",
                    num(text,"unemployed|berozgar|youth|yuva"),"💼"));

        if (has(l,"child","bal","orphan","abandoned","child labour",
                "bal majdoor","malnutrition","kuposhan"))
            needs.add(new CommunityNeed("Child Welfare","Critical",
                    num(text,"child|bachche|bal"),"👶"));

        if (needs.isEmpty())
            needs.add(new CommunityNeed("General Community Support",
                    "Moderate","Community members","🤝"));

        return needs;
    }

    boolean has(String text, String... kw) {
        for (String k : kw) if (text.contains(k)) return true;
        return false;
    }

    String num(String text, String near) {
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].toLowerCase().matches(".*(" + near + ").*")) {
                for (int j = Math.max(0,i-3); j < Math.min(words.length,i+3); j++) {
                    String c = words[j].replaceAll("[^0-9]","");
                    if (!c.isEmpty() && c.length() < 6) return c + " people";
                }
            }
        }
        return "Several people";
    }

    String loc(String text) {
        String[] cities = {"Raipur","Bilaspur","Durg","Bhilai","Korba",
                "Rajnandgaon","Jagdalpur","Ambikapur","Mumbai","Delhi","Pune",
                "Kolkata","Chennai","Hyderabad","Bengaluru","Ahmedabad",
                "Jaipur","Lucknow","Bhopal","Indore","Nagpur","Patna","Ranchi"};
        String lower = text.toLowerCase();
        for (String c : cities)
            if (lower.contains(c.toLowerCase())) return c;
        return "Local Area";
    }

    // ── TASK GENERATOR ───────────────────────────────────

    List<SuggestedTask> generateTasks(List<CommunityNeed> needs, String text) {
        List<SuggestedTask> tasks = new ArrayList<>();
        String location = loc(text);

        for (CommunityNeed n : needs) {
            switch (n.type) {
                case "Food & Nutrition":
                    tasks.add(new SuggestedTask("Food Distribution Drive",
                            "Organize food for " + n.affected + " at " + location,
                            "Food Distribution", n.urgency, "Any Skill", "10", location)); break;

                case "Medical Assistance":
                    tasks.add(new SuggestedTask("Free Medical Camp",
                            "Medical camp for " + n.affected,
                            "Medical Help", n.urgency, "Medical Help", "5", location)); break;

                case "Education Support":
                    tasks.add(new SuggestedTask("Teaching Program",
                            "Education support for " + n.affected,
                            "Education", n.urgency, "Teaching", "8", location)); break;

                case "Water & Sanitation":
                    tasks.add(new SuggestedTask("Water & Hygiene Drive",
                            "Clean water and hygiene kits distribute karo",
                            "Health", n.urgency, "Any Skill", "6", location)); break;

                case "Emergency Shelter":
                    tasks.add(new SuggestedTask("Disaster Relief Camp",
                            "Shelter and relief for " + n.affected,
                            "Disaster Relief", n.urgency, "Any Skill", "20", location)); break;

                case "Elderly Care":
                    tasks.add(new SuggestedTask("Elderly Support Visits",
                            "Regular visits for " + n.affected,
                            "Medical Help", n.urgency, "Medical Help", "4", location)); break;

                case "Environmental Cleanup":
                    tasks.add(new SuggestedTask("Community Cleanup Drive",
                            "Cleanup and plantation drive",
                            "Environment", n.urgency, "Any Skill", "15", location)); break;

                case "Women Empowerment":
                    tasks.add(new SuggestedTask("Women Skill Workshop",
                            "Skill training for women in " + location,
                            "Education", n.urgency, "Teaching", "5", location)); break;

                case "Disability Support":
                    tasks.add(new SuggestedTask("Disability Aid Camp",
                            "Assistive devices and support for " + n.affected,
                            "Medical Help", n.urgency, "Medical Help", "6", location)); break;

                case "Mental Health Support":
                    tasks.add(new SuggestedTask("Mental Health Counseling",
                            "Free counseling sessions for community",
                            "Health", n.urgency, "Medical Help", "3", location)); break;

                case "Employment & Livelihood":
                    tasks.add(new SuggestedTask("Skill Training Workshop",
                            "Vocational training for " + n.affected,
                            "Education", n.urgency, "Teaching", "10", location)); break;

                case "Child Welfare":
                    tasks.add(new SuggestedTask("Child Care & Nutrition Drive",
                            "Nutrition and care for " + n.affected,
                            "Medical Help", n.urgency, "Any Skill", "8", location)); break;

                default:
                    tasks.add(new SuggestedTask("Community Support Drive",
                            "General support in " + location,
                            "Event Management", n.urgency, "Any Skill", "10", location));
            }
        }
        return tasks;
    }

    // ── SHOW RESULTS ─────────────────────────────────────

    void showResults(List<CommunityNeed> needs, List<SuggestedTask> tasks) {
        panelResults.setVisibility(View.VISIBLE);
        layoutNeedsList.removeAllViews();
        layoutSuggestedTasks.removeAllViews();
        for (CommunityNeed n : needs) addNeedCard(n);
        for (SuggestedTask t : tasks) addTaskCard(t);
        Toast.makeText(this,
                needs.size() + " needs, " + tasks.size() + " tasks mile! ✅",
                Toast.LENGTH_LONG).show();
    }

    void addNeedCard(CommunityNeed need) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(24, 20, 24, 20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0,0,0,12); card.setLayoutParams(p);

        int col = need.urgency.equals("Critical") ? Color.parseColor("#C62828")
                : need.urgency.equals("Moderate") ? Color.parseColor("#F57F17")
                : Color.parseColor("#2E7D32");

        TextView badge = new TextView(this);
        badge.setText(need.emoji + "  " + need.urgency.toUpperCase());
        badge.setTextColor(Color.WHITE);
        badge.setBackgroundColor(col);
        badge.setTextSize(11f);
        badge.setPadding(20,8,20,8);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2,-2);
        bp.setMargins(0,0,0,10); badge.setLayoutParams(bp);
        card.addView(badge);

        TextView title = new TextView(this);
        title.setText(need.type); title.setTextSize(14f);
        title.setTextColor(Color.parseColor("#1A1A1A"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView sub = new TextView(this);
        sub.setText("👥 " + need.affected);
        sub.setTextSize(12f); sub.setTextColor(Color.parseColor("#555"));
        card.addView(sub);

        layoutNeedsList.addView(card);
    }

    void addTaskCard(SuggestedTask task) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(24, 20, 24, 20);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.setMargins(0,0,0,16); card.setLayoutParams(p);

        TextView title = new TextView(this);
        title.setText("💡 " + task.title); title.setTextSize(15f);
        title.setTextColor(Color.parseColor("#1A1A1A"));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1,-2);
        tp.setMargins(0,0,0,8); title.setLayoutParams(tp);
        card.addView(title);

        TextView desc = new TextView(this);
        desc.setText(task.description); desc.setTextSize(13f);
        desc.setTextColor(Color.parseColor("#555"));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1,-2);
        dp.setMargins(0,0,0,12); desc.setLayoutParams(dp);
        card.addView(desc);

        // Info row
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2);
        cp.setMargins(0,0,0,12); chips.setLayoutParams(cp);
        chip(chips, "📂 " + task.category, "#1565C0");
        chip(chips, "👥 " + task.volunteers, "#1A1A1A");
        chip(chips, "📍 " + task.location, "#555555");
        card.addView(chips);

        Button btn = new Button(this);
        btn.setText("✅ Save & Send to Dashboard");
        btn.setTextColor(Color.WHITE);
        btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        btn.setTextSize(13f);
        card.addView(btn);

        btn.setOnClickListener(v -> saveTaskAndGoToDashboard(task, btn));

        layoutSuggestedTasks.addView(card);
    }

    void chip(LinearLayout parent, String text, String color) {
        TextView c = new TextView(this);
        c.setText(text); c.setTextColor(Color.WHITE);
        c.setTextSize(11f); c.setBackgroundColor(Color.parseColor(color));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2,-2);
        p.setMargins(0,0,8,0); c.setLayoutParams(p);
        c.setPadding(16,6,16,6);
        parent.addView(c);
    }

    // ── SAVE TASK → DASHBOARD ────────────────────────────

    void saveTaskAndGoToDashboard(SuggestedTask task, Button btn) {
        btn.setText("⏳ Saving...");
        btn.setEnabled(false);

        // LocalTaskStore now saves to Firestore automatically
        LocalTaskStore.getInstance().addTask(
                new LocalTaskStore.LocalTask(
                        task.title, task.description, task.category,
                        task.urgency, task.skill, task.volunteers, task.location
                )
        );

        btn.setText("✅ Saved!");
        btn.setBackgroundTintList(ColorStateList.valueOf(
                Color.parseColor("#2E7D32")));

        Toast.makeText(this,
                "Task Firestore mein save ho gaya! ✅",
                Toast.LENGTH_SHORT).show();

        new android.os.Handler().postDelayed(() -> {
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("showTasks", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }, 800);
    }
    // ── DATA MODELS ──────────────────────────────────────

    static class CommunityNeed {
        String type, urgency, affected, emoji;
        CommunityNeed(String t, String u, String a, String e) {
            type=t; urgency=u; affected=a; emoji=e;
        }
    }

    static class SuggestedTask {
        String title, description, category, urgency, skill, volunteers, location;
        SuggestedTask(String ti, String d, String c, String u, String s, String v, String l) {
            title=ti; description=d; category=c; urgency=u;
            skill=s; volunteers=v; location=l;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        latinRecognizer.close();
        hindiRecognizer.close();
    }
}