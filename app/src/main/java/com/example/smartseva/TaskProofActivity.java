package com.example.smartseva;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class TaskProofActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FusedLocationProviderClient locationClient;

    // Views
    ImageView imgProofPreview;
    TextView tvGPSStatus, tvTaskTitle,
            tvTaskStatus, tvSubmitStatus;
    Button btnUploadPhoto, btnGetGPS,
            btnSubmitProof, btnVerifyProof;
    LinearLayout layoutProofHistory;
    ProgressBar progressSubmit;

    // Data
    String taskId, taskTitle, taskStatus, userRole;
    Uri selectedPhotoUri;
    Bitmap selectedBitmap;
    double currentLat = 0, currentLng = 0;
    boolean hasPhoto = false, hasGPS = false;

    static final int REQ_CAMERA    = 401;
    static final int REQ_GALLERY   = 402;
    static final int REQ_LOCATION  = 403;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_proof);

        mAuth          = FirebaseAuth.getInstance();
        db             = FirebaseFirestore.getInstance();
        locationClient = LocationServices
                .getFusedLocationProviderClient(this);

        android.content.SharedPreferences prefs =
                getSharedPreferences("SmartSeva", MODE_PRIVATE);
        userRole = prefs.getString("role", "Volunteer");

        taskId     = getIntent().getStringExtra("taskId");
        taskTitle  = getIntent().getStringExtra("taskTitle");
        taskStatus = getIntent().getStringExtra("taskStatus");

        bindViews();
        setupUI();
        loadProofHistory();
    }

    void bindViews() {
        imgProofPreview  = findViewById(R.id.imgProofPreview);
        tvGPSStatus      = findViewById(R.id.tvGPSStatus);
        tvTaskTitle      = findViewById(R.id.tvTaskTitle);
        tvTaskStatus     = findViewById(R.id.tvTaskStatus);
        tvSubmitStatus   = findViewById(R.id.tvSubmitStatus);
        btnUploadPhoto   = findViewById(R.id.btnUploadPhoto);
        btnGetGPS        = findViewById(R.id.btnGetGPS);
        btnSubmitProof   = findViewById(R.id.btnSubmitProof);
        btnVerifyProof   = findViewById(R.id.btnVerifyProof);
        layoutProofHistory = findViewById(
                R.id.layoutProofHistory);
        progressSubmit   = findViewById(
                R.id.progressSubmit);

        findViewById(R.id.btnBackProof)
                .setOnClickListener(v -> finish());
    }

    void setupUI() {
        tvTaskTitle.setText(
                taskTitle != null ? taskTitle : "Task");

        // Status badge
        tvTaskStatus.setText(
                taskStatus != null ? taskStatus : "Open");
        tvTaskStatus.setBackgroundColor(
                getStatusColor(taskStatus));

        // Role based UI
        if (userRole.equals("NGO")) {
            // NGO can verify
            btnSubmitProof.setVisibility(View.GONE);
            btnUploadPhoto.setVisibility(View.GONE);
            btnGetGPS.setVisibility(View.GONE);
            btnVerifyProof.setVisibility(View.VISIBLE);
            tvSubmitStatus.setText(
                    "Review submitted proofs below");
        } else {
            // Volunteer submits proof
            btnVerifyProof.setVisibility(View.GONE);
            btnSubmitProof.setVisibility(View.VISIBLE);
        }

        // Listeners
        btnUploadPhoto.setOnClickListener(v ->
                showPhotoOptions());
        btnGetGPS.setOnClickListener(v ->
                getCurrentLocation());
        btnSubmitProof.setOnClickListener(v ->
                submitProof());
        btnVerifyProof.setOnClickListener(v ->
                verifyAllProofs());
    }

    // ── PHOTO ─────────────────────────────────────────────

    void showPhotoOptions() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Upload Proof Photo")
                .setItems(new String[]{
                        "📷 Take Photo",
                        "🖼️ Choose from Gallery"
                }, (d, which) -> {
                    if (which == 0) takePhoto();
                    else openGallery();
                })
                .show();
    }

    void takePhoto() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA);
            return;
        }
        Intent i = new Intent(
                android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, REQ_CAMERA);
    }

    void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images
                        .Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, REQ_GALLERY);
    }

    @Override
    protected void onActivityResult(int req,
                                    int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null) return;

        if (req == REQ_CAMERA) {
            selectedBitmap = (Bitmap)
                    data.getExtras().get("data");
            imgProofPreview.setImageBitmap(selectedBitmap);
            hasPhoto = true;
            btnUploadPhoto.setText("✅ Photo Captured");
            btnUploadPhoto.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#2E7D32")));

        } else if (req == REQ_GALLERY) {
            try {
                Uri uri = data.getData();
                InputStream is =
                        getContentResolver().openInputStream(uri);
                selectedBitmap =
                        BitmapFactory.decodeStream(is);
                selectedPhotoUri = uri;
                imgProofPreview.setImageBitmap(
                        selectedBitmap);
                hasPhoto = true;
                btnUploadPhoto.setText("✅ Photo Selected");
                btnUploadPhoto.setBackgroundTintList(
                        android.content.res.ColorStateList
                                .valueOf(Color.parseColor("#2E7D32")));
            } catch (Exception e) {
                Toast.makeText(this,
                        "Error loading image: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
        updateSubmitButton();
    }

    // ── GPS ───────────────────────────────────────────────

    void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
            return;
        }

        tvGPSStatus.setText("📡 Getting location...");
        btnGetGPS.setEnabled(false);

        locationClient.getCurrentLocation(
                        LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLat = location.getLatitude();
                        currentLng = location.getLongitude();
                        hasGPS = true;

                        String gpsText = String.format(
                                "✅ GPS: %.4f, %.4f",
                                currentLat, currentLng);
                        tvGPSStatus.setText(gpsText);
                        tvGPSStatus.setTextColor(
                                Color.parseColor("#2E7D32"));

                        btnGetGPS.setText("✅ Location Captured");
                        btnGetGPS.setBackgroundTintList(
                                android.content.res.ColorStateList
                                        .valueOf(Color.parseColor(
                                                "#2E7D32")));
                    } else {
                        tvGPSStatus.setText(
                                "⚠️ Location not available");
                        btnGetGPS.setEnabled(true);
                    }
                    updateSubmitButton();
                })
                .addOnFailureListener(e -> {
                    tvGPSStatus.setText(
                            "❌ GPS Error: " + e.getMessage());
                    btnGetGPS.setEnabled(true);
                });
    }

    @Override
    public void onRequestPermissionsResult(int req,
                                           @NonNull String[] perms,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        if (results.length > 0
                && results[0] ==
                PackageManager.PERMISSION_GRANTED) {
            if (req == REQ_LOCATION) getCurrentLocation();
            if (req == REQ_CAMERA)   takePhoto();
        }
    }

    void updateSubmitButton() {
        boolean ready = hasPhoto && hasGPS;
        btnSubmitProof.setEnabled(ready);
        btnSubmitProof.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor(
                                ready ? "#1A1A2E" : "#AAAAAA")));

        if (!hasPhoto && !hasGPS)
            tvSubmitStatus.setText(
                    "Upload photo + capture GPS to submit");
        else if (!hasPhoto)
            tvSubmitStatus.setText(
                    "📸 Photo required");
        else if (!hasGPS)
            tvSubmitStatus.setText(
                    "📍 GPS location required");
        else
            tvSubmitStatus.setText(
                    "✅ Ready to submit proof!");
    }

    // ── SUBMIT PROOF ──────────────────────────────────────

    void submitProof() {
        if (!hasPhoto || !hasGPS) {
            Toast.makeText(this,
                    "Photo and GPS both required!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        progressSubmit.setVisibility(View.VISIBLE);
        btnSubmitProof.setEnabled(false);
        btnSubmitProof.setText("Submitting...");

        String uid = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid() : "unknown";

        android.content.SharedPreferences prefs =
                getSharedPreferences("SmartSeva", MODE_PRIVATE);
        String volunteerName = prefs.getString(
                "volunteerName", "Volunteer");

        String time = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()).format(new Date());

        // Save proof to Firestore
        Map<String, Object> proof = new HashMap<>();
        proof.put("taskId",         taskId);
        proof.put("taskTitle",      taskTitle);
        proof.put("volunteerId",    uid);
        proof.put("volunteerName",  volunteerName);
        proof.put("latitude",       currentLat);
        proof.put("longitude",      currentLng);
        proof.put("gpsLocation",    currentLat + ", "
                + currentLng);
        proof.put("submittedAt",    time);
        proof.put("timestamp",
                System.currentTimeMillis());
        proof.put("status",         "Pending Verification");
        proof.put("hasPhoto",       true);
        proof.put("hasGPS",         true);

        db.collection("task_proofs").add(proof)
                .addOnSuccessListener(ref -> {
                    // Update task status
                    if (taskId != null) {
                        db.collection("tasks")
                                .document(taskId)
                                .update("status", "Proof Submitted",
                                        "proofId", ref.getId());
                    }

                    progressSubmit.setVisibility(View.GONE);
                    btnSubmitProof.setText("✅ Proof Submitted!");
                    btnSubmitProof.setBackgroundTintList(
                            android.content.res.ColorStateList
                                    .valueOf(Color.parseColor("#2E7D32")));

                    tvSubmitStatus.setText(
                            "✅ Proof submitted! Waiting for NGO verification.");
                    tvSubmitStatus.setTextColor(
                            Color.parseColor("#2E7D32"));

                    // Notify NGO
                    NotificationHelper.notifyNewTask(this,
                            "🔍 Proof submitted for: " + taskTitle);

                    Toast.makeText(this,
                            "Proof submitted successfully! ✅",
                            Toast.LENGTH_LONG).show();

                    loadProofHistory();
                })
                .addOnFailureListener(e -> {
                    progressSubmit.setVisibility(View.GONE);
                    btnSubmitProof.setEnabled(true);
                    btnSubmitProof.setText("Submit Proof");
                    Toast.makeText(this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ── VERIFY PROOFS (NGO) ───────────────────────────────

    void verifyAllProofs() {
        db.collection("task_proofs")
                .whereEqualTo("taskId", taskId)
                .whereEqualTo("status", "Pending Verification")
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Toast.makeText(this,
                                "No pending proofs to verify!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Show verify dialog for each proof
                    for (com.google.firebase.firestore
                            .DocumentSnapshot doc
                            : snap.getDocuments()) {
                        showVerifyDialog(doc);
                    }
                });
    }

    void showVerifyDialog(
            com.google.firebase.firestore.DocumentSnapshot doc) {

        String volName = doc.getString("volunteerName");
        String gps     = doc.getString("gpsLocation");
        String time    = doc.getString("submittedAt");

        new android.app.AlertDialog.Builder(this)
                .setTitle("🔍 Verify Proof")
                .setMessage(
                        "Volunteer: " + volName
                                + "\n📍 GPS: " + gps
                                + "\n📅 Submitted: " + time
                                + "\n\nVerify this completion proof?")
                .setPositiveButton("✅ Verify & Complete",
                        (d, w) -> {
                            // Mark as verified
                            doc.getReference().update(
                                    "status", "Verified ✅");

                            // Update task as completed
                            if (taskId != null) {
                                db.collection("tasks")
                                        .document(taskId)
                                        .update("status", "Completed ✅");
                            }

                            Toast.makeText(this,
                                    "Task marked as Completed! ✅",
                                    Toast.LENGTH_SHORT).show();

                            // Notify volunteer
                            NotificationHelper
                                    .notifyApplicationResult(
                                            this, taskTitle, true);

                            tvTaskStatus.setText("Completed ✅");
                            tvTaskStatus.setBackgroundColor(
                                    Color.parseColor("#2E7D32"));

                            loadProofHistory();
                        })
                .setNegativeButton("❌ Reject",
                        (d, w) -> {
                            doc.getReference().update(
                                    "status", "Rejected ❌");

                            NotificationHelper
                                    .notifyApplicationResult(
                                            this, taskTitle, false);

                            Toast.makeText(this,
                                    "Proof rejected.",
                                    Toast.LENGTH_SHORT).show();
                            loadProofHistory();
                        })
                .show();
    }

    // ── PROOF HISTORY ─────────────────────────────────────

    void loadProofHistory() {
        layoutProofHistory.removeAllViews();

        String query = taskId != null ? taskId : "";

        db.collection("task_proofs")
                .whereEqualTo("taskId", query)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No proofs submitted yet");
                        empty.setTextColor(
                                Color.parseColor("#9CA3AF"));
                        empty.setTextSize(13f);
                        empty.setGravity(Gravity.CENTER);
                        empty.setPadding(0, 24, 0, 24);
                        layoutProofHistory.addView(empty);
                        return;
                    }

                    for (com.google.firebase.firestore
                            .DocumentSnapshot doc
                            : snap.getDocuments()) {
                        addProofCard(doc);
                    }
                })
                .addOnFailureListener(e -> {
                    // Show local proof if submitted
                    if (hasPhoto && hasGPS) {
                        TextView local = new TextView(this);
                        local.setText("✅ Proof submitted locally");
                        local.setTextColor(
                                Color.parseColor("#2E7D32"));
                        local.setTextSize(13f);
                        layoutProofHistory.addView(local);
                    }
                });
    }

    void addProofCard(
            com.google.firebase.firestore.DocumentSnapshot doc) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(28, 20, 28, 20);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cp);

        // Status + volunteer row
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trp =
                new LinearLayout.LayoutParams(-1, -2);
        trp.setMargins(0, 0, 0, 10);
        topRow.setLayoutParams(trp);

        TextView volName = new TextView(this);
        volName.setText("👤 " +
                doc.getString("volunteerName"));
        volName.setTextSize(14f);
        volName.setTextColor(Color.parseColor("#111827"));
        volName.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams vnp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        volName.setLayoutParams(vnp);
        topRow.addView(volName);

        String status = doc.getString("status");
        TextView statusBadge = new TextView(this);
        statusBadge.setText(status);
        statusBadge.setTextSize(10f);
        statusBadge.setTextColor(Color.WHITE);
        statusBadge.setBackgroundColor(
                status != null && status.contains("Verified")
                        ? Color.parseColor("#2E7D32")
                        : status != null && status.contains("Rejected")
                        ? Color.parseColor("#C62828")
                        : Color.parseColor("#F57F17"));
        statusBadge.setPadding(16, 6, 16, 6);
        topRow.addView(statusBadge);
        card.addView(topRow);

        // GPS info
        TextView gps = new TextView(this);
        gps.setText("📍 GPS: " +
                doc.getString("gpsLocation"));
        gps.setTextSize(12f);
        gps.setTextColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams gp =
                new LinearLayout.LayoutParams(-1, -2);
        gp.setMargins(0, 0, 0, 6);
        gps.setLayoutParams(gp);
        card.addView(gps);

        // Photo indicator
        TextView photo = new TextView(this);
        photo.setText(Boolean.TRUE.equals(
                doc.getBoolean("hasPhoto"))
                ? "📸 Photo: Submitted"
                : "📸 Photo: Not submitted");
        photo.setTextSize(12f);
        photo.setTextColor(Color.parseColor("#374151"));
        LinearLayout.LayoutParams pp =
                new LinearLayout.LayoutParams(-1, -2);
        pp.setMargins(0, 0, 0, 6);
        photo.setLayoutParams(pp);
        card.addView(photo);

        // Time
        TextView time = new TextView(this);
        time.setText("🕐 " + doc.getString("submittedAt"));
        time.setTextSize(11f);
        time.setTextColor(Color.parseColor("#9CA3AF"));
        card.addView(time);

        layoutProofHistory.addView(card);
    }

    int getStatusColor(String status) {
        if (status == null) return Color.parseColor("#6B7280");
        if (status.contains("Completed"))
            return Color.parseColor("#2E7D32");
        if (status.contains("Progress"))
            return Color.parseColor("#1565C0");
        if (status.contains("Proof"))
            return Color.parseColor("#F57F17");
        return Color.parseColor("#6B7280");
    }
}