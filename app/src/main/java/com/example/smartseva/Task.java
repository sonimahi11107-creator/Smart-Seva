package com.example.smartseva;

import com.google.firebase.Timestamp;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Task implements Serializable {

    // Firestore document ID
    private String taskId;

    // Core fields (match exactly what CreateTaskActivity saves)
    private String title;
    private String description;
    private String location;
    private String skills;
    private String category;
    private String urgency;
    private String ngoId;
    private String status;      // "Open" / "Assigned" / "Completed"

    // Applicant tracking: volunteerId → "Pending" / "Accepted" / "Rejected"
    private Map<String, String> applicantStatus;

    // Timestamp (Firestore server timestamp)
    private Timestamp createdAt;

    // ── Empty constructor required by Firestore ──
    public Task() {
        this.applicantStatus = new HashMap<>();
    }

    // ── Full constructor ──
    public Task(String title, String description, String location,
                String skills, String category, String urgency, String ngoId) {
        this.title       = title;
        this.description = description;
        this.location    = location;
        this.skills      = skills;
        this.category    = category;
        this.urgency     = urgency;
        this.ngoId       = ngoId;
        this.status      = "Open";
        this.applicantStatus = new HashMap<>();
    }

    // ── Getters ──
    public String getTaskId()       { return taskId; }
    public String getTitle()        { return title; }
    public String getDescription()  { return description; }
    public String getLocation()     { return location; }
    public String getSkills()       { return skills; }
    public String getCategory()     { return category; }
    public String getUrgency()      { return urgency; }
    public String getNgoId()        { return ngoId; }
    public String getStatus()       { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Map<String, String> getApplicantStatus() { return applicantStatus; }

    // ── Setters ──
    public void setTaskId(String taskId)     { this.taskId = taskId; }
    public void setTitle(String title)       { this.title = title; }
    public void setDescription(String desc)  { this.description = desc; }
    public void setLocation(String location) { this.location = location; }
    public void setSkills(String skills)     { this.skills = skills; }
    public void setCategory(String category) { this.category = category; }
    public void setUrgency(String urgency)   { this.urgency = urgency; }
    public void setNgoId(String ngoId)       { this.ngoId = ngoId; }
    public void setStatus(String status)     { this.status = status; }
    public void setCreatedAt(Timestamp t)    { this.createdAt = t; }
    public void setApplicantStatus(Map<String, String> m) { this.applicantStatus = m; }

    // ── Helper: apply for task ──
    public void applyVolunteer(String volunteerId) {
        if (applicantStatus == null) applicantStatus = new HashMap<>();
        applicantStatus.put(volunteerId, "Pending");
    }

    // ── Helper: urgency color hex ──
    public String getUrgencyColor() {
        if (urgency == null) return "#888888";
        switch (urgency) {
            case "Critical": return "#C62828";
            case "Moderate": return "#F57F17";
            case "Normal":   return "#2E7D32";
            default:         return "#888888";
        }
    }

    // ── Helper: status color hex ──
    public String getStatusColor() {
        if (status == null) return "#888888";
        switch (status) {
            case "Open":      return "#1565C0";
            case "Assigned":  return "#F57F17";
            case "Completed": return "#2E7D32";
            default:          return "#888888";
        }
    }
}