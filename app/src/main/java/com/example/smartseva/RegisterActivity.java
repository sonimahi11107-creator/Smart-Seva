package com.example.smartseva;
<<<<<<< HEAD
import android.content.Intent;
=======

>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

<<<<<<< HEAD
=======
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
public class RegisterActivity extends AppCompatActivity {

    // COMMON
    EditText etName, etEmail, etPhone, etPassword;

    // VOLUNTEER
<<<<<<< HEAD
    EditText etAge, etAvailability, etWorkType, etExperience;
    TextView tvGender;
    RadioGroup rgGender;
    RadioButton rbMale, rbFemale, rbOther;

    TextView tvSkills;
    CheckBox cbMedical, cbRescue, cbFood;
=======
    EditText etAge, etGender, etSkills, etAvailability, etWorkType, etExperience;
>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f

    // NGO
    EditText etRegNumber, etOrgType, etAddress, etCity, etDescription, etWebsite;

    Button btnRegister;
<<<<<<< HEAD

    String role;
=======
    String type;

    FirebaseFirestore db;
>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

<<<<<<< HEAD
=======
        db = FirebaseFirestore.getInstance();

>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
        // COMMON
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);

        // VOLUNTEER
        etAge = findViewById(R.id.etAge);
<<<<<<< HEAD
=======
        etGender = findViewById(R.id.etGender);
        etSkills = findViewById(R.id.etSkills);
>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
        etAvailability = findViewById(R.id.etAvailability);
        etWorkType = findViewById(R.id.etWorkType);
        etExperience = findViewById(R.id.etExperience);

<<<<<<< HEAD
        tvGender = findViewById(R.id.tvGender);
        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);
        rbOther = findViewById(R.id.rbOther);

        tvSkills = findViewById(R.id.tvSkills);
        cbMedical = findViewById(R.id.cbMedical);
        cbRescue = findViewById(R.id.cbRescue);
        cbFood = findViewById(R.id.cbFood);

=======
>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
        // NGO
        etRegNumber = findViewById(R.id.etRegNumber);
        etOrgType = findViewById(R.id.etOrgType);
        etAddress = findViewById(R.id.etAddress);
        etCity = findViewById(R.id.etCity);
        etDescription = findViewById(R.id.etDescription);
        etWebsite = findViewById(R.id.etWebsite);

        btnRegister = findViewById(R.id.btnRegister);

<<<<<<< HEAD
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
=======
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

>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
            etRegNumber.setVisibility(View.VISIBLE);
            etOrgType.setVisibility(View.VISIBLE);
            etAddress.setVisibility(View.VISIBLE);
            etCity.setVisibility(View.VISIBLE);
            etDescription.setVisibility(View.VISIBLE);
            etWebsite.setVisibility(View.VISIBLE);
<<<<<<< HEAD
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

        // 🔥 Move to Dashboard
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);

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
=======

            etAge.setVisibility(View.GONE);
            etGender.setVisibility(View.GONE);
            etSkills.setVisibility(View.GONE);
            etAvailability.setVisibility(View.GONE);
            etWorkType.setVisibility(View.GONE);
            etExperience.setVisibility(View.GONE);
        }
    }
}
>>>>>>> 7ea1fd67c95f2149531da91656258298ff557a8f
