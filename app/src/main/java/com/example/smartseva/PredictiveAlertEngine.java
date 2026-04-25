package com.example.smartseva;

import java.util.*;

public class PredictiveAlertEngine {

    public static class Prediction {
        public String title, description, category,
                urgency, icon, basis, actionLabel;
        public int confidence;
        public boolean isWeatherBased;
        public String dayLabel; // "Today", "Tomorrow", "Day 3" etc.

        public Prediction(String title, String description,
                          String category, String urgency, String icon,
                          String basis, int confidence) {
            this.title       = title;
            this.description = description;
            this.category    = category;
            this.urgency     = urgency;
            this.icon        = icon;
            this.basis       = basis;
            this.confidence  = confidence;
            this.actionLabel = "Create Task";
            this.dayLabel    = "Today";
        }
    }

    // ── Main Method ───────────────────────────────────────
    public static List<Prediction> generatePredictions(
            String location, String weatherCondition,
            double temperature) {

        List<Prediction> all = new ArrayList<>();

        all.addAll(getWeatherPredictions(
                weatherCondition, temperature));
        all.addAll(getSeasonPredictions());
        all.addAll(getLocationPredictions(location));
        all.addAll(getPastDataPredictions());

        // Remove duplicates by category
        List<Prediction> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Prediction p : all) {
            if (!seen.contains(p.category + p.title)) {
                unique.add(p);
                seen.add(p.category + p.title);
            }
        }

