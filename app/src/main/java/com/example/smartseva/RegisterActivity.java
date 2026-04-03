package com.example.smartseva;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etName, etEmail, etPhone, etPassword;
    EditText etAge, etGender, etSkills, etAvailability, etWorkType, etExperience;
    EditText etRegNumber, etOrgType, etAddress, etCity, etDescription, etWebsite;
    Button btnRegister;

    String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // COMMON
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        // VOLUNTEER
        etAge = findViewById(R.id.etAge);
        etGender = findViewById(R.id.etGender);
        etSkills = findViewById(R.id.etSkills);
        etAvailability = findViewById(R.id.etAvailability);
        etWorkType = findViewById(R.id.etWorkType);
        etExperience = findViewById(R.id.etExperience);

        // NGO
        etRegNumber = findViewById(R.id.etRegNumber);
        etOrgType = findViewById(R.id.etOrgType);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etDescription = findViewById(R.id.etDescription);
        etWebsite = findViewById(R.id.etWebsite);

        btnRegister = findViewById(R.id.btnRegister);

        role = getIntent().getStringExtra("role");

        setupUI();

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void setupUI() {

        if (role.equals("volunteer")) {

            etAge.setVisibility(View.VISIBLE);
            etGender.setVisibility(View.VISIBLE);
            etSkills.setVisibility(View.VISIBLE);
            etAvailability.setVisibility(View.VISIBLE);
            etWorkType.setVisibility(View.VISIBLE);
            etExperience.setVisibility(View.VISIBLE);

        } else {

            etRegNumber.setVisibility(View.VISIBLE);
            etOrgType.setVisibility(View.VISIBLE);
            etAddress.setVisibility(View.VISIBLE);
            etCity.setVisibility(View.VISIBLE);
            etDescription.setVisibility(View.VISIBLE);
            etWebsite.setVisibility(View.VISIBLE);
        }
    }

    private void registerUser() {

        String name = etName.getText().toString();
        String email = etEmail.getText().toString();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Registered as " + role, Toast.LENGTH_SHORT).show();
    }
}