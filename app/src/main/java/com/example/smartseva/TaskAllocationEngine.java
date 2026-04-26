package com.example.smartseva;

import java.util.*;

public class TaskAllocationEngine {

    // ── Volunteer Score Model ─────────────────────────────
    public static class VolunteerScore {
        public String uid, name, city, skills, availability;
        public int totalScore;
        public int skillScore, locationScore,
                availabilityScore, experienceScore;
        public String matchReason;

        public VolunteerScore(String uid, String name,
                              String city, String skills,
                              String availability) {
            this.uid          = uid;
            this.name         = name;
            this.city         = city;
            this.skills       = skills;
            this.availability = availability;
        }
    }

    // ── Main Scoring Method ───────────────────────────────
    public static List<VolunteerScore> rankVolunteers(
            List<VolunteerScore> volunteers,
            String taskSkill,
            String taskLocation,
            String taskUrgency,
            String taskDate) {

        for (VolunteerScore v : volunteers) {
            v.skillScore        = calcSkillScore(
                    v.skills, taskSkill);
            v.locationScore     = calcLocationScore(
                    v.city, taskLocation);
            v.availabilityScore = calcAvailabilityScore(
                    v.availability, taskDate);
            v.experienceScore   = calcExperienceScore(v.uid);

            // Weighted total score
            // Skill: 40%, Location: 25%,
            // Availability: 20%, Experience: 15%
            v.totalScore =
                    (v.skillScore        * 40 / 100) +
                            (v.locationScore     * 25 / 100) +
                            (v.availabilityScore * 20 / 100) +
                            (v.experienceScore   * 15 / 100);

            v.matchReason = buildReason(v);
        }

        // Sort by total score descending
        volunteers.sort((a, b) ->
                Integer.compare(b.totalScore, a.totalScore));

        return volunteers;
    }

    // ── Skill Score (0-100) ───────────────────────────────
    static int calcSkillScore(String volSkills,
                              String taskSkill) {
        if (volSkills == null || taskSkill == null) return 20;

        String vs = volSkills.toLowerCase();
        String ts = taskSkill.toLowerCase();

        // Exact match
        if (vs.contains(ts) || ts.contains(vs)) return 100;

        // Partial match mapping
        Map<String, String[]> skillMap = new HashMap<>();
        skillMap.put("medical",     new String[]{
                "health","nurse","doctor","first aid","pharmacy"});
        skillMap.put("teaching",    new String[]{
                "education","tutor","trainer","school"});
        skillMap.put("food",        new String[]{
                "cook","nutrition","distribution","catering"});
        skillMap.put("technical",   new String[]{
                "it","computer","software","engineering"});
        skillMap.put("event",       new String[]{
                "management","organize","coordinate","planning"});
        skillMap.put("environment", new String[]{
                "nature","plantation","clean","eco"});

        for (Map.Entry<String, String[]> entry
                : skillMap.entrySet()) {
            if (ts.contains(entry.getKey())) {
                for (String related : entry.getValue()) {
                    if (vs.contains(related)) return 75;
                }
            }
        }

        // Any skill match
        if (ts.contains("any") || vs.contains("any"))
            return 60;

        return 20; // No match
    }

    // ── Location Score (0-100) ────────────────────────────
    static int calcLocationScore(String volCity,
                                 String taskLocation) {
        if (volCity == null || taskLocation == null) return 30;

        String vc = volCity.toLowerCase().trim();
        String tl = taskLocation.toLowerCase().trim();

        // Same city
        if (tl.contains(vc) || vc.contains(tl)) return 100;

        // Same state — check common CG cities
        String[] cgCities = {
                "raipur", "bilaspur", "durg", "bhilai",
                "korba", "rajnandgaon", "jagdalpur",
                "ambikapur", "raigarh"};

        boolean volInCG = false, taskInCG = false;
        for (String city : cgCities) {
            if (vc.contains(city)) volInCG = true;
            if (tl.contains(city)) taskInCG = true;
        }
        if (volInCG && taskInCG) return 70;

        // Same state keywords
        if ((vc.contains("chhattisgarh") ||
                vc.contains(" cg")) &&
                (tl.contains("chhattisgarh") ||
                        tl.contains(" cg")))
            return 65;

        return 30; // Different location
    }

    // ── Availability Score (0-100) ────────────────────────
    static int calcAvailabilityScore(String availability,
                                     String taskDate) {
        if (availability == null) return 50;

        String av = availability.toLowerCase();

        // Full time — always available
        if (av.contains("full") || av.contains("always")
                || av.contains("flexible")) return 100;

        // Check if task date is weekend
        boolean isWeekend = false;
        if (taskDate != null) {
            try {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat(
                                "dd/MM/yyyy", java.util.Locale.getDefault());
                java.util.Date date = sdf.parse(taskDate);
                java.util.Calendar cal =
                        java.util.Calendar.getInstance();
                cal.setTime(date);
                int day = cal.get(java.util.Calendar.DAY_OF_WEEK);
                isWeekend = (day == java.util.Calendar.SATURDAY
                        || day == java.util.Calendar.SUNDAY);
            } catch (Exception ignored) {}
        }

        if (av.contains("weekend") && isWeekend)  return 100;
        if (av.contains("weekday") && !isWeekend) return 100;
        if (av.contains("weekend") && !isWeekend) return 40;
        if (av.contains("weekday") && isWeekend)  return 40;

        return 60; // Default
    }

    // ── Experience Score (0-100) ──────────────────────────
    static int calcExperienceScore(String uid) {
        // Count completed tasks from TaskStatusManager
        List<TaskStatusManager.TaskItem> myTasks =
                TaskStatusManager.getMyTasks();

        int completed = 0;
        for (TaskStatusManager.TaskItem t : myTasks) {
            if (TaskStatusManager.STATUS_RESOLVED
                    .equals(t.status)) {
                completed++;
            }
        }

        // Score based on completed tasks
        if (completed >= 10) return 100;
        if (completed >= 5)  return 80;
        if (completed >= 3)  return 60;
        if (completed >= 1)  return 40;
        return 20; // New volunteer
    }

    // ── Build Match Reason ────────────────────────────────
    static String buildReason(VolunteerScore v) {
        List<String> reasons = new ArrayList<>();

        if (v.skillScore >= 90)
            reasons.add("✅ Perfect skill match");
        else if (v.skillScore >= 70)
            reasons.add("✅ Good skill match");

        if (v.locationScore >= 90)
            reasons.add("📍 Same city");
        else if (v.locationScore >= 65)
            reasons.add("📍 Same state");

        if (v.availabilityScore >= 90)
            reasons.add("📅 Available on task date");

        if (v.experienceScore >= 60)
            reasons.add("⭐ Experienced volunteer");

        if (reasons.isEmpty())
            reasons.add("👤 General match");

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) sb.append(" • ");
            sb.append(reasons.get(i));
        }
        return sb.toString();
    }

    // ── Get Match Grade ───────────────────────────────────
    public static String getGrade(int score) {
        if (score >= 85) return "Excellent";
        if (score >= 70) return "Good";
        if (score >= 50) return "Fair";
        return "Low";
    }

    public static int getGradeColor(int score) {
        if (score >= 85) return 0xFF2E7D32; // Green
        if (score >= 70) return 0xFF1565C0; // Blue
        if (score >= 50) return 0xFFF57F17; // Orange
        return 0xFF9E9E9E;                  // Grey
    }
}