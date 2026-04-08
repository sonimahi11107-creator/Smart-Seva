package com.example.smartseva;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Task implements Serializable {

    String title, description, location, status;

    // 🔥 name → status
    Map<String, String> applicantStatus;

    // Constructor
    public Task(String title, String description, String location) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.status = "Pending"; // Default status
        this.applicantStatus = new HashMap<>();
    }

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, String> getApplicantStatus() {
        return applicantStatus;
    }
}