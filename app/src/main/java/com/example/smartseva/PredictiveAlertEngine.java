package com.example.smartseva;

import android.content.Context;
import java.util.*;

public class PredictiveAlertEngine {

    // ── Prediction Model ──────────────────────────────────
    public static class Prediction {
        public String title, description, category,
                urgency, icon, basis;
        public int confidence; // 0-100%

        public Prediction(String title, String description,
                          String category, String urgency,
                          String icon, String basis, int confidence) {
            this.title       = title;
            this.description = description;
            this.category    = category;
            this.urgency     = urgency;
            this.icon        = icon;
            this.basis       = basis;
            this.confidence  = confidence;
        }
    }

    // ── Main Prediction Method ────────────────────────────
    public static List<Prediction> generatePredictions(
            Context context, String location) {

        List<Prediction> predictions = new ArrayList<>();

        // 1. Season based
        predictions.addAll(getSeasonPredictions());

        // 2. Location based
        predictions.addAll(getLocationPredictions(location));

        // 3. Past data based
        predictions.addAll(getPastDataPredictions());

        // Sort by confidence
        predictions.sort((a, b) ->
                Integer.compare(b.confidence, a.confidence));

        return predictions;
    }

    // ── 1. SEASON BASED ───────────────────────────────────

    static List<Prediction> getSeasonPredictions() {
        List<Prediction> list = new ArrayList<>();
        Calendar cal   = Calendar.getInstance();
        int month      = cal.get(Calendar.MONTH); // 0-11
        int hour       = cal.get(Calendar.HOUR_OF_DAY);

        // Summer — March to June (2-5)
        if (month >= 2 && month <= 5) {
            list.add(new Prediction(
                    "Water Shortage Alert",
                    "Garmi ke season mein water crisis ka high risk hai. " +
                            "Clean drinking water distribution ki zaroorat pad " +
                            "sakti hai.",
                    "Water & Sanitation",
                    "Critical",
                    "💧",
                    "Season: Summer (Mar-Jun)",
                    92
            ));
            list.add(new Prediction(
                    "Heatwave Medical Emergency",
                    "Extreme heat se elderly aur children ko medical " +
                            "help ki zaroorat ho sakti hai. Medical camps " +
                            "prepare karein.",
                    "Medical Help",
                    "Critical",
                    "🌡️",
                    "Season: Peak Summer",
                    88
            ));
            list.add(new Prediction(
                    "Food Spoilage Risk",
                    "Garmi mein food storage mushkil ho jaata hai. " +
                            "Food distribution drives pehle se plan karein.",
                    "Food Distribution",
                    "Moderate",
                    "🍽️",
                    "Season: Summer",
                    75
            ));
        }

        // Monsoon — July to September (6-8)
        if (month >= 6 && month <= 8) {
            list.add(new Prediction(
                    "Flood Risk — Prepare Now",
                    "Monsoon season mein flood ka high risk hai. " +
                            "Relief teams aur shelter arrangements pehle se " +
                            "karein.",
                    "Disaster Relief",
                    "Critical",
                    "🌊",
                    "Season: Monsoon (Jul-Sep)",
                    95
            ));
            list.add(new Prediction(
                    "Waterborne Disease Alert",
                    "Baarish ke baad contaminated water se diseases " +
                            "failti hain. Medical camps aur water purification " +
                            "ki zaroorat hogi.",
                    "Medical Help",
                    "Critical",
                    "🦠",
                    "Season: Monsoon",
                    90
            ));
            list.add(new Prediction(
                    "Shelter for Homeless",
                    "Heavy rain mein homeless logon ko shelter ki " +
                            "zaroorat hogi.",
                    "Emergency Shelter",
                    "Critical",
                    "🏠",
                    "Season: Monsoon",
                    85
            ));
        }

        // Winter — November to February (10-1)
        if (month >= 10 || month <= 1) {
            list.add(new Prediction(
                    "Winter Clothing Drive",
                    "Sardi mein garib logon ko warm clothes ki zaroorat " +
                            "hogi. Donation drive abhi se shuru karein.",
                    "General Support",
                    "Moderate",
                    "🧥",
                    "Season: Winter (Nov-Feb)",
                    80
            ));
            list.add(new Prediction(
                    "Elderly Care Alert",
                    "Sardi mein elderly logon ko extra care ki zaroorat " +
                            "hoti hai. Regular visits plan karein.",
                    "Medical Help",
                    "Moderate",
                    "👴",
                    "Season: Winter",
                    78
            ));
        }

        // Festival season — Oct-Nov (9-10)
        if (month >= 9 && month <= 10) {
            list.add(new Prediction(
                    "Festival Food Distribution",
                    "Festival season mein food distribution drives " +
                            "zyada effective hoti hain.",
                    "Food Distribution",
                    "Normal",
                    "🎉",
                    "Season: Festival Season",
                    70
            ));
        }

        return list;
    }

    // ── 2. LOCATION BASED ─────────────────────────────────

