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

        String l1 = loc1.toLowerCase().trim();
        String l2 = loc2.toLowerCase().trim();

        // Exact city match
        if (l1.contains(l2) || l2.contains(l1)) return 30;

        // Same state (CG, Chhattisgarh etc.)
        String[] cgCities = {"raipur","bilaspur","durg","bhilai","korba",
                "rajnandgaon","jagdalpur","ambikapur","raigarh"};
        boolean l1CG = false, l2CG = false;
        for (String city : cgCities) {
            if (l1.contains(city) || l1.contains("cg") || l1.contains("chhattisgarh")) l1CG = true;
            if (l2.contains(city) || l2.contains("cg") || l2.contains("chhattisgarh")) l2CG = true;
        }
        if (l1CG && l2CG) return 20;

        // Common words
        String[] words1 = l1.split("[,\\s]+");
        String[] words2 = l2.split("[,\\s]+");
        for (String w1 : words1) {
            for (String w2 : words2) {
                if (w1.length() > 3 && w1.equals(w2)) return 25;
            }
        }

        return 5; // Different location but still show
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