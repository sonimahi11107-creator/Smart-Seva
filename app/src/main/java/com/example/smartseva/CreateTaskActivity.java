package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class CreateTaskActivity extends AppCompatActivity {

    EditText etTitle, etDesc, etLocation, etSkills;
    Button btnSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        etTitle = findViewById(R.id.etTaskTitle);
        etDesc = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etSkills = findViewById(R.id.etSkills);
        btnSubmit = findViewById(R.id.btnSubmitTask);

        btnSubmit.setOnClickListener(v -> {

            String title = etTitle.getText().toString();
            String desc = etDesc.getText().toString();
            String location = etLocation.getText().toString();

            if (title.isEmpty() || desc.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔥 Send data to Dashboard
            Intent intent = new Intent(CreateTaskActivity.this, DashboardActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("desc", desc);
            intent.putExtra("location", location);

            startActivity(intent);
        });
    }
}