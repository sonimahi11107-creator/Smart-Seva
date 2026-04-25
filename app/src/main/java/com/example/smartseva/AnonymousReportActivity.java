package com.example.smartseva;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.util.*;

public class AnonymousReportActivity extends AppCompatActivity {

    // Views
    Switch switchAnonymous;
    LinearLayout layoutContactInfo;
    EditText etReporterName, etReporterPhone;
    EditText etNeedSummary, etNeedDescription, etPeopleAffected, etNeedLocation;
    TextView errNeedSummary, errNeedDescription, errNeedLocation, tvUrgencyLabel;
    Spinner spinnerNeedCategory;
    Button btnBackReport, btnDetectLocation, btnSubmitReport;
    Button btnUrgency1, btnUrgency2, btnUrgency3, btnUrgency4, btnUrgency5;
    Button btnCaptureEvidence, btnGalleryEvidence;
    ImageView imgEvidencePreview;

    // Data
    int selectedUrgency = 0;
    boolean isAnonymous = true;
    Uri evidenceUri = null;

    static final int REQ_LOCATION  = 401;
    static final int REQ_CAMERA    = 402;
    static final int REQ_GALLERY   = 403;


    // Urgency labels
    String[] urgencyLabels = {
            "Select urgency level (1=Low, 5=Critical)",
            "1 — Low Priority (Can wait a few days)",
            "2 — Moderate (Needs attention this week)",
            "3 — Important (Should be addressed soon)",
            "4 — High (Needs help within 24-48 hrs)",
            "5 — Critical (Emergency! Immediate help needed!)"
    };

    String[] urgencyColors = {
            "#888888", "#4CAF50", "#8BC34A", "#FFC107", "#FF5722", "#C62828"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anonymous_report);

        // ── Views ──
        switchAnonymous     = findViewById(R.id.switchAnonymous);
        layoutContactInfo   = findViewById(R.id.layoutContactInfo);
        etReporterName      = findViewById(R.id.etReporterName);
        etReporterPhone     = findViewById(R.id.etReporterPhone);
        etNeedSummary       = findViewById(R.id.etNeedSummary);
        etNeedDescription   = findViewById(R.id.etNeedDescription);
        etPeopleAffected    = findViewById(R.id.etPeopleAffected);
        etNeedLocation      = findViewById(R.id.etNeedLocation);
        errNeedSummary      = findViewById(R.id.errNeedSummary);
        errNeedDescription  = findViewById(R.id.errNeedDescription);
        errNeedLocation     = findViewById(R.id.errNeedLocation);
        tvUrgencyLabel      = findViewById(R.id.tvUrgencyLabel);
        spinnerNeedCategory = findViewById(R.id.spinnerNeedCategory);
        btnBackReport       = findViewById(R.id.btnBackReport);
        btnDetectLocation   = findViewById(R.id.btnDetectLocation);
        btnSubmitReport     = findViewById(R.id.btnSubmitReport);
        btnUrgency1         = findViewById(R.id.btnUrgency1);
        btnUrgency2         = findViewById(R.id.btnUrgency2);
        btnUrgency3         = findViewById(R.id.btnUrgency3);
        btnUrgency4         = findViewById(R.id.btnUrgency4);
        btnUrgency5         = findViewById(R.id.btnUrgency5);
        btnCaptureEvidence  = findViewById(R.id.btnCaptureEvidence);
        btnGalleryEvidence  = findViewById(R.id.btnGalleryEvidence);
        imgEvidencePreview  = findViewById(R.id.imgEvidencePreview);

