package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    Button btnCreateTask;
    String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnCreateTask = findViewById(R.id.btnCreateTask);

        role = getIntent().getStringExtra("role");

        if (role.equals("organization")) {
            btnCreateTask.setVisibility(View.VISIBLE);
        }

        btnCreateTask.setOnClickListener(v -> {
            startActivity(new Intent(this, CreateTaskActivity.class));
        });
    }
}