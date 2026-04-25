package com.example.smartseva;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

public class SmartMatcher {

    // ── Match Score Models (unchanged) ──
    public static class MatchResult {
        public String volunteerId;
        public String name;
        public String city;
        public String skills;
        public String availability;
        public int matchScore;
        public String matchLabel;
        public String matchReason;
        public int experience;
        public double distanceKm = -1;
        public String distanceText = "";
        public int locationScore = 0;

        MatchResult(String volunteerId, String name, String city,
                    String skills, String availability, int experience) {
            this.volunteerId  = volunteerId;
            this.name         = name;
            this.city         = city;
            this.skills       = skills;
            this.availability = availability;
            this.experience   = experience;
        }
    }

    public static class TaskMatchResult {
        public String taskId;
        public String taskTitle;
        public String taskLocation;
        public String taskCategory;
        public String taskUrgency;
        public String taskSkill;
        public String taskDate;
        public String ngoId;
        public int matchScore;
        public String matchLabel;
        public String matchReason;

        TaskMatchResult(String taskId, String title, String location,
                        String category, String urgency, String skill, String date, String ngoId) {
            this.taskId       = taskId;
            this.taskTitle    = title;
            this.taskLocation = location;
            this.taskCategory = category;
            this.taskUrgency  = urgency;
            this.taskSkill    = skill;
            this.taskDate     = date;
            this.ngoId        = ngoId;
        }
    }

    // ── Callback interfaces ──
    public interface VolunteerMatchCallback {
        void onResult(List<MatchResult> results);
        void onError(String error);
    }

    public interface TaskMatchCallback {
        void onResult(List<TaskMatchResult> results);
        void onError(String error);
    }

    // ═══════════════════════════════════════
    // FIREBASE — NGO: Fetch volunteers + match
    // ═══════════════════════════════════════