        unique.sort((a, b) ->
                Integer.compare(b.confidence, a.confidence));
        return unique;
    }

    // ── Weekly Forecast ───────────────────────────────────
    public static List<Prediction> generateWeeklyForecast(
            String location) {
        List<Prediction> weekly = new ArrayList<>();
        String[] days = {"Today", "Tomorrow", "Day 3",
                "Day 4", "Day 5", "Day 6", "Day 7"};
        Calendar cal = Calendar.getInstance();

        for (int i = 0; i < 7; i++) {
            cal.add(Calendar.DAY_OF_YEAR, i == 0 ? 0 : 1);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int month     = cal.get(Calendar.MONTH);

            Prediction p = getDayPrediction(
                    days[i], dayOfWeek, month, location, i);
            if (p != null) weekly.add(p);
        }
        return weekly;
    }

    static Prediction getDayPrediction(String dayLabel,
                                       int dayOfWeek, int month,
                                       String location, int dayIndex) {

        // Weekend — more volunteers available
        boolean isWeekend = (dayOfWeek == Calendar.SATURDAY
                || dayOfWeek == Calendar.SUNDAY);

        // Summer months
        boolean isSummer = (month >= 2 && month <= 5);
        boolean isMonsoon = (month >= 6 && month <= 8);
        boolean isWinter = (month >= 10 || month <= 1);

        Prediction p;
        if (isSummer) {
            p = new Prediction(
                    isWeekend ? "Water Distribution Drive"
                            : "Water Point Setup",
                    isWeekend
                            ? "Weekend — more volunteers available. "
                            + "Ideal day for water distribution."
                            : "Setup water distribution points "
                            + "before weekend drive.",
                    "Water & Sanitation",
                    dayIndex < 2 ? "Critical" : "Moderate",
                    "💧",
                    "Season: Summer + " +
                            (isWeekend ? "Weekend" : "Weekday"),
                    isWeekend ? 90 : 72
            );
        } else if (isMonsoon) {
            p = new Prediction(
                    "Flood Monitoring — " + dayLabel,
                    "Monitor water levels and keep relief "
                            + "teams on standby.",
                    "Disaster Relief",
                    dayIndex < 3 ? "Critical" : "Moderate",
                    "🌊",
                    "Season: Monsoon",
                    85 - (dayIndex * 3)
            );
        } else if (isWinter) {
            p = new Prediction(
                    isWeekend ? "Winter Clothing Drive"
                            : "Shelter Check",
                    isWeekend
                            ? "Weekend drive for warm clothing "
                            + "distribution to homeless."
                            : "Check on homeless shelters "
                            + "and elderly in your area.",
                    "General Support",
                    "Moderate",
                    "🧥",
                    "Season: Winter + " +
                            (isWeekend ? "Weekend" : "Weekday"),
                    75 - (dayIndex * 2)
            );
        } else {
            p = new Prediction(
                    "Community Outreach — " + dayLabel,
                    "General community support and "
                            + "assessment for the day.",
                    "General Support",
                    "Normal",
                    "🤝",
                    "Regular schedule",
                    60
            );
        }

        p.dayLabel = dayLabel;
        return p;
    }

    // ── 1. WEATHER BASED ──────────────────────────────────

    static List<Prediction> getWeatherPredictions(
            String condition, double temp) {
        List<Prediction> list = new ArrayList<>();
        if (condition == null) condition = "";
        String c = condition.toLowerCase();

        // Extreme heat
        if (temp > 40) {
            list.add(new Prediction(
                    "Extreme Heat Emergency",
                    "Temperature above 40°C detected. "
                            + "Immediate medical support for elderly, "
                            + "children and outdoor workers needed.",
                    "Medical Help", "Critical", "🌡️",
                    "Weather: " + (int)temp + "°C", 96));
            list.add(new Prediction(
                    "Emergency Water Stations",
                    "Set up emergency water and ORS "
                            + "distribution points immediately.",
                    "Water & Sanitation", "Critical", "💧",
                    "Weather: Extreme Heat", 94));
        } else if (temp > 35) {
            list.add(new Prediction(
                    "Heat Advisory",
                    "High temperature alert. "
                            + "Hydration camps and cool shelter "
                            + "access needed.",
                    "Medical Help", "Moderate", "☀️",
                    "Weather: " + (int)temp + "°C", 85));
        }

        // Rain/Storm
        if (c.contains("rain") || c.contains("storm")
                || c.contains("thunderstorm")) {
            list.add(new Prediction(
                    "Flood Risk Alert",
                    "Heavy rain detected. "
                            + "Pre-position disaster relief teams "
                            + "and evacuation support.",
                    "Disaster Relief", "Critical", "⛈️",
                    "Weather: " + condition, 93));
            list.add(new Prediction(
                    "Waterborne Disease Prevention",
                    "Rain increases risk of contaminated "
                            + "water. Distribute water purification "
                            + "tablets and hygiene kits.",
                    "Water & Sanitation", "Critical", "🦠",
                    "Weather: Rain", 88));
        }

        // Cold weather
        if (temp < 10) {
            list.add(new Prediction(
                    "Cold Wave Alert",
                    "Temperature below 10°C. "
                            + "Homeless shelters and warm "
                            + "clothing distribution urgent.",
                    "Emergency Shelter", "Critical", "❄️",
                    "Weather: " + (int)temp + "°C", 91));
        }

        // Fog
        if (c.contains("fog") || c.contains("mist")) {
            list.add(new Prediction(
                    "Fog Safety Alert",
                    "Dense fog increases accident risk. "
                            + "Setup road safety volunteers "
                            + "at key points.",
                    "General Support", "Moderate", "🌫️",
                    "Weather: Fog", 76));
        }

        // Drizzle
        if (c.contains("drizzle")) {
            list.add(new Prediction(
                    "Shelter Access Needed",
                    "Light rain may affect outdoor workers "
                            + "and homeless. Ensure shelter "
                            + "access is available.",
                    "Emergency Shelter", "Normal", "🌧️",
                    "Weather: Drizzle", 68));
        }

        return list;
    }

    // ── 2. SEASON BASED ───────────────────────────────────

    static List<Prediction> getSeasonPredictions() {
        List<Prediction> list = new ArrayList<>();
        int month = Calendar.getInstance().get(Calendar.MONTH);

        // Summer
        if (month >= 2 && month <= 5) {
            list.add(new Prediction(
                    "Summer Water Crisis",
                    "High risk of water shortage in summer. "
                            + "Clean drinking water distribution "
                            + "may be required.",
                    "Water & Sanitation", "Critical", "💧",
                    "Season: Summer (Mar-Jun)", 92));
            list.add(new Prediction(
                    "Heatwave Medical Risk",
                    "Extreme heat may cause health issues "
                            + "for elderly and children. "
                            + "Prepare medical camps in advance.",
                    "Medical Help", "Critical", "🌡️",
                    "Season: Peak Summer", 88));
            list.add(new Prediction(
                    "Food Security Risk",
                    "Food storage becomes difficult "
                            + "in summer. Plan food distribution "
                            + "drives in advance.",
                    "Food Distribution", "Moderate", "🍽️",
                    "Season: Summer", 75));
            list.add(new Prediction(
                    "Child Dehydration Alert",
                    "Children are at high risk of "
                            + "dehydration during summer. "
                            + "Setup school hydration camps.",
                    "Child Welfare", "Critical", "👶",
                    "Season: Summer", 83));
        }

        // Monsoon
        if (month >= 6 && month <= 8) {
            list.add(new Prediction(
                    "Flood Preparedness",
                    "High risk of flooding during monsoon. "
                            + "Arrange relief teams and shelters.",
                    "Disaster Relief", "Critical", "🌊",
                    "Season: Monsoon (Jul-Sep)", 95));
            list.add(new Prediction(
                    "Waterborne Disease Alert",
                    "Diseases spread through contaminated "
                            + "water after rainfall. Medical camps "
                            + "and water purification needed.",
                    "Medical Help", "Critical", "🦠",
                    "Season: Monsoon", 90));
            list.add(new Prediction(
                    "Emergency Shelter Need",
                    "Homeless people need shelter "
                            + "during heavy rainfall.",
                    "Emergency Shelter", "Critical", "🏠",
                    "Season: Monsoon", 85));
            list.add(new Prediction(
                    "Agricultural Community Support",
                    "Farmers may need support during "
                            + "crop damage from heavy rains.",
                    "General Support", "Moderate", "🌾",
                    "Season: Monsoon", 78));
        }

        // Winter
        if (month >= 10 || month <= 1) {
            list.add(new Prediction(
                    "Winter Clothing Drive",
                    "Poor people will need warm clothes "
                            + "in winter. Start donation drives now.",
                    "General Support", "Moderate", "🧥",
                    "Season: Winter (Nov-Feb)", 80));
            list.add(new Prediction(
                    "Elderly Care Alert",
                    "Elderly people need extra care "
                            + "in winter. Plan regular visits.",
                    "Medical Help", "Moderate", "👴",
                    "Season: Winter", 78));
            list.add(new Prediction(
                    "Homeless Shelter Urgency",
                    "Homeless individuals need warm "
                            + "shelter in cold nights.",
                    "Emergency Shelter", "Critical", "🏠",
                    "Season: Winter", 88));
        }

        // Spring — Feb-Mar
        if (month >= 1 && month <= 2) {
            list.add(new Prediction(
                    "Health Camp Season",
                    "Spring is ideal for health camps "
                            + "and community checkups.",
                    "Medical Help", "Normal", "🌸",
                    "Season: Spring", 70));
        }

        // Festival — Oct-Nov
        if (month >= 9 && month <= 10) {
            list.add(new Prediction(
                    "Festival Food Distribution",
                    "Food distribution drives are more "
                            + "effective during festival season.",
                    "Food Distribution", "Normal", "🎉",
                    "Season: Festival Season", 70));
            list.add(new Prediction(
                    "Community Unity Drive",
                    "Festival season is ideal for "
                            + "community bonding events.",
                    "General Support", "Normal", "🎊",
                    "Season: Festival", 65));
        }

        return list;
    }

    // ── 3. LOCATION BASED ─────────────────────────────────

    static List<Prediction> getLocationPredictions(
            String location) {
        List<Prediction> list = new ArrayList<>();
        if (location == null) location = "";
        String loc = location.toLowerCase();

        // Chhattisgarh
        if (loc.contains("raipur") || loc.contains("cg")
                || loc.contains("chhattisgarh")
                || loc.contains("bilaspur")
                || loc.contains("durg")) {
            list.add(new Prediction(
                    "Tribal Education Support",
                    "Chronic need for education volunteers "
                            + "in tribal areas of Chhattisgarh.",
                    "Education", "Moderate", "📚",
                    "Location: Chhattisgarh", 85));
            list.add(new Prediction(
                    "Mining Area Health Risk",
                    "Health issues are common near "
                            + "mining areas. Medical camps required.",
                    "Medical Help", "Moderate", "⛏️",
                    "Location: CG Mining Belt", 80));
            list.add(new Prediction(
                    "Forest Area Food Security",
                    "Remote forest communities have "
                            + "limited food access.",
                    "Food Distribution", "Moderate", "🌲",
                    "Location: CG Forest Region", 77));
        }

        // Urban slum
        if (loc.contains("slum") || loc.contains("basti")
                || loc.contains("colony")
                || loc.contains("nagar")) {
            list.add(new Prediction(
                    "Urban Slum Food Security",
                    "Continuous risk of food insecurity "
                            + "in urban slum areas.",
                    "Food Distribution", "Critical", "🍽️",
                    "Location: Urban Slum", 88));
            list.add(new Prediction(
                    "Child Malnutrition Risk",
                    "Child malnutrition is a common "
                            + "problem in slum areas. Plan "
                            + "nutrition drives immediately.",
                    "Child Welfare", "Critical", "👶",
                    "Location: Urban Slum", 85));
            list.add(new Prediction(
                    "Women Safety & Empowerment",
                    "Women in slum areas need skill "
                            + "development and safety programs.",
                    "Women Empowerment", "Moderate", "👩",
                    "Location: Urban Slum", 78));
        }

        // Rural
        if (loc.contains("village") || loc.contains("gram")
                || loc.contains("rural")
                || loc.contains("gaon")) {
            list.add(new Prediction(
                    "Rural Water Access",
                    "Clean water access is limited "
                            + "in rural areas. Plan water "
                            + "distribution drives.",
                    "Water & Sanitation", "Critical", "💧",
                    "Location: Rural Area", 90));
            list.add(new Prediction(
                    "Rural Healthcare Gap",
                    "Healthcare access is very limited "
                            + "in rural areas. Mobile medical "
                            + "camps will be effective.",
                    "Medical Help", "Moderate", "🏥",
                    "Location: Rural Area", 87));
            list.add(new Prediction(
                    "Digital Literacy Need",
                    "Rural youth need digital literacy "
                            + "programs for employment.",
                    "Education", "Normal", "💻",
                    "Location: Rural Area", 72));
        }

        // Flood prone
        if (loc.contains("river") || loc.contains("nadi")
                || loc.contains("mahanadi")
                || loc.contains("ghats")) {
            list.add(new Prediction(
                    "Flood Prone Zone Alert",
                    "This area is flood prone. Keep "
                            + "disaster relief preparations "
                            + "ready in advance.",
                    "Disaster Relief", "Critical", "🌊",
                    "Location: Flood Prone Zone", 93));
        }

        // Industrial
        if (loc.contains("industrial") || loc.contains("factory")
                || loc.contains("plant")) {
            list.add(new Prediction(
                    "Industrial Area Health Risk",
                    "Pollution near industrial areas "
                            + "affects community health. "
                            + "Regular health camps needed.",
                    "Medical Help", "Moderate", "🏭",
                    "Location: Industrial Zone", 80));
        }

        if (list.isEmpty()) {
            list.add(new Prediction(
                    "Community Assessment Needed",
                    "A community needs assessment is "
                            + "required based on your location.",
                    "General Support", "Normal", "🗺️",
                    "Location: General", 60));
        }

        return list;
    }

    // ── 4. PAST DATA BASED ────────────────────────────────

    static List<Prediction> getPastDataPredictions() {
        List<Prediction> list = new ArrayList<>();
        List<LocalTaskStore.LocalTask> tasks =
                LocalTaskStore.getInstance().getTasks();
        if (tasks.isEmpty()) return list;

        Map<String, Integer> catCount  = new HashMap<>();
        Map<String, Integer> urgCount  = new HashMap<>();

        for (LocalTaskStore.LocalTask t : tasks) {
            if (t.category != null)
                catCount.put(t.category,
                        catCount.getOrDefault(t.category, 0) + 1);
            if (t.urgency != null)
                urgCount.put(t.urgency,
                        urgCount.getOrDefault(t.urgency, 0) + 1);
        }

        // Top category
        String topCat = "";
        int topCount  = 0;
        for (Map.Entry<String, Integer> e : catCount.entrySet())
            if (e.getValue() > topCount) {
                topCount = e.getValue();
                topCat   = e.getKey();
            }

        if (!topCat.isEmpty() && topCount >= 2) {
            list.add(new Prediction(
                    "Recurring Need: " + topCat,
                    topCat + " has appeared " + topCount
                            + " times in past tasks. "
                            + "This need is likely to recur.",
                    topCat, "Moderate", "🔄",
                    "Past Data: " + topCount + " tasks",
                    Math.min(70 + topCount * 5, 95)));
        }

        // Critical pattern
        int critCount = urgCount.getOrDefault("Critical", 0);
        if (critCount >= 2) {
            list.add(new Prediction(
                    "Critical Pattern Detected",
                    critCount + " critical tasks recorded. "
                            + "Your area has high-risk situations — "
                            + "stay prepared.",
                    "General Support", "Critical", "⚠️",
                    "Past Data: " + critCount + " critical",
                    85));
        }

        // High activity
        if (tasks.size() >= 3) {
            list.add(new Prediction(
                    "High Activity Zone",
                    tasks.size() + " tasks created so far. "
                            + "This is a high-need community zone.",
                    "General Support", "Moderate", "📊",
                    "Past Data: " + tasks.size() + " total",
                    75));
        }

        // Volunteer gap
        int totalVol = 0;
        for (LocalTaskStore.LocalTask t : tasks)
            try {
                totalVol += Integer.parseInt(
                        t.volunteers != null ? t.volunteers : "0");
            } catch (Exception ignored) {}

        if (totalVol > 20) {
            list.add(new Prediction(
                    "Volunteer Shortage Risk",
                    totalVol + " total volunteers needed "
                            + "across tasks. Consider recruiting "
                            + "more volunteers.",
                    "General Support", "Moderate", "👥",
                    "Past Data: Volunteer demand",
                    78));
        }

        return list;
    }
}