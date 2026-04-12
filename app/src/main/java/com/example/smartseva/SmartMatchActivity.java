package com.example.smartseva;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class SmartMatchActivity extends AppCompatActivity {

    TextView tvMatchTitle, tvMatchSubtitle;
    TextView tvTotalMatches, tvExcellentMatches, tvGoodMatches;
    Button btnBackMatch, btnMatchAll, btnMatchExcellent, btnMatchGood;
    ListView listMatchResults;

    String mode; // "NGO" or "Volunteer"
    String taskTitle, taskSkill, taskLocation, taskUrgency;
    String volSkills, volCity, volAvailability;

    // NGO mode — volunteer results
    List<SmartMatcher.MatchResult> volResults = new ArrayList<>();
    List<SmartMatcher.MatchResult> filteredVolResults = new ArrayList<>();

    // Volunteer mode — task results
    List<SmartMatcher.TaskMatchResult> taskResults = new ArrayList<>();
    List<SmartMatcher.TaskMatchResult> filteredTaskResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_match);

        // ── Views ──
        tvMatchTitle       = findViewById(R.id.tvMatchTitle);
        tvMatchSubtitle    = findViewById(R.id.tvMatchSubtitle);
        tvTotalMatches     = findViewById(R.id.tvTotalMatches);
        tvExcellentMatches = findViewById(R.id.tvExcellentMatches);
        tvGoodMatches      = findViewById(R.id.tvGoodMatches);
        btnBackMatch       = findViewById(R.id.btnBackMatch);
        btnMatchAll        = findViewById(R.id.btnMatchAll);
        btnMatchExcellent  = findViewById(R.id.btnMatchExcellent);
        btnMatchGood       = findViewById(R.id.btnMatchGood);
        listMatchResults   = findViewById(R.id.listMatchResults);

        // ── Intent Data ──
        mode           = getIntent().getStringExtra("mode");
        taskTitle      = getIntent().getStringExtra("taskTitle");
        taskSkill      = getIntent().getStringExtra("taskSkill");
        taskLocation   = getIntent().getStringExtra("taskLocation");
        taskUrgency    = getIntent().getStringExtra("taskUrgency");
        volSkills      = getIntent().getStringExtra("volSkills");
        volCity        = getIntent().getStringExtra("volCity");
        volAvailability= getIntent().getStringExtra("volAvailability");

        if (mode == null) mode = "Volunteer";

        // ── Setup ──
        if (mode.equals("NGO")) {
            setupNGOMode();
        } else {
            setupVolunteerMode();
        }

        // ── Listeners ──
        btnBackMatch.setOnClickListener(v -> finish());
        btnMatchAll.setOnClickListener(v -> {
            filterResults("all"); setTabActive(btnMatchAll); });
        btnMatchExcellent.setOnClickListener(v -> {
            filterResults("Excellent Match"); setTabActive(btnMatchExcellent); });
        btnMatchGood.setOnClickListener(v -> {
            filterResults("Good Match"); setTabActive(btnMatchGood); });
    }

    // ═══════════════════════════════════════
    // NGO MODE — Find volunteers for task
    // ═══════════════════════════════════════

    void setupNGOMode() {
        tvMatchTitle.setText("🧠 Smart Matching");
        tvMatchSubtitle.setText("Best volunteers for: " + taskTitle);

        List<SmartMatcher.MatchResult> volunteers = new ArrayList<>();
        volunteers.add(new SmartMatcher.MatchResult(
                "Amit Sahu", "Raipur, CG",
                "Medical Help, Food Distribution", "Weekdays", 2));
        volunteers.add(new SmartMatcher.MatchResult(
                "Priya Sharma", "Raipur, CG",
                "Teaching, Medical Help", "Weekends", 2));
        volunteers.add(new SmartMatcher.MatchResult(
                "Rahul Verma", "Bilaspur, CG",
                "Food Distribution, Event Management", "Both", 1));
        volunteers.add(new SmartMatcher.MatchResult(
                "Anjali Patel", "Durg, CG",
                "Social Media, Fundraising", "Weekdays", 3));
        volunteers.add(new SmartMatcher.MatchResult(
                "Sonu Kumar", "Korba, CG",
                "Technical, Event Management", "Weekends", 0));
        volunteers.add(new SmartMatcher.MatchResult(
                "Deepika Singh", "Jagdalpur, CG",
                "Teaching, Social Media", "Both", 1));

        boolean isUrgent = taskUrgency != null && taskUrgency.contains("Critical");
        String taskLoc = taskLocation != null ? taskLocation : "Raipur";

        // GPS se distance calculate karo
        geocodeAndMatch(taskLoc, volunteers, isUrgent);
    }

    void geocodeAndMatch(String taskLocation,
                         List<SmartMatcher.MatchResult> volunteers,
                         boolean isUrgent) {

        tvMatchSubtitle.setText("📡 Fetching locations...");

        // Task location geocode karo
        new Thread(() -> {
            double[] taskCoords = geocodeLocation(taskLocation);

            if (taskCoords == null) {
                runOnUiThread(() -> {
                    tvMatchSubtitle.setText("Location fetch failed — using name match");
                    volResults = SmartMatcher.matchVolunteersForTask(
                            taskSkill, taskLocation, "Weekends",
                            taskUrgency, volunteers);
                    filteredVolResults = new ArrayList<>(volResults);
                    updateSummary();
                    listMatchResults.setAdapter(new VolunteerMatchAdapter());
                });
                return;
            }

            double taskLat = taskCoords[0];
            double taskLon = taskCoords[1];

            // Har volunteer ki location geocode karo
            for (SmartMatcher.MatchResult vol : volunteers) {
                double[] volCoords = geocodeLocation(vol.city);

                if (volCoords != null) {
                    double distKm = SmartMatcher.calculateGPSDistance(
                            taskLat, taskLon, volCoords[0], volCoords[1]);

                    vol.distanceKm = distKm;

                    // 36hrs urgent rule — 100km se door exclude karo
                    if (isUrgent && distKm > 100) {
                        vol.matchScore  = 0;
                        vol.matchLabel  = "Too Far";
                        vol.matchReason = "⚠️ Urgent task — " +
                                String.format("%.0f", distKm) + "km away (max 100km)";
                        vol.locationScore = 0;
                    } else {
                        vol.locationScore = SmartMatcher
                                .getLocationScoreFromDistance(distKm);
                        vol.distanceText  = String.format("%.0f", distKm) + "km away";
                    }
                } else {
                    vol.locationScore = 10;
                    vol.distanceText  = "Distance unknown";
                }
            }

            // Ab final matching karo with GPS scores
            List<SmartMatcher.MatchResult> matched =
                    SmartMatcher.matchVolunteersForTaskWithGPS(
                            taskSkill, taskUrgency, volunteers);

            runOnUiThread(() -> {
                volResults         = matched;
                filteredVolResults = new ArrayList<>(volResults);
                tvMatchSubtitle.setText("Best volunteers for: " + taskTitle);
                updateSummary();
                listMatchResults.setAdapter(new VolunteerMatchAdapter());
            });

        }).start();
    }

    // ── Geocoding using OpenStreetMap Nominatim (free, no API key) ──
    double[] geocodeLocation(String locationName) {
        try {
            String encoded = android.net.Uri.encode(locationName);
            String url = "https://nominatim.openstreetmap.org/search?q=" +
                    encoded + "&format=json&limit=1";

            java.net.URL obj = new java.net.URL(url);
            java.net.HttpURLConnection con =
                    (java.net.HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "SmartSeva-App");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();

            String json = response.toString();
            if (json.equals("[]")) return null;

            // Parse lat lon manually (no library needed)
            int latIdx = json.indexOf("\"lat\":\"") + 7;
            int latEnd = json.indexOf("\"", latIdx);
            int lonIdx = json.indexOf("\"lon\":\"") + 7;
            int lonEnd = json.indexOf("\"", lonIdx);

            if (latIdx < 7 || lonIdx < 7) return null;

            double lat = Double.parseDouble(json.substring(latIdx, latEnd));
            double lon = Double.parseDouble(json.substring(lonIdx, lonEnd));

            return new double[]{lat, lon};

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════
    // VOLUNTEER MODE — Find tasks for volunteer
    // ═══════════════════════════════════════

    void setupVolunteerMode() {
        tvMatchTitle.setText("🧠 Smart Matching");
        tvMatchSubtitle.setText("Best tasks for your skills");

        // Sample tasks — Firebase teammate real data load karega
        List<SmartMatcher.TaskMatchResult> tasks = new ArrayList<>();
        tasks.add(new SmartMatcher.TaskMatchResult(
                "Food Distribution Drive", "Raipur, CG",
                "Food Distribution", "🔴 Critical (24 hrs)",
                "Food Distribution", "20/04/2026"));
        tasks.add(new SmartMatcher.TaskMatchResult(
                "Free Medical Camp", "Raipur, CG",
                "Medical Help", "🟡 Moderate (1 week)",
                "Medical Help", "25/04/2026"));
        tasks.add(new SmartMatcher.TaskMatchResult(
                "Tree Plantation Drive", "Bilaspur, CG",
                "Environment", "🟢 Normal",
                "Any Skill", "30/04/2026"));
        tasks.add(new SmartMatcher.TaskMatchResult(
                "Teaching Underprivileged Kids", "Raipur, CG",
                "Education", "🟡 Moderate (1 week)",
                "Teaching", "22/04/2026"));
        tasks.add(new SmartMatcher.TaskMatchResult(
                "Social Media Campaign", "Durg, CG",
                "Awareness", "🟢 Normal",
                "Social Media", "28/04/2026"));

        taskResults = SmartMatcher.matchTasksForVolunteer(
                volSkills != null ? volSkills : "Teaching",
                volCity != null ? volCity : "Raipur",
                volAvailability != null ? volAvailability : "Weekends",
                tasks);

        filteredTaskResults = new ArrayList<>(taskResults);
        updateSummary();
        listMatchResults.setAdapter(new TaskMatchAdapter());

        listMatchResults.setOnItemClickListener((parent, view, position, id) -> {
            SmartMatcher.TaskMatchResult task = filteredTaskResults.get(position);
            Intent intent = new Intent(this, TaskDetailActivity.class);
            intent.putExtra("taskTitle",    task.taskTitle);
            intent.putExtra("taskLocation", task.taskLocation);
            intent.putExtra("taskCategory", task.taskCategory);
            intent.putExtra("taskUrgency",  task.taskUrgency);
            intent.putExtra("taskSkill",    task.taskSkill);
            intent.putExtra("taskDate",     task.taskDate);
            intent.putExtra("taskDesc",     "This task matches your skills and location!");
            intent.putExtra("taskNGO",      "Smart Seva NGO");
            intent.putExtra("taskVolunteers", 5);
            startActivity(intent);
        });
    }

    // ═══════════════════════════════════════
    // FILTER + SUMMARY
    // ═══════════════════════════════════════

    void filterResults(String label) {
        if (mode.equals("NGO")) {
            filteredVolResults.clear();
            for (SmartMatcher.MatchResult v : volResults) {
                if (label.equals("all") || v.matchLabel.equals(label))
                    filteredVolResults.add(v);
            }
            listMatchResults.setAdapter(new VolunteerMatchAdapter());
        } else {
            filteredTaskResults.clear();
            for (SmartMatcher.TaskMatchResult t : taskResults) {
                if (label.equals("all") || t.matchLabel.equals(label))
                    filteredTaskResults.add(t);
            }
            listMatchResults.setAdapter(new TaskMatchAdapter());
        }
    }

    void updateSummary() {
        List<?> results = mode.equals("NGO") ? volResults : taskResults;
        int total = results.size(), excellent = 0, good = 0;

        for (Object r : results) {
            String label = mode.equals("NGO") ?
                    ((SmartMatcher.MatchResult) r).matchLabel :
                    ((SmartMatcher.TaskMatchResult) r).matchLabel;
            if (label.equals("Excellent Match")) excellent++;
            else if (label.equals("Good Match")) good++;
        }

        tvTotalMatches.setText(String.valueOf(total));
        tvExcellentMatches.setText(String.valueOf(excellent));
        tvGoodMatches.setText(String.valueOf(good));
    }

    void setTabActive(Button active) {
        Button[] tabs = {btnMatchAll, btnMatchExcellent, btnMatchGood};
        for (Button b : tabs) {
            b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            b.setTextColor(Color.parseColor("#888888"));
        }
        active.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        active.setTextColor(Color.WHITE);
    }

    int getLabelColor(String label) {
        switch (label) {
            case "Excellent Match": return Color.parseColor("#2E7D32");
            case "Good Match":      return Color.parseColor("#1565C0");
            case "Fair Match":      return Color.parseColor("#F57F17");
            default:                return Color.parseColor("#888888");
        }
    }

    // ═══════════════════════════════════════
    // VOLUNTEER MATCH ADAPTER (NGO mode)
    // ═══════════════════════════════════════

    class VolunteerMatchAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredVolResults.size(); }
        @Override public Object getItem(int pos) { return filteredVolResults.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(
                        R.layout.item_match_result, parent, false);

            SmartMatcher.MatchResult vol = filteredVolResults.get(position);
            bindMatchView(convertView, vol.matchScore, vol.name,
                    "📍 " + vol.city, "🛠️ " + vol.skills,
                    vol.matchLabel, vol.matchReason, "View Profile");
            return convertView;
        }
    }

    // ═══════════════════════════════════════
    // TASK MATCH ADAPTER (Volunteer mode)
    // ═══════════════════════════════════════

    class TaskMatchAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredTaskResults.size(); }
        @Override public Object getItem(int pos) { return filteredTaskResults.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(
                        R.layout.item_match_result, parent, false);

            SmartMatcher.TaskMatchResult task = filteredTaskResults.get(position);
            bindMatchView(convertView, task.matchScore, task.taskTitle,
                    "📍 " + task.taskLocation, task.taskUrgency + " • " + task.taskSkill,
                    task.matchLabel, task.matchReason, "View Task");
            return convertView;
        }
    }

    // ── Shared bind helper ──
    void bindMatchView(View v, int score, String name,
                       String city, String skills,
                       String label, String reason, String btnText) {

        LinearLayout scoreCircle = v.findViewById(R.id.layoutScoreCircle);
        TextView tvScore    = v.findViewById(R.id.tvMatchScore);
        TextView tvName     = v.findViewById(R.id.tvMatchName);
        TextView tvCity     = v.findViewById(R.id.tvMatchCity);
        TextView tvSkills   = v.findViewById(R.id.tvMatchSkills);
        TextView tvLabel    = v.findViewById(R.id.tvMatchLabel);
        TextView tvReason   = v.findViewById(R.id.tvMatchReason);
        ProgressBar progress= v.findViewById(R.id.progressMatch);
        TextView tvPercent  = v.findViewById(R.id.tvMatchPercent);
        Button btnAction    = v.findViewById(R.id.btnMatchAction);

        tvScore.setText(String.valueOf(score));
        tvName.setText(name);
        tvCity.setText(city);
        tvSkills.setText(skills);
        tvLabel.setText(label);
        tvReason.setText(reason);
        progress.setProgress(score);
        tvPercent.setText(score + "%");
        btnAction.setText(btnText);

        int color = getLabelColor(label);
        scoreCircle.setBackgroundColor(color);
        tvLabel.setBackgroundColor(color);
        progress.setProgressTintList(ColorStateList.valueOf(color));
    }
}