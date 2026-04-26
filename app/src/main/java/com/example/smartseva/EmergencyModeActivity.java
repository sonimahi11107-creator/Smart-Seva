package com.example.smartseva;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.*;

public class EmergencyModeActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Header
    TextView tvEmergencyStatus, tvLastActivated;

    // Emergency type buttons
    LinearLayout btnFlood, btnMedical, btnFood, btnCustom;

    // Active banner
    LinearLayout layoutActiveBanner;
    TextView tvActiveBannerType, tvActiveBannerMsg, tvActiveBannerTime;
    Button btnDeactivate;

    // History
    LinearLayout layoutHistory;

    // State
    boolean isActive = false;
    String activeType = "";
    Handler blinkHandler = new Handler(Looper.getMainLooper());
    Runnable blinkRunnable;
    boolean blinkState = false;

    // ✅ Real-time listener — memory leak se bachao
    ListenerRegistration emergencyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_mode);
        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();
        bindViews();

        // Loading state pehle dikhao
        tvEmergencyStatus.setText("⏳ Checking...");
        layoutActiveBanner.setVisibility(View.GONE);

        setListeners();
        listenCurrentEmergency(); // ✅ get() ki jagah real-time listener
        loadHistory();
    }

    void bindViews() {
        tvEmergencyStatus  = findViewById(R.id.tvEmergencyStatus);
        tvLastActivated    = findViewById(R.id.tvLastActivated);
        btnFlood           = findViewById(R.id.btnFlood);
        btnMedical         = findViewById(R.id.btnMedical);
        btnFood            = findViewById(R.id.btnFood);
        btnCustom          = findViewById(R.id.btnCustom);
        layoutActiveBanner = findViewById(R.id.layoutActiveBanner);
        tvActiveBannerType = findViewById(R.id.tvActiveBannerType);
        tvActiveBannerMsg  = findViewById(R.id.tvActiveBannerMsg);
        tvActiveBannerTime = findViewById(R.id.tvActiveBannerTime);
        btnDeactivate      = findViewById(R.id.btnDeactivate);
        layoutHistory      = findViewById(R.id.layoutHistory);
    }

    void setListeners() {
        findViewById(R.id.btnBackEmergency).setOnClickListener(
                v -> finish());

        btnFlood.setOnClickListener(v ->
                confirmActivate("🌊 Flood / Natural Disaster",
                        "Flood emergency declared!\nSaare volunteers turant " +
                                "relief operations ke liye report karein."));

        btnMedical.setOnClickListener(v ->
                confirmActivate("🏥 Medical Emergency",
                        "Medical emergency declared!\nMedical volunteers " +
                                "turant nearest center pe report karein."));

        btnFood.setOnClickListener(v ->
                confirmActivate("🍽️ Food Crisis",
                        "Food crisis declared!\nVolunteers food distribution " +
                                "points pe report karein."));

        btnCustom.setOnClickListener(v -> showCustomDialog());

        btnDeactivate.setOnClickListener(v -> confirmDeactivate());
    }

    // ── REAL-TIME LISTENER (get() replace) ───────────────
    void listenCurrentEmergency() {
        emergencyListener = db.collection("emergency")
                .document("current")
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null) {
                        showNormalStatus();
                        return;
                    }
                    if (doc.exists() &&
                            Boolean.TRUE.equals(doc.getBoolean("active"))) {
                        String type = doc.getString("type");
                        String msg  = doc.getString("message");

                        // ✅ Server timestamp handle karo
                        Long ts = null;
                        Object rawTs = doc.get("timestamp");
                        if (rawTs instanceof com.google.firebase.Timestamp) {
                            ts = ((com.google.firebase.Timestamp) rawTs)
                                    .toDate().getTime();
                        } else if (rawTs instanceof Long) {
                            ts = (Long) rawTs;
                        }

                        showActiveBanner(type, msg, ts);
                    } else {
                        showNormalStatus();
                    }
                });
    }

    // ── CONFIRM ACTIVATE ─────────────────────────────────
    void confirmActivate(String type, String message) {
        if (isActive) {
            Toast.makeText(this,
                    "Emergency pehle se active hai! Pehle deactivate karo.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Emergency Confirm")
                .setMessage("Yeh action saare volunteers ko alert karega!\n\n" +
                        "Type: " + type + "\n\nConfirm?")
                .setPositiveButton("🚨 ACTIVATE!", (d, w) ->
                        activate(type, message))
                .setNegativeButton("Cancel", null)
                .show();
    }

    void showCustomDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 24, 48, 0);

        TextView label = new TextView(this);
        label.setText("Emergency type:");
        label.setTextSize(13f);
        layout.addView(label);

        EditText etType = new EditText(this);
        etType.setHint("e.g. Fire, Earthquake...");
        layout.addView(etType);

        TextView label2 = new TextView(this);
        label2.setText("Message:");
        label2.setTextSize(13f);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 16, 0, 0);
        label2.setLayoutParams(lp);
        layout.addView(label2);

        EditText etMsg = new EditText(this);
        etMsg.setHint("Volunteers ke liye message...");
        etMsg.setMinLines(3);
        layout.addView(etMsg);

        new android.app.AlertDialog.Builder(this)
                .setTitle("✏️ Custom Emergency")
                .setView(layout)
                .setPositiveButton("🚨 Activate!", (d, w) -> {
                    String type = etType.getText().toString().trim();
                    String msg  = etMsg.getText().toString().trim();
                    if (type.isEmpty() || msg.isEmpty()) {
                        Toast.makeText(this,
                                "Sab fields fill karo!",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    confirmActivate("🚨 " + type, msg);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── ACTIVATE ─────────────────────────────────────────
    void activate(String type, String message) {
        isActive   = true;
        activeType = type;

        // 1. Banner dikhao
        showActiveBanner(type, message, System.currentTimeMillis());

        // 2. Notification bhejo
        NotificationHelper.sendEmergencyAlert(this, type, message);

        // 3. ✅ Server timestamp use karo
        Map<String, Object> data = new HashMap<>();
        data.put("type",        type);
        data.put("message",     message);
        data.put("active",      true);
        data.put("timestamp",   FieldValue.serverTimestamp()); // ✅ fixed
        data.put("activatedBy",
                mAuth.getCurrentUser() != null
                        ? mAuth.getCurrentUser().getUid() : "unknown");

        db.collection("emergency").document("current")
                .set(data)
                .addOnSuccessListener(v -> {
                    db.collection("emergency_history").add(data);
                    loadHistory();
                });

        // 4. Volunteers count karo
        db.collection("volunteer_users").get()
                .addOnSuccessListener(snap ->
                        Toast.makeText(this,
                                "🚨 " + snap.size() +
                                        " volunteers ko alert bheja gaya!",
                                Toast.LENGTH_LONG).show());
    }

    // ── SHOW ACTIVE BANNER ────────────────────────────────
    void showActiveBanner(String type, String msg, Long timestamp) {
        isActive = true;
        layoutActiveBanner.setVisibility(View.VISIBLE);
        tvActiveBannerType.setText(type);
        tvActiveBannerMsg.setText(msg);

        if (timestamp != null && timestamp > 0) {
            String time = new SimpleDateFormat(
                    "dd MMM, hh:mm a", Locale.getDefault())
                    .format(new Date(timestamp));
            tvActiveBannerTime.setText("Activated: " + time);
            tvLastActivated.setText("Last activated: " + time);
        }

        tvEmergencyStatus.setText("🔴 EMERGENCY ACTIVE");
        tvEmergencyStatus.setTextColor(Color.parseColor("#FF1744"));

        startBlinking();

        btnFlood.setAlpha(0.4f);
        btnMedical.setAlpha(0.4f);
        btnFood.setAlpha(0.4f);
        btnCustom.setAlpha(0.4f);
    }

    void showNormalStatus() {
        isActive = false;
        layoutActiveBanner.setVisibility(View.GONE);
        tvEmergencyStatus.setText("🟢 All Clear — No Emergency");
        tvEmergencyStatus.setTextColor(Color.parseColor("#00E676"));
        stopBlinking();

        btnFlood.setAlpha(1f);
        btnMedical.setAlpha(1f);
        btnFood.setAlpha(1f);
        btnCustom.setAlpha(1f);
    }

    // ── BLINKING EFFECT ───────────────────────────────────
    void startBlinking() {
        blinkRunnable = new Runnable() {
            @Override
            public void run() {
                blinkState = !blinkState;
                layoutActiveBanner.setBackgroundColor(
                        Color.parseColor(blinkState
                                ? "#B71C1C" : "#FF1744"));
                blinkHandler.postDelayed(this, 800);
            }
        };
        blinkHandler.post(blinkRunnable);
    }

    void stopBlinking() {
        if (blinkRunnable != null)
            blinkHandler.removeCallbacks(blinkRunnable);
    }

    // ── DEACTIVATE ────────────────────────────────────────
    void confirmDeactivate() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Deactivate Emergency?")
                .setMessage("Emergency mode band karna chahte ho?")
                .setPositiveButton("Haan, Band Karo", (d, w) -> {
                    db.collection("emergency")
                            .document("current")
                            .update("active", false)
                            .addOnSuccessListener(v -> {
                                showNormalStatus();
                                Toast.makeText(this,
                                        "✅ Emergency deactivated!",
                                        Toast.LENGTH_SHORT).show();
                                loadHistory();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── HISTORY ───────────────────────────────────────────
    void loadHistory() {
        layoutHistory.removeAllViews();

        db.collection("emergency_history")
                .orderBy("timestamp",
                        com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("Koi emergency history nahi hai");
                        empty.setTextColor(Color.parseColor("#9E9EB8"));
                        empty.setTextSize(13f);
                        empty.setPadding(0, 8, 0, 8);
                        layoutHistory.addView(empty);
                        return;
                    }

                    for (com.google.firebase.firestore.DocumentSnapshot doc
                            : snap.getDocuments()) {
                        String type    = doc.getString("type");
                        Boolean active = doc.getBoolean("active");

                        // ✅ Server timestamp handle karo
                        String time = "Unknown";
                        Object rawTs = doc.get("timestamp");
                        if (rawTs instanceof com.google.firebase.Timestamp) {
                            time = new SimpleDateFormat(
                                    "dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    .format(((com.google.firebase.Timestamp) rawTs)
                                            .toDate());
                        } else if (rawTs instanceof Long) {
                            time = new SimpleDateFormat(
                                    "dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    .format(new Date((Long) rawTs));
                        }

                        addHistoryCard(type, time,
                                Boolean.TRUE.equals(active));
                    }
                })
                .addOnFailureListener(e -> {
                    TextView err = new TextView(this);
                    err.setText("History load nahi hui");
                    err.setTextColor(Color.parseColor("#9E9EB8"));
                    err.setTextSize(12f);
                    layoutHistory.addView(err);
                });
    }

    void addHistoryCard(String type, String time, boolean active) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundColor(Color.parseColor("#1A1A2E"));
        card.setPadding(24, 16, 24, 16);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 8);
        card.setLayoutParams(cp);

        TextView icon = new TextView(this);
        icon.setText("🚨");
        icon.setTextSize(18f);
        LinearLayout.LayoutParams ip =
                new LinearLayout.LayoutParams(-2, -2);
        ip.setMargins(0, 0, 16, 0);
        icon.setLayoutParams(ip);
        card.addView(icon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        textCol.setLayoutParams(tp);

        TextView tvType = new TextView(this);
        tvType.setText(type != null ? type : "Emergency");
        tvType.setTextSize(13f);
        tvType.setTextColor(Color.parseColor("#E0E0E0"));
        tvType.setTypeface(null, Typeface.BOLD);
        textCol.addView(tvType);

        TextView tvTime = new TextView(this);
        tvTime.setText(time);
        tvTime.setTextSize(11f);
        tvTime.setTextColor(Color.parseColor("#9E9EB8"));
        textCol.addView(tvTime);
        card.addView(textCol);

        TextView status = new TextView(this);
        status.setText(active ? "ACTIVE" : "Ended");
        status.setTextSize(10f);
        status.setTextColor(Color.WHITE);
        status.setBackgroundColor(Color.parseColor(
                active ? "#FF1744" : "#2E7D32"));
        status.setPadding(16, 6, 16, 6);
        card.addView(status);

        layoutHistory.addView(card);
    }

    // ✅ Memory leak fix — listener detach karo
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBlinking();
        if (emergencyListener != null)
            emergencyListener.remove();
    }
}