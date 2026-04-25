package com.example.smartseva;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocalTaskStore {

    private static LocalTaskStore instance;
    private final List<LocalTask> tasks = new ArrayList<>();

    private LocalTaskStore() {}

    public static LocalTaskStore getInstance() {
        if (instance == null) instance = new LocalTaskStore();
        return instance;
    }

    // ✅ Saves to both memory AND Firestore
    public void addTask(LocalTask task) {
        tasks.add(0, task); // memory cache

        // Save to Firestore
        FirebaseFirestore db  = FirebaseFirestore.getInstance();
        FirebaseAuth auth     = FirebaseAuth.getInstance();
        String ngoId          = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("title",       task.title);
        data.put("description", task.description);
        data.put("category",    task.category);
        data.put("urgency",     task.urgency);
        data.put("skills",      task.skill);
        data.put("volunteers",  task.volunteers);
        data.put("location",    task.location);
        data.put("ngoId",       ngoId);
        data.put("status",      "Open");
        data.put("source",      "DataCollection"); // marks it came from AI scan
        data.put("createdAt",   FieldValue.serverTimestamp());

        db.collection("tasks")
                .add(data)
                .addOnSuccessListener(ref -> task.firestoreId = ref.getId())
                .addOnFailureListener(e ->
                        android.util.Log.e("LocalTaskStore",
                                "Firestore save failed: " + e.getMessage()));
    }

    public List<LocalTask> getTasks() { return tasks; }

    public void clear() { tasks.clear(); }

    // ── Task Model ──
    public static class LocalTask {
        public String title, description, category,
                urgency, skill, volunteers, location;
        public String firestoreId; // ✅ set after Firestore save
        public long   createdAt;

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