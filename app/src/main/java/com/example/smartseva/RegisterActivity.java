package com.example.smartseva;

import android.content.Intent;
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
    EditText etAge, etAvailability, etWorkType, etExperience;
    TextView tvGender, tvSkills;
    RadioGroup rgGender;
    RadioButton rbMale, rbFemale, rbOther;
    CheckBox cbMedical, cbRescue, cbFood;

    // NGO
    EditText etRegNumber, etOrgType, etAddress, etCity, etDescription, etWebsite;

    Button btnRegister;
    String role;
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
        etAvailability = findViewById(R.id.etAvailability);
        etWorkType = findViewById(R.id.etWorkType);
        etExperience = findViewById(R.id.etExperience);
        tvGender = findViewById(R.id.tvGender);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);
        tvSkills = findViewById(R.id.tvSkills);
        cbMedical = findViewById(R.id.cbMedical);
        cbRescue = findViewById(R.id.cbRescue);
        cbFood = findViewById(R.id.cbFood);

        // NGO
        etRegNumber = findViewById(R.id.etRegNumber);
        etOrgType = findViewById(R.id.etOrgType);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etDescription = findViewById(R.id.etDescription);
        etWebsite = findViewById(R.id.etWebsite);

        btnRegister = findViewById(R.id.btnRegister);

        // Get role (handle both "role" and "type" keys for safety)
        role = getIntent().getStringExtra("role");
        if (role == null) {
            role = getIntent().getStringExtra("type");
        }

        setupUI();
        setDynamicHints();

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void setupUI() {
        if ("volunteer".equalsIgnoreCase(role)) {
            etAge.setVisibility(View.VISIBLE);
            etAvailability.setVisibility(View.VISIBLE);
            etWorkType.setVisibility(View.VISIBLE);
            etExperience.setVisibility(View.VISIBLE);
            tvSkills.setVisibility(View.VISIBLE);
            cbMedical.setVisibility(View.VISIBLE);
            cbRescue.setVisibility(View.VISIBLE);
            cbFood.setVisibility(View.VISIBLE);
            tvGender.setVisibility(View.VISIBLE);
            rgGender.setVisibility(View.VISIBLE);

            // Hide NGO fields
            etRegNumber.setVisibility(View.GONE);
            etOrgType.setVisibility(View.GONE);
            etAddress.setVisibility(View.GONE);
            etCity.setVisibility(View.GONE);
            etDescription.setVisibility(View.GONE);
            etWebsite.setVisibility(View.GONE);
        } else {
            // Assume NGO
            etAge.setVisibility(View.GONE);
            etAvailability.setVisibility(View.GONE);
            etWorkType.setVisibility(View.GONE);
            etExperience.setVisibility(View.GONE);
            tvSkills.setVisibility(View.GONE);
            cbMedical.setVisibility(View.GONE);
            cbRescue.setVisibility(View.GONE);
            cbFood.setVisibility(View.GONE);
            tvGender.setVisibility(View.GONE);
            rgGender.setVisibility(View.GONE);

            // Show NGO fields
            etRegNumber.setVisibility(View.VISIBLE);
            etOrgType.setVisibility(View.VISIBLE);
            etAddress.setVisibility(View.VISIBLE);
            etCity.setVisibility(View.VISIBLE);
            etDescription.setVisibility(View.VISIBLE);
            etWebsite.setVisibility(View.VISIBLE);
        }
    }

    private void setDynamicHints() {
        if ("NGO".equalsIgnoreCase(role)) {
            etName.setHint(getString(R.string.ngo_name));
        } else {
            etName.setHint(getString(R.string.name));
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("phone", phone);
        user.put("role", role);

        if ("volunteer".equalsIgnoreCase(role)) {
            user.put("age", etAge.getText().toString());
            user.put("availability", etAvailability.getText().toString());
            user.put("workType", etWorkType.getText().toString());
            user.put("experience", etExperience.getText().toString());

            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedGender = findViewById(selectedId);
                user.put("gender", selectedGender.getText().toString());
            }

            StringBuilder skills = new StringBuilder();
            if (cbMedical.isChecked()) skills.append("Medical ");
            if (cbRescue.isChecked()) skills.append("Rescue ");
            if (cbFood.isChecked()) skills.append("Food ");
            user.put("skills", skills.toString().trim());

            db.collection("volunteers")
                    .add(user)
                    .addOnSuccessListener(doc -> finalizeRegistration())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            user.put("regNumber", etRegNumber.getText().toString());
            user.put("orgType", etOrgType.getText().toString());
            user.put("address", etAddress.getText().toString());
            user.put("city", etCity.getText().toString());
            user.put("description", etDescription.getText().toString());
            user.put("website", etWebsite.getText().toString());

            db.collection("ngos")
                    .add(user)
                    .addOnSuccessListener(doc -> finalizeRegistration())
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void finalizeRegistration() {
        Toast.makeText(this, "Registered successfully as " + role, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);
        finish();
    }
}
