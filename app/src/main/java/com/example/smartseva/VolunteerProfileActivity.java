package com.example.smartseva;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class VolunteerProfileActivity extends AppCompatActivity {

    TextView tvName, tvSkills, tvExperience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_profile);

        tvName = findViewById(R.id.tvName);
        tvSkills = findViewById(R.id.tvSkills);
        tvExperience = findViewById(R.id.tvExperience);

        // 🔥 Get data from Intent
        String name = getIntent().getStringExtra("name");

        // Dummy data (later from Firebase)
        tvName.setText(name);
        tvSkills.setText("Skills: Medical, Rescue");
        tvExperience.setText("Experience: 2 years");
    }
}