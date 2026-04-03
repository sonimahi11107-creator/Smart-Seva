package com.example.smartseva;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    // COMMON
    EditText etName, etEmail, etPhone, etPassword;

    // VOLUNTEER
    EditText etAge, etAvailability, etWorkType, etExperience;
    TextView tvGender;
    RadioGroup rgGender;
    RadioButton rbMale, rbFemale, rbOther;

    TextView tvSkills;
    CheckBox cbMedical, cbRescue, cbFood;

    // NGO
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

        // Get role
        role = getIntent().getStringExtra("role");

        setupUI();
        setDynamicHints();

        btnRegister.setOnClickListener(v -> registerUser());
    }

    // 🔥 Dynamic UI
    private void setupUI() {

        if ("volunteer".equals(role)) {
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

        } else if ("NGO".equals(role)) {
            etRegNumber.setVisibility(View.VISIBLE);
            etOrgType.setVisibility(View.VISIBLE);
            etAddress.setVisibility(View.VISIBLE);
            etCity.setVisibility(View.VISIBLE);
            etDescription.setVisibility(View.VISIBLE);
            etWebsite.setVisibility(View.VISIBLE);
        }
    }

    // ✅ Fix Name Field
    private void setDynamicHints() {
        if ("NGO".equals(role)) {
            etName.setHint(getString(R.string.ngo_name));
        } else {
            etName.setHint(getString(R.string.name));
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder skills = new StringBuilder();
        if ("volunteer".equals(role)) {
            int selectedId = rgGender.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Select Gender", Toast.LENGTH_SHORT).show();
                return;
            }

            if (cbMedical.isChecked()) skills.append("Medical ");
            if (cbRescue.isChecked()) skills.append("Rescue ");
            if (cbFood.isChecked()) skills.append("Food ");
        }

        String msg = "Registered as " + role;
        if (skills.length() > 0) {
            msg += " with skills: " + skills.toString();
        }
        
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
