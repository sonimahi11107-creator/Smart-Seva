package com.example.smartseva;

import java.util.*;

public class TaskStatusManager {

    // ── Status Constants ──
    public static final String STATUS_OPEN        = "Open";
    public static final String STATUS_ASSIGNED    = "Assigned";
    public static final String STATUS_IN_PROGRESS = "In Progress";
    public static final String STATUS_RESOLVED    = "Resolved";

    // ── Task Model ──
    public static class TaskItem {
        public String id;
        public String title;
        public String description;
        public String category;
        public String urgency;
        public String location;
        public String date;
        public String skill;
        public int volunteersNeeded;
        public String status;
        public String assignedVolunteer;
        public String assignedVolunteerPhone;
        public long createdAt;
        public int impactScore;

        public TaskItem(String id, String title, String description,
                        String category, String urgency, String location,
                        String date, String skill, int volunteersNeeded) {
            this.id               = id;
            this.title            = title;
            this.description      = description;
            this.category         = category;
            this.urgency          = urgency;
            this.location         = location;
            this.date             = date;
            this.skill            = skill;
            this.volunteersNeeded = volunteersNeeded;
            this.status           = STATUS_OPEN;
            this.assignedVolunteer= "";
            this.createdAt        = System.currentTimeMillis();
            this.impactScore      = calculateImpactScore(urgency, volunteersNeeded);
        }

        int calculateImpactScore(String urgency, int volunteers) {
            int base = volunteers * 10;
            if (urgency.contains("Critical")) return base + 50;
            if (urgency.contains("Moderate")) return base + 25;
            return base;
        }
    }

    // ── Singleton store ──
    private static final List<TaskItem> taskStore = new ArrayList<>();
    // Volunteer's applied/assigned tasks
    private static final List<TaskItem> myTasks   = new ArrayList<>();

    // ── Add Task ──
    public static void addTask(TaskItem task) {
        taskStore.add(0, task); // newest first
    }

    // ── Get All Tasks ──
    public static List<TaskItem> getAllTasks() {
        if (taskStore.isEmpty()) loadSampleTasks();
        return new ArrayList<>(taskStore);
    }

    // ── Get Tasks by Status ──
    public static List<TaskItem> getTasksByStatus(String status) {
        List<TaskItem> result = new ArrayList<>();
        for (TaskItem t : getAllTasks())
            if (t.status.equals(status)) result.add(t);
        return result;
    }

    // ── Update Status ──
    public static String getNextStatus(String current) {
        switch (current) {
            case STATUS_OPEN:        return STATUS_ASSIGNED;
            case STATUS_ASSIGNED:    return STATUS_IN_PROGRESS;
            case STATUS_IN_PROGRESS: return STATUS_RESOLVED;
            default:                 return STATUS_RESOLVED;
        }
    }

    public static void updateStatus(String taskId, String newStatus) {
        for (TaskItem t : taskStore)
            if (t.id.equals(taskId)) { t.status = newStatus; break; }
        // Firebase teammate yahan Firestore update karega
    }

    public static void assignVolunteer(String taskId,
                                       String volunteerName,
                                       String volunteerPhone) {
        for (TaskItem t : taskStore) {
            if (t.id.equals(taskId)) {
                t.assignedVolunteer      = volunteerName;
                t.assignedVolunteerPhone = volunteerPhone;
                t.status                 = STATUS_ASSIGNED;
                // Add to myTasks for volunteer
                myTasks.add(t);
                break;
            }
        }
    }

    // ── My Tasks (Volunteer) ──
    public static List<TaskItem> getMyTasks() {
        if (myTasks.isEmpty()) loadSampleMyTasks();
        return new ArrayList<>(myTasks);
    }

    public static void applyForTask(TaskItem task, String volunteerName) {
        // Check if already applied
        for (TaskItem t : myTasks)
            if (t.id.equals(task.id)) return;
        task.assignedVolunteer = volunteerName;
        myTasks.add(task);
    }

    // ── Status Color ──
    public static int getStatusColor(String status) {
        switch (status) {
            case STATUS_OPEN:        return 0xFF1565C0; // Blue
            case STATUS_ASSIGNED:    return 0xFFF57F17; // Orange
            case STATUS_IN_PROGRESS: return 0xFF6A1B9A; // Purple
            case STATUS_RESOLVED:    return 0xFF2E7D32; // Green
            default:                 return 0xFF888888;
        }
    }

    // ── Status Step Number ──
    public static int getStatusStep(String status) {
        switch (status) {
            case STATUS_OPEN:        return 1;
            case STATUS_ASSIGNED:    return 2;
            case STATUS_IN_PROGRESS: return 3;
            case STATUS_RESOLVED:    return 4;
            default:                 return 1;
        }
    }

    // ── Sample Data ──
    static void loadSampleTasks() {
        taskStore.add(new TaskItem("T001",
                "Food Distribution Drive",
                "Distribute food packets to 150 families in Dharampura area.",
                "Food Distribution", "🔴 Critical (24 hrs)",
                "Raipur, CG", "20/04/2026", "Food Distribution", 10));

        taskStore.add(new TaskItem("T002",
                "Free Medical Camp",
                "Set up medical camp for elderly patients in slum area.",
                "Medical Help", "🟡 Moderate (1 week)",
                "Bilaspur, CG", "25/04/2026", "Medical Help", 5));

        taskStore.add(new TaskItem("T003",
                "Teaching Underprivileged Kids",
                "Provide free tuition to 30 children from low income families.",
                "Education", "🟢 Normal",
                "Raipur, CG", "22/04/2026", "Teaching", 3));

        taskStore.add(new TaskItem("T004",
                "Tree Plantation Drive",
                "Plant 200 trees in the industrial area to reduce pollution.",
                "Environment", "🟢 Normal",
                "Durg, CG", "30/04/2026", "Any Skill", 15));

        // Set some statuses for demo
        taskStore.get(1).status = STATUS_ASSIGNED;
        taskStore.get(1).assignedVolunteer = "Priya Sharma";
        taskStore.get(2).status = STATUS_IN_PROGRESS;
        taskStore.get(2).assignedVolunteer = "Rahul Verma";
    }

    static void loadSampleMyTasks() {
        if (taskStore.isEmpty()) loadSampleTasks();
        // Volunteer ke assigned tasks
        TaskItem t1 = taskStore.get(1);
        TaskItem t2 = taskStore.get(2);
        myTasks.add(t1);
        myTasks.add(t2);
    }

    // ── Stats ──
    public static int countByStatus(String status) {
        int count = 0;
        for (TaskItem t : getAllTasks())
            if (t.status.equals(status)) count++;
        return count;
    }
}