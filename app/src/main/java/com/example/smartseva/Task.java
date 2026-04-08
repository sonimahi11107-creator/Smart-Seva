package com.example.smartseva;

import java.util.ArrayList;
import java.util.List;

public class Task {

    String title, description, location;
    List<String> applicants; // 🔥 store applied users

    // Constructor
    public Task(String title, String description, String location) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.applicants = new ArrayList<>();
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }

    public List<String> getApplicants() {
        return applicants;
    }
}