package com.example.smartseva;

import java.util.ArrayList;
import java.util.List;

public class LocalTaskStore {

    private static LocalTaskStore instance;
    private final List<LocalTask> tasks = new ArrayList<>();

    private LocalTaskStore() {}

    public static LocalTaskStore getInstance() {
        if (instance == null) instance = new LocalTaskStore();
        return instance;
    }

    public void addTask(LocalTask task) {
        tasks.add(0, task); // newest first
    }

    public List<LocalTask> getTasks() {
        return tasks;
    }

    public void clear() {
        tasks.clear();
    }

    // ── Task Model ──────────────────────────────
    public static class LocalTask {
        public String title, description, category,
                urgency, skill, volunteers, location;
        public long createdAt;

        public LocalTask(String title, String description,
                         String category, String urgency,
                         String skill, String volunteers, String location) {
            this.title       = title;
            this.description = description;
            this.category    = category;
            this.urgency     = urgency;
            this.skill       = skill;
            this.volunteers  = volunteers;
            this.location    = location;
            this.createdAt   = System.currentTimeMillis();
        }
    }
}