    public static void fetchAndMatchVolunteers(
            String taskSkill,
            String taskLocation,
            String taskUrgency,
            VolunteerMatchCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("volunteer_users").get()
                .addOnSuccessListener(snap -> {
                    List<MatchResult> volunteers = new ArrayList<>();

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name  = doc.getString("name");
                        String city  = doc.getString("city");
                        String avail = doc.getString("availableDays");

                        // Build skills string from boolean fields
                        StringBuilder skills = new StringBuilder();
                        String[] skillKeys   = {"teaching","medical","food",
                                "event","fundraising","technical","socialMedia"};
                        String[] skillLabels = {"Teaching","Medical Help",
                                "Food Distribution","Event Management",
                                "Fundraising","Technical","Social Media"};
                        for (int i = 0; i < skillKeys.length; i++) {
                            Boolean val = doc.getBoolean(skillKeys[i]);
                            if (Boolean.TRUE.equals(val)) {
                                if (skills.length() > 0) skills.append(", ");
                                skills.append(skillLabels[i]);
                            }
                        }

                        // Experience: map spinner value to number
                        String expStr = doc.getString("experience");
                        int exp = 0;
                        if (expStr != null) {
                            if (expStr.contains("1+"))        exp = 2;
                            else if (expStr.contains("less")) exp = 1;
                        }

                        volunteers.add(new MatchResult(
                                doc.getId(),
                                name  != null ? name  : "Unknown",
                                city  != null ? city  : "",
                                skills.toString(),
                                avail != null ? avail : "",
                                exp
                        ));
                    }

                    // Run matching algorithm
                    List<MatchResult> matched = matchVolunteersForTask(
                            taskSkill, taskLocation, "Weekends", taskUrgency, volunteers);
                    callback.onResult(matched);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ═══════════════════════════════════════
    // FIREBASE — Volunteer: Fetch tasks + match
    // ═══════════════════════════════════════

    public static void fetchAndMatchTasks(
            String volunteerId,
            TaskMatchCallback callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First get volunteer's own profile
        db.collection("volunteer_users").document(volunteerId).get()
                .addOnSuccessListener(volDoc -> {
                    if (!volDoc.exists()) {
                        callback.onError("Volunteer profile not found");
                        return;
                    }

                    String volCity  = volDoc.getString("city");
                    String volAvail = volDoc.getString("availableDays");

                    // Build volunteer skills string
                    StringBuilder skills = new StringBuilder();
                    String[] skillKeys   = {"teaching","medical","food",
                            "event","fundraising","technical","socialMedia"};
                    String[] skillLabels = {"Teaching","Medical Help",
                            "Food Distribution","Event Management",
                            "Fundraising","Technical","Social Media"};
                    for (int i = 0; i < skillKeys.length; i++) {
                        Boolean val = volDoc.getBoolean(skillKeys[i]);
                        if (Boolean.TRUE.equals(val)) {
                            if (skills.length() > 0) skills.append(", ");
                            skills.append(skillLabels[i]);
                        }
                    }

                    String volSkills = skills.toString();

                    // Now fetch open tasks
                    db.collection("tasks")
                            .whereEqualTo("status", "Open")
                            .get()
                            .addOnSuccessListener(taskSnap -> {
                                List<TaskMatchResult> tasks = new ArrayList<>();

                                for (DocumentSnapshot doc : taskSnap.getDocuments()) {
                                    tasks.add(new TaskMatchResult(
                                            doc.getId(),
                                            doc.getString("title"),
                                            doc.getString("location"),
                                            doc.getString("category"),
                                            doc.getString("urgency"),
                                            doc.getString("skills"),
                                            doc.getString("date"),
                                            doc.getString("ngoId")
                                    ));
                                }

                                // Run matching
                                List<TaskMatchResult> matched = matchTasksForVolunteer(
                                        volSkills,
                                        volCity  != null ? volCity  : "",
                                        volAvail != null ? volAvail : "",
                                        tasks);
                                callback.onResult(matched);
                            })
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ═══════════════════════════════════════
    // MATCHING ALGORITHMS (unchanged logic)
    // ═══════════════════════════════════════

    public static List<MatchResult> matchVolunteersForTask(
            String taskSkill, String taskLocation,
            String taskAvailability, String taskUrgency,
            List<MatchResult> allVolunteers) {

        for (MatchResult vol : allVolunteers) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            int skillScore = calculateSkillMatch(taskSkill, vol.skills);
            score += skillScore;
            if (skillScore >= 35)      reasons.add("✅ Skills match perfectly");
            else if (skillScore >= 20) reasons.add("🟡 Skills partially match");

            int locationScore = calculateLocationMatch(taskLocation, vol.city);
            score += locationScore;
            if (locationScore >= 25)      reasons.add("📍 Same city");
            else if (locationScore >= 15) reasons.add("📍 Nearby location");

            int availScore = calculateAvailabilityMatch(taskAvailability, vol.availability);
            score += availScore;
            if (availScore >= 15) reasons.add("⏰ Available on required days");

            int expScore = Math.min(vol.experience * 3, 10);
            score += expScore;
            if (expScore > 0) reasons.add("💼 Has experience");

            vol.matchScore  = Math.min(score, 100);
            vol.matchReason = String.join(" • ", reasons);
            vol.matchLabel  = getMatchLabel(vol.matchScore);
        }

        Collections.sort(allVolunteers, (a, b) -> b.matchScore - a.matchScore);
        return allVolunteers;
    }

    public static List<MatchResult> matchVolunteersWithGPS(
            String taskSkill, String taskUrgency,
            List<MatchResult> volunteers) {

        for (MatchResult vol : volunteers) {
            if ("Too Far".equals(vol.matchLabel)) continue;
            int score = 0;
            List<String> reasons = new ArrayList<>();

            int skillScore = calculateSkillMatch(taskSkill, vol.skills);
            score += skillScore;
            if (skillScore >= 35)      reasons.add("✅ Skills match perfectly");
            else if (skillScore >= 20) reasons.add("🟡 Skills partially match");

            score += vol.locationScore;
            if (vol.locationScore >= 25)      reasons.add("📍 Very nearby (" + vol.distanceText + ")");
            else if (vol.locationScore >= 15) reasons.add("📍 " + vol.distanceText);
            else if (vol.locationScore > 0)   reasons.add("📍 Far (" + vol.distanceText + ")");

            score += 15;
            reasons.add("⏰ Available");

            int expScore = Math.min(vol.experience * 3, 10);
            score += expScore;
            if (expScore > 0) reasons.add("💼 Has experience");

            vol.matchScore  = Math.min(score, 100);
            vol.matchReason = String.join(" • ", reasons);
            vol.matchLabel  = getMatchLabel(vol.matchScore);
        }

        Collections.sort(volunteers, (a, b) -> {
            if ("Too Far".equals(a.matchLabel) && !"Too Far".equals(b.matchLabel)) return 1;
            if (!"Too Far".equals(a.matchLabel) && "Too Far".equals(b.matchLabel)) return -1;
            return b.matchScore - a.matchScore;
        });
        return volunteers;
    }

    public static List<TaskMatchResult> matchTasksForVolunteer(
            String volSkills, String volCity,
            String volAvailability, List<TaskMatchResult> allTasks) {

        for (TaskMatchResult task : allTasks) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            int skillScore = calculateSkillMatch(task.taskSkill, volSkills);
            score += skillScore;
            if (skillScore >= 35)      reasons.add("✅ Matches your skills");
            else if (skillScore >= 20) reasons.add("🟡 Partially matches");

            int locationScore = calculateLocationMatch(task.taskLocation, volCity);
            score += locationScore;
            if (locationScore >= 25)      reasons.add("📍 Near your location");
            else if (locationScore >= 15) reasons.add("📍 Reachable");

            int availScore = calculateAvailabilityMatch(task.taskUrgency, volAvailability);
            score += availScore;
            if (availScore >= 15) reasons.add("⏰ Fits your schedule");

            if (task.taskUrgency != null && task.taskUrgency.contains("Critical")) {
                score += 10;
                reasons.add("🔴 Urgent need!");
            }

            task.matchScore  = Math.min(score, 100);
            task.matchReason = String.join(" • ", reasons);
            task.matchLabel  = getMatchLabel(task.matchScore);
        }

        Collections.sort(allTasks, (a, b) -> b.matchScore - a.matchScore);
        return allTasks;
    }

    // ═══════════════════════════════════════
    // HELPERS (unchanged)
    // ═══════════════════════════════════════

    static int calculateSkillMatch(String required, String available) {
        if (required == null || available == null) return 0;
        if (required.equalsIgnoreCase("Any Skill")) return 30;
        String req = required.toLowerCase();
        String avl = available.toLowerCase();
        if (avl.contains(req)) return 40;
        String[] reqWords = req.split("[,\\s]+");
        String[] avlWords = avl.split("[,\\s]+");
        int matched = 0;
        for (String rw : reqWords)
            for (String aw : avlWords)
                if (rw.length() > 3 && aw.contains(rw)) matched++;
        if (matched > 0) return Math.min(matched * 15, 35);
        Map<String, String[]> categoryMap = new HashMap<>();
        categoryMap.put("medical",  new String[]{"medical","health","doctor","nurse"});
        categoryMap.put("teaching", new String[]{"teaching","tutoring","education"});
        categoryMap.put("food",     new String[]{"food","cooking","distribution"});
        categoryMap.put("technical",new String[]{"technical","app","web","coding"});
        categoryMap.put("social",   new String[]{"social media","marketing","content"});
        categoryMap.put("event",    new String[]{"event","management","organize"});
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
        return 15;
    }

    static int calculateAvailabilityMatch(String required, String available) {
        if (required == null || available == null) return 10;
        String req = required.toLowerCase();
        String avl = available.toLowerCase();
        if (avl.contains("both"))                                return 20;
        if (req.contains("weekend") && avl.contains("weekend")) return 20;
        if (req.contains("weekday") && avl.contains("weekday")) return 20;
        if (req.contains("critical") && avl.contains("both"))   return 20;
        if (req.contains("critical") && avl.contains("weekday"))return 15;
        return 10;
    }

    public static double calculateGPSDistance(
            double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static int getLocationScoreFromDistance(double distanceKm) {
        if (distanceKm <= 10)  return 30;
        if (distanceKm <= 30)  return 25;
        if (distanceKm <= 60)  return 20;
        if (distanceKm <= 100) return 15;
        return 5;
    }

    static String getMatchLabel(int score) {
        if (score >= 75) return "Excellent Match";
        if (score >= 50) return "Good Match";
        if (score >= 30) return "Fair Match";
        return "Low Match";
    }
}