    static List<Prediction> getLocationPredictions(
            String location) {
        List<Prediction> list = new ArrayList<>();
        if (location == null) location = "";
        String loc = location.toLowerCase();

        // Chhattisgarh specific
        if (loc.contains("raipur") || loc.contains("chhattisgarh")
                || loc.contains("bilaspur") || loc.contains("durg")) {
            list.add(new Prediction(
                    "Tribal Area Education Need",
                    "Chhattisgarh ke tribal areas mein education " +
                            "volunteers ki chronic zaroorat hai.",
                    "Education",
                    "Moderate",
                    "📚",
                    "Location: Chhattisgarh",
                    85
            ));
            list.add(new Prediction(
                    "Mining Area Health Risk",
                    "Mining areas ke paas health issues common hain. " +
                            "Medical camps ki zaroorat hai.",
                    "Medical Help",
                    "Moderate",
                    "⛏️",
                    "Location: CG Mining Belt",
                    80
            ));
        }

        // Urban slum areas
        if (loc.contains("slum") || loc.contains("basti")
                || loc.contains("nagar")) {
            list.add(new Prediction(
                    "Urban Slum Food Security",
                    "Urban slum areas mein food insecurity ka " +
                            "continuous risk rehta hai.",
                    "Food Distribution",
                    "Critical",
                    "🍽️",
                    "Location: Urban Slum",
                    88
            ));
            list.add(new Prediction(
                    "Child Malnutrition Risk",
                    "Slum areas mein child malnutrition common problem " +
                            "hai. Nutrition drives zaroor plan karein.",
                    "Child Welfare",
                    "Critical",
                    "👶",
                    "Location: Urban Slum",
                    85
            ));
        }

        // Rural areas
        if (loc.contains("village") || loc.contains("gram")
                || loc.contains("gaon") || loc.contains("rural")) {
            list.add(new Prediction(
                    "Rural Water Access",
                    "Rural areas mein clean water access limited hota " +
                            "hai. Water distribution zaroor plan karein.",
                    "Water & Sanitation",
                    "Critical",
                    "💧",
                    "Location: Rural Area",
                    90
            ));
            list.add(new Prediction(
                    "Rural Healthcare Gap",
                    "Rural areas mein healthcare access bahut kam hai. " +
                            "Mobile medical camps effective honge.",
                    "Medical Help",
                    "Moderate",
                    "🏥",
                    "Location: Rural Area",
                    87
            ));
        }

        // Flood prone areas
        if (loc.contains("river") || loc.contains("nadi")
                || loc.contains("ghats") || loc.contains("mahanadi")) {
            list.add(new Prediction(
                    "Flood Prone Zone Alert",
                    "Yeh area flood prone hai. Disaster relief " +
                            "preparations pehle se rakhein.",
                    "Disaster Relief",
                    "Critical",
                    "🌊",
                    "Location: Flood Prone",
                    93
            ));
        }

        // Default — general India
        if (list.isEmpty()) {
            list.add(new Prediction(
                    "General Community Assessment",
                    "Location ke hisaab se community needs assess " +
                            "karne ki zaroorat hai.",
                    "General Support",
                    "Normal",
                    "🗺️",
                    "Location: General",
                    60
            ));
        }

        return list;
    }

    // ── 3. PAST DATA BASED ────────────────────────────────

    static List<Prediction> getPastDataPredictions() {
        List<Prediction> list = new ArrayList<>();

        List<LocalTaskStore.LocalTask> pastTasks =
                LocalTaskStore.getInstance().getTasks();

        if (pastTasks.isEmpty()) return list;

        // Count categories
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Integer> urgencyCount  = new HashMap<>();

        for (LocalTaskStore.LocalTask t : pastTasks) {
            if (t.category != null) {
                categoryCount.put(t.category,
                        categoryCount.getOrDefault(t.category, 0) + 1);
            }
            if (t.urgency != null) {
                urgencyCount.put(t.urgency,
                        urgencyCount.getOrDefault(t.urgency, 0) + 1);
            }
        }

        // Most common category — predict repeat need
        String topCategory = "";
        int topCount = 0;
        for (Map.Entry<String, Integer> e : categoryCount.entrySet()) {
            if (e.getValue() > topCount) {
                topCount    = e.getValue();
                topCategory = e.getKey();
            }
        }

        if (!topCategory.isEmpty() && topCount >= 2) {
            list.add(new Prediction(
                    "Recurring Need: " + topCategory,
                    topCategory + " ke " + topCount +
                            " tasks pehle bhi create ho chuke hain. " +
                            "Yeh need dobara arise ho sakti hai.",
                    topCategory,
                    "Moderate",
                    "🔄",
                    "Past Data: " + topCount + " similar tasks",
                    70 + Math.min(topCount * 5, 25)
            ));
        }

        // High critical tasks
        int criticalCount =
                urgencyCount.getOrDefault("Critical", 0);
        if (criticalCount >= 2) {
            list.add(new Prediction(
                    "Critical Pattern Detected",
                    "Pichle " + criticalCount +
                            " critical tasks the. Aapke area mein " +
                            "critical situations common hain — pehle se " +
                            "prepare rahein.",
                    "General Support",
                    "Critical",
                    "⚠️",
                    "Past Data: " + criticalCount + " critical tasks",
                    85
            ));
        }

        // Recent activity pattern
        if (pastTasks.size() >= 3) {
            list.add(new Prediction(
                    "High Activity Area",
                    "Aapke area mein " + pastTasks.size() +
                            " tasks already create ho chuke hain. " +
                            "Yeh area high need zone hai.",
                    "General Support",
                    "Moderate",
                    "📊",
                    "Past Data: " + pastTasks.size() + " total tasks",
                    75
            ));
        }

        return list;
    }
}