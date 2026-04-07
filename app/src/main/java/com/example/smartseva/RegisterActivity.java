package com.example.smartseva;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // COMMON
    EditText etName, etEmail, etPhone, etPassword;

    // VOLUNTEER
    EditText etAge, etGender, etSkills, etAvailability, etWorkType, etExperience;

    // NGO
    EditText etRegNumber, etOrgType, etAddress, etCity, etDescription, etWebsite;

    Button btnRegister;
    String type;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = FirebaseFirestore.getInstance();

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

        type = getIntent().getStringExtra("type");

        setupUI();

        btnRegister.setOnClickListener(v -> {

            String name = etName.getText().toString();
            String email = etEmail.getText().toString();
            String phone = etPhone.getText().toString();

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Fill required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> user = new HashMap<>();

            user.put("name", name);
            user.put("email", email);
            user.put("phone", phone);
            user.put("type", type);

            if ("volunteer".equals(type)) {

                user.put("age", etAge.getText().toString());
                user.put("gender", etGender.getText().toString());
                user.put("skills", etSkills.getText().toString());
                user.put("availability", etAvailability.getText().toString());
                user.put("workType", etWorkType.getText().toString());
                user.put("experience", etExperience.getText().toString());

            } else {

                user.put("regNumber", etRegNumber.getText().toString());
                user.put("orgType", etOrgType.getText().toString());
                user.put("address", etAddress.getText().toString());
                user.put("city", etCity.getText().toString());
                user.put("description", etDescription.getText().toString());
                user.put("website", etWebsite.getText().toString());
            }

            if ("volunteer".equals(type)) {

                db.collection("volunteers")
                        .add(user)
                        .addOnSuccessListener(documentReference ->
                                Toast.makeText(RegisterActivity.this, "Volunteer Registered", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            } else {

                db.collection("ngos")
                        .add(user)
                        .addOnSuccessListener(documentReference ->
                                Toast.makeText(RegisterActivity.this, "NGO Registered", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }


        });
    }

    private void setupUI() {

        if ("volunteer".equals(type)) {

            etAge.setVisibility(View.VISIBLE);
            etGender.setVisibility(View.VISIBLE);
            etSkills.setVisibility(View.VISIBLE);
            etAvailability.setVisibility(View.VISIBLE);
            etWorkType.setVisibility(View.VISIBLE);
            etExperience.setVisibility(View.VISIBLE);

            etRegNumber.setVisibility(View.GONE);
            etOrgType.setVisibility(View.GONE);
            etAddress.setVisibility(View.GONE);
            etCity.setVisibility(View.GONE);
            etDescription.setVisibility(View.GONE);
            etWebsite.setVisibility(View.GONE);

        } else {

            etRegNumber.setVisibility(View.VISIBLE);
            etOrgType.setVisibility(View.VISIBLE);
            etAddress.setVisibility(View.VISIBLE);
            etCity.setVisibility(View.VISIBLE);
            etDescription.setVisibility(View.VISIBLE);
            etWebsite.setVisibility(View.VISIBLE);

            etAge.setVisibility(View.GONE);
            etGender.setVisibility(View.GONE);
            etSkills.setVisibility(View.GONE);
            etAvailability.setVisibility(View.GONE);
            etWorkType.setVisibility(View.GONE);
            etExperience.setVisibility(View.GONE);
        }
    }
}