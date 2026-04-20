package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class DataCollectionActivity extends AppCompatActivity {

    // Views
    PreviewView previewView;
    Button btnScan, btnBack, btnCreateTask;
    TextView tvScanStatus, tvExtractedTitle, tvExtractedDesc,
            tvExtractedLocation, tvExtractedUrgency, tvExtractedSkill;
    LinearLayout layoutResults, layoutScanning;
    ProgressBar progressScan;

    // Camera
    ImageCapture imageCapture;
    TextRecognizer recognizer;

    // Firebase
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    // Extracted Data
    String extractedTitle    = "";
    String extractedDesc     = "";
    String extractedLocation = "";
    String extractedUrgency  = "Moderate";
    String extractedSkill    = "Any Skill";
    String extractedVolunteers = "5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Camera permission check
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, 100);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_collection);

        // Firebase
        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // ML Kit
        recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);

        // Views
        previewView        = findViewById(R.id.previewView);
        btnScan            = findViewById(R.id.btnScan);
        btnBack            = findViewById(R.id.btnBack);
        btnCreateTask      = findViewById(R.id.btnCreateTask);
        tvScanStatus       = findViewById(R.id.tvScanStatus);
        tvExtractedTitle   = findViewById(R.id.tvExtractedTitle);
        tvExtractedDesc    = findViewById(R.id.tvExtractedDesc);
        tvExtractedLocation= findViewById(R.id.tvExtractedLocation);
        tvExtractedUrgency = findViewById(R.id.tvExtractedUrgency);
        tvExtractedSkill   = findViewById(R.id.tvExtractedSkill);
        layoutResults      = findViewById(R.id.layoutResults);
        layoutScanning     = findViewById(R.id.layoutScanning);
        progressScan       = findViewById(R.id.progressScan);

        // Start Camera
        startCamera();

        // Listeners
        btnBack.setOnClickListener(v -> finish());

        btnScan.setOnClickListener(v -> captureAndScan());

        btnCreateTask.setOnClickListener(v -> autoCreateTask());
    }

    // ═══════════════════════════════════════
    // CAMERA SETUP
    // ═══════════════════════════════════════


    void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                // Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // Image Capture
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // Camera selector — back camera
                CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

                provider.unbindAll();
                provider.bindToLifecycle(this, selector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera error: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    // ════════════════════════════════
    // CAPTURE + SCAN
    // ════════════════════════════════

    void captureAndScan() {
        if (imageCapture == null) return;

        // Show scanning state
        btnScan.setEnabled(false);
        btnScan.setText("Scanning...");
        progressScan.setVisibility(View.VISIBLE);
        tvScanStatus.setText("📸 Capturing image...");
        layoutResults.setVisibility(View.GONE);

        imageCapture.takePicture(
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {

                    @androidx.camera.core.ExperimentalGetImage
                    @Override
                    public void onCaptureSuccess(ImageProxy imageProxy) {
                        tvScanStatus.setText("🔍 Reading text with AI...");

                        // ML Kit text recognition
                        if (imageProxy.getImage() == null) {
                            imageProxy.close();
                            runOnUiThread(() -> {
                                tvScanStatus.setText("❌ Image capture failed. Try again.");
                                btnScan.setEnabled(true);
                                btnScan.setText("📷 Scan Survey");
                                progressScan.setVisibility(View.GONE);
                            });
                            return;
                        }

                        InputImage image = InputImage.fromMediaImage(
                                imageProxy.getImage(),
                                imageProxy.getImageInfo().getRotationDegrees());
                        recognizer.process(image)
                                .addOnSuccessListener(visionText -> {
                                    imageProxy.close();
                                    String scannedText = visionText.getText();

                                    if (scannedText.isEmpty()) {
                                        tvScanStatus.setText(
                                                "❌ No text found. Try again with better lighting.");
                                        btnScan.setEnabled(true);
                                        btnScan.setText("📷 Scan Survey");
                                        progressScan.setVisibility(View.GONE);
                                    } else {
                                        tvScanStatus.setText("✅ Text extracted! Processing...");
                                        extractDataFromText(scannedText);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    imageProxy.close();
                                    tvScanStatus.setText("❌ Scan failed: " + e.getMessage());
                                    btnScan.setEnabled(true);
                                    btnScan.setText("📷 Scan Survey");
                                    progressScan.setVisibility(View.GONE);
                                });
                    }

                    @Override
                    public void onError(ImageCaptureException e) {
                        tvScanStatus.setText("❌ Camera error: " + e.getMessage());
                        btnScan.setEnabled(true);
                        btnScan.setText("📷 Scan Survey");
                        progressScan.setVisibility(View.GONE);
                    }
                });
    }

    // ═══════════════════════════════════════
    // AI DATA EXTRACTION
    // ═══════════════════════════════════════

    void extractDataFromText(String rawText) {
        String text = rawText.toLowerCase().trim();
        String[] lines = rawText.split("\n");

        // ── Extract Title ──
        // Pehli non-empty line jo meaningful ho
        // ── Extract Title ──
        extractedTitle = "Community Need"; // default

        for (String line : lines) {
            String trimmed = line.trim();

            // Skip karo: empty lines, IDs, phone numbers, short words
            if (trimmed.length() < 5) continue;
            if (trimmed.matches(".*[0-9a-f]{8}-.*")) continue; // UUID skip
            if (trimmed.matches(".*\\d{10}.*")) continue; // phone skip
            if (trimmed.toLowerCase().startsWith("phone")) continue;
            if (trimmed.toLowerCase().startsWith("date")) continue;
            if (trimmed.matches("[^a-zA-Z]*")) continue; // only numbers/symbols

            // ✅ Ye line title hai
            extractedTitle = trimmed.length() > 60
                    ? trimmed.substring(0, 60)
                    : trimmed;
            break;
        }

        // ── Extract Description ──
        StringBuilder desc = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.equals(extractedTitle) && trimmed.length() > 10) {
                desc.append(trimmed).append(" ");
                count++;
                if (count >= 3) break;
            }
        }
        extractedDesc = desc.length() > 0 ? desc.toString().trim()
                : "Community need identified through field survey.";

        // ── Extract Location — SMART VERSION ──
        extractedLocation = ""; // default empty

        // Step 1: "location:", "place:", "area:", "address:" ke baad ka text lo
        java.util.regex.Pattern locPattern = java.util.regex.Pattern.compile(
                "(?:location|place|area|address|venue|jagah|sthan)[:\\s]+([^\\n,]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher locMatcher = locPattern.matcher(rawText);
        if (locMatcher.find()) {
            extractedLocation = locMatcher.group(1).trim();
        }

        // Step 2: Pincode dhundo — usse location nikalo
        if (extractedLocation.isEmpty()) {
            java.util.regex.Pattern pinPattern =
                    java.util.regex.Pattern.compile("\\b(4[0-9]{5})\\b");
            java.util.regex.Matcher pinMatcher = pinPattern.matcher(rawText);
            if (pinMatcher.find()) {
                extractedLocation = "Pincode: " + pinMatcher.group(1);
            }
        }

        // Step 3: Known CG cities + towns
        if (extractedLocation.isEmpty()) {
            String[] cgCities = {
                    "Raipur", "Bilaspur", "Durg", "Korba", "Rajnandgaon",
                    "Jagdalpur", "Ambikapur", "Raigarh", "Bhilai", "Dhamtari",
                    "Mahasamund", "Kanker", "Kondagaon", "Bijapur", "Narayanpur",
                    "Balod", "Bemetara", "Mungeli", "Surajpur", "Balrampur",
                    "Gariaband", "Balodabazar", "Kabirdham", "Sukma", "Bastar"
            };
            for (String city : cgCities) {
                if (text.contains(city.toLowerCase())) {
                    extractedLocation = city + ", CG";
                    break;
                }
            }
        }

        // Step 4: Koi bhi capitalized word jo city jaisa lage
        if (extractedLocation.isEmpty()) {
            java.util.regex.Pattern capPattern =
                    java.util.regex.Pattern.compile("\\b([A-Z][a-z]{3,})\\b");
            java.util.regex.Matcher capMatcher = capPattern.matcher(rawText);
            while (capMatcher.find()) {
                String word = capMatcher.group(1);
                // Common words skip karo
                if (!word.matches("The|And|For|This|That|With|From|" +
                        "Name|Date|Area|Task|Need|Help|Please|Survey")) {
                    extractedLocation = word;
                    break;
                }
            }
        }

        // Step 5: Sach mein kuch nahi mila
        if (extractedLocation.isEmpty()) {
            extractedLocation = "Not specified";
        }

        // ── Extract Urgency ──
        if (text.contains("urgent") || text.contains("critical") ||
                text.contains("emergency") || text.contains("immediate") ||
                text.contains("asap") || text.contains("jaldi") ||
                text.contains("turant")) {
            extractedUrgency = "🔴 Critical (24 hrs)";
        } else if (text.contains("moderate") || text.contains("soon") ||
                text.contains("week") || text.contains("hafte")) {
            extractedUrgency = "🟡 Moderate (1 week)";
        } else {
            extractedUrgency = "🟢 Normal";
        }

        // ── Extract Skill ──
        if (text.contains("medical") || text.contains("health") ||
                text.contains("doctor") || text.contains("nurse") ||
                text.contains("dawai") || text.contains("hospital")) {
            extractedSkill = "Medical Help";
        } else if (text.contains("teach") || text.contains("education") ||
                text.contains("school") || text.contains("student") ||
                text.contains("padhai") || text.contains("siksha")) {
            extractedSkill = "Teaching";
        } else if (text.contains("food") || text.contains("hunger") ||
                text.contains("meal") || text.contains("ration") ||
                text.contains("khana") || text.contains("bhojan")) {
            extractedSkill = "Food Distribution";
        } else if (text.contains("tree") || text.contains("environment") ||
                text.contains("plant") || text.contains("clean") ||
                text.contains("safai") || text.contains("prakriti")) {
            extractedSkill = "Environment";
        } else if (text.contains("flood") || text.contains("disaster") ||
                text.contains("relief") || text.contains("rescue") ||
                text.contains("aapda") || text.contains("baarish")) {
            extractedSkill = "Disaster Relief";
        } else if (text.contains("fund") || text.contains("money") ||
                text.contains("donation") || text.contains("paisa")) {
            extractedSkill = "Fundraising";
        } else {
            extractedSkill = "Any Skill";
        }

        // ── Extract Volunteers ──
        java.util.regex.Pattern volPattern = java.util.regex.Pattern.compile(
                "(\\d+)\\s*(?:volunteer|people|person|members|log|vyakti|jan)");
        java.util.regex.Matcher volMatcher = volPattern.matcher(text);
        if (volMatcher.find()) {
            extractedVolunteers = volMatcher.group(1);
        } else {
            // Koi bhi standalone number 1-100 ke beech
            java.util.regex.Pattern numPattern =
                    java.util.regex.Pattern.compile("\\b([1-9][0-9]?)\\b");
            java.util.regex.Matcher numMatcher = numPattern.matcher(text);
            if (numMatcher.find()) {
                extractedVolunteers = numMatcher.group(1);
            } else {
                extractedVolunteers = "5";
            }
        }

        showExtractedResults();
    }

    // ═══════════════════════════════════════
    // SHOW RESULTS
    // ═══════════════════════════════════════

    void showExtractedResults() {
        progressScan.setVisibility(View.GONE);
        btnScan.setEnabled(true);
        btnScan.setText("🔄 Scan Again");

        tvExtractedTitle.setText("📋 " + extractedTitle);
        tvExtractedDesc.setText("📝 " + extractedDesc);
        tvExtractedLocation.setText("📍 " + extractedLocation);
        tvExtractedUrgency.setText("⚡ " + extractedUrgency);
        tvExtractedSkill.setText("🛠️ " + extractedSkill);

        layoutResults.setVisibility(View.VISIBLE);
        tvScanStatus.setText("📋 Tips: Clearly write Location:, Urgency:, Skills: on paper for best results");
    }

    // ═══════════════════════════════════════
    // AUTO CREATE TASK IN FIRESTORE
    // ═══════════════════════════════════════

    void autoCreateTask() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateTask.setEnabled(false);
        btnCreateTask.setText("Creating...");

        Map<String, Object> task = new HashMap<>();
        task.put("ngoId",              mAuth.getCurrentUser().getUid());
        task.put("title",              extractedTitle);
        task.put("description",        extractedDesc);
        task.put("category",           extractedSkill);
        task.put("urgency",            extractedUrgency);
        task.put("skill",              extractedSkill);
        task.put("volunteersRequired", extractedVolunteers);
        task.put("location",           extractedLocation);
        task.put("date",               "TBD");
        task.put("status",             "Active");
        task.put("source",             "AI Survey Scanner"); // ✅ AI se aaya
        task.put("timestamp",          System.currentTimeMillis());

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(ref -> {
                    // LocalTaskStore mein bhi add karo
                    LocalTaskStore.getInstance().addTask(
                            new LocalTaskStore.LocalTask(
                                    extractedTitle, extractedDesc,
                                    extractedSkill, extractedUrgency,
                                    extractedSkill, extractedVolunteers,
                                    extractedLocation));

                    Toast.makeText(this,
                            "✅ Task auto-created from survey!",
                            Toast.LENGTH_LONG).show();

                    // Dashboard pe wapas jao
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.putExtra("role", "NGO");
                    intent.putExtra("showTasks", true);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnCreateTask.setEnabled(true);
                    btnCreateTask.setText("✅ Create Task from Survey");
                    Toast.makeText(this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}