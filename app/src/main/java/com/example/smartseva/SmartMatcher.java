package com.example.smartseva;

import java.util.*;

public class SmartMatcher {

    // ── Match Score Model ──
    public static class MatchResult {
        public String name;
        public String city;
        public String skills;
        public String availability;
        public int matchScore;       // 0-100
        public String matchLabel;    // "Excellent / Good / Fair"
        public String matchReason;   // Why matched
        public int experience;

        public double distanceKm = -1;
        public String distanceText = "";
        public int locationScore = 0;

        // GPS location scores already set hain — sirf skills + avail calculate karo
        public static List<MatchResult> matchVolunteersForTaskWithGPS(
                String taskSkill,
                String taskUrgency,
                List<MatchResult> volunteers) {

            for (MatchResult vol : volunteers) {
                // Already excluded (Too Far)
                if (vol.matchLabel != null && vol.matchLabel.equals("Too Far")) continue;

                int score = 0;
                List<String> reasons = new ArrayList<>();

                // ── Skills (40 pts) ──
                int skillScore = calculateSkillMatch(taskSkill, vol.skills);
                score += skillScore;
                if (skillScore >= 35)      reasons.add("✅ Skills match perfectly");
                else if (skillScore >= 20) reasons.add("🟡 Skills partially match");

                // ── GPS Location (30 pts) ──
                score += vol.locationScore;
                if (vol.locationScore >= 25)      reasons.add("📍 Very nearby (" + vol.distanceText + ")");
                else if (vol.locationScore >= 15) reasons.add("📍 " + vol.distanceText);
                else if (vol.locationScore > 0)   reasons.add("📍 Far but reachable (" + vol.distanceText + ")");

                // ── Availability (20 pts) ──
                int availScore = 15; // default
                score += availScore;
                reasons.add("⏰ Available on required days");

                // ── Experience (10 pts) ──
                int expScore = Math.min(vol.experience * 3, 10);
                score += expScore;
                if (expScore > 0) reasons.add("💼 Has experience");

                vol.matchScore  = Math.min(score, 100);
                vol.matchReason = String.join(" • ", reasons);
                vol.matchLabel  = getMatchLabel(vol.matchScore);
            }

            Collections.sort(volunteers, (a, b) -> {
                // Too Far wale sabse neeche
                if (a.matchLabel.equals("Too Far") && !b.matchLabel.equals("Too Far")) return 1;
                if (!a.matchLabel.equals("Too Far") && b.matchLabel.equals("Too Far")) return -1;
                return b.matchScore - a.matchScore;
            });

            return volunteers;
        }

        MatchResult(String name, String city, String skills,
                    String availability, int experience) {
            this.name         = name;
            this.city         = city;
            this.skills       = skills;
            this.availability = availability;
            this.experience   = experience;
        }
    }

    public static class TaskMatchResult {
        public String taskTitle;
        public String taskLocation;
        public String taskCategory;
        public String taskUrgency;
        public String taskSkill;
        public String taskDate;
        public int matchScore;
        public String matchLabel;
        public String matchReason;

        TaskMatchResult(String title, String location, String category,
                        String urgency, String skill, String date) {
            this.taskTitle    = title;
            this.taskLocation = location;
            this.taskCategory = category;
            this.taskUrgency  = urgency;
            this.taskSkill    = skill;
            this.taskDate     = date;
        }
    }

    // ═══════════════════════════════════════
    // NGO → Find matching volunteers for a task
    // ═══════════════════════════════════════

    public static List<MatchResult> matchVolunteersForTask(
            String taskSkill,
            String taskLocation,
            String taskAvailability,
            List<MatchResult> allVolunteers) {

        for (MatchResult vol : allVolunteers) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // ── 1. Skills Match (40 points) ──
            int skillScore = calculateSkillMatch(taskSkill, vol.skills);
            score += skillScore;
            if (skillScore >= 35) reasons.add("✅ Skills match perfectly");
            else if (skillScore >= 20) reasons.add("🟡 Skills partially match");

            // ── 2. Location Match (30 points) ──
            int locationScore = calculateLocationMatch(taskLocation, vol.city);
            score += locationScore;
            if (locationScore >= 25) reasons.add("📍 Same city/area");
            else if (locationScore >= 15) reasons.add("📍 Nearby location");

            // ── 3. Availability Match (20 points) ──
            int availScore = calculateAvailabilityMatch(taskAvailability, vol.availability);
            score += availScore;
            if (availScore >= 15) reasons.add("⏰ Available on required days");

            // ── 4. Experience Bonus (10 points) ──
            int expScore = Math.min(vol.experience * 3, 10);
            score += expScore;
            if (expScore > 0) reasons.add("💼 Has volunteering experience");

            vol.matchScore  = Math.min(score, 100);
            vol.matchReason = String.join(" • ", reasons);
            vol.matchLabel  = getMatchLabel(vol.matchScore);
        }

        // Sort by score descending
        Collections.sort(allVolunteers,
                (a, b) -> b.matchScore - a.matchScore);

