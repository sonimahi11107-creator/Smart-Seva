package com.example.smartseva;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.auth.AuthResult;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    FirebaseFirestore db;
    StorageReference storageRef;

    // ── Tab ──
    Button btnTabNGO, btnTabVolunteer;
    LinearLayout layoutNGO, layoutVolunteer;

    // ── NGO Fields ──
    EditText etOrgName, etRegNo, etNGOEmail, etNGOPhone, etNGOAddress,
            etNGOPassword, etNGOConfirmPassword;
    TextView errOrgName, errRegNo, errNGOEmail, errNGOPhone, errNGOAddress,
            errNGOPassword, errNGOConfirmPassword;
    Spinner spinnerNGOType;

    // ── Volunteer Multi-Step ──
    LinearLayout volStep1, volStep2, volStep3;
    TextView step1Indicator, step2Indicator, step3Indicator;

    // Step 1
    EditText etVolFullName, etVolDOB, etVolPhone, etVolEmail, etVolCity, etVolPincode;
    TextView errVolFullName, errVolDOB, errVolPhone, errVolEmail, errVolCity, errVolPincode;
    Spinner spinnerGender, spinnerVolState;
    ImageView imgProfilePhoto;
    Uri selectedProfileUri = null;
    private static final int PICK_PROFILE_REQUEST = 102;

    // Step 2
    CheckBox cbTeaching, cbMedical, cbFood, cbEvent, cbFundraising,
            cbTechnical, cbSocialMedia, cbOtherSkill;
    EditText etOtherSkill;
    TextView errSkills, errCauses;
    Spinner spinnerAvailDays, spinnerAvailTime;
    CheckBox cbEducation, cbEnvironment, cbAnimal, cbWomen, cbHealth, cbDisaster;

    // Step 3
    Spinner spinnerVehicle, spinnerTravel, spinnerExperience, spinnerIDType;
    EditText etLanguages, etExpDescription, etIDNumber, etVolPassword, etVolConfirmPassword;
    TextView errLanguages, errIDNumber, errVolPassword, errVolConfirmPassword;
    CheckBox cbNotifyNearby, cbNotifyUrgent, cbNotifyEvents;

    // Photo
    ImageView imgPassportPhoto;
    TextView errPassportPhoto;
    Uri selectedPhotoUri = null;
    private static final int PICK_IMAGE_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // ── Tab ──
        btnTabNGO       = findViewById(R.id.btnTabNGO);
        btnTabVolunteer = findViewById(R.id.btnTabVolunteer);
        layoutNGO       = findViewById(R.id.layoutNGO);
        layoutVolunteer = findViewById(R.id.layoutVolunteer);

        // ── NGO Views ──
        etOrgName             = findViewById(R.id.etOrgName);
        etRegNo               = findViewById(R.id.etRegNo);
        etNGOEmail            = findViewById(R.id.etNGOEmail);
        etNGOPhone            = findViewById(R.id.etNGOPhone);
        etNGOAddress          = findViewById(R.id.etNGOAddress);
        etNGOPassword         = findViewById(R.id.etNGOPassword);
        etNGOConfirmPassword  = findViewById(R.id.etNGOConfirmPassword);
        errOrgName            = findViewById(R.id.errOrgName);
        errRegNo              = findViewById(R.id.errRegNo);
        errNGOEmail           = findViewById(R.id.errNGOEmail);
        errNGOPhone           = findViewById(R.id.errNGOPhone);
        errNGOAddress         = findViewById(R.id.errNGOAddress);
        errNGOPassword        = findViewById(R.id.errNGOPassword);
        errNGOConfirmPassword = findViewById(R.id.errNGOConfirmPassword);
        spinnerNGOType        = findViewById(R.id.spinnerNGOType);

        // ── Volunteer Step Views ──
        volStep1       = findViewById(R.id.volStep1);
        volStep2       = findViewById(R.id.volStep2);
        volStep3       = findViewById(R.id.volStep3);
        step1Indicator = findViewById(R.id.step1Indicator);
        step2Indicator = findViewById(R.id.step2Indicator);
        step3Indicator = findViewById(R.id.step3Indicator);

        // Step 1
        etVolFullName  = findViewById(R.id.etVolFullName);
        etVolDOB       = findViewById(R.id.etVolDOB);
        etVolPhone     = findViewById(R.id.etVolPhone);
        etVolEmail     = findViewById(R.id.etVolEmail);
        etVolCity      = findViewById(R.id.etVolCity);
        etVolPincode   = findViewById(R.id.etVolPincode);
        errVolFullName = findViewById(R.id.errVolFullName);
        errVolDOB      = findViewById(R.id.errVolDOB);
        errVolPhone    = findViewById(R.id.errVolPhone);
        errVolEmail    = findViewById(R.id.errVolEmail);
        errVolCity     = findViewById(R.id.errVolCity);
        errVolPincode  = findViewById(R.id.errVolPincode);
        spinnerGender  = findViewById(R.id.spinnerGender);
        spinnerVolState= findViewById(R.id.spinnerVolState);

        // Step 2
        cbTeaching    = findViewById(R.id.cbTeaching);
        cbMedical     = findViewById(R.id.cbMedical);
        cbFood        = findViewById(R.id.cbFood);
        cbEvent       = findViewById(R.id.cbEvent);
        cbFundraising = findViewById(R.id.cbFundraising);
        cbTechnical   = findViewById(R.id.cbTechnical);
        cbSocialMedia = findViewById(R.id.cbSocialMedia);
        cbOtherSkill  = findViewById(R.id.cbOtherSkill);
        etOtherSkill  = findViewById(R.id.etOtherSkill);
        errSkills     = findViewById(R.id.errSkills);
        errCauses     = findViewById(R.id.errCauses);
        spinnerAvailDays = findViewById(R.id.spinnerAvailDays);
        spinnerAvailTime = findViewById(R.id.spinnerAvailTime);
        cbEducation   = findViewById(R.id.cbEducation);
        cbEnvironment = findViewById(R.id.cbEnvironment);
        cbAnimal      = findViewById(R.id.cbAnimal);
        cbWomen       = findViewById(R.id.cbWomen);
        cbHealth      = findViewById(R.id.cbHealth);
        cbDisaster    = findViewById(R.id.cbDisaster);

        // Step 3
        spinnerVehicle       = findViewById(R.id.spinnerVehicle);
        spinnerTravel        = findViewById(R.id.spinnerTravel);
        spinnerExperience    = findViewById(R.id.spinnerExperience);
        spinnerIDType        = findViewById(R.id.spinnerIDType);
        etLanguages          = findViewById(R.id.etLanguages);
        etExpDescription     = findViewById(R.id.etExpDescription);
        etIDNumber           = findViewById(R.id.etIDNumber);
        etVolPassword        = findViewById(R.id.etVolPassword);
        etVolConfirmPassword = findViewById(R.id.etVolConfirmPassword);
        errLanguages         = findViewById(R.id.errLanguages);
        errIDNumber          = findViewById(R.id.errIDNumber);
        errVolPassword       = findViewById(R.id.errVolPassword);
        errVolConfirmPassword= findViewById(R.id.errVolConfirmPassword);
        cbNotifyNearby       = findViewById(R.id.cbNotifyNearby);
        cbNotifyUrgent       = findViewById(R.id.cbNotifyUrgent);
        cbNotifyEvents       = findViewById(R.id.cbNotifyEvents);

        // ── Spinners ──
        spinnerNGOType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select type","Trust","Society","Section 8 Company","Charitable Organisation","Other"}));

        setupVolunteerSpinners();

        // ── Listeners ──
        btnTabNGO.setOnClickListener(v -> showNGO());
        btnTabVolunteer.setOnClickListener(v -> showVolunteer());

        findViewById(R.id.btnRegisterNGO).setOnClickListener(v -> validateAndRegisterNGO());

        etVolDOB.setOnClickListener(v -> showDatePicker());

        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);
        findViewById(R.id.btnUploadProfilePhoto).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_PROFILE_REQUEST);
        });

        cbOtherSkill.setOnCheckedChangeListener((btn, checked) ->
                etOtherSkill.setVisibility(checked ? View.VISIBLE : View.GONE));

        spinnerExperience.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                etExpDescription.setVisibility(pos >= 2 ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });

        findViewById(R.id.btnStep1Next).setOnClickListener(v -> validateStep1());
        findViewById(R.id.btnStep2Back).setOnClickListener(v -> goToStep(1));
        findViewById(R.id.btnStep2Next).setOnClickListener(v -> validateStep2());
        findViewById(R.id.btnStep3Back).setOnClickListener(v -> goToStep(2));
        findViewById(R.id.btnRegisterVolunteer).setOnClickListener(v -> validateStep3());
    }

    // ═══════════════════════════════════════
    // TAB SWITCHING
    // ═══════════════════════════════════════

    void showNGO() {
        layoutNGO.setVisibility(View.VISIBLE);
        layoutVolunteer.setVisibility(View.GONE);
        btnTabNGO.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        btnTabNGO.setTextColor(Color.WHITE);
        btnTabVolunteer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        btnTabVolunteer.setTextColor(Color.parseColor("#1A1A1A"));
    }

    void showVolunteer() {
        layoutNGO.setVisibility(View.GONE);
        layoutVolunteer.setVisibility(View.VISIBLE);
        btnTabVolunteer.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1A1A1A")));
        btnTabVolunteer.setTextColor(Color.WHITE);
        btnTabNGO.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
        btnTabNGO.setTextColor(Color.parseColor("#1A1A1A"));
    }

    // ═══════════════════════════════════════
    // SPINNER SETUP
    // ═══════════════════════════════════════

    void setupVolunteerSpinners() {
        spinnerGender.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Gender","Male","Female","Other","Prefer not to say"}));

        String[] states = {"Select State","Andhra Pradesh","Arunachal Pradesh","Assam","Bihar",
                "Chhattisgarh","Goa","Gujarat","Haryana","Himachal Pradesh","Jharkhand","Karnataka",
                "Kerala","Madhya Pradesh","Maharashtra","Manipur","Meghalaya","Mizoram","Nagaland",
                "Odisha","Punjab","Rajasthan","Sikkim","Tamil Nadu","Telangana","Tripura",
                "Uttar Pradesh","Uttarakhand","West Bengal","Delhi","Other"};
        spinnerVolState.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, states));

        spinnerAvailDays.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Days","Weekdays","Weekends","Both"}));

        spinnerAvailTime.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select Time","Full-time","Part-time","Few hours a week"}));

        spinnerVehicle.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select","Yes","No"}));

        spinnerTravel.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select","Yes","No"}));

        spinnerExperience.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select","No experience","Yes (less than 1 year)","Yes (1+ years)"}));

        spinnerIDType.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Select ID","Aadhaar Card","PAN Card","Passport","Voter ID","Driving Licence"}));
    }

    // ═══════════════════════════════════════
    // DATE PICKER
    // ═══════════════════════════════════════

    void showDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            java.util.Calendar dob = java.util.Calendar.getInstance();
            dob.set(year, month, day);
            java.util.Calendar today = java.util.Calendar.getInstance();
            int age = today.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR);
            if (today.get(java.util.Calendar.DAY_OF_YEAR) < dob.get(java.util.Calendar.DAY_OF_YEAR)) age--;
            if (age < 18) {
                errVolDOB.setText("You must be at least 18 years old");
            } else {
                etVolDOB.setText(String.format("%02d/%02d/%04d", day, month + 1, year));
                errVolDOB.setText("");
            }
        }, cal.get(java.util.Calendar.YEAR) - 18,
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    // ═══════════════════════════════════════
    // STEP NAVIGATION
    // ═══════════════════════════════════════

    void goToStep(int step) {
        volStep1.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        volStep2.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        volStep3.setVisibility(step == 3 ? View.VISIBLE : View.GONE);
        step1Indicator.setBackgroundResource(step == 1 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);
        step2Indicator.setBackgroundResource(step == 2 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);
        step3Indicator.setBackgroundResource(step == 3 ? R.drawable.step_active_bg : R.drawable.step_inactive_bg);
        step1Indicator.setTextColor(step == 1 ? Color.WHITE : Color.parseColor("#AAAAAA"));
        step2Indicator.setTextColor(step == 2 ? Color.WHITE : Color.parseColor("#AAAAAA"));
        step3Indicator.setTextColor(step == 3 ? Color.WHITE : Color.parseColor("#AAAAAA"));
    }

    // ═══════════════════════════════════════
    // VALIDATION HELPERS
    // ═══════════════════════════════════════

    boolean checkEmpty(EditText et, TextView err, String msg) {
        if (et.getText().toString().trim().isEmpty()) { err.setText(msg); return false; }
        err.setText(""); return true;
    }

    boolean checkEmail(EditText et, TextView err) {
        String v = et.getText().toString().trim();
        if (v.isEmpty()) { err.setText("Email is required"); return false; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(v).matches()) {
            err.setText("Enter a valid email address"); return false; }
        err.setText(""); return true;
    }

    boolean checkPhone(EditText et, TextView err) {
        String v = et.getText().toString().trim();
        if (v.length() != 10) { err.setText("Phone must be exactly 10 digits"); return false; }
        if (!v.matches("[6-9][0-9]{9}")) { err.setText("Enter a valid Indian mobile number"); return false; }
        err.setText(""); return true;
    }

    boolean checkPassword(EditText et, TextView err) {
        String v = et.getText().toString();
        if (v.length() < 8) { err.setText("Min 8 characters required"); return false; }
        if (!v.matches(".*\\d.*")) { err.setText("Include at least one number"); return false; }
        err.setText(""); return true;
    }

    boolean checkConfirm(EditText pass, EditText confirm, TextView err) {
        if (!pass.getText().toString().equals(confirm.getText().toString())) {
            err.setText("Passwords do not match"); return false; }
        err.setText(""); return true;
    }

    // ═══════════════════════════════════════
    // NGO VALIDATION
    // ═══════════════════════════════════════

    void validateAndRegisterNGO() {
        boolean ok = true;
        if (!checkEmpty(etOrgName, errOrgName, "Organisation name is required")) ok = false;
        if (!checkEmpty(etRegNo, errRegNo, "Registration number is required")) ok = false;
        if (spinnerNGOType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select NGO type", Toast.LENGTH_SHORT).show(); ok = false; }
        if (!checkEmail(etNGOEmail, errNGOEmail)) ok = false;
        if (!checkPhone(etNGOPhone, errNGOPhone)) ok = false;
        if (!checkEmpty(etNGOAddress, errNGOAddress, "Address is required")) ok = false;
        if (!checkPassword(etNGOPassword, errNGOPassword)) ok = false;
        if (!checkConfirm(etNGOPassword, etNGOConfirmPassword, errNGOConfirmPassword)) ok = false;

        if (ok) {

            String email = etNGOEmail.getText().toString().trim();
            String password = etNGOPassword.getText().toString().trim();

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            String userId = mAuth.getCurrentUser().getUid();

                            Map<String, Object> ngo = new HashMap<>();
                            ngo.put("orgName", etOrgName.getText().toString());
                            ngo.put("email", email);
                            ngo.put("phone", etNGOPhone.getText().toString());
                            ngo.put("type", spinnerNGOType.getSelectedItem().toString());
                            ngo.put("address", etNGOAddress.getText().toString());
                            ngo.put("registrationNumber", etRegNo.getText().toString()); // FIX

                            db.collection("NGOs")
                                    .document(userId)
                                    .set(ngo)
                                    .addOnSuccessListener(unused -> {
                                        showCongratulationsAndProceed(
                                                etOrgName.getText().toString(),
                                                email,
                                                etNGOPhone.getText().toString()
                                        );
                                    })
                                    .addOnFailureListener(e -> {

                                        // 🔥 IMPORTANT FIX
                                        mAuth.getCurrentUser().delete();

                                        Toast.makeText(this,
                                                "Firestore failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(this,
                                    "Auth failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    // ═══════════════════════════════════════
    // VOLUNTEER STEP VALIDATIONS
    // ═══════════════════════════════════════

    void validateStep1() {
        boolean ok = true;
        if (selectedProfileUri == null) {
            Toast.makeText(this, "Please upload your profile photo", Toast.LENGTH_SHORT).show();
            ok = false;
        }
        String name = etVolFullName.getText().toString().trim();
        if (name.isEmpty()) { errVolFullName.setText("Full name is required"); ok = false; }
        else if (!name.matches("[a-zA-Z ]+")) { errVolFullName.setText("Name should contain letters only"); ok = false; }
        else errVolFullName.setText("");

        if (spinnerGender.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show(); ok = false; }

        if (etVolDOB.getText().toString().isEmpty()) { errVolDOB.setText("Date of birth is required"); ok = false; }
        else errVolDOB.setText("");

        if (!checkPhone(etVolPhone, errVolPhone)) ok = false;
        if (!checkEmail(etVolEmail, errVolEmail)) ok = false;

        if (etVolCity.getText().toString().trim().isEmpty()) { errVolCity.setText("City is required"); ok = false; }
        else errVolCity.setText("");

        if (spinnerVolState.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select state", Toast.LENGTH_SHORT).show(); ok = false; }

        String pin = etVolPincode.getText().toString().trim();
        if (pin.length() != 6) { errVolPincode.setText("Enter valid 6-digit pincode"); ok = false; }
        else errVolPincode.setText("");

        if (ok) goToStep(2);
    }

    void validateStep2() {
        boolean ok = true;
        boolean anySkill = cbTeaching.isChecked() || cbMedical.isChecked() ||
                cbFood.isChecked() || cbEvent.isChecked() || cbFundraising.isChecked() ||
                cbTechnical.isChecked() || cbSocialMedia.isChecked() || cbOtherSkill.isChecked();
        if (!anySkill) { errSkills.setText("Please select at least one skill"); ok = false; }
        else errSkills.setText("");

        if (spinnerAvailDays.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select available days", Toast.LENGTH_SHORT).show(); ok = false; }
        if (spinnerAvailTime.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Select time commitment", Toast.LENGTH_SHORT).show(); ok = false; }

        boolean anyCause = cbEducation.isChecked() || cbEnvironment.isChecked() ||
                cbAnimal.isChecked() || cbWomen.isChecked() || cbHealth.isChecked() || cbDisaster.isChecked();
        if (!anyCause) { errCauses.setText("Please select at least one cause"); ok = false; }
        else errCauses.setText("");

        if (ok) goToStep(3);
    }

    void validateStep3() {
        boolean ok = true;
        if (etLanguages.getText().toString().trim().isEmpty()) {
            errLanguages.setText("Please enter languages you know"); ok = false; }
        else errLanguages.setText("");

        if (spinnerIDType.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select ID type", Toast.LENGTH_SHORT).show(); ok = false; }

        if (!checkEmpty(etIDNumber, errIDNumber, "ID number is required")) ok = false;
        if (!checkPassword(etVolPassword, errVolPassword)) ok = false;
        if (!checkConfirm(etVolPassword, etVolConfirmPassword, errVolConfirmPassword)) ok = false;

        if (ok) {

            String name  = etVolFullName.getText().toString().trim();
            String email = etVolEmail.getText().toString().trim();
            String phone = etVolPhone.getText().toString().trim();
            String password = etVolPassword.getText().toString();

            // 🔹 CREATE USER (AUTH)
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {

                            String userId = mAuth.getCurrentUser().getUid();

                            // 🔹 COLLECT ALL DATA
                            Map<String, Object> user = new HashMap<>();

                            // BASIC
                            user.put("name", name);
                            user.put("email", email);
                            user.put("phone", phone);
                            user.put("gender", spinnerGender.getSelectedItem().toString());
                            user.put("dob", etVolDOB.getText().toString());
                            user.put("city", etVolCity.getText().toString());
                            user.put("state", spinnerVolState.getSelectedItem().toString());
                            user.put("pincode", etVolPincode.getText().toString());

                            // SKILLS
                            user.put("teaching", cbTeaching.isChecked());
                            user.put("medical", cbMedical.isChecked());
                            user.put("food", cbFood.isChecked());
                            user.put("event", cbEvent.isChecked());
                            user.put("fundraising", cbFundraising.isChecked());
                            user.put("technical", cbTechnical.isChecked());
                            user.put("socialMedia", cbSocialMedia.isChecked());
                            user.put("otherSkill", etOtherSkill.getText().toString());

                            // AVAILABILITY
                            user.put("availableDays", spinnerAvailDays.getSelectedItem().toString());
                            user.put("availableTime", spinnerAvailTime.getSelectedItem().toString());

                            // CAUSES
                            user.put("education", cbEducation.isChecked());
                            user.put("environment", cbEnvironment.isChecked());
                            user.put("animal", cbAnimal.isChecked());
                            user.put("women", cbWomen.isChecked());
                            user.put("health", cbHealth.isChecked());
                            user.put("disaster", cbDisaster.isChecked());

                            // STEP 3
                            user.put("languages", etLanguages.getText().toString());
                            user.put("vehicle", spinnerVehicle.getSelectedItem().toString());
                            user.put("travel", spinnerTravel.getSelectedItem().toString());
                            user.put("experience", spinnerExperience.getSelectedItem().toString());
                            user.put("idType", spinnerIDType.getSelectedItem().toString());
                            user.put("idNumber", etIDNumber.getText().toString());

                            // 🔹 SAVE TO FIRESTORE
                            db.collection("volunteers")
                                    .document(userId)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {

                                        Toast.makeText(this, "Volunteer Registered!", Toast.LENGTH_SHORT).show();
                                        showCongratulationsAndProceed(name, email, phone);

                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });

                        } else {
                            Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }

                    });
        }
    }

    // ═══════════════════════════════════════
    // PHOTO PICKER
    // ═══════════════════════════════════════

    void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_PROFILE_REQUEST) {
                selectedProfileUri = data.getData();
                imgProfilePhoto.setImageURI(selectedProfileUri);
            } else if (requestCode == PICK_IMAGE_REQUEST) {
                selectedPhotoUri = data.getData();
                if (imgPassportPhoto != null) imgPassportPhoto.setImageURI(selectedPhotoUri);
                if (errPassportPhoto != null) errPassportPhoto.setText("");
            }
        }
    }

    void showCongratulationsAndProceed(String name, String email, String phone) {

        // ── SMS sending ──
        try {
            android.telephony.SmsManager sms = android.telephony.SmsManager.getDefault();
            String smsText = "Congratulations " + name + "! You have successfully registered on Smart Seva. Welcome to the community!";
            sms.sendTextMessage("+91" + phone, null, smsText, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // ── Congratulations Dialog ──
        new android.app.AlertDialog.Builder(this)
                .setTitle("🎉 Registration Successful!")
                .setMessage("Welcome, " + name + "!\n\n" +
                        "✅ A confirmation has been sent to:\n" +
                        "📧 " + email + "\n" +
                        "📱 " + phone + "\n\n" +
                        "Please login to continue.")
                .setCancelable(false)
                .setPositiveButton("Go to Login", (dialog, which) -> {
                    dialog.dismiss();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .show();
    }

}