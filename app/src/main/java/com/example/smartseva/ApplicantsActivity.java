package com.example.smartseva;

import android.os.Bundle;
import android.widget.ListView;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Map;

public class ApplicantsActivity extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicants);

        //Get full task
        Task task = (Task) getIntent().getSerializableExtra("task");

        ArrayList<String> applicants = new ArrayList<>();
        
        if (task != null && task.getApplicantStatus() != null) {
            applicants.addAll(task.getApplicantStatus().keySet());
        }

        listView = findViewById(R.id.listApplicants);

        if (applicants.isEmpty()) {
            applicants.add("No applicants yet");
        }

        // Create and set the adapter
        ApplicantAdapter adapter = new ApplicantAdapter(this, applicants, task);
        listView.setAdapter(adapter);


        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            
            if (!selectedName.equals("No applicants yet")) {
                Intent intent = new Intent(this, VolunteerProfileActivity.class);
                intent.putExtra("name", selectedName);
                startActivity(intent);
            }
        });

    }
    public void updateTaskStatus(String status) {
        // For demo (later Firebase)
        Toast.makeText(this, "Task Status: " + status, Toast.LENGTH_SHORT).show();
    }
}
