package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class CreateTaskActivity extends AppCompatActivity {

    EditText etTitle, etDesc, etLocation, etSkills, etVolunteers;
    Spinner spinnerCategory, spinnerUrgency;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        // ✅ ALL findViewByIds together
        etTitle         = findViewById(R.id.etTaskTitle);
        etDesc          = findViewById(R.id.etDescription);
        etLocation      = findViewById(R.id.etLocation);
        etSkills        = findViewById(R.id.etSkills);
        etVolunteers    = findViewById(R.id.etVolunteers);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUrgency  = findViewById(R.id.spinnerUrgency);
        btnSubmit       = findViewById(R.id.btnSubmitTask);

        // ✅ Spinner setup OUTSIDE button listener
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Education", "Health", "Food", "Environment",
                        "Disaster Relief", "Animal Welfare", "General"});
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        ArrayAdapter<String> urgAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Low", "Moderate", "Critical"});
        urgAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUrgency.setAdapter(urgAdapter);

        // ✅ Button listener only handles submit logic
        btnSubmit.setOnClickListener(v -> {
            String title    = etTitle.getText().toString().trim();
            String desc     = etDesc.getText().toString().trim();
            String location = etLocation.getText().toString().trim();
            String skills   = etSkills.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            LocalTaskStore.LocalTask task = new LocalTaskStore.LocalTask(
                    title,
                    desc,
                    spinnerCategory.getSelectedItem().toString(),
                    spinnerUrgency.getSelectedItem().toString(),
                    skills,
                    etVolunteers.getText().toString().isEmpty()
                            ? "1" : etVolunteers.getText().toString(),
                    location
            );
            LocalTaskStore.getInstance().addTask(task);

            Toast.makeText(this, "Task created! ✅", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CreateTaskActivity.this, DashboardActivity.class);
            intent.putExtra("showTasks", true);
            startActivity(intent);
            finish();
        });
    }
}