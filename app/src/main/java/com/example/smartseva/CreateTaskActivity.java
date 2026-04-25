package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;
import java.util.Map;

public class CreateTaskActivity extends AppCompatActivity {

    EditText etTitle, etDesc, etLocation, etSkills;
    Spinner spinnerCategory, spinnerUrgency;
    Button btnSubmit;
    ProgressBar progressBar;

    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etTitle         = findViewById(R.id.etTaskTitle);
        etDesc          = findViewById(R.id.etDescription);
        etLocation      = findViewById(R.id.etLocation);
        etSkills        = findViewById(R.id.etSkills);
        btnSubmit       = findViewById(R.id.btnSubmitTask);
        progressBar     = findViewById(R.id.progressBar);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUrgency  = findViewById(R.id.spinnerUrgency);

        // Category spinner
        spinnerCategory.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{
                        "Select Category",
                        "Food Distribution",
                        "Medical Help",
                        "Education",
                        "Rescue Work",
                        "Environment",
                        "Event Management",
                        "Other"
                }));

        // Urgency spinner
        spinnerUrgency.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Urgency", "Critical", "Moderate", "Normal"}));

        btnSubmit.setOnClickListener(v -> submitTask());
    }

    void submitTask() {
        String title    = etTitle.getText().toString().trim();
        String desc     = etDesc.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String skills   = etSkills.getText().toString().trim();

        // Validation
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            return;
        }
        if (desc.isEmpty()) {
            etDesc.setError("Description is required");
            return;
        }
        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            return;
        }
        if (spinnerCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spinnerUrgency.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select urgency level", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String ngoId = mAuth.getCurrentUser().getUid();

        // Build task map
        Map<String, Object> task = new HashMap<>();
        task.put("title",       title);
        task.put("description", desc);
        task.put("location",    location);
        task.put("skills",      skills);
        task.put("category",    spinnerCategory.getSelectedItem().toString());
        task.put("urgency",     spinnerUrgency.getSelectedItem().toString());
        task.put("ngoId",       ngoId);
        task.put("status",      "Open");           // Open / Assigned / Completed
        task.put("createdAt",   FieldValue.serverTimestamp());

        // Save to Firestore under "tasks" collection
        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(docRef -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Task created successfully! ✅",
                            Toast.LENGTH_SHORT).show();

                    // Go to Dashboard
                    Intent intent = new Intent(this, DashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this,
                            "Failed to create task: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }
}