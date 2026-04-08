package com.example.smartseva;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ApplicantsActivity extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_applicants);

        listView = findViewById(R.id.listApplicants);

        // 🔥 Get data from Intent
        ArrayList<String> applicants = getIntent().getStringArrayListExtra("applicants");

        if (applicants == null) {
            applicants = new ArrayList<>();
            applicants.add("No applicants yet");
        }

        // Create and set the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, applicants);
        listView.setAdapter(adapter);

        // Show in ListView
        ArrayList<String> finalApplicants = applicants;
        listView.setOnItemClickListener((parent, view, position, id) -> {

            String selectedName = finalApplicants.get(position);
            
            if (!selectedName.equals("No applicants yet")) {
                Intent intent = new Intent(this, VolunteerProfileActivity.class);
                intent.putExtra("name", selectedName);
                startActivity(intent);
            }
        });
    }
}