        // ── Spinner ──
        spinnerNeedCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "Select Category",
                        "🍽️ Food & Nutrition",
                        "🏥 Medical Help",
                        "📚 Education",
                        "💧 Water & Sanitation",
                        "🏠 Shelter",
                        "👴 Elderly Care",
                        "🌱 Environment",
                        "👩 Women Support",
                        "🆘 Disaster Relief",
                        "🤝 Other"
                }));

        // ── Listeners ──
        btnBackReport.setOnClickListener(v -> finish());

        switchAnonymous.setOnCheckedChangeListener((btn, checked) -> {
            isAnonymous = checked;
            layoutContactInfo.setVisibility(checked ? View.GONE : View.VISIBLE);
        });

        btnUrgency1.setOnClickListener(v -> setUrgency(1));
        btnUrgency2.setOnClickListener(v -> setUrgency(2));
        btnUrgency3.setOnClickListener(v -> setUrgency(3));
        btnUrgency4.setOnClickListener(v -> setUrgency(4));
        btnUrgency5.setOnClickListener(v -> setUrgency(5));

        btnDetectLocation.setOnClickListener(v -> detectLocation());

        btnCaptureEvidence.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQ_CAMERA);
        });

        btnGalleryEvidence.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQ_GALLERY);
        });

        btnSubmitReport.setOnClickListener(v -> submitReport());
    }

    // ═══════════════════════════════════════
    // URGENCY SELECTOR
    // ═══════════════════════════════════════

    void setUrgency(int level) {
        selectedUrgency = level;
        Button[] btns = {btnUrgency1, btnUrgency2,
                btnUrgency3, btnUrgency4, btnUrgency5};

        for (int i = 0; i < btns.length; i++) {
            if (i < level) {
                btns[i].setBackgroundTintList(ColorStateList.valueOf(
                        Color.parseColor(urgencyColors[level])));
                btns[i].setTextColor(Color.WHITE);
            } else {
                btns[i].setBackgroundTintList(
                        ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
                btns[i].setTextColor(Color.parseColor("#1A1A1A"));
            }
        }

        tvUrgencyLabel.setText(urgencyLabels[level]);
        tvUrgencyLabel.setTextColor(Color.parseColor(urgencyColors[level]));
    }

    // ═══════════════════════════════════════
    // GPS LOCATION
    // ═══════════════════════════════════════

    void detectLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
            return;
        }

        btnDetectLocation.setText("📡 Detecting...");
        btnDetectLocation.setEnabled(false);

        android.location.LocationManager locationManager =
                (android.location.LocationManager)
                        getSystemService(LOCATION_SERVICE);

        try {
            Location location = locationManager
                    .getLastKnownLocation(
                            android.location.LocationManager.GPS_PROVIDER);

            if (location == null) {
                location = locationManager.getLastKnownLocation(
                        android.location.LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                reverseGeocode(location);
            } else {
                btnDetectLocation.setText("📡 Use My Current Location");
                btnDetectLocation.setEnabled(true);
                Toast.makeText(this,
                        "Please enable GPS and try again.",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            btnDetectLocation.setText("📡 Use My Current Location");
            btnDetectLocation.setEnabled(true);
        }
    }

    void reverseGeocode(Location location) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(), location.getLongitude(), 1);

                runOnUiThread(() -> {
                    btnDetectLocation.setText("📡 Use My Current Location");
                    btnDetectLocation.setEnabled(true);

                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String locationText = "";
                        if (addr.getSubLocality() != null)
                            locationText += addr.getSubLocality() + ", ";
                        if (addr.getLocality() != null)
                            locationText += addr.getLocality() + ", ";
                        if (addr.getAdminArea() != null)
                            locationText += addr.getAdminArea();

                        etNeedLocation.setText(locationText.trim());
                        Toast.makeText(this,
                                "📍 Location detected!", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    btnDetectLocation.setText("📡 Use My Current Location");
                    btnDetectLocation.setEnabled(true);
                });
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectLocation();
        }
    }

    // ═══════════════════════════════════════
    // IMAGE PICKER
    // ═══════════════════════════════════════

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;

        if (req == REQ_GALLERY && data != null) {
            evidenceUri = data.getData();
            imgEvidencePreview.setVisibility(View.VISIBLE);
            imgEvidencePreview.setImageURI(evidenceUri);
        } else if (req == REQ_CAMERA && data != null) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            imgEvidencePreview.setVisibility(View.VISIBLE);
            imgEvidencePreview.setImageBitmap(photo);
        }
    }

    // ═══════════════════════════════════════
    // VALIDATION + SUBMIT
    // ═══════════════════════════════════════

    void submitReport() {
        boolean ok = true;

        // Summary
        String summary = etNeedSummary.getText().toString().trim();
        if (summary.isEmpty()) {
            errNeedSummary.setText("Please provide a short summary");
            ok = false;
        } else errNeedSummary.setText("");

        // Category
        if (spinnerNeedCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this,
                    "Please select a category", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        // Urgency
        if (selectedUrgency == 0) {
            Toast.makeText(this,
                    "Please select urgency level (1-5)", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        // Description
        String desc = etNeedDescription.getText().toString().trim();
        if (desc.length() < 20) {
            errNeedDescription.setText(
                    "Please describe in at least 20 characters");
            ok = false;
        } else errNeedDescription.setText("");

        // Location
        String location = etNeedLocation.getText().toString().trim();
        if (location.isEmpty()) {
            errNeedLocation.setText("Please provide location");
            ok = false;
        } else errNeedLocation.setText("");

        if (ok) {
            // Build report
            String category    = spinnerNeedCategory.getSelectedItem().toString();
            String people      = etPeopleAffected.getText().toString().trim();
            String reporterName= isAnonymous ? "Anonymous" :
                    etReporterName.getText().toString().trim();
            String urgencyText = getUrgencyText(selectedUrgency);

            // ✅ Data ready — Firebase teammate Firestore mein save karega
            showConfirmationDialog(summary, category, urgencyText, location);
        }
    }

    String getUrgencyText(int level) {
        switch (level) {
            case 1: return "🟢 Low";
            case 2: return "🟡 Moderate";
            case 3: return "🟠 Important";
            case 4: return "🔴 High";
            case 5: return "🚨 Critical";
            default: return "Unknown";
        }
    }

    void showConfirmationDialog(String summary, String category,
                                String urgency, String location) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("✅ Report Submitted!")
                .setMessage(
                        "🔒 " + (isAnonymous ? "Anonymous Report" : "Your Report") +
                                "\n\n📋 " + summary +
                                "\n📂 " + category +
                                "\n⚡ Urgency: " + urgency +
                                "\n📍 " + location +
                                "\n\n✅ NGOs in your area have been notified." +
                                "\n🕐 Expected response: " +
                                (selectedUrgency >= 4 ? "Within 2-4 hours" :
                                        selectedUrgency >= 3 ? "Within 24 hours" : "Within 2-3 days")
                )
                .setCancelable(false)
                .setPositiveButton("View on Map", (dialog, which) -> {
                    startActivity(new Intent(this, MapViewActivity.class));
                    finish();
                })
                .setNegativeButton("Done", (dialog, which) -> finish())
                .show();

        // Add to local task store for demo
        String urgencyForTask = selectedUrgency >= 4 ?
                "🔴 Critical (24 hrs)" :
                selectedUrgency >= 3 ? "🟡 Moderate (1 week)" : "🟢 Normal";

        TaskStatusManager.TaskItem newTask =
                new TaskStatusManager.TaskItem(
                        "A" + System.currentTimeMillis(),
                        summary,
                        etNeedDescription.getText().toString().trim(),
                        category,
                        urgencyForTask,
                        location,
                        new java.text.SimpleDateFormat("dd/MM/yyyy",
                                Locale.getDefault()).format(new java.util.Date()),
                        "Any Skill",
                        10);
        TaskStatusManager.addTask(newTask);
    }
}