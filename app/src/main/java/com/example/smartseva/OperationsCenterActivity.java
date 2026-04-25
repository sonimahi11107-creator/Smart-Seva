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
import com.google.firebase.firestore.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class OperationsCenterActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    // Header
    TextView tvOpsTitle, tvOpsSubtitle, tvLiveIndicator;

    // Stats
    TextView tvStatTasks, tvStatVolunteers, tvStatApplications,
            tvStatCritical, tvStatCompleted, tvStatPending;

    // Chart
    LinearLayout layoutChart;

    // Activity Feed
    LinearLayout layoutFeed;
    TextView tvFeedEmpty;

    // Auto refresh
    Handler refreshHandler = new Handler(Looper.getMainLooper());
    Runnable refreshRunnable;
    boolean isLive = true;

    // Data
    List<ActivityItem> feedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operations_center);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        bindViews();
        setListeners();
        loadAllData();
        startLiveRefresh();
    }

    void bindViews() {
        tvOpsTitle        = findViewById(R.id.tvOpsTitle);
        tvOpsSubtitle     = findViewById(R.id.tvOpsSubtitle);
        tvLiveIndicator   = findViewById(R.id.tvLiveIndicator);
        tvStatTasks       = findViewById(R.id.tvStatTasks);
        tvStatVolunteers  = findViewById(R.id.tvStatVolunteers);
        tvStatApplications= findViewById(R.id.tvStatApplications);
        tvStatCritical    = findViewById(R.id.tvStatCritical);
        tvStatCompleted   = findViewById(R.id.tvStatCompleted);
        tvStatPending     = findViewById(R.id.tvStatPending);
        layoutChart       = findViewById(R.id.layoutChart);
        layoutFeed        = findViewById(R.id.layoutFeed);
        tvFeedEmpty       = findViewById(R.id.tvFeedEmpty);
    }

    void setListeners() {
        findViewById(R.id.btnBackOps).setOnClickListener(v -> finish());

        // Live toggle
        tvLiveIndicator.setOnClickListener(v -> {
            isLive = !isLive;
            if (isLive) {
                tvLiveIndicator.setText("🟢 LIVE");
                tvLiveIndicator.setTextColor(Color.parseColor("#00E676"));
                startLiveRefresh();
            } else {
                tvLiveIndicator.setText("⏸ PAUSED");
                tvLiveIndicator.setTextColor(Color.parseColor("#FFA726"));
                refreshHandler.removeCallbacks(refreshRunnable);
            }
        });

        // Manual refresh
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            loadAllData();
            Toast.makeText(this, "Refreshed! ✅", Toast.LENGTH_SHORT).show();
        });
    }

    // ── LIVE REFRESH ──────────────────────────────────────

    void startLiveRefresh() {
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                if (isLive) {
                    loadAllData();
                    refreshHandler.postDelayed(this, 30000); // 30 sec
                }
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 30000);
    }

    // ── LOAD ALL DATA ─────────────────────────────────────
    void loadAllData() {
        // Update time
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date());
        tvOpsSubtitle.setText("Last updated: " + time);

        // ── Tasks — NGO filter hatao, sab tasks lo ──
        db.collection("tasks")
                .get()
                .addOnSuccessListener(snap -> {
                    int total = snap.size();
                    int critical = 0, completed = 0, pending = 0;
                    Map<String, Integer> categoryCount = new LinkedHashMap<>();

                    for (DocumentSnapshot doc : snap) {
                        String urgency  = doc.getString("urgency");
                        String status   = doc.getString("status");
                        String category = doc.getString("category");

                        if (urgency != null && urgency.contains("Critical")) critical++;
                        if ("Completed".equals(status)) completed++;
                        else pending++;

                        if (category != null) {
                            categoryCount.put(category,
                                    categoryCount.getOrDefault(category, 0) + 1);
                        }
                    }

                    // LocalTaskStore se bhi add karo
                    List<LocalTaskStore.LocalTask> localTasks =
                            LocalTaskStore.getInstance().getTasks();

                    for (LocalTaskStore.LocalTask t : localTasks) {
                        if (t.category != null) {
                            categoryCount.put(t.category,
                                    categoryCount.getOrDefault(t.category, 0) + 1);
                        }
                        if ("Critical".equals(t.urgency)) critical++;
                        else pending++;
                    }

                    int grandTotal = total + localTasks.size();
                    tvStatTasks.setText(String.valueOf(grandTotal));
                    tvStatCritical.setText(String.valueOf(critical));
                    tvStatCompleted.setText(String.valueOf(completed));
                    tvStatPending.setText(String.valueOf(pending));

                    buildChart(categoryCount, grandTotal);
                })
                .addOnFailureListener(e -> {
                    // Firebase fail — sirf local show karo
                    List<LocalTaskStore.LocalTask> localTasks =
                            LocalTaskStore.getInstance().getTasks();
                    tvStatTasks.setText(String.valueOf(localTasks.size()));
                    tvStatPending.setText(String.valueOf(localTasks.size()));

                    Map<String, Integer> catMap = new LinkedHashMap<>();
                    for (LocalTaskStore.LocalTask t : localTasks) {
                        if (t.category != null)
                            catMap.put(t.category,
                                    catMap.getOrDefault(t.category, 0) + 1);
                    }
                    buildChart(catMap, localTasks.size());
                });

        // ── Volunteers ──
        db.collection("volunteer_users")
                .get()
                .addOnSuccessListener(snap ->
                        tvStatVolunteers.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e ->
                        tvStatVolunteers.setText("—"));

        // ── Applications — sab lo ──
        db.collection("applications")
                .get()
                .addOnSuccessListener(snap ->
                        tvStatApplications.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e ->
                        tvStatApplications.setText("—"));

        // ── Activity Feed ──
        loadActivityFeed();
    }

    // ── CHART ─────────────────────────────────────────────

    void buildChart(Map<String, Integer> data, int total) {
        layoutChart.removeAllViews();
        if (data.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No task data yet");
            empty.setTextColor(Color.parseColor("#9E9EB8"));
            empty.setTextSize(13f);
            layoutChart.addView(empty);
            return;
        }

        String[] colors = {"#1565C0","#C62828","#2E7D32",
                "#F57F17","#6A1B9A","#00838F","#EF6C00"};
        int colorIdx = 0;

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String cat   = entry.getKey();
            int count    = entry.getValue();
            float percent= total > 0 ? (count * 100f / total) : 0;

            // Row container
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams rp =
                    new LinearLayout.LayoutParams(-1, -2);
            rp.setMargins(0, 0, 0, 14);
            row.setLayoutParams(rp);

            // Label row
            LinearLayout labelRow = new LinearLayout(this);
            labelRow.setOrientation(LinearLayout.HORIZONTAL);
            labelRow.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams lrp =
                    new LinearLayout.LayoutParams(-1, -2);
            lrp.setMargins(0, 0, 0, 6);
            labelRow.setLayoutParams(lrp);

            TextView label = new TextView(this);
            label.setText(cat);
            label.setTextSize(13f);
            label.setTextColor(Color.parseColor("#E0E0E0"));
            LinearLayout.LayoutParams llp =
                    new LinearLayout.LayoutParams(0, -2, 1f);
            label.setLayoutParams(llp);
            labelRow.addView(label);

            TextView countTv = new TextView(this);
            countTv.setText(count + " (" + (int)percent + "%)");
            countTv.setTextSize(12f);
            countTv.setTextColor(Color.parseColor("#9E9EB8"));
            labelRow.addView(countTv);
            row.addView(labelRow);

            // Progress bar background
            FrameLayout barBg = new FrameLayout(this);
            barBg.setBackgroundColor(Color.parseColor("#2D2D44"));
            LinearLayout.LayoutParams bgp =
                    new LinearLayout.LayoutParams(-1, 20);
            barBg.setLayoutParams(bgp);

            // Progress bar fill
            View fill = new View(this);
            String barColor = colors[colorIdx % colors.length];
            fill.setBackgroundColor(Color.parseColor(barColor));

            // Animate width
            int maxWidth = getResources().getDisplayMetrics().widthPixels - 96;
            int fillWidth = (int)(maxWidth * percent / 100f);
            FrameLayout.LayoutParams fp =
                    new FrameLayout.LayoutParams(fillWidth, -1);
            fill.setLayoutParams(fp);
            barBg.addView(fill);
            row.addView(barBg);

            layoutChart.addView(row);
            colorIdx++;
        }
    }

    // ── ACTIVITY FEED ─────────────────────────────────────

    void loadActivityFeed() {
        feedItems.clear();
        layoutFeed.removeAllViews();

        // Applications — sab lo, filter mat lagao
        db.collection("applications")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap) {
                        String name      = doc.getString("name");
                        String taskTitle = doc.getString("taskTitle");
                        String status    = doc.getString("status");
                        feedItems.add(new ActivityItem(
                                "apply",
                                (name != null ? name : "A volunteer") +
                                        " applied for " +
                                        (taskTitle != null ? taskTitle : "a task"),
                                "Application • " + (status != null ? status : "Pending"),
                                System.currentTimeMillis()
                        ));
                    }

                    // Tasks feed
                    db.collection("tasks")
                            .get()
                            .addOnSuccessListener(taskSnap -> {
                                for (DocumentSnapshot doc : taskSnap) {
                                    String title  = doc.getString("title");
                                    String status = doc.getString("status");
                                    feedItems.add(new ActivityItem(
                                            "task",
                                            "Task created: " +
                                                    (title != null ? title : "New Task"),
                                            "Task • " + (status != null ? status : "Active"),
                                            doc.getLong("timestamp") != null
                                                    ? doc.getLong("timestamp")
                                                    : System.currentTimeMillis()
                                    ));
                                }

                                // LocalTaskStore
                                for (LocalTaskStore.LocalTask t :
                                        LocalTaskStore.getInstance().getTasks()) {
                                    feedItems.add(new ActivityItem(
                                            "task",
                                            "Task created: " + t.title,
                                            "Task • " + t.urgency,
                                            t.createdAt
                                    ));
                                }

                                // Sort newest first
                                feedItems.sort((a, b) ->
                                        Long.compare(b.timestamp, a.timestamp));

                                renderFeed();
                            });
                })
                .addOnFailureListener(e -> {
                    // Sirf local tasks dikhao
                    for (LocalTaskStore.LocalTask t :
                            LocalTaskStore.getInstance().getTasks()) {
                        feedItems.add(new ActivityItem(
                                "task",
                                "Task created: " + t.title,
                                "Task • " + t.urgency,
                                t.createdAt
                        ));
                    }
                    renderFeed();
                });
    }

    void loadTaskFeed(String uid) {
        db.collection("tasks")
                .whereEqualTo("ngoId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap) {
                        String title  = doc.getString("title");
                        String status = doc.getString("status");
                        feedItems.add(new ActivityItem(
                                "task",
                                "Task created: " +
                                        (title != null ? title : "New Task"),
                                "Task • " + (status != null ? status : "Active"),
                                doc.getLong("timestamp") != null
                                        ? doc.getLong("timestamp") : 0L
                        ));
                    }

                    // Add LocalTaskStore items
                    for (LocalTaskStore.LocalTask t :
                            LocalTaskStore.getInstance().getTasks()) {
                        feedItems.add(new ActivityItem(
                                "task",
                                "Task created: " + t.title,
                                "Task • " + t.urgency,
                                t.createdAt
                        ));
                    }

                    // Sort by time
                    feedItems.sort((a, b) ->
                            Long.compare(b.timestamp, a.timestamp));

                    renderFeed();
                })
                .addOnFailureListener(e -> {
                    // Just show local tasks
                    for (LocalTaskStore.LocalTask t :
                            LocalTaskStore.getInstance().getTasks()) {
                        feedItems.add(new ActivityItem(
                                "task",
                                "Task created: " + t.title,
                                "Task • " + t.urgency,
                                t.createdAt
                        ));
                    }
                    renderFeed();
                });
    }

    void renderFeed() {
        layoutFeed.removeAllViews();
        if (feedItems.isEmpty()) {
            tvFeedEmpty.setVisibility(View.VISIBLE);
            return;
        }
        tvFeedEmpty.setVisibility(View.GONE);
        for (ActivityItem item : feedItems) {
            addFeedCard(item);
        }
    }

    void addFeedCard(ActivityItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundColor(Color.parseColor("#16213E"));
        card.setPadding(32, 20, 32, 20);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 2);
        card.setLayoutParams(cp);

        // Icon circle
        TextView icon = new TextView(this);
        icon.setText(item.type.equals("apply") ? "🙋" : "📋");
        icon.setTextSize(20f);
        icon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip =
                new LinearLayout.LayoutParams(48, 48);
        ip.setMargins(0, 0, 16, 0);
        icon.setLayoutParams(ip);
        card.addView(icon);

        // Text column
        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        textCol.setLayoutParams(tp);

        TextView title = new TextView(this);
        title.setText(item.title);
        title.setTextSize(13f);
        title.setTextColor(Color.parseColor("#E0E0E0"));
        title.setTypeface(null, Typeface.NORMAL);
        textCol.addView(title);

        TextView sub = new TextView(this);
        sub.setText(item.subtitle);
        sub.setTextSize(11f);
        sub.setTextColor(Color.parseColor("#9E9EB8"));
        LinearLayout.LayoutParams sp =
                new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, 4, 0, 0);
        sub.setLayoutParams(sp);
        textCol.addView(sub);

        card.addView(textCol);

        // Time
        TextView time = new TextView(this);
        String timeStr = item.timestamp > 0
                ? new SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(new Date(item.timestamp))
                : "Now";
        time.setText(timeStr);
        time.setTextSize(10f);
        time.setTextColor(Color.parseColor("#6B6B8A"));
        card.addView(time);

        // Left color bar
        String barColor = item.type.equals("apply")
                ? "#00E676" : "#448AFF";
        card.setBackgroundColor(Color.parseColor("#16213E"));

        // Add left border effect
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams wp =
                new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(0, 0, 0, 2);
        wrapper.setLayoutParams(wp);

        View bar = new View(this);
        bar.setBackgroundColor(Color.parseColor(barColor));
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(4, -1);
        bar.setLayoutParams(bp);
        wrapper.addView(bar);

        card.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        wrapper.addView(card);

        layoutFeed.addView(wrapper);
    }

    // ── DATA MODEL ────────────────────────────────────────

    static class ActivityItem {
        String type, title, subtitle;
        long timestamp;
        ActivityItem(String t, String ti, String s, long ts) {
            type = t; title = ti; subtitle = s; timestamp = ts;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        refreshHandler.removeCallbacks(refreshRunnable);
    }
}