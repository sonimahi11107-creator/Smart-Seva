package com.example.smartseva;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;

public class DynamicAllocationActivity extends AppCompatActivity {

    FirebaseFirestore db;

    // Task info
    TextView tvTaskTitle, tvTaskSkill,
            tvTaskLocation, tvTaskUrgency;

    // Results
    LinearLayout layoutResults;
    TextView tvResultCount, tvLoadingMsg;
    ProgressBar progressBar;

    // Task data from intent
    String taskTitle, taskSkill, taskLocation,
            taskUrgency, taskDate, taskId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dynamic_allocation);

        db = FirebaseFirestore.getInstance();

        // Get task data
        taskTitle    = getIntent().getStringExtra("taskTitle");
        taskSkill    = getIntent().getStringExtra("taskSkill");
        taskLocation = getIntent().getStringExtra("taskLocation");
        taskUrgency  = getIntent().getStringExtra("taskUrgency");
        taskDate     = getIntent().getStringExtra("taskDate");
        taskId       = getIntent().getStringExtra("taskId");

        bindViews();
        populateTaskInfo();
        findBestVolunteers();
    }

    void bindViews() {
        tvTaskTitle    = findViewById(R.id.tvTaskTitle);
        tvTaskSkill    = findViewById(R.id.tvTaskSkill);
        tvTaskLocation = findViewById(R.id.tvTaskLocation);
        tvTaskUrgency  = findViewById(R.id.tvTaskUrgency);
        layoutResults  = findViewById(R.id.layoutResults);
        tvResultCount  = findViewById(R.id.tvResultCount);
        tvLoadingMsg   = findViewById(R.id.tvLoadingMsg);
        progressBar    = findViewById(R.id.progressBar);

        findViewById(R.id.btnBackAlloc)
                .setOnClickListener(v -> finish());
    }

    void populateTaskInfo() {
        tvTaskTitle.setText(
                taskTitle != null ? taskTitle : "Task");
        tvTaskSkill.setText(
                "🛠️ " + (taskSkill != null
                        ? taskSkill : "Any Skill"));
        tvTaskLocation.setText(
                "📍 " + (taskLocation != null
                        ? taskLocation : "Location"));

        int urgColor = "Critical".contains(
                taskUrgency != null ? taskUrgency : "")
                ? Color.parseColor("#C62828")
                : Color.parseColor("#F57F17");
        tvTaskUrgency.setText(
                taskUrgency != null ? taskUrgency : "Normal");
        tvTaskUrgency.setBackgroundColor(urgColor);
    }

    // ── FIND BEST VOLUNTEERS ──────────────────────────────

    void findBestVolunteers() {
        progressBar.setVisibility(View.VISIBLE);
        tvLoadingMsg.setVisibility(View.VISIBLE);
        tvLoadingMsg.setText(
                "🔍 Analyzing volunteers...");

        db.collection("volunteer_users")
                .get()
                .addOnSuccessListener(snap -> {
                    List<TaskAllocationEngine.VolunteerScore>
                            volunteers = new ArrayList<>();

                    for (DocumentSnapshot doc : snap) {
                        String uid   = doc.getId();
                        String name  = doc.getString("name");
                        String city  = doc.getString("city");
                        String avail =
                                doc.getString("availableDays");

                        // Build skills string
                        StringBuilder skills =
                                new StringBuilder();
                        if (Boolean.TRUE.equals(
                                doc.getBoolean("teaching")))
                            skills.append("Teaching, ");
                        if (Boolean.TRUE.equals(
                                doc.getBoolean("medical")))
                            skills.append("Medical, ");
                        if (Boolean.TRUE.equals(
                                doc.getBoolean("food")))
                            skills.append("Food, ");
                        if (Boolean.TRUE.equals(
                                doc.getBoolean("event")))
                            skills.append("Event, ");
                        if (Boolean.TRUE.equals(
                                doc.getBoolean("technical")))
                            skills.append("Technical, ");

                        String skillStr = skills.length() > 2
                                ? skills.substring(0,
                                skills.length() - 2)
                                : "General";

                        TaskAllocationEngine.VolunteerScore vs =
                                new TaskAllocationEngine
                                        .VolunteerScore(
                                        uid, name != null
                                        ? name : "Volunteer",
                                        city != null ? city : "",
                                        skillStr,
                                        avail != null ? avail : "");
                        volunteers.add(vs);
                    }

                    // Rank volunteers
                    List<TaskAllocationEngine.VolunteerScore>
                            ranked = TaskAllocationEngine
                            .rankVolunteers(
                                    volunteers,
                                    taskSkill,
                                    taskLocation,
                                    taskUrgency,
                                    taskDate);

                    progressBar.setVisibility(View.GONE);
                    tvLoadingMsg.setVisibility(View.GONE);
                    showResults(ranked);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    tvLoadingMsg.setText(
                            "Error loading volunteers: "
                                    + e.getMessage());
                });
    }

    // ── SHOW RESULTS ──────────────────────────────────────

    void showResults(
            List<TaskAllocationEngine.VolunteerScore>
                    ranked) {

        layoutResults.removeAllViews();

        if (ranked.isEmpty()) {
            showEmpty();
            return;
        }

        // Filter score > 30
        List<TaskAllocationEngine.VolunteerScore> suitable
                = new ArrayList<>();
        for (TaskAllocationEngine.VolunteerScore v : ranked)
            if (v.totalScore > 30) suitable.add(v);

        tvResultCount.setText(
                suitable.size() + " suitable volunteers found");

        if (suitable.isEmpty()) {
            showEmpty();
            return;
        }

        // Show top 10
        int limit = Math.min(suitable.size(), 10);
        for (int i = 0; i < limit; i++) {
            addVolunteerCard(suitable.get(i), i + 1);
        }
    }

    void addVolunteerCard(
            TaskAllocationEngine.VolunteerScore v,
            int rank) {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(32, 24, 32, 24);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(-1, -2);
        cp.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cp);

        // Top row — rank + name + score
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams trp =
                new LinearLayout.LayoutParams(-1, -2);
        trp.setMargins(0, 0, 0, 12);
        topRow.setLayoutParams(trp);

        // Rank badge
        TextView rankBadge = new TextView(this);
        rankBadge.setText("#" + rank);
        rankBadge.setTextSize(12f);
        rankBadge.setTextColor(Color.WHITE);
        rankBadge.setBackgroundColor(rank == 1
                ? Color.parseColor("#F59E0B")  // Gold
                : rank == 2
                ? Color.parseColor("#9CA3AF")  // Silver
                : rank == 3
                ? Color.parseColor("#92400E")  // Bronze
                : Color.parseColor("#1A1A2E")); // Dark
        rankBadge.setPadding(16, 8, 16, 8);
        rankBadge.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams rbp =
                new LinearLayout.LayoutParams(-2, -2);
        rbp.setMargins(0, 0, 12, 0);
        rankBadge.setLayoutParams(rbp);
        topRow.addView(rankBadge);

        // Name + city
        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ncp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        nameCol.setLayoutParams(ncp);

        TextView name = new TextView(this);
        name.setText(v.name);
        name.setTextSize(15f);
        name.setTextColor(Color.parseColor("#111827"));
        name.setTypeface(null, Typeface.BOLD);
        nameCol.addView(name);

        TextView city = new TextView(this);
        city.setText("📍 " + (v.city.isEmpty()
                ? "Location unknown" : v.city));
        city.setTextSize(12f);
        city.setTextColor(Color.parseColor("#6B7280"));
        nameCol.addView(city);
        topRow.addView(nameCol);

        // Total score
        int gradeColor =
                TaskAllocationEngine.getGradeColor(v.totalScore);
        TextView score = new TextView(this);
        score.setText(v.totalScore + "%");
        score.setTextSize(18f);
        score.setTextColor(gradeColor);
        score.setTypeface(null, Typeface.BOLD);
        topRow.addView(score);
        card.addView(topRow);

        // Grade badge
        TextView grade = new TextView(this);
        grade.setText(
                TaskAllocationEngine.getGrade(v.totalScore)
                        + " Match");
        grade.setTextSize(11f);
        grade.setTextColor(Color.WHITE);
        grade.setBackgroundColor(gradeColor);
        grade.setPadding(16, 4, 16, 4);
        LinearLayout.LayoutParams gp =
                new LinearLayout.LayoutParams(-2, -2);
        gp.setMargins(0, 0, 0, 12);
        grade.setLayoutParams(gp);
        card.addView(grade);

        // Score breakdown
        LinearLayout breakdown = new LinearLayout(this);
        breakdown.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bp =
                new LinearLayout.LayoutParams(-1, -2);
        bp.setMargins(0, 0, 0, 12);
        breakdown.setLayoutParams(bp);

        addScoreChip(breakdown,
                "Skill", v.skillScore, "#1565C0");
        addScoreChip(breakdown,
                "Location", v.locationScore, "#2E7D32");
        addScoreChip(breakdown,
                "Avail.", v.availabilityScore, "#F57F17");
        addScoreChip(breakdown,
                "Exp.", v.experienceScore, "#6A1B9A");
        card.addView(breakdown);

        // Skills
        if (v.skills != null && !v.skills.isEmpty()) {
            TextView skills = new TextView(this);
            skills.setText("🛠️ " + v.skills);
            skills.setTextSize(12f);
            skills.setTextColor(
                    Color.parseColor("#374151"));
            LinearLayout.LayoutParams sp =
                    new LinearLayout.LayoutParams(-1, -2);
            sp.setMargins(0, 0, 0, 6);
            skills.setLayoutParams(sp);
            card.addView(skills);
        }

        // Match reason
        TextView reason = new TextView(this);
        reason.setText(v.matchReason);
        reason.setTextSize(11f);
        reason.setTextColor(Color.parseColor("#6B7280"));
        LinearLayout.LayoutParams rp =
                new LinearLayout.LayoutParams(-1, -2);
        rp.setMargins(0, 0, 0, 16);
        reason.setLayoutParams(rp);
        card.addView(reason);

        // Divider
        View div = new View(this);
        div.setBackgroundColor(
                Color.parseColor("#F3F4F6"));
        LinearLayout.LayoutParams dp =
                new LinearLayout.LayoutParams(-1, 1);
        dp.setMargins(0, 0, 0, 16);
        div.setLayoutParams(dp);
        card.addView(div);

        // Assign button
        Button btnAssign = new Button(this);
        btnAssign.setText(rank == 1
                ? "⭐ Assign Best Match" : "✅ Assign Volunteer");
        btnAssign.setTextColor(Color.WHITE);
        btnAssign.setBackgroundTintList(
                ColorStateList.valueOf(gradeColor));
        btnAssign.setTextSize(13f);
        card.addView(btnAssign);

        btnAssign.setOnClickListener(view ->
                confirmAssign(v, btnAssign));

        layoutResults.addView(card);
    }

    void addScoreChip(LinearLayout parent,
                      String label, int score, String color) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundColor(
                Color.parseColor("#F9FAFB"));
        chip.setPadding(12, 8, 12, 8);
        LinearLayout.LayoutParams cp =
                new LinearLayout.LayoutParams(0, -2, 1f);
        cp.setMargins(0, 0, 6, 0);
        chip.setLayoutParams(cp);

        TextView val = new TextView(this);
        val.setText(score + "%");
        val.setTextSize(13f);
        val.setTextColor(Color.parseColor(color));
        val.setTypeface(null, Typeface.BOLD);
        val.setGravity(Gravity.CENTER);
        chip.addView(val);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextSize(10f);
        lbl.setTextColor(Color.parseColor("#9CA3AF"));
        lbl.setGravity(Gravity.CENTER);
        chip.addView(lbl);

        parent.addView(chip);
    }

    // ── CONFIRM ASSIGN ────────────────────────────────────

    void confirmAssign(
            TaskAllocationEngine.VolunteerScore v,
            Button btn) {

        new android.app.AlertDialog.Builder(this)
                .setTitle("Assign Volunteer?")
                .setMessage(
                        "Assign " + v.name + " to:\n\n\""
                                + taskTitle + "\"?\n\n"
                                + "Match Score: " + v.totalScore + "%\n"
                                + v.matchReason)
                .setPositiveButton("✅ Yes, Assign!",
                        (d, w) -> assignVolunteer(v, btn))
                .setNegativeButton("Cancel", null)
                .show();
    }

    void assignVolunteer(
            TaskAllocationEngine.VolunteerScore v,
            Button btn) {

        // Firebase mein update karo
        if (taskId != null && !taskId.isEmpty()) {
            Map<String, Object> update = new HashMap<>();
            update.put("assignedVolunteerId", v.uid);
            update.put("assignedVolunteerName", v.name);
            update.put("status", "Assigned");
            update.put("matchScore", v.totalScore);

            db.collection("tasks").document(taskId)
                    .update(update)
                    .addOnSuccessListener(unused -> {
                        // Notification bhejo
                        NotificationHelper.notifyNewApplication(
                                this, v.name, taskTitle);
                    });
        }

        // UI update
        btn.setText("✅ Assigned to " + v.name + "!");
        btn.setBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#2E7D32")));
        btn.setEnabled(false);

        Toast.makeText(this,
                "✅ " + v.name + " assigned! "
                        + "Match score: " + v.totalScore + "%",
                Toast.LENGTH_LONG).show();
    }

    void showEmpty() {
        tvResultCount.setText("No suitable volunteers found");

        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundColor(Color.WHITE);
        empty.setPadding(40, 60, 40, 60);

        TextView emoji = new TextView(this);
        emoji.setText("🔍");
        emoji.setTextSize(40f);
        emoji.setGravity(Gravity.CENTER);
        empty.addView(emoji);

        TextView msg = new TextView(this);
        msg.setText("No volunteers match this task yet.\n"
                + "More volunteers will appear as they register.");
        msg.setTextSize(14f);
        msg.setTextColor(Color.parseColor("#6B7280"));
        msg.setGravity(Gravity.CENTER);
        empty.addView(msg);

        layoutResults.addView(empty);
    }
}