package com.example.smartseva;

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

            Toast.makeText(this, "Task Created: " + title, Toast.LENGTH_SHORT).show();
        });
    }
}