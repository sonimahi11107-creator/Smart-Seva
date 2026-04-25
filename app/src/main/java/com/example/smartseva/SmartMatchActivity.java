package com.example.smartseva;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;

public class SmartMatchActivity extends AppCompatActivity {

    TextView tvMatchTitle, tvMatchSubtitle;
    TextView tvTotalMatches, tvExcellentMatches, tvGoodMatches;
    Button btnBackMatch, btnMatchAll, btnMatchExcellent, btnMatchGood;
    ListView listMatchResults;
    ProgressBar progressLoading;

    String mode, taskTitle, taskSkill, taskLocation, taskUrgency;

    List<SmartMatcher.MatchResult>     volResults          = new ArrayList<>();
    List<SmartMatcher.MatchResult>     filteredVolResults  = new ArrayList<>();
    List<SmartMatcher.TaskMatchResult> taskResults         = new ArrayList<>();
    List<SmartMatcher.TaskMatchResult> filteredTaskResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_match);

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
        progressLoading    = findViewById(R.id.progressLoading); // add in XML

        mode         = getIntent().getStringExtra("mode");
        taskTitle    = getIntent().getStringExtra("taskTitle");
        taskSkill    = getIntent().getStringExtra("taskSkill");
        taskLocation = getIntent().getStringExtra("taskLocation");
        taskUrgency  = getIntent().getStringExtra("taskUrgency");

        if (mode == null) mode = "Volunteer";

        btnBackMatch.setOnClickListener(v -> finish());
        btnMatchAll.setOnClickListener(v -> {
            filterResults("all");
            setTabActive(btnMatchAll);
        });
        btnMatchExcellent.setOnClickListener(v -> {
            filterResults("Excellent Match");
            setTabActive(btnMatchExcellent);
        });
        btnMatchGood.setOnClickListener(v -> {
            filterResults("Good Match");
            setTabActive(btnMatchGood);
        });

        setTabActive(btnMatchAll);

        // ✅ Load real data from Firebase
        if (mode.equals("NGO")) {
            setupNGOMode();
        } else {
            setupVolunteerMode();
        }
    }

    // ═══════════════════════════════════════
    // NGO MODE — fetch real volunteers
    // ═══════════════════════════════════════

    void setupNGOMode() {
        tvMatchTitle.setText("🧠 Smart Matching");
        tvMatchSubtitle.setText("📡 Finding best volunteers...");
        setLoading(true);

        String skill    = taskSkill    != null ? taskSkill    : "Any Skill";
        String location = taskLocation != null ? taskLocation : "";
        String urgency  = taskUrgency  != null ? taskUrgency  : "Normal";

        SmartMatcher.fetchAndMatchVolunteers(skill, location, urgency,
                new SmartMatcher.VolunteerMatchCallback() {
                    @Override
                    public void onResult(List<SmartMatcher.MatchResult> results) {
                        setLoading(false);

                        if (results.isEmpty()) {
                            tvMatchSubtitle.setText("No volunteers found yet.");
                            return;
                        }

                        // Run GPS geocoding in background
                        geocodeAndMatch(location, results,
                                urgency.contains("Critical"));
                    }

                    @Override
                    public void onError(String error) {
                        setLoading(false);
                        Toast.makeText(SmartMatchActivity.this,
                                "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    void geocodeAndMatch(String taskLoc,
                         List<SmartMatcher.MatchResult> volunteers,
                         boolean isUrgent) {
        tvMatchSubtitle.setText("📡 Calculating distances...");

        new Thread(() -> {
            double[] taskCoords = geocodeLocation(taskLoc);

            for (SmartMatcher.MatchResult vol : volunteers) {
                double[] volCoords = geocodeLocation(vol.city);
                if (taskCoords != null && volCoords != null) {
                    double distKm = SmartMatcher.calculateGPSDistance(
                            taskCoords[0], taskCoords[1],
                            volCoords[0],  volCoords[1]);
                    vol.distanceKm   = distKm;
                    vol.distanceText = String.format("%.0f km away", distKm);

                    if (isUrgent && distKm > 100) {
                        vol.matchScore  = 0;
                        vol.matchLabel  = "Too Far";
                        vol.matchReason = "⚠️ Urgent task — "
                                + String.format("%.0f", distKm)
                                + "km away (max 100km)";
                        vol.locationScore = 0;
                    } else {
                        vol.locationScore =
                                SmartMatcher.getLocationScoreFromDistance(distKm);
                    }
                } else {
                    vol.locationScore = 10;
                    vol.distanceText  = "Distance unknown";
                }
            }

            List<SmartMatcher.MatchResult> matched =
                    SmartMatcher.matchVolunteersWithGPS(
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

    // ═══════════════════════════════════════
    // VOLUNTEER MODE — fetch real tasks
    // ═══════════════════════════════════════

    void setupVolunteerMode() {
        tvMatchTitle.setText("🧠 Smart Matching");
        tvMatchSubtitle.setText("📡 Finding best tasks for you...");
        setLoading(true);

        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (currentUid.isEmpty()) {
            setLoading(false);
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        SmartMatcher.fetchAndMatchTasks(currentUid,
                new SmartMatcher.TaskMatchCallback() {
                    @Override
                    public void onResult(List<SmartMatcher.TaskMatchResult> results) {
                        setLoading(false);
                        taskResults         = results;
                        filteredTaskResults = new ArrayList<>(taskResults);
                        tvMatchSubtitle.setText("Best tasks matching your profile");
                        updateSummary();
                        listMatchResults.setAdapter(new TaskMatchAdapter());

                        listMatchResults.setOnItemClickListener((parent, view, pos, id) -> {
                            if (pos >= filteredTaskResults.size()) return;
                            SmartMatcher.TaskMatchResult task = filteredTaskResults.get(pos);
                            Intent intent = new Intent(SmartMatchActivity.this,
                                    TaskDetailActivity.class);
                            intent.putExtra("taskId",       task.taskId);
                            intent.putExtra("taskTitle",    task.taskTitle);
                            intent.putExtra("taskLocation", task.taskLocation);
                            intent.putExtra("taskCategory", task.taskCategory);
                            intent.putExtra("taskUrgency",  task.taskUrgency);
                            intent.putExtra("taskSkill",    task.taskSkill);
                            intent.putExtra("taskDate",     task.taskDate);
                            intent.putExtra("taskNGO",      task.ngoId);
                            intent.putExtra("taskDesc",     "This task matches your skills!");
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onError(String error) {
                        setLoading(false);
                        Toast.makeText(SmartMatchActivity.this,
                                "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ═══════════════════════════════════════
    // GEOCODING (unchanged)
    // ═══════════════════════════════════════

    double[] geocodeLocation(String locationName) {
        try {
            String encoded = android.net.Uri.encode(locationName);
            String url = "https://nominatim.openstreetmap.org/search?q="
                    + encoded + "&format=json&limit=1";
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
            int latIdx = json.indexOf("\"lat\":\"") + 7;
            int latEnd = json.indexOf("\"", latIdx);
            int lonIdx = json.indexOf("\"lon\":\"") + 7;
            int lonEnd = json.indexOf("\"", lonIdx);
            if (latIdx < 7 || lonIdx < 7) return null;
            double lat = Double.parseDouble(json.substring(latIdx, latEnd));
            double lon = Double.parseDouble(json.substring(lonIdx, lonEnd));
            return new double[]{lat, lon};
        } catch (Exception e) {
            return null;
        }
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
            String label = mode.equals("NGO")
                    ? ((SmartMatcher.MatchResult) r).matchLabel
                    : ((SmartMatcher.TaskMatchResult) r).matchLabel;
            if ("Excellent Match".equals(label)) excellent++;
            else if ("Good Match".equals(label)) good++;
        }
        tvTotalMatches.setText(String.valueOf(total));
        tvExcellentMatches.setText(String.valueOf(excellent));
        tvGoodMatches.setText(String.valueOf(good));
    }

    void setLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        listMatchResults.setVisibility(loading ? View.GONE : View.VISIBLE);
    }

    void setTabActive(Button active) {
        Button[] tabs = {btnMatchAll, btnMatchExcellent, btnMatchGood};
        for (Button b : tabs) {
            b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            b.setTextColor(Color.parseColor("#888888"));
        }
        active.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        active.setTextColor(Color.WHITE);
    }

    int getLabelColor(String label) {
        if ("Excellent Match".equals(label)) return Color.parseColor("#2E7D32");
        if ("Good Match".equals(label))      return Color.parseColor("#1565C0");
        if ("Fair Match".equals(label))      return Color.parseColor("#F57F17");
        if ("Too Far".equals(label))         return Color.parseColor("#C62828");
        return Color.parseColor("#888888");
    }

    // ═══════════════════════════════════════
    // ADAPTERS
    // ═══════════════════════════════════════

    class VolunteerMatchAdapter extends BaseAdapter {
        @Override public int getCount()            { return filteredVolResults.size(); }
        @Override public Object getItem(int pos)   { return filteredVolResults.get(pos); }
        @Override public long getItemId(int pos)   { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(
                        R.layout.item_match_result, parent, false);
            SmartMatcher.MatchResult vol = filteredVolResults.get(position);
            bindView(convertView, vol.matchScore, vol.name,
                    "📍 " + vol.city, "🛠️ " + vol.skills,
                    vol.matchLabel, vol.matchReason,
                    "View Profile", vol.distanceText, position);
            return convertView;
        }
    }

    class TaskMatchAdapter extends BaseAdapter {
        @Override public int getCount()            { return filteredTaskResults.size(); }
        @Override public Object getItem(int pos)   { return filteredTaskResults.get(pos); }
        @Override public long getItemId(int pos)   { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(
                        R.layout.item_match_result, parent, false);
            SmartMatcher.TaskMatchResult task = filteredTaskResults.get(position);
            bindView(convertView, task.matchScore, task.taskTitle,
                    "📍 " + task.taskLocation,
                    task.taskUrgency + " • " + task.taskSkill,
                    task.matchLabel, task.matchReason,
                    "View Task", "", position);
            return convertView;
        }
    }

    void bindView(View v, int score, String name, String city,
                  String skills, String label, String reason,
                  String btnText, String distanceText, int position) {

        LinearLayout scoreCircle = v.findViewById(R.id.layoutScoreCircle);
        TextView tvScore   = v.findViewById(R.id.tvMatchScore);
        TextView tvName    = v.findViewById(R.id.tvMatchName);
        TextView tvCity    = v.findViewById(R.id.tvMatchCity);
        TextView tvDist    = v.findViewById(R.id.tvMatchDistance);
        TextView tvSkills  = v.findViewById(R.id.tvMatchSkills);
        TextView tvLabel   = v.findViewById(R.id.tvMatchLabel);
        TextView tvReason  = v.findViewById(R.id.tvMatchReason);
        ProgressBar prog   = v.findViewById(R.id.progressMatch);
        TextView tvPercent = v.findViewById(R.id.tvMatchPercent);
        Button btnAction   = v.findViewById(R.id.btnMatchAction);

        tvScore.setText(String.valueOf(score));
        tvName.setText(name);
        tvCity.setText(city);

        if (distanceText != null && !distanceText.isEmpty()) {
            tvDist.setVisibility(View.VISIBLE);
            tvDist.setText("📏 " + distanceText);
        } else {
            tvDist.setVisibility(View.GONE);
        }

        tvSkills.setText(skills);
        tvLabel.setText(label);
        tvReason.setText(reason);
        prog.setProgress(score);
        tvPercent.setText(score + "%");
        btnAction.setText(btnText);

        int color = getLabelColor(label);
        scoreCircle.setBackgroundColor(color);
        tvLabel.setBackgroundColor(color);
        prog.setProgressTintList(ColorStateList.valueOf(color));

        btnAction.setTag(position);
        btnAction.setOnClickListener(btn -> {
            int pos = (int) btn.getTag();
            if (mode.equals("NGO")) {
                if (pos >= filteredVolResults.size()) return;
                SmartMatcher.MatchResult vol = filteredVolResults.get(pos);
                Intent intent = new Intent(SmartMatchActivity.this,
                        VolunteerProfileActivity.class);
                intent.putExtra("volunteerId",  vol.volunteerId); // ✅ real ID
                intent.putExtra("name",         vol.name);
                intent.putExtra("city",         vol.city);
                intent.putExtra("skills",       vol.skills);
                intent.putExtra("availability", vol.availability);
                intent.putExtra("experience",   vol.experience);
                startActivity(intent);
            } else {
                if (pos >= filteredTaskResults.size()) return;
                SmartMatcher.TaskMatchResult task = filteredTaskResults.get(pos);
                Intent intent = new Intent(SmartMatchActivity.this,
                        TaskDetailActivity.class);
                intent.putExtra("taskId",       task.taskId);     // ✅ real ID
                intent.putExtra("taskTitle",    task.taskTitle);
                intent.putExtra("taskLocation", task.taskLocation);
                intent.putExtra("taskCategory", task.taskCategory);
                intent.putExtra("taskUrgency",  task.taskUrgency);
                intent.putExtra("taskSkill",    task.taskSkill);
                intent.putExtra("taskDate",     task.taskDate);
                intent.putExtra("taskNGO",      task.ngoId);
                intent.putExtra("taskDesc",     "This task matches your skills!");
                startActivity(intent);
            }
        });
    }
}