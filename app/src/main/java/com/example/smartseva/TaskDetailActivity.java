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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class TaskDetailActivity extends AppCompatActivity {

    // Firebase
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Views
    ImageView imgTaskDetail;
    TextView tvDetailTitle, tvDetailCategory, tvDetailSkill, tvDetailDate;
    TextView tvDetailVolunteers, tvDetailNGO, tvDetailDesc;
    TextView tvDetailLocation, tvUrgencyBadge;
    WebView webViewMap;
    Button btnBack, btnOpenFullMap, btnApply;
    LinearLayout layoutAlreadyApplied, layoutApplyBar;

    // Task Data (Intent se aayega)
    String taskTitle, taskDesc, taskCategory, taskUrgency;
    String taskDate, taskLocation, taskNGO, taskSkill;
    int taskVolunteers;
    boolean alreadyApplied = false;

    // Notification
    private static final String CHANNEL_ID = "smart_seva_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_detail);

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

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // ── Get Intent Data ──
        Intent intent = getIntent();
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

        // ── Populate UI ──
        populateUI();

        // ── Map ──
        setupMap();

        // ── Listeners ──
        btnBack.setOnClickListener(v -> finish());

        btnOpenFullMap.setOnClickListener(v -> openInGoogleMaps());

        btnApply.setOnClickListener(v -> applyForTask());

        // ── Notification Channel ──
        createNotificationChannel();
    }

    // ═══════════════════════════════════════
    // POPULATE UI
    // ═══════════════════════════════════════

    void populateUI() {
        tvDetailTitle.setText(taskTitle != null ? taskTitle : "Task Title");
        tvDetailDesc.setText(taskDesc != null ? taskDesc : "No description provided.");
        tvDetailCategory.setText(taskCategory != null ? taskCategory : "General");
        tvDetailSkill.setText(taskSkill != null ? taskSkill : "Any Skill");
        tvDetailDate.setText(taskDate != null ? taskDate : "—");
        tvDetailVolunteers.setText(taskVolunteers + " needed");
        tvDetailNGO.setText(taskNGO != null ? taskNGO : "—");
        tvDetailLocation.setText("📍 " + (taskLocation != null ? taskLocation : "Location not specified"));

        // Urgency Badge
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

        // Already applied check
        if (alreadyApplied) {
            layoutAlreadyApplied.setVisibility(android.view.View.VISIBLE);
            btnApply.setText("Applied ✅");
            btnApply.setEnabled(false);
            btnApply.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
        }
    }

    // ═══════════════════════════════════════
    // MAP SETUP
    // ═══════════════════════════════════════

    void setupMap() {
        WebSettings settings = webViewMap.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webViewMap.setWebViewClient(new WebViewClient());

        // OpenStreetMap (free, no API key needed)
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
                "     .bindPopup('<b>" + (taskTitle != null ? taskTitle.replace("'", "\\'") : "Task") + "</b><br>" +
                (taskLocation != null ? taskLocation.replace("'", "\\'") : "Location") + "')" +
                "     .openPopup();" +
                "  } else {" +
                "    document.getElementById('map').innerHTML=" +
                "    '<p style=text-align:center;padding:20px>Location not found on map</p>';" +
                "  }" +
                "})" +
                ".catch(e=>{" +
                "    document.getElementById('map').innerHTML=" +
                "    '<p style=text-align:center;padding:20px>Map unavailable offline</p>';" +
                "});" +
                "</script></body></html>";

        webViewMap.loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null);
    }

    // ═══════════════════════════════════════
    // OPEN IN GOOGLE MAPS
    // ═══════════════════════════════════════

    void openInGoogleMaps() {
        String location = taskLocation != null ? taskLocation : "Raipur";
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(location));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Google Maps nahi hai toh browser mein kholo
            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(location)));
            startActivity(browserIntent);
        }
    }

    // ═══════════════════════════════════════
    // APPLY FOR TASK
    // ═══════════════════════════════════════

    void applyForTask() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to apply", Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("Apply for Task")
                .setMessage("Do you want to apply for:\n\n\"" + taskTitle + "\"?\n\n" +
                        "📍 " + taskLocation + "\n📅 " + taskDate)
                .setPositiveButton("Yes, Apply!", (dialog, which) -> {

                    String uid = mAuth.getCurrentUser().getUid();

                    // 1. Pehle Volunteer ki details fetch karo
                    db.collection("volunteer_users").document(uid).get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    String name = documentSnapshot.getString("name");
                                    String city = documentSnapshot.getString("city");
                                    
                                    // Skills string banao
                                    StringBuilder skills = new StringBuilder();
                                    if (Boolean.TRUE.equals(documentSnapshot.getBoolean("teaching"))) skills.append("Teaching, ");
                                    if (Boolean.TRUE.equals(documentSnapshot.getBoolean("medical"))) skills.append("Medical, ");
                                    if (Boolean.TRUE.equals(documentSnapshot.getBoolean("food"))) skills.append("Food, ");
                                    if (Boolean.TRUE.equals(documentSnapshot.getBoolean("event"))) skills.append("Event, ");
                                    // ... aur bhi skills add kar sakte hain

                                    // 2. Application save karo
                                    Map<String, Object> app = new HashMap<>();
                                    app.put("volunteerId", uid);
                                    app.put("taskTitle", taskTitle);
                                    app.put("name", name != null ? name : "Unknown");
                                    app.put("city", city != null ? city : "Unknown");
                                    app.put("skills", skills.length() > 2 ? skills.substring(0, skills.length()-2) : "General");
                                    app.put("appliedTime", "Just now");
                                    app.put("status", "Pending");
                                    app.put("availability", documentSnapshot.getString("availableDays"));
                                    app.put("experience", 1); // Sample experience

                                    db.collection("applications")
                                            .add(app)
                                            .addOnSuccessListener(ref -> {
                                                showLocalNotification();
                                                updateUIForApplied();
                                                Toast.makeText(this, "Applied successfully! 🎉", Toast.LENGTH_LONG).show();
                                            });
                                }
                            });
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
                android.content.res.ColorStateList.valueOf(Color.parseColor("#AAAAAA")));
    }

    // ═══════════════════════════════════════
    // NOTIFICATION
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Application Submitted! ✅")
                .setContentText("You applied for: " + taskTitle)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("You applied for \"" + taskTitle + "\" at " + taskLocation +
                                ".\n\nThe NGO will review your application and contact you soon."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) getSystemService(android.content.Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}