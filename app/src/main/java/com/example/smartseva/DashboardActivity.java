package com.example.smartseva;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class DashboardActivity extends AppCompatActivity {

    Button btnCreateTask;

    RecyclerView recyclerView;
    TaskAdapter adapter;
    List<Task> taskList;
    String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        btnCreateTask = findViewById(R.id.btnCreateTask);

        recyclerView = findViewById(R.id.recyclerTasks);

// Layout manager (vertical list)
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

// List create
        taskList = new ArrayList<>();

// Dummy data
        taskList.add(new Task("Food Distribution", "Help poor people", "Delhi"));
        taskList.add(new Task("Medical Camp", "Assist doctors", "Mumbai"));
        taskList.add(new Task("Teaching Kids", "Teach students", "Jaipur"));

// Adapter set
        adapter = new TaskAdapter(taskList);
        recyclerView.setAdapter(adapter);

        // Get role from intent
        role = getIntent().getStringExtra("role");

        // Safe check for role to avoid NullPointerException
        if ("organization".equals(role) || "NGO".equals(role)) {
            btnCreateTask.setVisibility(View.VISIBLE);
        } else {
            btnCreateTask.setVisibility(View.GONE);
        }

        btnCreateTask.setOnClickListener(v -> 
            startActivity(new Intent(this, CreateTaskActivity.class))
        );
    }
}
