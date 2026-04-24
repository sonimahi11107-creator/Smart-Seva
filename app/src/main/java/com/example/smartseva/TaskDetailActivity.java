package com.example.smartseva;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    ImageView imgTaskDetail;
    TextView tvDetailTitle, tvDetailCategory, tvDetailSkill, tvDetailDate;
    TextView tvDetailVolunteers, tvDetailNGO, tvDetailDesc;
    TextView tvDetailLocation, tvUrgencyBadge;
    WebView webViewMap;
    Button btnBack, btnOpenFullMap, btnApply;
    LinearLayout layoutAlreadyApplied, layoutApplyBar;

    String taskTitle, taskDesc, taskCategory, taskUrgency;
    String taskDate, taskLocation, taskNGO, taskSkill;
    int taskVolunteers;
    boolean alreadyApplied = false;

    private static final String CHANNEL_ID = "smart_seva_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

        imgTaskDetail        = findViewById(R.id.imgTaskDetail);
        tvDetailTitle        = findViewById(R.id.tvDetailTitle);
        tvDetailCategory     = findViewById(R.id.tvDetailCategory);
        tvDetailSkill        = findViewById(R.id.tvDetailSkill);
        tvDetailDate         = findViewById(R.id.tvDetailDate);
        tvDetailVolunteers   = findViewById(R.id.tvDetailVolunteers);
        tvDetailNGO          = findViewById(R.id.tvDetailNGO);
        tvDetailDesc         = findViewById(R.id.tvDetailDesc);
        tvDetailLocation     = findViewById(R.id.tvDetailLocation);
        tvUrgencyBadge       = findViewById(R.id.tvUrgencyBadge);
        webViewMap           = findViewById(R.id.webViewMap);
        btnBack              = findViewById(R.id.btnBack);
        btnOpenFullMap       = findViewById(R.id.btnOpenFullMap);
        btnApply             = findViewById(R.id.btnApply);
        layoutAlreadyApplied = findViewById(R.id.layoutAlreadyApplied);
        layoutApplyBar       = findViewById(R.id.layoutApplyBar);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        Intent intent  = getIntent();
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

        btnBack.setOnClickListener(v -> finish());
        btnOpenFullMap.setOnClickListener(v -> openInGoogleMaps());
        btnApply.setOnClickListener(v -> applyForTask());
    }

    // ── POPULATE UI ──────────────────────────────────────

    void populateUI() {
        tvDetailTitle.setText(taskTitle != null ? taskTitle : "Task Title");
        tvDetailDesc.setText(taskDesc != null ? taskDesc : "No description.");
        tvDetailCategory.setText(taskCategory != null ? taskCategory : "General");
        tvDetailSkill.setText(taskSkill != null ? taskSkill : "Any Skill");
        tvDetailDate.setText(taskDate != null ? taskDate : "—");
        tvDetailVolunteers.setText(taskVolunteers + " needed");
        tvDetailNGO.setText(taskNGO != null ? taskNGO : "—");
        tvDetailLocation.setText("📍 " + (taskLocation != null
                ? taskLocation : "Not specified"));

        if (taskUrgency != null) {
            tvUrgencyBadge.setText(taskUrgency);
            tvUrgencyBadge.setBackgroundColor(
                    taskUrgency.contains("Critical") ? Color.parseColor("#C62828")
                            : taskUrgency.contains("Moderate") ? Color.parseColor("#F57F17")
                            : Color.parseColor("#2E7D32"));
        }

        if (alreadyApplied) {
            layoutAlreadyApplied.setVisibility(android.view.View.VISIBLE);
            btnApply.setText("Applied ✅");
            btnApply.setEnabled(false);
            btnApply.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            Color.parseColor("#AAAAAA")));
        }
    }

    // ── MAP ───────────────────────────────────────────────

    void setupMap() {
        WebSettings s = webViewMap.getSettings();
        s.setJavaScriptEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webViewMap.setWebViewClient(new WebViewClient());

        String loc     = taskLocation != null ? taskLocation : "Raipur, Chhattisgarh";
        String encLoc  = Uri.encode(loc);
        String title   = taskTitle != null ? taskTitle.replace("'", "\\'") : "Task";
        String locClean= loc.replace("'", "\\'");

        String html =
                "<!DOCTYPE html><html><head>" +
                        "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                        "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'/>" +
                        "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>" +
                        "<style>html,body,#map{width:100%;height:100%;margin:0;padding:0}</style>" +
                        "</head><body><div id='map'></div><script>" +
                        "fetch('https://nominatim.openstreetmap.org/search?q=" + encLoc +
                        "&format=json&limit=1').then(r=>r.json()).then(d=>{" +
                        "if(d.length>0){var la=parseFloat(d[0].lat),lo=parseFloat(d[0].lon);" +
                        "var m=L.map('map').setView([la,lo],14);" +
                        "L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(m);" +
                        "L.marker([la,lo]).addTo(m).bindPopup('<b>" + title + "</b><br>" + locClean + "').openPopup();" +
                        "}else{document.getElementById('map').innerHTML='<p style=text-align:center;padding:20px>Location not found</p>';}}" +
                        ").catch(e=>{document.getElementById('map').innerHTML='<p style=text-align:center;padding:20px>Map unavailable</p>';});" +
                        "</script></body></html>";

        webViewMap.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    // ── GOOGLE MAPS ───────────────────────────────────────

    void openInGoogleMaps() {
        String loc = taskLocation != null ? taskLocation : "Raipur";
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(loc));
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        i.setPackage("com.google.android.apps.maps");
        if (i.resolveActivity(getPackageManager()) != null) {
            startActivity(i);
        } else {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query="
                            + Uri.encode(loc))));
        }
    }

    // ── APPLY ─────────────────────────────────────────────

    void applyForTask() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(TaskDetailActivity.this,
                    "Please login to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(TaskDetailActivity.this)
                .setTitle("Apply for Task")
                .setMessage("Apply for:\n\n\"" + taskTitle + "\"\n\n📍 "
                        + taskLocation + "\n📅 " + taskDate)
                .setPositiveButton("Yes, Apply!", (dialog, which) -> {

                    String uid = mAuth.getCurrentUser().getUid();

                    db.collection("volunteer_users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (doc.exists()) {
                                    String name = doc.getString("name");
                                    String city = doc.getString("city");

                                    StringBuilder skills = new StringBuilder();
                                    if (Boolean.TRUE.equals(doc.getBoolean("teaching")))
                                        skills.append("Teaching, ");
                                    if (Boolean.TRUE.equals(doc.getBoolean("medical")))
                                        skills.append("Medical, ");
                                    if (Boolean.TRUE.equals(doc.getBoolean("food")))
                                        skills.append("Food, ");
                                    if (Boolean.TRUE.equals(doc.getBoolean("event")))
                                        skills.append("Event, ");

                                    Map<String, Object> app = new HashMap<>();
                                    app.put("volunteerId",  uid);
                                    app.put("taskTitle",    taskTitle);
                                    app.put("name",         name != null ? name : "Unknown");
                                    app.put("city",         city != null ? city : "Unknown");
                                    app.put("skills",       skills.length() > 2
                                            ? skills.substring(0, skills.length() - 2) : "General");
                                    app.put("appliedTime",  "Just now");
                                    app.put("status",       "Pending");
                                    app.put("availability", doc.getString("availableDays"));
                                    app.put("experience",   1);

                                    db.collection("applications").add(app)
                                            .addOnSuccessListener(ref -> {
                                                showLocalNotification();
                                                updateUIForApplied();
                                                NotificationHelper.notifyNewApplication(
                                                        TaskDetailActivity.this,
                                                        mAuth.getCurrentUser().getEmail(),
                                                        taskTitle);
                                                Toast.makeText(TaskDetailActivity.this,
                                                        "Applied successfully! 🎉",
                                                        Toast.LENGTH_LONG).show();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(TaskDetailActivity.this,
                                                            "Error: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show());
                                } else {
                                    Toast.makeText(TaskDetailActivity.this,
                                            "Profile nahi mili! Pehle profile complete karo.",
                                            Toast.LENGTH_LONG).show();
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(TaskDetailActivity.this,
                                            "Error: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void updateUIForApplied() {
        alreadyApplied = true;
        layoutAlreadyApplied.setVisibility(android.view.View.VISIBLE);
        btnApply.setText("Applied ✅");
        btnApply.setEnabled(false);
        btnApply.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        Color.parseColor("#AAAAAA")));
    }

    // ── NOTIFICATION ──────────────────────────────────────

    void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Smart Seva",
                    NotificationManager.IMPORTANCE_HIGH);
            NotificationManager nm =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    void showLocalNotification() {
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(TaskDetailActivity.this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle("Application Submitted! ✅")
                        .setContentText("You applied for: " + taskTitle)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("You applied for \"" + taskTitle
                                        + "\" at " + taskLocation
                                        + ".\n\nNGO will contact you soon."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify((int) System.currentTimeMillis(), b.build());
    }
}