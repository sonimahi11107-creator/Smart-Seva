package com.example.smartseva;

public class Task {

    // Variables (data fields)
    String title;
    String description;
    String location;

    // 🔥 Constructor (VERY IMPORTANT)
    public Task(String title, String description, String location) {
        this.title = title;
        this.description = description;
        this.location = location;
    }

    // Getters (data access)
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }
}