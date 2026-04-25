package com.example.smartseva;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public class LeaderboardActivity extends AppCompatActivity {

    // Views
    TextView tv1stName, tv1stScore, tv2ndName, tv2ndScore,
            tv3rdName, tv3rdScore;
    TextView tvTotalHeroes, tvTotalTasksDone, tvTotalImpactPts;
    Button btnBackLeaderboard, btnTabAllTime, btnTabMonthly, btnTabWeekly;
    ListView listLeaderboard;

    // Data
    List<Hero> allHeroes = new ArrayList<>();
    String currentTab = "alltime";

    // ── Hero Model ──
    static class Hero {
        String name, city, skills, badge;
        int tasksDone, impactScore, hoursGiven;
        boolean hasVehicle;

        Hero(String name, String city, String skills,
             int tasksDone, int impactScore, int hoursGiven) {
            this.name        = name;
            this.city        = city;
            this.skills      = skills;
            this.tasksDone   = tasksDone;
            this.impactScore = impactScore;
            this.hoursGiven  = hoursGiven;
            this.badge       = calculateBadge(tasksDone, impactScore);
        }

        static String calculateBadge(int tasks, int score) {
            if (score >= 500)  return "🦸 Super Hero";
            if (score >= 300)  return "⭐ Champion";
            if (score >= 200)  return "🔥 Top Performer";
            if (score >= 100)  return "💪 Active Hero";
            if (tasks >= 3)    return "🌟 Rising Star";
            return "🌱 Newcomer";
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // ── Views ──
        tv1stName          = findViewById(R.id.tv1stName);
        tv1stScore         = findViewById(R.id.tv1stScore);
        tv2ndName          = findViewById(R.id.tv2ndName);
        tv2ndScore         = findViewById(R.id.tv2ndScore);
        tv3rdName          = findViewById(R.id.tv3rdName);
        tv3rdScore         = findViewById(R.id.tv3rdScore);
        tvTotalHeroes      = findViewById(R.id.tvTotalHeroes);
        tvTotalTasksDone   = findViewById(R.id.tvTotalTasksDone);
        tvTotalImpactPts   = findViewById(R.id.tvTotalImpactPts);
        btnBackLeaderboard = findViewById(R.id.btnBackLeaderboard);
        btnTabAllTime      = findViewById(R.id.btnTabAllTime);
        btnTabMonthly      = findViewById(R.id.btnTabMonthly);
        btnTabWeekly       = findViewById(R.id.btnTabWeekly);
        listLeaderboard    = findViewById(R.id.listLeaderboard);

        // ── Listeners ──
        btnBackLeaderboard.setOnClickListener(v -> finish());

        btnTabAllTime.setOnClickListener(v -> {
            currentTab = "alltime";
            setTabActive(btnTabAllTime);
            loadLeaderboard();
        });
        btnTabMonthly.setOnClickListener(v -> {
            currentTab = "monthly";
            setTabActive(btnTabMonthly);
            loadLeaderboard();
        });
        btnTabWeekly.setOnClickListener(v -> {
            currentTab = "weekly";
            setTabActive(btnTabWeekly);
            loadLeaderboard();
        });

        // ── Load Data ──
        loadSampleHeroes();
        loadLeaderboard();

        // ── Click on hero ──
        listLeaderboard.setOnItemClickListener((parent, view, position, id) -> {
            Hero hero = allHeroes.get(position);
            showHeroDialog(hero, position + 1);
        });
    }

    // ═══════════════════════════════════════
    // SAMPLE DATA
    // ═══════════════════════════════════════

    void loadSampleHeroes() {
        // Firebase teammate yahan Firestore se real data load karega
        allHeroes.clear();
        allHeroes.add(new Hero("Priya Sharma",   "Raipur, CG",
                "Teaching, Medical",    12, 580, 48));
        allHeroes.add(new Hero("Amit Sahu",      "Raipur, CG",
                "Food Distribution",   10, 420, 40));
        allHeroes.add(new Hero("Anjali Patel",   "Durg, CG",
                "Social Media",         8, 310, 32));
        allHeroes.add(new Hero("Rahul Verma",    "Bilaspur, CG",
                "Event Management",     7, 260, 28));
        allHeroes.add(new Hero("Deepika Singh",  "Raipur, CG",
                "Teaching",             6, 210, 24));
        allHeroes.add(new Hero("Sonu Kumar",     "Korba, CG",
                "Technical",            5, 180, 20));
        allHeroes.add(new Hero("Meena Yadav",    "Raipur, CG",
                "Medical Help",         4, 140, 16));
        allHeroes.add(new Hero("Rohan Gupta",    "Bhilai, CG",
                "Any Skill",            3, 100, 12));
        allHeroes.add(new Hero("Sunita Devi",    "Rajnandgaon, CG",
                "Teaching",             2,  60,  8));
        allHeroes.add(new Hero("Vikram Singh",   "Raipur, CG",
                "Food Distribution",    1,  20,  4));

        // Sort by impact score
        Collections.sort(allHeroes,
                (a, b) -> b.impactScore - a.impactScore);
    }

    // ═══════════════════════════════════════
    // LOAD LEADERBOARD
    // ═══════════════════════════════════════

    void loadLeaderboard() {
        List<Hero> heroes = getFilteredHeroes();

        if (heroes.isEmpty()) {
            tv1stName.setText("—");
            tv1stScore.setText("0 pts");
            tv2ndName.setText("—");
            tv2ndScore.setText("0 pts");
            tv3rdName.setText("—");
            tv3rdScore.setText("0 pts");
            return;
        }

        // Podium
        if (heroes.size() >= 1) {
            tv1stName.setText(heroes.get(0).name);
            tv1stScore.setText(heroes.get(0).impactScore + " pts");
        }
        if (heroes.size() >= 2) {
            tv2ndName.setText(heroes.get(1).name);
            tv2ndScore.setText(heroes.get(1).impactScore + " pts");
        }
        if (heroes.size() >= 3) {
            tv3rdName.setText(heroes.get(2).name);
            tv3rdScore.setText(heroes.get(2).impactScore + " pts");
        }

        // Summary stats
        int totalTasks = 0, totalPts = 0;
        for (Hero h : heroes) {
            totalTasks += h.tasksDone;
            totalPts   += h.impactScore;
        }
        tvTotalHeroes.setText(String.valueOf(heroes.size()));
        tvTotalTasksDone.setText(String.valueOf(totalTasks));
        tvTotalImpactPts.setText(String.valueOf(totalPts));

        // List
        listLeaderboard.setAdapter(new HeroAdapter(heroes));
    }

    List<Hero> getFilteredHeroes() {
        // Firebase teammate tab-wise real data filter karega
        // Abhi sabke liye same data
        List<Hero> filtered = new ArrayList<>(allHeroes);
        switch (currentTab) {
            case "monthly":
                // Show top 7 for monthly
                return filtered.subList(0, Math.min(7, filtered.size()));
            case "weekly":
                // Show top 4 for weekly
                return filtered.subList(0, Math.min(4, filtered.size()));
            default:
                return filtered;
        }
    }

    // ═══════════════════════════════════════
    // HERO DIALOG
    // ═══════════════════════════════════════

    void showHeroDialog(Hero hero, int rank) {
        String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;
        new android.app.AlertDialog.Builder(this)
                .setTitle(medal + " " + hero.name)
                .setMessage(
                        "📍 " + hero.city + "\n\n" +
                                "🛠️ Skills: " + hero.skills + "\n\n" +
                                "✅ Tasks Done: " + hero.tasksDone + "\n" +
                                "⭐ Impact Score: " + hero.impactScore + " pts\n" +
                                "⏰ Hours Given: " + hero.hoursGiven + " hrs\n\n" +
                                "🏅 Badge: " + hero.badge
                )
                .setPositiveButton("Close", null)
                .show();
    }

    // ═══════════════════════════════════════
    // TAB
    // ═══════════════════════════════════════

    void setTabActive(Button active) {
        Button[] tabs = {btnTabAllTime, btnTabMonthly, btnTabWeekly};
        for (Button b : tabs) {
            b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            b.setTextColor(Color.parseColor("#888888"));
        }
        active.setBackgroundTintList(
                ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        active.setTextColor(Color.WHITE);
    }

    // ═══════════════════════════════════════
    // ADAPTER
    // ═══════════════════════════════════════

    class HeroAdapter extends BaseAdapter {
        List<Hero> heroes;
        HeroAdapter(List<Hero> heroes) { this.heroes = heroes; }

        @Override public int getCount() { return heroes.size(); }
        @Override public Object getItem(int pos) { return heroes.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(
                        R.layout.item_leaderboard, parent, false);

            Hero hero = heroes.get(position);
            int rank  = position + 1;

            TextView tvRank      = convertView.findViewById(R.id.tvRank);
            TextView tvName      = convertView.findViewById(R.id.tvHeroName);
            TextView tvCity      = convertView.findViewById(R.id.tvHeroCity);
            TextView tvTasksDone = convertView.findViewById(R.id.tvHeroTasksDone);
            TextView tvBadge     = convertView.findViewById(R.id.tvHeroBadge);
            TextView tvScore     = convertView.findViewById(R.id.tvHeroScore);

            // Rank
            tvRank.setText(String.valueOf(rank));

            // Rank colors
            int rankColor;
            switch (rank) {
                case 1:  rankColor = Color.parseColor("#FFD700"); break;
                case 2:  rankColor = Color.parseColor("#C0C0C0"); break;
                case 3:  rankColor = Color.parseColor("#CD7F32"); break;
                default: rankColor = Color.parseColor("#1A1A1A"); break;
            }
            tvRank.setBackgroundTintList(
                    ColorStateList.valueOf(rankColor));
            tvRank.setTextColor(rank <= 3 ?
                    Color.parseColor("#1A1A1A") : Color.WHITE);

            tvName.setText(hero.name);
            tvCity.setText("📍 " + hero.city);
            tvTasksDone.setText("✅ " + hero.tasksDone + " tasks");
            tvBadge.setText(hero.badge);
            tvScore.setText(String.valueOf(hero.impactScore));

            // Score color based on rank
            tvScore.setTextColor(rank == 1 ?
                    Color.parseColor("#FFD700") :
                    rank <= 3 ? Color.parseColor("#1A1A1A") :
                            Color.parseColor("#555555"));

            return convertView;
        }
    }
}