        return allVolunteers;
    }

    // ═══════════════════════════════════════
    // VOLUNTEER → Find matching tasks
    // ═══════════════════════════════════════

    public static List<TaskMatchResult> matchTasksForVolunteer(
            String volSkills,
            String volCity,
            String volAvailability,
            List<TaskMatchResult> allTasks) {

        for (TaskMatchResult task : allTasks) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // ── 1. Skills Match (40 points) ──
            int skillScore = calculateSkillMatch(task.taskSkill, volSkills);
            score += skillScore;
            if (skillScore >= 35) reasons.add("✅ Matches your skills");
            else if (skillScore >= 20) reasons.add("🟡 Partially matches skills");

            // ── 2. Location Match (30 points) ──
            int locationScore = calculateLocationMatch(task.taskLocation, volCity);
            score += locationScore;
            if (locationScore >= 25) reasons.add("📍 Near your location");
            else if (locationScore >= 15) reasons.add("📍 Reachable location");

            // ── 3. Availability Match (20 points) ──
            int availScore = calculateAvailabilityMatch(task.taskUrgency, volAvailability);
            score += availScore;
            if (availScore >= 15) reasons.add("⏰ Fits your schedule");

            // ── 4. Urgency Bonus (10 points) ──
            if (task.taskUrgency != null && task.taskUrgency.contains("Critical")) {
                score += 10;
                reasons.add("🔴 Urgent need!");
            }

            task.matchScore  = Math.min(score, 100);
            task.matchReason = String.join(" • ", reasons);
            task.matchLabel  = getMatchLabel(task.matchScore);
        }

        // Sort by score descending
        Collections.sort(allTasks,
                (a, b) -> b.matchScore - a.matchScore);

        return allTasks;
    }

    // ═══════════════════════════════════════
    // MATCHING ALGORITHMS
    // ═══════════════════════════════════════

    static int calculateSkillMatch(String required, String available) {
        if (required == null || available == null) return 0;
        if (required.equalsIgnoreCase("Any Skill")) return 30;

        String req = required.toLowerCase();
        String avl = available.toLowerCase();

        // Exact match
        if (avl.contains(req)) return 40;

        // Keyword matching
        String[] reqWords = req.split("[,\\s]+");
        String[] avlWords = avl.split("[,\\s]+");
        int matched = 0;
        for (String rw : reqWords) {
            for (String aw : avlWords) {
                if (rw.length() > 3 && aw.contains(rw)) matched++;
            }
        }
        if (matched > 0) return Math.min(matched * 15, 35);

        // Category matching
        Map<String, String[]> categoryMap = new HashMap<>();
        categoryMap.put("medical", new String[]{"medical","health","doctor","nurse","first aid"});
        categoryMap.put("teaching", new String[]{"teaching","tutoring","education","trainer"});
        categoryMap.put("food", new String[]{"food","cooking","distribution","catering"});
        categoryMap.put("technical", new String[]{"technical","app","web","coding","developer"});
        categoryMap.put("social", new String[]{"social media","marketing","content","media"});
        categoryMap.put("event", new String[]{"event","management","organize","coordinator"});

        for (Map.Entry<String, String[]> entry : categoryMap.entrySet()) {
            boolean reqMatch = false, avlMatch = false;
            for (String kw : entry.getValue()) {
                if (req.contains(kw)) reqMatch = true;
                if (avl.contains(kw)) avlMatch = true;
            }
            if (reqMatch && avlMatch) return 25;
        }

        return 0;
    }

    static int calculateLocationMatch(String loc1, String loc2) {
        if (loc1 == null || loc2 == null) return 10;
        if (loc1.toLowerCase().trim().equals(loc2.toLowerCase().trim())) return 30;
        // Baaki matching as fallback
        return 15;
    }

    // ── Real GPS distance (Haversine Formula) ──
    public static double calculateGPSDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        final int EARTH_RADIUS = 6371; // km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) *
                        Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c; // distance in km
    }

    // ── Location score from GPS distance ──
    public static int getLocationScoreFromDistance(double distanceKm) {
        if (distanceKm <= 10)  return 30; // Same area
        if (distanceKm <= 30)  return 25; // Very nearby
        if (distanceKm <= 60)  return 20; // Nearby
        if (distanceKm <= 100) return 15; // Acceptable
        return 5;                          // Too far
    }

    static int calculateAvailabilityMatch(String required, String available) {
        if (required == null || available == null) return 10;

        String req = required.toLowerCase();
        String avl = available.toLowerCase();

        if (avl.contains("both")) return 20; // Available both days
        if (req.contains("weekend") && avl.contains("weekend")) return 20;
        if (req.contains("weekday") && avl.contains("weekday")) return 20;
        if (req.contains("critical") && avl.contains("both")) return 20;
        if (req.contains("critical") && avl.contains("weekday")) return 15;

        return 10; // Partial match
    }

    static String getMatchLabel(int score) {
        if (score >= 75) return "Excellent Match";
        if (score >= 50) return "Good Match";
        if (score >= 30) return "Fair Match";
        return "Low Match";
    }
}