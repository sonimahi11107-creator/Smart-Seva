package com.example.smartseva;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnVolunteer, btnOrg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnVolunteer = findViewById(R.id.btnVolunteer);
        btnOrg = findViewById(R.id.btnOrg);

        btnVolunteer.setOnClickListener(v -> openRegister("volunteer"));
        btnOrg.setOnClickListener(v -> openRegister("organization"));
    }

    private void openRegister(String role) {
        Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);
    }
}