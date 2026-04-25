package com.example.smartseva;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    // Views
    ImageView imgTaskDetail;
    TextView tvDetailTitle, tvDetailCategory, tvDetailSkill, tvDetailDate;
    TextView tvDetailVolunteers, tvDetailNGO, tvDetailDesc;
    TextView tvDetailLocation, tvUrgencyBadge;
    WebView webViewMap;
    Button btnBack, btnOpenFullMap, btnApply;
    LinearLayout layoutAlreadyApplied, layoutApplyBar;

    // Task Data
    String taskId, taskTitle, taskDesc, taskCategory, taskUrgency;
    String taskDate, taskLocation, taskNGO, taskSkill;
    int taskVolunteers;
    boolean alreadyApplied = false;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    private static final String CHANNEL_ID = "smart_seva_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        // ── Views ──
        imgTaskDetail      = findViewById(R.id.imgTaskDetail);
        tvDetailTitle      = findViewById(R.id.tvDetailTitle);
        tvDetailCategory   = findViewById(R.id.tvDetailCategory);
        tvDetailSkill      = findViewById(R.id.tvDetailSkill);
        tvDetailDate       = findViewById(R.id.tvDetailDate);
        tvDetailVolunteers = findViewById(R.id.tvDetailVolunteers);
        tvDetailNGO        = findViewById(R.id.tvDetailNGO);
        tvDetailDesc       = findViewById(R.id.tvDetailDesc);
        tvDetailLocation   = findViewById(R.id.tvDetailLocation);
        tvUrgencyBadge     = findViewById(R.id.tvUrgencyBadge);
        webViewMap         = findViewById(R.id.webViewMap);
        btnBack            = findViewById(R.id.btnBack);
        btnOpenFullMap     = findViewById(R.id.btnOpenFullMap);
        btnApply           = findViewById(R.id.btnApply);
        layoutAlreadyApplied = findViewById(R.id.layoutAlreadyApplied);
        layoutApplyBar     = findViewById(R.id.layoutApplyBar);

        // ── Intent Data ──
        Intent intent  = getIntent();
        taskId         = intent.getStringExtra("taskId");
        taskTitle      = intent.getStringExtra("taskTitle");
        taskDesc       = intent.getStringExtra("taskDesc");
        taskCategory   = intent.getStringExtra("taskCategory");
        taskUrgency    = intent.getStringExtra("taskUrgency");
        taskDate       = intent.getStringExtra("taskDate");
        taskLocation   = intent.getStringExtra("taskLocation");
        taskNGO        = intent.getStringExtra("taskNGO");
        taskSkill      = intent.getStringExtra("taskSkill");
        taskVolunteers = intent.getIntExtra("taskVolunteers", 1);
        alreadyApplied = intent.getBooleanExtra("alreadyApplied", false);

        populateUI();
        setupMap();
        createNotificationChannel();

        // ✅ Check if already applied from Firestore
        checkIfAlreadyApplied();

        btnBack.setOnClickListener(v -> finish());
        btnOpenFullMap.setOnClickListener(v -> openInGoogleMaps());
        btnApply.setOnClickListener(v -> applyForTask());
    }

    // ═══════════════════════════════════════
    // FIREBASE — Check existing application
    // ═══════════════════════════════════════

    void checkIfAlreadyApplied() {
        if (taskId == null || mAuth.getCurrentUser() == null) return;

        String volunteerId = mAuth.getCurrentUser().getUid();

        db.collection("applications")
                .whereEqualTo("taskId",      taskId)
                .whereEqualTo("volunteerId", volunteerId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        alreadyApplied = true;
                        setAppliedUI();
                    }
                });
    }

    // ═══════════════════════════════════════
    // FIREBASE — Apply for task
    // ═══════════════════════════════════════

    void applyForTask() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Apply for Task")
                .setMessage("Do you want to apply for:\n\n\""
                        + taskTitle + "\"?\n\n📍 "
                        + taskLocation + "\n📅 " + taskDate)
                .setPositiveButton("Yes, Apply!", (dialog, which) -> {

                    String volunteerId = mAuth.getCurrentUser().getUid();

                    // ✅ Save application to Firestore
                    Map<String, Object> application = new HashMap<>();
                    application.put("taskId",      taskId);
                    application.put("taskTitle",   taskTitle);
                    application.put("taskLocation",taskLocation);
                    application.put("taskCategory",taskCategory);
                    application.put("volunteerId", volunteerId);
                    application.put("ngoId",       taskNGO);
                    application.put("status",      "Pending");
                    application.put("appliedAt",   FieldValue.serverTimestamp());

                    db.collection("applications")
                            .add(application)
                            .addOnSuccessListener(ref -> {
                                // ✅ Show notification
                                showLocalNotification();

                                // ✅ Update UI
                                alreadyApplied = true;
                                setAppliedUI();

                                Toast.makeText(this,
                                        "Applied successfully! NGO will contact you. 🎉",
                                        Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this,
                                            "Failed to apply: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ═══════════════════════════════════════
    // UI HELPERS
    // ═══════════════════════════════════════

    void populateUI() {
        tvDetailTitle.setText(taskTitle != null ? taskTitle : "Task Title");
        tvDetailDesc.setText(taskDesc != null ? taskDesc : "No description provided.");
        tvDetailCategory.setText(taskCategory != null ? taskCategory : "General");
        tvDetailSkill.setText(taskSkill != null ? taskSkill : "Any Skill");
        tvDetailDate.setText(taskDate != null ? taskDate : "—");
        tvDetailVolunteers.setText(taskVolunteers + " needed");
        tvDetailNGO.setText(taskNGO != null ? taskNGO : "—");
        tvDetailLocation.setText("📍 " + (taskLocation != null
                ? taskLocation : "Location not specified"));

        if (taskUrgency != null) {
            tvUrgencyBadge.setText(taskUrgency);
            if (taskUrgency.contains("Critical")) {
                tvUrgencyBadge.setBackgroundColor(Color.parseColor("#C62828"));
            } else if (taskUrgency.contains("Moderate")) {
                tvUrgencyBadge.setBackgroundColor(Color.parseColor("#F57F17"));
            } else {
                tvUrgencyBadge.setBackgroundColor(Color.parseColor("#2E7D32"));
            }
        }

        if (alreadyApplied) setAppliedUI();
    }

    void setAppliedUI() {
        layoutAlreadyApplied.setVisibility(android.view.View.VISIBLE);
        btnApply.setText("Applied ✅");
        btnApply.setEnabled(false);
        btnApply.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#AAAAAA")));
    }

    // ═══════════════════════════════════════
    // MAP SETUP (unchanged)
    // ═══════════════════════════════════════

    void setupMap() {
        WebSettings settings = webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webViewMap.setWebViewClient(new WebViewClient());

        String location = taskLocation != null ? taskLocation : "Raipur, Chhattisgarh";
        String encodedLocation = Uri.encode(location);

        String mapHtml = "<!DOCTYPE html><html><head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                "<style>html,body,#map{width:100%;height:100%;margin:0;padding:0;}</style>" +
                "</head><body><div id='map'></div>" +
                "<script>" +
                "fetch('https://nominatim.openstreetmap.org/search?q=" + encodedLocation +
                "&format=json&limit=1')" +
                ".then(r=>r.json())" +
                ".then(data=>{" +
                "  if(data.length>0){" +
                "    var lat=parseFloat(data[0].lat);" +
                "    var lon=parseFloat(data[0].lon);" +
                "    var map=L.map('map').setView([lat,lon],14);" +
                "    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);" +
                "    L.marker([lat,lon]).addTo(map)" +
                "     .bindPopup('<b>" +
                (taskTitle != null ? taskTitle.replace("'", "\\'") : "Task") +
                "</b><br>" +
                (taskLocation != null ? taskLocation.replace("'", "\\'") : "Location") +
                "')" +
                "     .openPopup();" +
                "  } else {" +
                "    document.getElementById('map').innerHTML=" +
                "    '<p style=text-align:center;padding:20px>Location not found</p>';" +
                "  }" +
                "})" +
                ".catch(e=>{" +
                "    document.getElementById('map').innerHTML=" +
                "    '<p style=text-align:center;padding:20px>Map unavailable offline</p>';" +
                "});" +
                "</script></body></html>";

        webViewMap.loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null);
    }

    void openInGoogleMaps() {
        String location = taskLocation != null ? taskLocation : "Raipur";
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query="
                            + Uri.encode(location))));
        }
    }

    // ═══════════════════════════════════════
    // NOTIFICATION (unchanged)
    // ═══════════════════════════════════════

    void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Smart Seva Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Task application notifications");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    void showLocalNotification() {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Application Submitted! ✅")
                        .setContentText("You applied for: " + taskTitle)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("You applied for \"" + taskTitle
                                        + "\" at " + taskLocation
                                        + ".\n\nThe NGO will review your application soon."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager manager = (NotificationManager)
                getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (manager != null)
            manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}